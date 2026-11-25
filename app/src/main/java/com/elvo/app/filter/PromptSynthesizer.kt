package com.elvo.app.filter

data class SynthesizedRule(
    val title: String,
    val subtitle: String
)

object PromptSynthesizer {

    fun synthesize(prompt: String): List<SynthesizedRule> {
        val lower = prompt.lowercase().trim()
        val rules = mutableListOf<SynthesizedRule>()

        // The browser guard appears only when the prompt asks for it.
        val wantsCleanSight = PromptVocabulary.mentionsAny(lower, PromptVocabulary.cleanSightKeywords)
        if (wantsCleanSight) {
            rules.add(
                SynthesizedRule(
                    title = PromptVocabulary.sightGuardTitle,
                    subtitle = "Browsers stay open while impure pages are kept from your eyes"
                )
            )
        }

        if (lower.contains("instagram")) {
            rules.add(
                SynthesizedRule(
                    title = "Instagram",
                    subtitle = "Reels and feed scrolling are interrupted"
                )
            )
        }

        if (lower.contains("social") || lower.contains("doomscroll") || lower.contains("reddit") || lower.contains("tiktok") || lower.contains("twitter") || lower.contains("x") || lower.contains("threads") || lower.contains("facebook")) {
            rules.add(
                SynthesizedRule(
                    title = "Social media feeds",
                    subtitle = "TikTok, X, Reddit, Threads and other feeds"
                )
            )
        }

        if (lower.contains("youtube") || lower.contains("shorts") || lower.contains("video") || lower.contains("reels")) {
            rules.add(
                SynthesizedRule(
                    title = "Short videos",
                    subtitle = "YouTube Shorts, Instagram Reels and video loops"
                )
            )
        }

        if (lower.contains("curfew") || lower.contains("pm") || lower.contains("am") || lower.contains("night") || lower.contains("time") || lower.contains("minute") || lower.contains("hour") || lower.contains("daily")) {
            rules.add(
                SynthesizedRule(
                    title = "Time limits",
                    subtitle = "Your to-do list and daily limits"
                )
            )
        }

        if (lower.contains("gambling") || lower.contains("betting") || lower.contains("game") || lower.contains("games")) {
            rules.add(
                SynthesizedRule(
                    title = "Gambling and games",
                    subtitle = "Gambling sites and casino apps are kept away"
                )
            )
        }

        // If no predefined keywords matched, generate rules directly from user's input sentences or phrases!
        if (rules.isEmpty()) {
            val phrases = prompt.split(".", ",", ";", "and").map { it.trim() }.filter { it.length > 3 }
            if (phrases.isNotEmpty()) {
                phrases.take(3).forEach { phrase ->
                    val capitalized = phrase.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    rules.add(
                        SynthesizedRule(
                            title = capitalized,
                            subtitle = "Watched on this device"
                        )
                    )
                }
            } else {
                rules.add(
                    SynthesizedRule(
                        title = "Your rule",
                        subtitle = prompt
                    )
                )
            }
        }

        return rules
    }
}
