package edu.zjnu.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import edu.zjnu.CallLlm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 教学版 Agent：把三个核心概念拆得清清楚楚。
 *
 * <ol>
 *   <li><b>任务规划</b>：用户目标进来后，先让模型口头拆解步骤（{@link #plan}），
 *       把规划结果作为一次 assistant 消息塞进上下文，后续轮次模型会沿着它走。</li>
 *   <li><b>工具调用</b>：ReAct 风格的循环 —— model → tool → model，直到模型不再请求工具。
 *       每个 tool call 的结果按 {@code tool_call_id} 回填，模型就能多轮对照。</li>
 *   <li><b>上下文管理</b>：累积 messages，超出 {@link #MAX_TOKENS} 时从最早的非系统消息开始丢
 *       （{@link #truncateIfNeeded}），避免越走越长撑爆窗口。</li>
 * </ol>
 *
 * <p>对应 Python 版 AwesomeAgentPython/openai/tool_use.py；
 * 那个版本只有"一次工具调用 + 一次最终回答"，是个 demo；
 * 这个版本升级成"多轮循环 + 规划 + 上下文治理"，便于学习时看清骨架。
 */
public final class Agent {

    /** ponytail: 全局硬阈值；需要按模型名动态取上下文窗口上限时，再换成查表。 */
    private static final int MAX_TOKENS = 6000;
    /** ponytail: 防失控兜底；正常对话 3-5 轮就该结束，8 轮够用。 */
    private static final int MAX_TURNS = 8;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ToolRegistry tools;
    private final Encoding encoding;

    /** 跨 {@code run()} 共享的对话上下文；同一个 Agent 实例多轮对话就靠它。 */
    private final List<ChatCompletionMessageParam> messages = new ArrayList<>();
    /** 只在第一回合注入 plan，避免每问一句都把规划步骤再打一遍。 */
    private boolean firstTurn = true;

    public Agent(ToolRegistry tools) {
        this.tools = tools;
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    /**
     * 跑一个回合：把用户输入接上现有上下文，按需调工具，回一段最终文本。
     * 同一个 Agent 实例上反复调用 {@code run(...)} 就是连续对话 —— 上下文跨回合保留。
     *
     * <p>流程：plan（仅首回合） → 循环 model/tool → 模型不再要工具就退出。
     */
    public String run(String userGoal) {
        ensureInitialized();
        messages.add(userMessage(userGoal));

        // 1) 任务规划：只在第一回合做一次，结果灌进上下文。
        if (firstTurn) {
            String planText = plan(userGoal);
            messages.add(assistantMessage("【规划】\n" + planText));
            firstTurn = false;
        }

        // 2) 主循环：每轮问一次模型，按需调工具，直到模型不再要工具。
        for (int turn = 0; turn < MAX_TURNS; turn++) {
            truncateIfNeeded(messages);

            ChatCompletionMessage msg = callLlm(messages);
            String content = msg.content().orElse("");
            List<ChatCompletionMessageToolCall> toolCalls = msg.toolCalls().orElse(Collections.emptyList());

            // 把 assistant 的回复（content + tool_calls）回写，下一轮才能引用
            messages.add(ChatCompletionMessageParam.ofAssistant(msg.toParam()));

            if (toolCalls.isEmpty()) {
                return content; // 模型不再调工具 → 最终自然语言答案
            }

            // 顺序执行每个 tool call，结果逐条加进 messages
            for (ChatCompletionMessageToolCall call : toolCalls) {
                String result = runToolCall(call);
                messages.add(toolResultMessage(call.asFunction().id(), result));
                log(call, result);
            }
        }
        return "已达最大轮次 " + MAX_TURNS + "，请检查是否陷入循环。";
    }

    // -------- 规划 --------

    /**
     * 让模型先把目标拆成 2~4 个步骤。单独一次调用，temperature=0，
     * 拿到结果后作为 assistant 消息回填 —— 这样后续轮次模型会"知道"现在在做计划的哪一步。
     */
    private String plan(String goal) {
        ChatCompletionCreateParams.Builder b = ChatCompletionCreateParams.builder()
                .messages(Arrays.asList(
                        systemMessage("你是一个规划器。把用户目标拆成 2-4 步，简短描述每一步并编号；不要调用任何工具。"),
                        userMessage(goal)))
                .temperature(0.0);
        String text = CallLlm.chat(b);
        System.out.println("[plan]\n" + text);
        return text;
    }

    // -------- 上下文管理 --------

    /**
     * 总长超 {@link #MAX_TOKENS} 时，从最早的非 system 消息开始丢。
     * ponytail: 按"整体求和再删头"实现，没做精确逐 token 回填；够研究用，
     * 要更稳就改成"边删边检查、删到刚好不超"的两指针版本。
     */
    private void truncateIfNeeded(List<ChatCompletionMessageParam> messages) {
        int total = messages.stream().mapToInt(m -> encoding.countTokens(stringify(m))).sum();
        if (total <= MAX_TOKENS) return;

        int dropped = 0;
        // 跳过 [0] 的 system；保留最后一条 user/assistant 不丢
        while (messages.size() > 2 && total > MAX_TOKENS) {
            ChatCompletionMessageParam first = messages.get(1);
            if (first.system().isPresent()) break; // 防御：理论上不该到这里
            int removed = encoding.countTokens(stringify(first));
            messages.remove(1);
            total -= removed;
            dropped++;
        }
        if (dropped > 0) {
            System.out.println("[context] 总长 " + total + " tokens，丢弃最早 " + dropped + " 条非系统消息");
        }
    }

    /** 把消息粗略转成可计 token 的字符串（仅用于截断判断，不要求严格等于模型看到的字节） */
    private static String stringify(ChatCompletionMessageParam m) {
        if (m.system().isPresent())    return m.system().get().content().toString();
        if (m.user().isPresent())      return m.user().get().content().toString();
        if (m.assistant().isPresent()) return m.assistant().get().content().map(Object::toString).orElse("");
        if (m.tool().isPresent())      return m.tool().get().content().toString();
        return "";
    }

    // -------- 工具执行 --------

    private String runToolCall(ChatCompletionMessageToolCall call) {
        try {
            Tool tool = tools.get(call.asFunction().function().name());
            Map<String, Object> args = parseArgs(call.asFunction().function().arguments());
            Object result = tool.execute(args);
            return String.valueOf(result);
        } catch (Exception e) {
            return "工具调用失败：" + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** 模型回传的 arguments 是 JSON 字符串，信任它（来自模型），出错就当工具失败回填。 */
    private static Map<String, Object> parseArgs(String arguments) {
        try {
            return MAPPER.readValue(arguments, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析工具参数：" + arguments, e);
        }
    }

    private ChatCompletionMessage callLlm(List<ChatCompletionMessageParam> messages) {
        ChatCompletionCreateParams.Builder b = ChatCompletionCreateParams.builder()
                .messages(messages)
                .tools(tools.toOpenAiTools());
        return CallLlm.createCompletion(b).choices().get(0).message();
    }

    // -------- 消息构造辅助 --------

    /** system 提示只注入一次，避免每回合重复；幂等。 */
    private void ensureInitialized() {
        if (messages.isEmpty()) {
            messages.add(systemMessage("你是一个会规划的助理。先在脑中规划步骤，再调用工具，最后用自然语言总结。"));
        }
    }

    private static ChatCompletionMessageParam systemMessage(String content) {
        return ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(content).build());
    }

    private static ChatCompletionMessageParam userMessage(String content) {
        return ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(content).build());
    }

    private static ChatCompletionMessageParam assistantMessage(String content) {
        return ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder().content(content).build());
    }

    private static ChatCompletionMessageParam toolResultMessage(String toolCallId, String content) {
        return ChatCompletionMessageParam.ofTool(
                ChatCompletionToolMessageParam.builder()
                        .toolCallId(toolCallId)
                        .content(content)
                        .build());
    }

    private static void log(ChatCompletionMessageToolCall call, Object result) {
        ChatCompletionMessageFunctionToolCall.Function fn = call.asFunction().function();
        System.out.println("[tool] " + fn.name() + "(" + fn.arguments() + ") -> " + result);
    }

    // -------- 入口演示（交互 CLI） --------

    public static void main(String[] args) {
        Agent agent = new Agent(new ToolRegistry().register(new WeatherTool()));

        System.out.println("Agent 已就绪。输入问题开始对话，输入 exit / quit 或 Ctrl-D 退出。");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n> ");
                if (!scanner.hasNextLine()) break; // EOF (Ctrl-D)
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;
                if ("exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) break;
                System.out.println(agent.run(input));
            }
        }
        System.out.println("\nbye.");
    }
}
