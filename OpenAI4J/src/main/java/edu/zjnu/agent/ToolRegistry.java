package edu.zjnu.agent;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionTool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具登记册：
 * <ul>
 *   <li>agent 这边按 {@link Tool} 接口操作（执行 / 按名查找）</li>
 *   <li>请求大模型时统一转成 SDK 的 {@link ChatCompletionTool}（function calling schema）</li>
 * </ul>
 *
 * <p>这种"自家抽象 ↔ SDK 适配层"的拆分，避免每个 Tool 实现都直接依赖 openai-java 的类型。
 * 加工具：写一个 {@code Tool} 实现 + 一行 {@code registry.register(...)}，完事。
 */
public final class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry register(Tool tool) {
        if (tools.put(tool.name(), tool) != null) {
            throw new IllegalStateException("工具名重复：" + tool.name());
        }
        return this;
    }

    public Tool get(String name) {
        Tool t = tools.get(name);
        if (t == null) {
            throw new IllegalArgumentException("未登记的工具：" + name);
        }
        return t;
    }

    /** 把所有登记的工具转成 SDK 的 ChatCompletionTool 列表（喂给请求构造器） */
    public List<ChatCompletionTool> toOpenAiTools() {
        return tools.values().stream().map(this::toOpenAiTool).collect(Collectors.toList());
    }

    private ChatCompletionTool toOpenAiTool(Tool tool) {
        FunctionDefinition def = FunctionDefinition.builder()
                .name(tool.name())
                .description(tool.description())
                .parameters(toFunctionParameters(tool.parametersSchema()))
                .build();
        return ChatCompletionTool.ofFunction(ChatCompletionFunctionTool.builder()
                .function(def)
                .build());
    }

    private FunctionParameters toFunctionParameters(Map<String, Object> schema) {
        // Map<String, Object> -> Map<String, JsonValue>：openai-java 内部走 Jackson 反射，
        // 这里省一层手写 JSON 字符串 —— openai-java 已经把 Jackson 拉进了 classpath。
        Map<String, JsonValue> json = new LinkedHashMap<>();
        schema.forEach((k, v) -> json.put(k, JsonValue.from(v)));
        return FunctionParameters.builder().additionalProperties(json).build();
    }
}
