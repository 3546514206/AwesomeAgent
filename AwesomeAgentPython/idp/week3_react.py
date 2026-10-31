"""第三周：ReAct 循环——思考 → 行动 → 观察 → 再思考 → 最终回答

第二周是「一轮定生死」：模型给一次 tool_call，人执行一次，多了就够不着。
ReAct 把它变成循环：模型每一轮都可以再要工具，直到它自己觉得信息够了才收口。
"""

import json

from common import ask_loop, create_completion
from week2_tool import TOOLS, dispatch

MAX_STEPS = 5  # 单个任务最多几轮工具调用，防止模型绕圈


def react(messages: list[dict]) -> tuple[str, list[dict]]:
    """跑通一次完整 ReAct 链路

    Args:
        messages: 初始消息列表（不会被修改）
    Returns:
        (最终回答, 完整消息轨迹)
    """
    trace = list(messages)

    for step in range(MAX_STEPS):
        # 思考：模型自己决定是直接回答，还是还要调工具
        msg = create_completion(trace, tools=TOOLS).choices[0].message
        if not msg.tool_calls:
            return msg.content or "", trace

        # 行动：把 assistant 的 tool_calls 写回轨迹，模型下一轮能回看自己做过什么
        trace.append(msg.model_dump(exclude_none=True))
        for call in msg.tool_calls:
            args = json.loads(call.function.arguments)
            print(f"[第{step + 1}轮] 行动: {call.function.name}({args})")

            # 观察：真正执行工具，把结果作为 tool 消息回传给模型
            result = dispatch(call.function.name, args)
            print(f"          观察: {result}")
            trace.append({
                "role": "tool",
                "tool_call_id": call.id,
                "content": str(result),
            })

    raise RuntimeError(f"超过最大步数 {MAX_STEPS}，模型仍未给出最终回答")


if __name__ == "__main__":
    def handle(question: str) -> str:
        """一个问题 = 一条独立的 ReAct 链路"""
        answer, _ = react([{"role": "user", "content": question}])
        return answer

    print("提示：试试问「长沙现在天气怎么样？顺便对比一下杭州。」看它连续转几轮。")
    ask_loop(handle)
