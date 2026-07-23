import os
from typing import Literal

from agentscope.agent import Agent
from agentscope.credential import CredentialBase
from agentscope.credential import OpenAICredential
from pydantic import ConfigDict, Field, SecretStr


class MyProviderCredential(CredentialBase):
    model_config = ConfigDict(title="My Provider API")
    type: Literal["my_provider_credential"] = "my_provider_credential"
    api_key: SecretStr = Field(
        default_factory=lambda: SecretStr(os.environ["MINIMAX_API_KEY"]),
        description="API key for My Provider.",
    )
    base_url: str = Field(default="https://api.minimaxi.com/v1")


async def main() -> None:
    agent = Agent(
        name="first_agent",
        system_prompt="You are a helpful assistant named Friday.",
        model=OpenAICredential
    )

    print(agent.reply("Hello World!"))



