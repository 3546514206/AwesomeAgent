import os
from openai import OpenAI

from stringutil import is_empty

base_url = os.getenv("LLM_API_BASE_URL_SIMPLE", "")
api_key = os.getenv("LLM_API_KEY", "")
model = os.getenv("LLM_MODEL", "")

os.environ["OPENAI_BASE_URL"] = base_url
os.environ["OPENAI_API_KEY"] = api_key

if is_empty(model) or is_empty(api_key) or is_empty(base_url):
    raise SystemExit("请先设置环境变量！")

client = OpenAI()

for tep in [0, 0.3, 0.6, 0.9, 1.2, 1.5, 1.8, 2]:
    completion = client.chat.completions.create(
        model=model,
        messages=[
            {
                "role": "user",
                "content": "你是谁？"
            }
        ],
        temperature=tep
    )

    print(completion.choices[0].message.content)
    print("===============================================================")
