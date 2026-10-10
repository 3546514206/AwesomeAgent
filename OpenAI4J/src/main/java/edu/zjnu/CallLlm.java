package edu.zjnu;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * 调用 LLM 对话接口（对应 Python 版 AwesomeAgentPython/openai/call_llm.py）
 *
 * <p>需要设置环境变量：LLM_API_BASE_URL_SIMPLE、LLM_API_KEY、LLM_MODEL
 */
public final class CallLlm {

    /**
     * 调用 LLM 对话接口，返回原始 completion（含 choices、usage 等）
     *
     * <p>messages 及 temperature、tools 等参数由调用方通过 builder 传入（对应 Python 的 **kwargs）
     */
    public static ChatCompletion createCompletion(ChatCompletionCreateParams.Builder params) {
        String baseUrl = System.getenv("LLM_API_BASE_URL_SIMPLE");
        String apiKey = System.getenv("LLM_API_KEY");
        String model = System.getenv("LLM_MODEL");

        if (isBlank(baseUrl) || isBlank(apiKey) || isBlank(model)) {
            throw new IllegalStateException("请先设置环境变量！LLM_API_BASE_URL_SIMPLE / LLM_API_KEY / LLM_MODEL");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        return client.chat().completions().create(params.model(model).build());
    }

    /**
     * 便捷封装：只返回回复文本
     */
    public static String chat(ChatCompletionCreateParams.Builder params) {
        return createCompletion(params).choices().get(0).message().content().orElse("");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static void main(String[] args) {
        System.out.println(chat(ChatCompletionCreateParams.builder()
                .addSystemMessage("不啰嗦！")
                .addUserMessage("你是谁？")));
    }
}
