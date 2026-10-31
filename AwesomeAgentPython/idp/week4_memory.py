"""第四周：给 Agent 加短期记忆

模型本身没有记忆——每次请求都是全新的。所谓「记住」，就是把之前的对话
连同工具调用轨迹一起塞回 messages 里，用上下文窗口换记忆。
"""

from collections import deque

from common import ask_loop
from week3_react import react


class Agent:
    """带短期记忆的 Agent：每轮提问都把历史一起送给模型"""

    def __init__(self, system_prompt: str = "你是一个简洁、务实的助手。", max_messages: int = 12):
        """
        Args:
            system_prompt: 系统提示词
            max_messages:  记忆里最多保留多少条消息，超出自动丢弃最早的
        """
        self.system_prompt = system_prompt
        self.history = deque(maxlen=max_messages)  # 短期记忆：先进先出

    def chat(self, user_input: str) -> str:
        """处理一轮对话，返回模型回答，同时更新记忆

        Args:
            user_input: 用户这一轮说的话
        Returns:
            str: 模型回答
        """
        self.history.append({"role": "user", "content": user_input})
        messages = [{"role": "system", "content": self.system_prompt}, *self.history]
        before = len(messages)

        answer, trace = react(messages)

        # 工具调用轨迹也要进记忆，否则下一轮模型会重复调同一个工具
        for message in trace[before:]:
            self.history.append(message)
        self.history.append({"role": "assistant", "content": answer})
        return answer


if __name__ == "__main__":
    agent = Agent()
    print("提示：先说「我叫杨海波，在杭州做开发」，再问「我叫什么？」、"
          "「那边现在天气怎么样？」，看它这次还记得。")
    ask_loop(agent.chat)
