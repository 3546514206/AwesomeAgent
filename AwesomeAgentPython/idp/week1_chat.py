"""第一周：跑通一次 LLM 调用，实现单轮对话

要点：
1. base_url / api_key / model 三个环境变量缺一不可（call_llm.py 里已做校验）
2. system 提示词定角色和语气，user 是本轮问题
3. 模型没有记忆：每次请求都是全新的，喂进去什么它才知道什么
"""

from common import ask_loop, create_completion

SYSTEM_PROMPT = "你是一个简洁、务实的助手，回答不超过三句话。"


def chat(user_input: str) -> str:
    """单轮对话：一次请求换一次回答，不携带任何历史

    Args:
        user_input: 用户输入
    Returns:
        str: 模型回答
    """
    completion = create_completion([
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": user_input},
    ])
    return completion.choices[0].message.content


if __name__ == "__main__":
    print("提示：模型没有记忆，问完一句再问「我刚才说了什么」，它答不上来。")
    ask_loop(chat)
