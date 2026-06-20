# Auto-generated Python dataclass for Codex hook response schema
"""
Data class for Codex hook response JSON schema.

Fields
------
continue (bool): Whether processing should continue.
stopReason (Optional[str]): Reason for stopping, if any.
systemMessage (Optional[str]): Optional system message.
suppressOutput (bool): Whether to suppress output.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional
import json


@dataclass
class CodexHookResponse:
    """Typed representation of the JSON schema expected by Codex hooks."""

    continue_: bool                     # `continue` is a Python keyword → use a trailing underscore
    stopReason: Optional[str] = None
    systemMessage: Optional[str] = None
    suppressOutput: bool = False

    def to_dict(self) -> dict:
        """Return a JSON‑serialisable dict (converts the `continue_` field)."""
        return {
            "continue": self.continue_,
            "stopReason": self.stopReason,
            "systemMessage": self.systemMessage,
            "suppressOutput": self.suppressOutput,
        }

    def to_json(self) -> str:
        """Serialize the object to a JSON string."""
        return json.dumps(self.to_dict(), ensure_ascii=False, indent=2)

    @staticmethod
    def from_dict(data: dict) -> "CodexHookResponse":
        """Create an instance from a dict (expects the original field names)."""
        return CodexHookResponse(
            continue_=data["continue"],
            stopReason=data.get("stopReason"),
            systemMessage=data.get("systemMessage"),
            suppressOutput=data.get("suppressOutput", False),
        )

    @staticmethod
    def from_json(json_str: str) -> "CodexHookResponse":
        """Parse a JSON string into a `CodexHookResponse`."""
        data = json.loads(json_str)
        return CodexHookResponse.from_dict(data)


if __name__ == "__main__":
    example = CodexHookResponse(
        continue_=True,
        stopReason=None,
        systemMessage=None,
        suppressOutput=False,
    )
    print("Serialized →", example.to_json())
    # round‑trip test
    roundtrip = CodexHookResponse.from_json(example.to_json())
    assert example == roundtrip
