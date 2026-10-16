package edu.zjnu.agent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 极简的"查天气"工具：内置一张轻量对照表，根据经纬度估算一个温度。
 *
 * <p>对应 Python 版 AwesomeAgentPython/openai/weather.py；这里去掉了对 openmeteo 的真实 HTTP 调用，
 * 留作教学示例。接真实 API 时把 {@link #execute(Map)} 换成 OkHttp/HttpURLConnection 实现即可，
 * 这层接口不用动。
 *
 * <p>为什么要 mock：依赖越少越好跑通，agent 的"工具调用循环"才是这次学习的重点，
 * 工具本身能不能真查到天气是次要的。
 */
public final class WeatherTool implements Tool {

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public String description() {
        return "查询指定经纬度的当前温度（摄氏度）。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("latitude", JsonSchema.number("纬度，如杭州 30.25、长沙 28.23"));
        properties.put("longitude", JsonSchema.number("经度，如杭州 120.17、长沙 112.94"));
        return JsonSchema.object(properties, "latitude", "longitude");
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        double lat = ((Number) arguments.get("latitude")).doubleValue();
        double lon = ((Number) arguments.get("longitude")).doubleValue();
        // ponytail: 估算式 mock —— 真实场景应当替成 open-meteo 调用
        // 以长沙为基点叠距离带来的扰动，重复查询稳定；目的只是让 agent 走完链路。
        double temperature = 22.0 + Math.abs(lat - 28.23) * 0.3 + Math.abs(lon - 112.94) * 0.05;
        return String.format("%.1f", temperature);
    }
}
