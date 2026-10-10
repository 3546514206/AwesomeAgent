package edu.zjnu;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

/**
 * jtokkit 分词器示例（对应 Python 版 AwesomeAgentPython/openai/tokenizer.py）
 *
 * <p>LLM 不直接读字符串，读的是 token id：编码表（BPE）把文本切成子词，
 * 每个子词对应词表里的一个整数。计费、上下文窗口都按 token 数算，不按字数。
 */
public final class Tokenizer {

    public static void main(String[] args) {
        // 注册表会从 jar 里加载 BPE 编码表，建一次反复用（Encoding 是线程安全的）
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE); // GPT-3.5 / GPT-4 系列用的编码表

        String text = "你是谁？";
        // encode 返回的是 jtokkit 自家的 IntArrayList（int 原生数组，省掉装箱），
        // 想要 java.util.List 就调 boxed()
        IntArrayList tokenIds = encoding.encode(text);

        System.out.println(tokenIds.boxed());
        System.out.println("Token数：" + tokenIds.size());
        System.out.println("解码还原：" + encoding.decode(tokenIds));
    }
}
