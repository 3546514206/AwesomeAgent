package edu.zjnu.samples;


import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;

import java.util.UUID;

/**
 * FirstAgent
 *
 * @Date 2026-06-21 22:54
 * @Author 杨海波
 **/
public class FirstAgent {

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




        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId("setsunayang")
                .build();

        agent.call(new UserMessage("我叫杨海波，今天准备一个关于 ReAct 的技术分享。"),ctx).block();

        agent.call(new UserMessage("我叫什么？我今天要干什么？"), ctx).block();
    }
}
