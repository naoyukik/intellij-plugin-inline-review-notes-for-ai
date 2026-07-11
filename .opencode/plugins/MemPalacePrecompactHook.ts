import type { Plugin } from "@opencode-ai/plugin"

export const MemPalacePrecompactHook: Plugin = async (ctx) => {
    return {
        "experimental.session.compacting": async (input, output) => {
            // Inject additional context into the compaction prompt
            output.context.push(`
## Custom instructions

- Please add the necessary information to mempalace.
`)
        },
    }
}