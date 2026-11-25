package com.elvo.app.filter

import android.view.accessibility.AccessibilityNodeInfo

/**
 * What the browser guard found on the current screen.
 */
data class BrowserInspection(
    val url: String = "",
    val pageText: String = "",
    val match: AdultContentMatch = AdultContentMatch.Clean
)

/**
 * Reads the address bar and the visible page text from an Android browser's
 * accessibility tree and asks [AdultContentDetector] whether the page is adult.
 *
 * Only the URL bar and the page content are inspected. Browser chrome (toolbar,
 * tab strip, menus, omnibox suggestions) is skipped so normal Chrome usage and
 * the new-tab page can never be mistaken for adult content.
 */
object BrowserPageInspector {

    private const val MAX_NODES = 300
    private const val MAX_TEXT_LENGTH = 12_000

    private val urlBarHints = listOf(
        "url_bar", "urlbar", "url_field", "url_view", "omnibox", "omnibar",
        "location_bar", "address_bar", "search_bar", "search_box"
    )

    private val browserChromeHints = listOf(
        "toolbar", "tab_switcher", "tabstrip", "tab_strip", "menu", "button",
        "suggestion", "suggestions", "dropdown", "popup", "search_box",
        "url_bar", "urlbar", "url_field", "url_view", "location_bar",
        "address_bar", "omnibar", "home_button", "new_tab", "progress"
    )

    fun inspect(root: AccessibilityNodeInfo?): BrowserInspection {
        val extracted = extract(root)
        return BrowserInspection(
            url = extracted.urlBarTexts.firstOrNull().orEmpty(),
            pageText = extracted.pageText,
            match = evaluate(extracted.urlBarTexts, extracted.pageUrls, extracted.pageText)
        )
    }

    /**
     * Pure decision function, kept separate from the accessibility tree so it
     * can be unit tested.
     *
     * A fresh new-tab page is always allowed. Otherwise adult URLs win first,
     * then visible adult page text.
     */
    fun evaluate(
        urlBarTexts: List<String>,
        pageUrls: List<String>,
        pageText: String
    ): AdultContentMatch {
        val bars = urlBarTexts.map { it.trim() }.filter { it.isNotBlank() }
        val urls = pageUrls.map { it.trim() }.filter { it.isNotBlank() }

        // A recognised new-tab / internal browser page is always safe.
        if (bars.isNotEmpty() && bars.all {
                AdultContentDetector.isFreshTabUrl(it) || AdultContentDetector.isInternalBrowserUrl(it)
            }
        ) {
            return AdultContentMatch.Clean
        }

        // While the user is still typing a query on a new-tab page, do not
        // block on the partial text. A real URL (with a dot/scheme) is still
        // checked immediately, and the results page is checked when it loads.
        val freshPage = pageText.isNotBlank() && AdultContentDetector.isFreshTabText(pageText)
        val queryLikeBars = bars.isNotEmpty() && bars.all {
            !looksLikeUrl(it) && !AdultContentDetector.isInternalBrowserUrl(it)
        }
        if (freshPage && (bars.isEmpty() || queryLikeBars)) {
            return AdultContentMatch.Clean
        }

        for (url in bars + urls) {
            if (AdultContentDetector.isInternalBrowserUrl(url) ||
                AdultContentDetector.isFreshTabUrl(url)
            ) {
                continue
            }
            val match = AdultContentDetector.isAdultUrl(url)
            if (match.isAdult) return match
        }

        return AdultContentDetector.containsAdultText(pageText)
    }

    private data class Extracted(
        val urlBarTexts: List<String>,
        val pageUrls: List<String>,
        val pageText: String
    )

    private fun extract(root: AccessibilityNodeInfo?): Extracted {
        if (root == null) return Extracted(emptyList(), emptyList(), "")

        val urlBarTexts = LinkedHashSet<String>()
        val pageUrls = LinkedHashSet<String>()
        val pageText = StringBuilder()
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Boolean>>()
        queue.add(root to false)

        var inspected = 0
        while (queue.isNotEmpty() && inspected < MAX_NODES && pageText.length < MAX_TEXT_LENGTH) {
            val (node, insideChromeUi) = queue.removeFirst()
            inspected++

            val viewId = node.viewIdResourceName.orEmpty().lowercase()
            val isUrlBar = urlBarHints.any { viewId.contains(it) } ||
                    (node.className?.toString() == "android.widget.EditText" && node.isEditable)
            val isChromeUi = insideChromeUi || browserChromeHints.any { viewId.contains(it) }

            val text = node.text?.toString()?.trim().orEmpty()
            val description = node.contentDescription?.toString()?.trim().orEmpty()

            if (isUrlBar) {
                if (text.isNotBlank()) urlBarTexts.add(text)
                if (description.isNotBlank() && looksLikeUrl(description)) {
                    urlBarTexts.add(description)
                }
            } else if (!isChromeUi) {
                if (text.isNotBlank()) {
                    if (looksLikeUrl(text)) {
                        pageUrls.add(text)
                    } else {
                        pageText.append(text).append(' ')
                    }
                }
                if (description.isNotBlank() && description != text) {
                    if (looksLikeUrl(description)) {
                        pageUrls.add(description)
                    } else {
                        pageText.append(description).append(' ')
                    }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child -> queue.add(child to isChromeUi) }
            }
        }

        return Extracted(
            urlBarTexts = urlBarTexts.toList(),
            pageUrls = pageUrls.toList(),
            pageText = pageText.toString().trim()
        )
    }

    private fun looksLikeUrl(text: String): Boolean {
        val value = text.trim()
        if (value.isBlank() || value.length > 300 || value.contains(' ')) return false
        val lower = value.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://") ||
            lower.startsWith("www.") || lower.contains("://")
        ) {
            return true
        }
        return Regex(
            "^[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}(:\\d+)?(/[^\\s]*)?$",
            RegexOption.IGNORE_CASE
        ).matches(value)
    }
}
