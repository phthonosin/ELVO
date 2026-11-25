package com.elvo.app.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultContentDetectorTest {

    // ---------------------------------------------------------------
    // URL detection
    // ---------------------------------------------------------------

    @Test
    fun `known adult domain is blocked`() {
        assertTrue(AdultContentDetector.isAdultUrl("https://www.pornhub.com/watch/123").isAdult)
        assertTrue(AdultContentDetector.isAdultUrl("https://xvideos.com").isAdult)
        assertTrue(AdultContentDetector.isAdultUrl("https://onlyfans.com/user").isAdult)
    }

    @Test
    fun `adult top level domain is blocked`() {
        assertTrue(AdultContentDetector.isAdultUrl("https://example.xxx").isAdult)
        assertTrue(AdultContentDetector.isAdultUrl("https://example.porn").isAdult)
    }

    @Test
    fun `adult token in host is blocked`() {
        assertTrue(AdultContentDetector.isAdultUrl("https://my-porn-site.com").isAdult)
        assertTrue(AdultContentDetector.isAdultUrl("https://pornography.com").isAdult)
        assertTrue(AdultContentDetector.isAdultUrl("https://freehentai.example.com").isAdult)
    }

    @Test
    fun `normal domains containing sex are allowed`() {
        assertFalse(AdultContentDetector.isAdultUrl("https://sussex.gov.uk").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("https://www.essex.ac.uk").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("https://www.middlesex.gov.uk").isAdult)
    }

    @Test
    fun `normal sites are allowed`() {
        assertFalse(AdultContentDetector.isAdultUrl("https://www.google.com").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("https://www.youtube.com/shorts/abc").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("https://www.instagram.com/reels/").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("https://en.wikipedia.org/wiki/Chrome").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("https://www.reddit.com/r/sex/").isAdult)
    }

    @Test
    fun `browser internal pages are always allowed`() {
        assertFalse(AdultContentDetector.isAdultUrl("chrome://settings").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("chrome://newtab").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("about:blank").isAdult)
        assertFalse(AdultContentDetector.isAdultUrl("edge://history").isAdult)
        assertTrue(AdultContentDetector.isInternalBrowserUrl("chrome://settings"))
    }

    @Test
    fun `adult search query is blocked`() {
        assertTrue(
            AdultContentDetector.isAdultUrl("https://www.google.com/search?q=porn").isAdult
        )
        assertTrue(
            AdultContentDetector.isAdultUrl("https://duckduckgo.com/?q=nude+girls").isAdult
        )
    }

    @Test
    fun `sex education search is allowed`() {
        assertFalse(
            AdultContentDetector.isAdultUrl("https://www.google.com/search?q=sex+education").isAdult
        )
        assertFalse(
            AdultContentDetector.isAdultUrl("https://www.google.com/search?q=adult+education").isAdult
        )
    }

    @Test
    fun `adult url inside redirect query is blocked`() {
        assertTrue(
            AdultContentDetector.isAdultUrl(
                "https://www.google.com/url?q=https://pornhub.com/watch"
            ).isAdult
        )
    }

    // ---------------------------------------------------------------
    // Visible page text
    // ---------------------------------------------------------------

    @Test
    fun `explicit page text is blocked`() {
        assertTrue(AdultContentDetector.containsAdultText("Watch free porn videos now").isAdult)
        assertTrue(AdultContentDetector.containsAdultText("Sex video collection").isAdult)
        assertTrue(AdultContentDetector.containsAdultText("Adult videos").isAdult)
        assertTrue(AdultContentDetector.containsAdultText("Nude girls video").isAdult)
        assertTrue(AdultContentDetector.containsAdultText("Escort service in London").isAdult)
    }

    @Test
    fun `normal social media text is allowed`() {
        assertFalse(AdultContentDetector.containsAdultText("Instagram reels and explore").isAdult)
        assertFalse(AdultContentDetector.containsAdultText("YouTube Shorts").isAdult)
        assertFalse(AdultContentDetector.containsAdultText("Recommended for you").isAdult)
    }

    @Test
    fun `sex education and health text is allowed`() {
        assertFalse(AdultContentDetector.containsAdultText("Sex education for teenagers").isAdult)
        assertFalse(AdultContentDetector.containsAdultText("Sexual health and consent").isAdult)
        assertFalse(AdultContentDetector.containsAdultText("Sex therapy and counselling").isAdult)
        assertFalse(AdultContentDetector.containsAdultText("Adult education classes").isAdult)
        assertFalse(AdultContentDetector.containsAdultText("Adult education website").isAdult)
    }

    @Test
    fun `ambiguous words without context are allowed`() {
        assertFalse(AdultContentDetector.containsAdultText("Ford Escort review").isAdult)
        assertFalse(AdultContentDetector.containsAdultText("Nude sculpture at the museum").isAdult)
        assertFalse(AdultContentDetector.containsAdultText("XXX: Return of Xander Cage").isAdult)
    }

    @Test
    fun `ambiguous words with explicit context are blocked`() {
        assertTrue(AdultContentDetector.containsAdultText("XXX videos").isAdult)
        assertTrue(AdultContentDetector.containsAdultText("Sex toys shop").isAdult)
    }

    // ---------------------------------------------------------------
    // Browser page inspector decisions
    // ---------------------------------------------------------------

    @Test
    fun `fresh tab with normal shortcuts is allowed`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = listOf("Search or type URL"),
            pageUrls = emptyList(),
            pageText = "Instagram YouTube Gmail Chrome"
        )
        assertFalse(match.isAdult)
    }

    @Test
    fun `blank address bar with fresh tab text is allowed`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = emptyList(),
            pageUrls = emptyList(),
            pageText = "Search or type web address"
        )
        assertFalse(match.isAdult)
    }

    @Test
    fun `adult url is blocked even with clean page text`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = listOf("https://www.pornhub.com"),
            pageUrls = emptyList(),
            pageText = "Home page"
        )
        assertTrue(match.isAdult)
    }

    @Test
    fun `explicit page text on clean url is blocked`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = listOf("https://www.example.com"),
            pageUrls = emptyList(),
            pageText = "Free porn videos"
        )
        assertTrue(match.isAdult)
    }

    @Test
    fun `adult link on the page is blocked`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = listOf("https://www.example.com"),
            pageUrls = listOf("https://pornhub.com/watch"),
            pageText = "A normal article"
        )
        assertTrue(match.isAdult)
    }

    @Test
    fun `missing address bar still blocks explicit page text`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = emptyList(),
            pageUrls = emptyList(),
            pageText = "Free porn videos"
        )
        assertTrue(match.isAdult)
    }

    @Test
    fun `normal chrome page is allowed`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = listOf("https://www.google.com"),
            pageUrls = emptyList(),
            pageText = "Google Search I'm Feeling Lucky"
        )
        assertFalse(match.isAdult)
    }

    @Test
    fun `typing a query on a fresh tab is not blocked`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = listOf("porn"),
            pageUrls = emptyList(),
            pageText = "Search or type URL"
        )
        assertFalse(match.isAdult)
    }

    @Test
    fun `typing an adult url on a fresh tab is blocked`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = listOf("pornhub.com"),
            pageUrls = emptyList(),
            pageText = "Search or type URL"
        )
        assertTrue(match.isAdult)
    }

    @Test
    fun `blank page text does not hide an adult address`() {
        val match = BrowserPageInspector.evaluate(
            urlBarTexts = listOf("porn"),
            pageUrls = emptyList(),
            pageText = ""
        )
        assertTrue(match.isAdult)
    }
}
