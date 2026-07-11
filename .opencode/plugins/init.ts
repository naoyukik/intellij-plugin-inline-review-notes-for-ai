// .opencode/plugins/hello-plugin.ts
import type { Plugin } from "@opencode-ai/plugin"

export const init: Plugin = async ({ client }) => {
    return {
        event: async ({ event }) => {
            if (event.type === "session.created") {
                const sessionId = (event as any).properties?.sessionID ?? (event as any).sessionID
                if (sessionId) {
                    await client.session.prompt({
                        path: { id: sessionId },
                        body: {
                            noReply: true,
                            parts: [{
                                type: "text",
                                text: "mempalace から 今のプロジェクトの最新のインデックスを取得せよ。"
                            }],
                        },
                    })
                }
            }
        },
    }
}