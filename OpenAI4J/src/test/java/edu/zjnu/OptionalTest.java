package edu.zjnu;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Optional 演示：一个"可能装着值、也可能为空"的盒子。
 *
 * <p>背景：SDK 的 choices().get(0).message().content() 返回 Optional&lt;String&gt;，
 * 因为协议里 content 字段可为 null（模型走工具调用时就没有文本）。
 */
public class OptionalTest {

    // ---- 装箱：怎么造一个 Optional ----

    /**
     * of(null) 在装箱那一刻就炸 —— of 只用于"我确定非空"
     */
    @Test(expected = NullPointerException.class)
    public void of_rejectsNull() {
        Optional.of(null);
    }

    /**
     * ofNullable(null) 装成空盒子 empty，不炸 —— SDK 内部对可空字段就是这么包装的
     */
    @Test
    public void ofNullable_toleratesNull() {
        assertFalse(Optional.ofNullable(null).isPresent());
        assertTrue(Optional.ofNullable("有值").isPresent());
    }

    // ---- 取值：空盒子必须表态 ----

    /**
     * 模型正常回复：content 有值
     */
    @Test
    public void contentPresent_getsDirectly() {
        Optional<String> content = mockContent("你好");

        assertTrue(content.isPresent());            // 盒子里有东西吗
        assertEquals("你好", content.get());         // 确信有值才这么取
        assertEquals("你好", content.orElse("空"));  // 有值时 orElse 不生效
    }

    /**
     * 模型走工具调用：content 为 null —— CallLlm.chat 里的兜底写法
     */
    @Test
    public void contentAbsent_fallsBackToDefault() {
        Optional<String> content = mockContent(null);

        assertFalse(content.isPresent());
        assertEquals("", content.orElse(""));                              // 兜底常量
        assertEquals("（无文本）", content.orElseGet(() -> "（无文本）")); // 惰性求值：需要现算时用
    }

    /**
     * 空盒子上裸调 get() 当场炸 NoSuchElementException —— 这正是 Optional 的价值：
     * 错误炸在"取值的现场"，而不是像 null 那样被传递到很远的地方才 NPE
     */
    @Test(expected = NoSuchElementException.class)
    public void get_onEmptyBox_throwsImmediately() {
        Optional.<String>empty().get();
    }

    /**
     * orElseThrow：把"可能为空"升级为明确的业务异常
     */
    @Test(expected = IllegalStateException.class)
    public void orElseThrow_raisesBusinessException() {
        Optional.<String>empty().orElseThrow(() -> new IllegalStateException("模型没返回文本"));
    }

    // ---- 链式：盒子上直接变换，不会 NPE ----

    /**
     * map：有值就变换，空盒子原样传递空，链条中间不需要判空
     */
    @Test
    public void map_chainsWithoutNullCheck() {
        assertEquals(Optional.of(5), Optional.of("hello").map(String::length));
        assertEquals(Optional.empty(), Optional.<String>empty().map(String::length));
    }

    /**
     * filter：不满足条件就变空盒子
     */
    @Test
    public void filter_unmatched_becomesEmpty() {
        assertEquals(Optional.empty(), Optional.of("  ").filter(s -> !s.trim().isEmpty()));
    }

    // ---- 还原 SDK 场景 ----

    /**
     * choices().get(0).message().content().orElse("") 在两种模型行为下的取值结果
     */
    @Test
    public void chatText_simulatesBothModelBehaviors() {
        assertEquals("答案", chatText(mockContent("答案"))); // 正常回复
        assertEquals("", chatText(mockContent(null)));       // 工具调用，content 为 null
    }

    /**
     * 模拟 ChatCompletionMessage.content()：字段可能为 null，就包成 Optional 返回
     */
    private static Optional<String> mockContent(String s) {
        return Optional.ofNullable(s);
    }

    /**
     * 模拟 CallLlm.chat 的取值策略
     */
    private static String chatText(Optional<String> content) {
        return content.orElse("");
    }
}
