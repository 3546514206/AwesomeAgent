package edu.zjnu.agent;

import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Agent 上下文管理的关键不变量测试 —— 这是分支循环里的核心逻辑，不能单靠 demo 通过就算对。
 *
 * <p>不用发请求：直接构造 SDK 消息对象，调私有 {@code truncateIfNeeded}。
 * {@code MAX_TOKENS} 是 {@code private static final int}，javac 会把它内联成常量，
 * 所以走的是真实编译进去的 6000 阈值（测试用足够长的字符串撑爆它）。
 */
public class AgentContextTest {

    private static ChatCompletionMessageParam sysMsg(String content) {
        return ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(content).build());
    }

    private static ChatCompletionMessageParam usrMsg(String content) {
        return ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(content).build());
    }

    private static ChatCompletionMessageParam astMsg(String content) {
        return ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder().content(content).build());
    }

    private static void invokeTruncate(Agent agent, List<ChatCompletionMessageParam> messages) throws Exception {
        Method m = Agent.class.getDeclaredMethod("truncateIfNeeded", List.class);
        m.setAccessible(true);
        m.invoke(agent, messages);
    }

    @Test
    public void underBudget_keepsEverything() throws Exception {
        Agent agent = new Agent(new ToolRegistry());
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(sysMsg("你是助手"));
        messages.add(usrMsg("今天天气？"));

        invokeTruncate(agent, messages);

        assertEquals(2, messages.size());
    }

    @Test
    public void overBudget_dropsOldestNonSystemKeepsSystemAndLastUser() throws Exception {
        // Agent.MAX_TOKENS = 6000（compile-time 内联）；准备 4 条共 ~12000 tokens 撑爆它
        Agent agent = new Agent(new ToolRegistry());
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(sysMsg("sys"));
        messages.add(usrMsg(bigString(3000)));
        messages.add(astMsg(bigString(3000)));
        messages.add(usrMsg(bigString(3000)));

        invokeTruncate(agent, messages);

        assertTrue("system 必须保留", messages.get(0).system().isPresent());
        assertTrue("最后一条必须是最新 user，不能丢",
                messages.get(messages.size() - 1).user().isPresent());
        assertTrue("丢完了 system + 末 user 应只剩 2 条，actual=" + messages.size(),
                messages.size() == 2);
    }

    /**
     * CL100K_BASE 对重复 'a' 的压缩比约 1 token / 8 字符；对中文 1 char ≈ 1 token。
     * 这里用中文按 1:1 算，方便精确控制 token 数。
     */
    private static String bigString(int targetTokens) {
        StringBuilder sb = new StringBuilder(targetTokens);
        for (int i = 0; i < targetTokens; i++) sb.append('中');
        return sb.toString();
    }
}
