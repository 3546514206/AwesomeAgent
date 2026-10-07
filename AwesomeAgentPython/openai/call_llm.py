import os

from openai import OpenAI

from stringutil import is_empty


def create_completion(messages: list[dict], **kwargs):
    """
    调用 LLM 对话接口，返回原始 completion

    Args:
        messages: OpenAI 格式的消息列表
        **kwargs: 其他参数原样透传（如 temperature、tools、tool_choice）
    Returns:
        ChatCompletion: 原始响应对象（含 choices、usage 等）
    """
    base_url = os.getenv("LLM_API_BASE_URL_SIMPLE", "")
    api_key = os.getenv("LLM_API_KEY", "")
    model = os.getenv("LLM_MODEL", "")

    if is_empty(base_url) or is_empty(api_key) or is_empty(model):
        raise SystemExit("请先设置环境变量！")

    client = OpenAI(base_url=base_url, api_key=api_key)
    return client.chat.completions.create(model=model, messages=messages, **kwargs)


def chat(messages: list[dict], **kwargs) -> str:
    """便捷封装：只返回回复文本"""
    return create_completion(messages, **kwargs).choices[0].message.content


if __name__ == "__main__":
    print(chat([
        {"role": "system", "content": "不啰嗦！"},
        {"role": "user", "content": "你是谁？"},
    ]))
