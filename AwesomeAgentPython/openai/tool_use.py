import json
import sys
from pathlib import Path

# weather.py 在同级目录 open-meteo/ 下，加入模块搜索路径
from call_llm import create_completion
from weather import get_weather

# 1. 工具清单：把 weather.py 的查询能力描述成 OpenAI function calling 格式上送给大模型
TOOLS = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "查询指定经纬度的当前天气，返回当前温度",
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

question = "长沙现在天气怎么样？"
messages = [{"role": "user", "content": question}]

# 2. 第一次调用：大模型判断选不选工具、怎么传参
msg = create_completion(messages, tools=TOOLS).choices[0].message

if not msg.tool_calls:
    print(msg.content)
    raise SystemExit("模型没有选择调用工具")

tool_call = msg.tool_calls[0]
args = json.loads(tool_call.function.arguments)
print(f"模型选择工具: {tool_call.function.name}({args})")

# 3. agent 根据模型的返回结果执行工具，真正查询天气
result = get_weather(**args)

# 4. 工具结果回传，模型组织成自然语言回答
messages.append(msg)
messages.append({
    "role": "tool",
    "tool_call_id": tool_call.id,
    "content": f"当前温度: {result}°C",
})
answer = create_completion(messages).choices[0].message.content
print("===============================================================")
print(answer)
