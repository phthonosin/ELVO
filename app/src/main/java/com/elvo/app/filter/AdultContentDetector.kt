package com.elvo.app.filter

import java.net.URI
import java.net.URLDecoder

/**
 * Result of inspecting a browser URL or a piece of visible page text.
 */
data class AdultContentMatch(
    val isAdult: Boolean,
    val reason: String = "",
    val matchedTerm: String = ""
) {
    companion object {
        val Clean = AdultContentMatch(false)
    }
}

/**
 * Pure on-device adult-content detector used by the browser guard.
 *
 * The detector only looks at the browser URL and the visible page text.
 * It never treats a browser package itself as a violation, which is what
 * keeps Chrome (and every other browser) usable while adult pages are
 * blocked.
 */
object AdultContentDetector {

    private val knownAdultDomains = setOf(
        "pornhub.com", "pornhub.org", "pornhub.net", "pornhubpremium.com",
        "xvideos.com", "xvideos.es", "xnxx.com", "xhamster.com", "xhamster.desi",
        "xhamster2.com", "xhamster3.com", "xhamsterlive.com", "youporn.com",
        "redtube.com", "chaturbate.com", "onlyfans.com", "fansly.com",
        "stripchat.com", "brazzers.com", "spankbang.com", "eporner.com",
        "beeg.com", "motherless.com", "porn.com", "tube8.com", "youjizz.com",
        "txxx.com", "hclips.com", "tnaflix.com", "drtuber.com", "sunporno.com",
        "porntrex.com", "sex.com", "adultfriendfinder.com", "livejasmin.com",
        "cam4.com", "bongacams.com", "camsoda.com", "myfreecams.com",
        "flirt4free.com", "imlive.com", "rule34.xxx", "nhentai.net",
        "e-hentai.org", "hanime.tv", "hentaihaven.xxx", "fakku.net",
        "literotica.com", "asstr.org", "nudevista.com", "theporndude.com",
        "pornmd.com", "thumbzilla.com", "perfectgirls.net", "gotporn.com",
        "sexvid.xxx", "pornone.com", "hqporner.com", "porngo.com", "ixxx.com",
        "4tube.com", "cliphunter.com", "fuq.com", "porn300.com", "anyporn.com",
        "vipporns.com", "upornia.com", "hellporno.com", "definebabe.com",
        "nuvid.com", "pornhat.com", "sexu.com", "pornzog.com", "xcafe.com",
        "pornoxo.com", "bravotube.net", "empflix.com", "pornhd.com"
    )

    private val adultTlds = setOf("xxx", "porn", "sex", "adult")

    private val strongHostTokens = setOf(
        "porn", "xxx", "nsfw", "hentai", "erotic", "onlyfans", "fansly",
        "chaturbate", "stripchat", "livejasmin", "bongacams", "camsoda",
        "myfreecams", "adultfriendfinder", "brazzers", "spankbang", "eporner",
        "beeg", "motherless", "redtube", "youporn", "xhamster", "xvideos",
        "xnxx", "pornhub", "rule34", "nhentai", "hanime", "fakku",
        "literotica", "sexcam", "camsex", "nudevista", "thumbzilla", "pornmd"
    )

    private val strongTextTerms = listOf(
        "porn", "pornography", "pornographic", "nsfw", "hentai",
        "erotic", "onlyfans", "fansly", "chaturbate", "stripchat", "camgirl",
        "cam girl", "brazzers", "spankbang", "eporner", "beeg",
        "xvideos", "pornhub", "xnxx", "xhamster", "youporn", "redtube",
        "blowjob", "blow job", "handjob", "hand job", "cumshot", "creampie",
        "orgy", "threesome", "dildo", "bdsm", "milf", "hardcore porn",
        "sex video", "sex videos", "sex cam", "sex chat", "sex tape",
        "adult video", "adult videos", "adult content", "adult site",
        "adult website"
    )

    /**
     * Words that are adult in one context and completely normal in another
     * ("Ford Escort", "nude sculpture", "sex education"). They only block when
     * a clear explicit context is present.
     */
    private val ambiguousTextTerms = listOf("nude", "naked", "escort", "xxx")

    private val xxxContextTerms = listOf(
        "porn", "sex", "adult", "tube", "cam", "cams", "chat", "nude",
        "naked", "nsfw", "hentai", "onlyfans", "escort", "hardcore",
        "video", "videos"
    )

    private val nudeContextTerms = listOf(
        "photo", "photos", "pic", "pics", "image", "images", "video", "videos",
        "girl", "girls", "woman", "women", "teen", "teens", "model", "models",
        "porn", "sex", "xxx", "nsfw", "cam", "cams", "chat", "adult", "explicit",
        "beach", "massage", "escort"
    )

    private val escortContextTerms = listOf(
        "service", "services", "agency", "girl", "girls", "woman", "women",
        "massage", "adult", "call", "companion", "dating"
    )

    private val explicitContextTerms = listOf(
        "video", "videos", "tube", "cam", "cams", "chat", "porn", "nude",
        "naked", "explicit", "hardcore", "milf", "orgy", "escort", "massage",
        "dating", "hookup", "fetish", "bdsm", "anal", "oral", "cum", "orgasm",
        "xxx", "nsfw", "hentai", "stripper", "striptease", "brothel",
        "toy", "toys", "shop", "store", "story", "stories", "position",
        "positions", "movie", "movies"
    )

    private val educationalContextTerms = listOf(
        "education", "educational", "health", "healthy", "therapy", "therapist",
        "abuse", "harassment", "discrimination", "sexism", "biology",
        "reproduction", "reproductive", "consent", "puberty", "anatomy",
        "disease", "infection", "counseling", "counselling", "marriage",
        "equality", "orientation", "identity", "assault", "trafficking"
    )

    private val urlPathStrongTerms = listOf(
        "porn", "nsfw", "hentai", "onlyfans", "chaturbate", "stripchat",
        "xvideos", "pornhub", "xnxx", "xhamster", "youporn", "redtube",
        "brazzers", "spankbang", "eporner", "beeg", "motherless", "sexcam",
        "camsex", "nudevista", "rule34", "nhentai", "hanime", "fakku"
    )

    private val termRegexCache: Map<String, Regex> by lazy {
        (strongTextTerms + ambiguousTextTerms + xxxContextTerms +
                explicitContextTerms + educationalContextTerms +
                nudeContextTerms + escortContextTerms)
            .distinct()
            .associateWith { term ->
                Regex("\\b${Regex.escape(term)}\\b", RegexOption.IGNORE_CASE)
            }
    }

    /**
     * Scans visible page text for explicit adult signals.
     *
     * Ambiguous words such as "sex" only trigger a block when they appear
     * with explicit context (for example "sex video" or "sex chat") and not
     * in an educational / health context.
     */
    fun containsAdultText(text: String): AdultContentMatch {
        if (text.isBlank()) return AdultContentMatch.Clean
        val lower = text.lowercase()

        for (term in strongTextTerms) {
            if (containsTerm(lower, term)) {
                return AdultContentMatch(true, "explicit page term", term)
            }
        }

        val educational = educationalContextTerms.any { containsTerm(lower, it) }

        if (containsTerm(lower, "xxx") && !educational &&
            xxxContextTerms.any { containsTerm(lower, it) }
        ) {
            return AdultContentMatch(true, "explicit context around 'xxx'", "xxx")
        }

        if (containsTerm(lower, "nude") || containsTerm(lower, "naked")) {
            if (!educational && nudeContextTerms.any { containsTerm(lower, it) }) {
                return AdultContentMatch(true, "explicit context around nudity", "nude")
            }
        }

        if (containsTerm(lower, "escort") && escortContextTerms.any { containsTerm(lower, it) }) {
            return AdultContentMatch(true, "escort service context", "escort")
        }

        if (containsTerm(lower, "sex")) {
            val explicit = explicitContextTerms.any { containsTerm(lower, it) }
            if (!educational && explicit) {
                return AdultContentMatch(true, "explicit context around 'sex'", "sex")
            }
        }

        if (containsTerm(lower, "adult")) {
            val adultContentWords = listOf(
                "video", "videos", "content", "site", "website",
                "movie", "movies", "chat", "cam", "cams",
                "toy", "toys", "shop", "store", "game", "games",
                "dating", "massage", "club", "novelty"
            )
            if (!educational && adultContentWords.any { containsTerm(lower, it) }) {
                return AdultContentMatch(true, "adult content phrase", "adult")
            }
        }

        return AdultContentMatch.Clean
    }

    /**
     * Checks a browser address-bar value. Known adult domains, adult TLDs,
     * explicit terms in the URL path and adult search queries are rejected.
     * Browser-internal pages (chrome://, about:, etc.) are always allowed.
     */
    fun isAdultUrl(rawUrl: String): AdultContentMatch {
        val url = rawUrl.trim()
        if (url.isBlank()) return AdultContentMatch.Clean
        if (isInternalBrowserUrl(url)) return AdultContentMatch.Clean

        val lower = url.lowercase()
        val host = extractHost(lower)
        if (host.isNotBlank()) {
            val domainMatch = matchAdultDomain(host)
            if (domainMatch != null) return domainMatch
        }

        val decoded = safeDecode(lower)
        val query = extractQuery(decoded)
        if (query.isNotBlank()) {
            val queryMatch = inspectSearchQuery(query)
            if (queryMatch.isAdult) return queryMatch
        }

        val path = extractPath(decoded)
        if (path.isNotBlank()) {
            for (term in urlPathStrongTerms) {
                if (path.contains(term)) {
                    return AdultContentMatch(true, "adult term in URL path", term)
                }
            }
        }

        return AdultContentMatch.Clean
    }

    /** Browser chrome pages such as settings, history and bookmarks. */
    fun isInternalBrowserUrl(rawUrl: String): Boolean {
        val lower = rawUrl.trim().lowercase()
        return lower.startsWith("chrome://") ||
                lower.startsWith("chrome-search://") ||
                lower.startsWith("chrome-native://") ||
                lower.startsWith("about:") ||
                lower.startsWith("edge://") ||
                lower.startsWith("brave://") ||
                lower.startsWith("firefox://") ||
                lower.startsWith("opera://") ||
                lower.startsWith("vivaldi://") ||
                lower.startsWith("view-source:") ||
                lower.startsWith("data:")
    }

    /** A new-tab / blank page must never be treated as a violation. */
    fun isFreshTabUrl(rawUrl: String): Boolean {
        val lower = rawUrl.trim().lowercase()
        return lower.isBlank() ||
                lower == "new tab" ||
                lower == "search or type url" ||
                lower == "search or type web address" ||
                lower == "search or type a url" ||
                lower.contains("chrome://newtab") ||
                lower.contains("chrome://new-tab-page") ||
                lower.contains("chrome-search://local-ntp") ||
                lower.contains("about:blank") ||
                lower.contains("edge://newtab") ||
                lower.contains("brave://newtab") ||
                lower.contains("firefox://newtab") ||
                lower.contains("opera://startpage")
    }

    /** Recognises the text Chrome exposes for its empty new-tab page. */
    fun isFreshTabText(text: String): Boolean {
        val lower = text.trim().lowercase()
        if (lower.isBlank()) return true
        if (lower == "new tab") return true
        return lower.contains("search or type url") ||
                lower.contains("search or type web address") ||
                lower.contains("search or type a url") ||
                lower.contains("chrome://newtab") ||
                lower.contains("about:blank")
    }

    private fun matchAdultDomain(host: String): AdultContentMatch? {
        if (knownAdultDomains.any { host == it || host.endsWith(".$it") }) {
            return AdultContentMatch(true, "known adult domain", host)
        }

        val tld = host.substringAfterLast('.', "")
        if (tld in adultTlds) {
            return AdultContentMatch(true, "adult top-level domain", tld)
        }

        val parts = host.split('.', '-', '_').filter { it.isNotBlank() }
        for (part in parts) {
            if (part in setOf("sex", "porn", "xxx", "nsfw", "hentai", "nude", "naked", "erotic", "adult")) {
                return AdultContentMatch(true, "adult term in domain", part)
            }
            val token = strongHostTokens.firstOrNull { token ->
                when {
                    token == "porn" || token == "xxx" -> part.contains(token)
                    token.length <= 4 -> part == token
                    else -> part.contains(token)
                }
            }
            if (token != null) {
                return AdultContentMatch(true, "adult domain token", token)
            }
        }
        return null
    }

    private fun inspectSearchQuery(query: String): AdultContentMatch {
        val decoded = safeDecode(query).replace('+', ' ').trim()
        if (decoded.isBlank()) return AdultContentMatch.Clean

        val textMatch = containsAdultText(decoded)
        if (textMatch.isAdult) return textMatch

        val educational = educationalContextTerms.any { containsTerm(decoded, it) }
        if (!educational) {
            val words = decoded.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
            val direct = words.firstOrNull {
                it in setOf("sex", "nude", "naked", "xxx", "porn", "nsfw")
            }
            if (direct != null) {
                return AdultContentMatch(true, "adult search query", direct)
            }
        }

        return AdultContentMatch.Clean
    }

    private fun extractHost(url: String): String {
        return try {
            val normalized = if (url.contains("://")) url else "https://$url"
            URI(normalized).host?.lowercase()?.removePrefix("www.") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractQuery(url: String): String {
        return try {
            val normalized = if (url.contains("://")) url else "https://$url"
            URI(normalized).rawQuery ?: ""
        } catch (_: Exception) {
            url.substringAfter('?', "").substringBefore('#')
        }
    }

    private fun extractPath(url: String): String {
        return try {
            val normalized = if (url.contains("://")) url else "https://$url"
            safeDecode(URI(normalized).path ?: "")
        } catch (_: Exception) {
            ""
        }
    }

    private fun containsTerm(text: String, term: String): Boolean {
        val regex = termRegexCache[term] ?: return false
        return regex.containsMatchIn(text)
    }

    private fun safeDecode(value: String): String {
        return try {
            URLDecoder.decode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }
    }
}
