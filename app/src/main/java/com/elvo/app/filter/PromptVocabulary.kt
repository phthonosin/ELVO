package com.elvo.app.filter

/** Shared prompt vocabulary used by both the live rules and the settings preview. */
internal object PromptVocabulary {

    /** Words that ask for a clean sight guard, never a blanket browser block. */
    val cleanSightKeywords = listOf(
        "adult", "porn", "nsfw", "explicit", "xxx", "hentai", "impure", "impurity"
    )

    /** Phrases that mean "social media" as one directive. */
    val socialMediaPhrases = listOf(
        "social media", "socials", "social apps", "social networks", "doomscroll", "doomscrolling"
    )

    /** Known social packages used when the user names social media as a whole. */
    val socialPackages = mapOf(
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "tiktok" to "com.zhiliaoapp.musically",
        "reddit" to "com.reddit.frontpage",
        "snapchat" to "com.snapchat.android",
        "twitter" to "com.twitter.android"
    )

    const val sightGuardTitle = "Unclean pages in browsers"

    fun mentionsAny(lower: String, keywords: List<String>): Boolean =
        keywords.any { lower.contains(it) }
}
