// .opencode/plugins/hello-plugin.ts
import type { Plugin } from "@opencode-ai/plugin"

export const MemPalaceSessionEndHook: Plugin = async ({ client }) => {
    return {
        event: async ({ event }) => {
            if (event.type === "session.deleted") {
                const sessionId = (event as any).properties?.sessionID ?? (event as any).sessionID
                if (sessionId) {
                    await client.session.prompt({
                        path: { id: sessionId },
                        body: {
                            noReply: true,
                            parts: [{
                                type: "text",
                                text: "Please add the necessary information to mempalace"
                            }],
                        },
                    })
                }
            }
        },
    }
}