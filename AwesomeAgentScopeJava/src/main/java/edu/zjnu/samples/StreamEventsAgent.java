package edu.zjnu.samples;


import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;

/**
 * StreamEventsAgent
 *
 * @Date 2026-06-21 23:51
 * @Author 杨海波
 **/
public class StreamEventsAgent {

    public static void main(String[] args) {
        OpenAIChatModel customModel = OpenAIChatModel.builder()
                .apiKey(System.getProperty("openai.key"))
                .baseUrl("https://open.bigmodel.cn/api/paas/v4/")
                .modelName("glm-5.2")
                .build();

        HarnessAgent agent = HarnessAgent.builder()
                .name("note-taker")
                .sysPrompt("你是一个帮助用户做笔记的助手!")
                .model(customModel)
                .build();

        agent.streamEvents(new UserMessage("帮我把今天的关键点列三条。"))
                .doOnNext(event -> {
                    if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                        // 模型返回的流式文本片段 —— 追加到界面或标准输出
                        System.out.print(((TextBlockDeltaEvent) event).getDelta());
                    } else if (event.getType() == AgentEventType.TOOL_CALL_START) {
                        // 智能体即将调用工具 —— 展示调用信息
                        System.out.println("\n[tool] " + ((ToolCallStartEvent) event).getToolCallName());
                    }
                    // 其他事件：思考块、工具结果、回复结束等
                }).blockLast();
    }
}
