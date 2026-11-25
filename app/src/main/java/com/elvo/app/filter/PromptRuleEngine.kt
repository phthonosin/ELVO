package com.elvo.app.filter

import java.time.LocalTime

/** A single line in the plain-language review of what the prompt blocks. */
data class PlanBullet(
    val bulletText: String,
    val detailText: String
)

/** A time window taken from the user's own words, e.g. "after 10 PM". */
data class ActiveWindow(val startHour: Int, val endHour: Int, val description: String) {
    fun allowsNow(): Boolean {
        val now = LocalTime.now()
        val minute = now.hour * 60 + now.minute
        val start = startHour * 60
        val end = endHour * 60
        return if (end >= start) minute in start until end else minute >= start || minute < end
    }
}

data class GovernancePlan(
    val rawPrompt: String,
    val targetAppPackages: Set<String>,
    val packageKeywords: Set<String>,
    val bulletPoints: List<PlanBullet>,
    val filterBrowserAdult: Boolean,
    val activeWindow: ActiveWindow?
)

object PromptRuleEngine {

    private val afterRegex = Regex("\\bafter\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
    private val beforeRegex = Regex("\\bbefore\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
    private val betweenRegex = Regex(
        "\\bbetween\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:and|to|-|–)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b",
        RegexOption.IGNORE_CASE
    )

    fun generatePlan(prompt: String): GovernancePlan {
        val lower = prompt.lowercase().trim()
        val window = parseWindow(lower)
        val windowNote = if (window == null) "" else " Applies ${window.description}."

        val targetAppPackages = mutableSetOf<String>()
        val packageKeywords = mutableSetOf<String>()
        val bulletPoints = mutableListOf<PlanBullet>()
        var hasSpecificDirective = false

        // Adult filtering exists only when the user asks for it. It is a content
        // filter on browsers, never a blanket browser block.
        val wantsAdult = PromptVocabulary.mentionsAny(lower, PromptVocabulary.cleanSightKeywords)
        if (wantsAdult) {
            bulletPoints.add(
                PlanBullet(
                    bulletText = PromptVocabulary.sightGuardTitle,
                    detailText = "Browsers stay open. Impure sites, pages and searches are kept from your eyes.$windowNote"
                )
            )
            hasSpecificDirective = true
        }

        // "Social media" is one directive, not a hidden list of named apps.
        val wantsSocial = PromptVocabulary.mentionsAny(lower, PromptVocabulary.socialMediaPhrases)
        if (wantsSocial) {
            PromptVocabulary.socialPackages.values.forEach { targetAppPackages.add(it) }
            bulletPoints.add(
                PlanBullet(
                    bulletText = "Social media",
                    detailText = "Social apps are paused.$windowNote"
                )
            )
            hasSpecificDirective = true
        }

        // Individually named apps.
        if (lower.contains("instagram") || lower.contains("insta") || lower.contains("reels")) {
            targetAppPackages.add("com.instagram.android")
            packageKeywords.add("instagram")
            if (!wantsSocial) {
                bulletPoints.add(
                    PlanBullet(
                        bulletText = "Instagram",
                        detailText = "Reels and feed scrolling are interrupted.$windowNote"
                    )
                )
            }
            hasSpecificDirective = true
        }

        if (lower.contains("youtube") || lower.contains("shorts")) {
            targetAppPackages.add("com.google.android.youtube")
            packageKeywords.add("youtube")
            bulletPoints.add(
                PlanBullet(
                    bulletText = "YouTube Shorts",
                    detailText = "Short videos and loops are interrupted.$windowNote"
                )
            )
            hasSpecificDirective = true
        }

        if (lower.contains("tiktok")) {
            targetAppPackages.add("com.zhiliaoapp.musically")
            packageKeywords.add("tiktok")
            if (!wantsSocial) {
                bulletPoints.add(
                    PlanBullet(
                        bulletText = "TikTok",
                        detailText = "Short videos are interrupted.$windowNote"
                    )
                )
            }
            hasSpecificDirective = true
        }

        if (lower.contains("reddit") && !wantsSocial) {
            bulletPoints.add(
                PlanBullet(
                    bulletText = "Reddit",
                    detailText = "Feed scrolling is interrupted.$windowNote"
                )
            )
        }
        if (lower.contains("reddit")) {
            targetAppPackages.add("com.reddit.frontpage")
            packageKeywords.add("reddit")
            hasSpecificDirective = true
        }

        if (lower.contains("twitter") || lower.contains(" x ")) {
            targetAppPackages.add("com.twitter.android")
            packageKeywords.add("twitter")
            if (!wantsSocial) {
                bulletPoints.add(
                    PlanBullet(
                        bulletText = "X / Twitter",
                        detailText = "Feed scrolling is interrupted.$windowNote"
                    )
                )
            }
            hasSpecificDirective = true
        }

        if (lower.contains("facebook") || lower.contains("fb")) {
            targetAppPackages.add("com.facebook.katana")
            packageKeywords.add("facebook")
            if (!wantsSocial) {
                bulletPoints.add(
                    PlanBullet(
                        bulletText = "Facebook",
                        detailText = "Feed scrolling is interrupted.$windowNote"
                    )
                )
            }
            hasSpecificDirective = true
        }

        if (lower.contains("snapchat") || lower.contains("snap")) {
            targetAppPackages.add("com.snapchat.android")
            packageKeywords.add("snapchat")
            if (!wantsSocial) {
                bulletPoints.add(
                    PlanBullet(
                        bulletText = "Snapchat",
                        detailText = "Feed scrolling is interrupted.$windowNote"
                    )
                )
            }
            hasSpecificDirective = true
        }

        if (lower.contains("gambling") || lower.contains("betting") || lower.contains("casino")) {
            bulletPoints.add(
                PlanBullet(
                    bulletText = "Gambling",
                    detailText = "Gambling sites and casino apps are kept away.$windowNote"
                )
            )
            hasSpecificDirective = true
        }

        // A time limit alone is not a thing to block; it narrows the rules above.
        // If the user only gave a time with no target, show their own words.
        if (!hasSpecificDirective) {
            bulletPoints.add(
                PlanBullet(
                    bulletText = "Your rule",
                    detailText = prompt
                )
            )
        }

        return GovernancePlan(
            rawPrompt = prompt,
            targetAppPackages = targetAppPackages,
            packageKeywords = packageKeywords,
            bulletPoints = bulletPoints.distinctBy { it.bulletText },
            filterBrowserAdult = wantsAdult,
            activeWindow = window
        )
    }

    private fun parseWindow(lower: String): ActiveWindow? {
        betweenRegex.find(lower)?.let { match ->
            val from = hourOf(match.groupValues[1], match.groupValues[3], after = false)
            val to = hourOf(match.groupValues[4], match.groupValues[6], after = true)
            if (from != null && to != null && from != to) {
                return ActiveWindow(from, to, "between ${clockLabel(from)} and ${clockLabel(to)}")
            }
        }
        afterRegex.find(lower)?.let { match ->
            val hour = hourOf(match.groupValues[1], match.groupValues[3], after = true) ?: return@let
            return ActiveWindow(hour, 24, "after ${clockLabel(hour)}")
        }
        beforeRegex.find(lower)?.let { match ->
            val hour = hourOf(match.groupValues[1], match.groupValues[3], after = false) ?: return@let
            return ActiveWindow(0, hour, "before ${clockLabel(hour)}")
        }
        return null
    }

    private fun hourOf(raw: String, meridiem: String, after: Boolean): Int? {
        val value = raw.toIntOrNull() ?: return null
        if (value !in 0..23) return null
        if (meridiem.isNotBlank()) {
            val hour = value % 12
            return if (meridiem.startsWith('p', ignoreCase = true)) hour + 12 else hour
        }
        if (after && value in 1..11) return value + 12
        return value
    }

    private fun clockLabel(hour: Int): String {
        val suffix = if (hour >= 12 && hour < 24) "PM" else "AM"
        val display = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        return "$display $suffix"
    }
}
