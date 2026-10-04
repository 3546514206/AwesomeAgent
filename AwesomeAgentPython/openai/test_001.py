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



completion = client.chat.completions.create(
    model=model,
    messages=[
        {
            "role": "user",
            "content": "你是谁？"
        }
    ]
)

print(completion.choices[0].message.content)

