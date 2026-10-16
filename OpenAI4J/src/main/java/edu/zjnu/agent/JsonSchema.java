package edu.zjnu.agent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON Schema 的极简构造器，避免每个 Tool 实现都写一堆嵌套 Map。
 *
 * <p>支持的对象：{@link #object}、{@link #number}、{@link #string}、{@link #bool}。
 * 返回的都是 LinkedHashMap 以保证遍历顺序符合人类阅读顺序。
 */
final class JsonSchema {

    private JsonSchema() {}

    static Map<String, Object> number(String description) {
        return prop("number", description);
    }

    static Map<String, Object> string(String description) {
        return prop("string", description);
    }

    static Map<String, Object> bool(String description) {
        return prop("boolean", description);
    }

    static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && required.length > 0) {
            schema.put("required", Arrays.asList(required));
        }
        return schema;
    }

    private static Map<String, Object> prop(String type, String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("description", description);
        return p;
    }
}
