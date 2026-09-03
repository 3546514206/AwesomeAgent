import os

from crewai import Agent, Task, Crew, Process
from crewai.llm import LLM


def _required_env(key: str) -> str:
    value = os.environ.get(key)
    if value is None or value == '':
        raise ValueError(f"环境变量 {key} 未设置")
    return value


llm = LLM(
    # 关键： 必须包含服务商 deepseek，然后再写模型名 deepseek-v4-flash 或 deepseek-v4-pro
    model="deepseek/deepseek-v4-flash",
    # 设置 API key
    api_key=_required_env("DEEPSEEK_API_KEY"),
    api_base="https://api.deepseek.com/v1",
    temperature=0.7)

# =====================================================
# Agents
# =====================================================
researcher = Agent(
    role="技术研究员",
    goal="寻找最新、准确、可验证的技术资料，并给出代码证明。",
    backstory="擅长系统性分析技术问题，注重事实和可复现性。",
    llm=llm,
    verbose=True,
    allow_delegation=False,
)

writer = Agent(
    role="技术博客作家",
    goal="将研究成果整理成适合初学者阅读的技术文章。",
    backstory="擅长将复杂概念拆解为清晰步骤，并提供完整示例。",
    llm=llm,
    verbose=True,
    allow_delegation=True,
)

# =====================================================
# Tasks
# =====================================================
research_task = Task(
    description=(
        "深入研究：使用 Python 进行自动化数据清洗。\n"
        "重点包括：pandas、numpy、缺失值、重复值、格式不一致问题。\n"
        "必须提供完整、可运行的代码示例。"
    ),
    agent=researcher,
    expected_output=(
        "一份研究报告，包含问题分类、解决方案代码和完整清洗流程。"
    ),
)

write_task = Task(
    description=(
        "基于研究报告，撰写入门级技术博客。\n"
        "标题：《Python 数据清洗入门：用 pandas 告别脏数据》。"
    ),
    agent=writer,
    context=[research_task],
    expected_output="不少于 800 字的 Markdown 技术博客。",
)

# =====================================================
# Crew
# =====================================================
crew = Crew(
    agents=[researcher, writer],
    tasks=[research_task, write_task],
    process=Process.sequential,
    verbose=True,
)

# =====================================================
# Run
# =====================================================
if __name__ == "__main__":
    result = crew.kickoff()

    output = result.raw

    with open("python_data_cleaning_blog.md", "w", encoding="utf-8") as f:
        f.write(output)
