"""第二周：让模型调用 Tool

模型只会「说」不会「做」——它输出的是意图（调哪个工具、传什么参数），
真正的数据要靠程序去取。链路四步：给工具清单 → 模型选工具 → 程序执行 → 结果回传。
"""

import json

from common import ask_loop, create_completion, get_weather

# 1. 工具清单：把 weather 的查询能力描述成 function calling 格式上送给模型
TOOLS = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "查询指定经纬度的当前天气，返回当前温度和湿度",
        "parameters": {
            "type": "object",
            "properties": {
                "latitude": {"type": "number", "description": "纬度，如杭州 30.25"},
                "longitude": {"type": "number", "description": "经度，如杭州 120.17"},
            },
            "required": ["latitude", "longitude"],
        },
    },
}]


def dispatch(name: str, args: dict) -> str:
    """按工具名分发执行

    Args:
        name: 工具名
        args: 模型给的参数（已从 JSON 字符串解析成 dict）
    Returns:
        str: 工具执行结果
    """
    if name == "get_weather":
        return get_weather(**args)
    raise ValueError(f"未知工具: {name}")


def run(question: str) -> str:
    """一次完整的工具调用链路

    Args:
        question: 用户问题
    Returns:
        str: 模型结合工具结果的最终回答
    """
    messages = [{"role": "user", "content": question}]

    # 2. 第一次调用：模型判断选不选工具、怎么传参
    msg = create_completion(messages, tools=TOOLS).choices[0].message
    if not msg.tool_calls:
        return msg.content or ""

    tool_call = msg.tool_calls[0]
    args = json.loads(tool_call.function.arguments)
    print(f"模型选择工具: {tool_call.function.name}({args})")

    # 3. agent 执行工具，真正取数据
    result = dispatch(tool_call.function.name, args)

    # 4. 结果回传，模型把数据组织成自然语言回答
    messages.append(msg)
    messages.append({
        "role": "tool",
        "tool_call_id": tool_call.id,
        "content": result,
    })
    return create_completion(messages).choices[0].message.content or ""


if __name__ == "__main__":
    print("提示：试试问「杭州现在天气怎么样？」或「查一下深圳和广州的温度」。")
    ask_loop(run)
