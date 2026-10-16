package edu.zjnu.agent;

import java.util.Map;

/**
 * 工具抽象：把任意能力（天气、搜索、数据库…）按 OpenAI function calling 的格式暴露给大模型。
 *
 * <p>一个工具必须给出三件事——
 * <ul>
 *   <li>{@link #name()}：模型按名字调用，必须全局唯一</li>
 *   <li>{@link #description()}：让模型判断"什么时候该用它"</li>
 *   <li>{@link #parametersSchema()}：JSON Schema，描述参数结构（type/required/properties…）</li>
 * </ul>
 *
 * <p>{@link #execute(Map)} 接收模型回传的参数（已在 agent 里反序列化成 Map），
 * 返回结果建议用字符串或可 JSON 序列化的简单对象。
 *
 * <p>对应 Python 版 AwesomeAgentPython/openai/weather.py 的函数式写法；
 * 这里抽成接口，方便后续把各种能力（网页搜索、SQL 查询…）通过同一个 registry 注册。
 */
public interface Tool {

    String name();

    String description();

    /**
     * JSON Schema（顶层必须是 {@code {"type":"object","properties":{...},"required":[...]}}）。
     * 用 Map&lt;String,Object&gt; 而不是字符串，给注册器留出"自动转 OpenAI SDK 类型"的余地。
     */
    Map<String, Object> parametersSchema();

    Object execute(Map<String, Object> arguments);
}
