package com.oryno.piggy_ledger.ai

object AiSanitizer {
    /**
     * Completely removes all traces of internal reasoning, thinking tokens,
     * <think> tags, markdown thinking blocks, and unclosed thought tags from AI responses.
     */
    fun sanitizeThinking(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var cleaned = raw

        // 1. Strip complete XML/HTML thinking tags: <think>...</think>, <thought>...</thought>, <thinking>...</thinking>, <reasoning>...</reasoning>, <thought_process>...</thought_process>
        cleaned = cleaned.replace(Regex("""(?is)<\s*(?:think|thought|thinking|reasoning|thought_process)\b[^>]*>[\s\S]*?<\s*/\s*(?:think|thought|thinking|reasoning|thought_process)\s*>"""), "")

        // 2. Strip bracketed think blocks: [THINK]...[/THINK], [thought]...[/thought], [thinking]...[/thinking], [reasoning]...[/reasoning]
        cleaned = cleaned.replace(Regex("""(?is)\[\s*(?:think|thought|thinking|reasoning)\b[^\]]*\][\s\S]*?\[\s*/\s*(?:think|thought|thinking|reasoning)\s*\]"""), "")

        // 3. Strip unclosed opening think tags (if response stream truncated or stopped mid-thought)
        cleaned = cleaned.replace(Regex("""(?is)<\s*(?:think|thought|thinking|reasoning|thought_process)\b[^>]*>[\s\S]*$"""), "")
        cleaned = cleaned.replace(Regex("""(?is)\[\s*(?:think|thought|thinking|reasoning)\b[^\]]*\][\s\S]*$"""), "")

        // 4. Clean up any leftover stray tag markers
        cleaned = cleaned.replace(Regex("""(?is)<\s*/?\s*(?:think|thought|thinking|reasoning|thought_process)\b[^>]*>"""), "")
        cleaned = cleaned.replace(Regex("""(?is)\[\s*/?\s*(?:think|thought|thinking|reasoning)\b[^\]]*\]"""), "")

        // 5. Strip Markdown-style thinking headers & paragraphs (e.g. "**Thinking Process:**", "### Thinking Process", "*Thought Process:*", "Chain of Thought:")
        cleaned = cleaned.replace(
            Regex("""(?is)(?:^|\n)(?:#{1,4}\s*|\*{1,2}|_{1,2})?\s*(?:thinking process|thought process|internal reasoning|chain of thought|reasoning process|my thinking)\b[\s\S]*?(?=(?:\n\s*(?:#{1,4}\s*|\*{1,2}[A-Z]|Answer:|Conclusion:|\n\n[A-Z0-9]))|\z)"""),
            ""
        )

        // 6. Strip leading or standalone lines starting with Thinking: / Thought: / Reasoning:
        cleaned = cleaned.replace(Regex("""(?im)^\s*(?:Thinking|Thought|Reasoning|Internal Thoughts):\s*.*$"""), "")

        cleaned = cleaned.trim()
        return cleaned
    }
}
