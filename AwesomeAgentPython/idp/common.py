"""idp 带教的公共入口：环境变量封装 + 天气工具 + 终端交互循环

这个模块自成一体，不依赖仓库里其它目录——带教代码要能单独拷走、单独跑，
IDE 也能静态解析。参考代码在 openai/ 下（只读对照）：call_llm.py / weather.py。
"""

import os

from openai import OpenAI


def create_completion(messages: list[dict], **kwargs):
    """调用 LLM 对话接口，返回原始 completion

    Args:
        messages: OpenAI 格式的消息列表
        **kwargs: 其他参数原样透传（如 temperature、tools、tool_choice）
    Returns:
        ChatCompletion: 原始响应对象（含 choices、usage 等）
    """
    base_url = os.getenv("LLM_API_BASE_URL_SIMPLE", "")
    api_key = os.getenv("LLM_API_KEY", "")
    model = os.getenv("LLM_MODEL", "")
    if not base_url or not api_key or not model:
        raise SystemExit("请先设置环境变量：LLM_API_BASE_URL_SIMPLE / LLM_API_KEY / LLM_MODEL")

    client = OpenAI(base_url=base_url, api_key=api_key)
    return client.chat.completions.create(model=model, messages=messages, **kwargs)


def get_weather(latitude: float, longitude: float) -> str:
    """查询指定经纬度的当前天气

    Args:
        latitude:  纬度
        longitude: 经度
    Returns:
        str: 当前温度和湿度
    """
    # 延迟导入：只有第二周起才用得到天气，别让第一周被这个依赖卡住
    import openmeteo_requests

    responses = openmeteo_requests.Client().weather_api(
        "https://api.open-meteo.com/v1/forecast",
        params={
            "latitude": latitude,
            "longitude": longitude,
            "current": ["temperature_2m", "relative_humidity_2m"],
        },
    )
    current = responses[0].Current()
    temperature = round(current.Variables(0).Value(), 1)
    humidity = round(current.Variables(1).Value())
    return f"温度 {temperature}°C，湿度 {humidity}%"


def ask_loop(handler, quit_words: tuple[str, ...] = ("exit", "quit")) -> None:
    """终端交互循环：每次输入交给 handler 处理，输入 exit/quit 退出

    Args:
        handler:    处理函数，签名 (user_input: str) -> str
        quit_words: 退出指令
    """
    print("输入你的问题，输入 exit 退出：")
    while True:
        user_input = input("\n你> ").strip()
        if user_input in quit_words:
            break
        if not user_input:
            continue
        print("\n助手> " + handler(user_input))


__all__ = ["create_completion", "get_weather", "ask_loop"]
