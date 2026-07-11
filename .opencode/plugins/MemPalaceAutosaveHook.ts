// .opencode/plugins/mempalace-autosave.ts
//
// MemPalace 自動保存フック(OpenCode版)
//
// N回の応答完了ごとに、AIに MemPalace への保存を促す。
// session.idle 時に client.session.prompt(noReply:true) でコンテキストを注入する。

import type { Plugin } from "@opencode-ai/plugin"
import * as fs from "fs"
import * as path from "path"
import * as os from "os"

// === 設定 ===
const SAVE_INTERVAL = 15
const STATE_DIR = path.join(os.homedir(), ".mempalace", "hook_state_opencode")
const LOG_FILE = path.join(__dirname, "mempalace-debug.log")

function log(msg: string) {
    const line = `[${new Date().toISOString()}] ${msg}\n`
    fs.appendFileSync(LOG_FILE, line)
}

function ensureStateDir() {
    fs.mkdirSync(STATE_DIR, { recursive: true })
}

function loadCount(sessionID: string): number {
    const file = path.join(STATE_DIR, `${sessionID}.count`)
    try {
        return parseInt(fs.readFileSync(file, "utf-8").trim(), 10) || 0
    } catch {
        return 0
    }
}

function saveCount(sessionID: string, count: number) {
    ensureStateDir()
    fs.writeFileSync(path.join(STATE_DIR, `${sessionID}.count`), String(count))
}

export const MemPalaceAutosaveHook: Plugin = async ({ client }) => {
    log("initialized")

    return {
        event: async ({ event }) => {
            if (event.type !== "session.idle") return

            const sessionID =
                (event as any).properties?.sessionID ??
                (event as any).sessionID ??
                "default"

            log(`session.idle detected, sessionID=${sessionID}`)

            const count = loadCount(sessionID) + 1

            if (count < SAVE_INTERVAL) {
                saveCount(sessionID, count)
                log(`count=${count}/${SAVE_INTERVAL}, skipping`)
                return
            }

            saveCount(sessionID, 0)
            log(`threshold reached, injecting save prompt`)

            try {
                await client.session.prompt({
                    path: { id: sessionID },
                    body: {
                        noReply: true,
                        parts: [
                            {
                                type: "text",
                                text:
                                    "MemPalace save checkpoint. Write a brief session diary entry covering key topics, decisions, and code changes since the last save. Use verbatim quotes where possible. Continue after saving."
                            },
                        ],
                    },
                })
                log("save prompt injected successfully")
            } catch (err) {
                log(`FAILED to inject save prompt: ${err}`)
            }
        },
    }
}
