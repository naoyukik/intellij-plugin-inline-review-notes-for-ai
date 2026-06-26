#!/usr/bin/env python3
import json
import sys

from CodexHookResponse import CodexHookResponse

def main():
    # Windows等の環境でのエンコーディングエラーを防ぐため、stdout/stderrをUTF-8に再構成する
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    if hasattr(sys.stderr, 'reconfigure'):
        sys.stderr.reconfigure(encoding='utf-8')

    # 1. ログ出力（stderrへ送る）
    sys.stderr.write("Hook: Synchronizing with Memory Palace...\n")

    # 2. stdinからの入力読み取り
    try:
        input_data = json.load(sys.stdin)
    except Exception:
        input_data = {}

    # 3. 追加コンテキストの定義
    # Memory MCP (Knowledge Graph) から MemPalace (AAAK) への移行を反映
    memory_instruction = (
        "自己認識のため、自分の名前を宣言せよ。"
        "スキル referencing-commit-convention, operating-jetbrains-projects, operating-git を読み込め。"
        "mempalace_get_taxonomy を実行して、Memory Palace の最新のインデックス（地図）を読み取れ。"
        "準備ができたら報告せよ。"
    )

    # 4. JSONレスポンスの構築
    response = CodexHookResponse(
        continue_=True,
        systemMessage=memory_instruction,
        suppressOutput=False,
        hookSpecificOutput={
            "hookEventName": "SessionStart",
            "additionalContext": "init Syncing Active (via Python)"
        }
    )

    # 5. stdoutへの出力（JSONのみ！）
    print(response.to_json())
    sys.exit(0) # 正常終了

if __name__ == "__main__":
    main()
