package com.elvo.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.elvo.app.filter.BrowserPageInspector
import com.elvo.app.filter.GovernancePlan
import com.elvo.app.filter.PromptRuleEngine
import com.elvo.app.data.prefs.PreferencesRepository
import com.elvo.app.domain.model.BlockMode
import com.elvo.app.enforce.Enforcer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ElvoAccessibilityService : AccessibilityService() {

    private lateinit var enforcer: Enforcer
    private lateinit var prefsRepo: PreferencesRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentBlockMode: BlockMode = BlockMode.VIDEO
    private var activePromptText: String = ""
    private var currentGovernancePlan: GovernancePlan? = null
    private var blockUninstallEnabled = true
    private var lastBlockTime = 0L
    private var lastBrowserScanTime = 0L
    private var browserScanJob: Job? = null

    companion object {
        private const val TAG = "ELVO_ACCESSIBILITY"
        private const val BLOCK_COOLDOWN_MS = 1200L
        private const val BROWSER_SCAN_INTERVAL_MS = 400L

        private val browserPackageHints = listOf(
            "chrome", "browser", "sbrowser", "firefox", "mozilla", "opera",
            "duckduckgo", "brave", "edge", "emmx", "vivaldi", "yandex",
            "ucmobile", "puffin", "googlequicksearchbox", "webview"
        )
    }

    override fun onCreate() {
        super.onCreate()
        enforcer = Enforcer(this)
        prefsRepo = PreferencesRepository(this)
        Log.i(TAG, "ELVO Accessibility Service initialized")

        // Observe BlockMode
        serviceScope.launch {
            prefsRepo.selectedBlockMode.collectLatest { modeStr ->
                currentBlockMode = try {
                    BlockMode.valueOf(modeStr)
                } catch (_: Exception) {
                    BlockMode.VIDEO
                }
                Log.i(TAG, "Accessibility Service active BlockMode updated to: $currentBlockMode")
            }
        }

        // Observe the active prompt and rebuild the blocking rules from it.
        serviceScope.launch {
            prefsRepo.activePromptText.collectLatest { prompt ->
                activePromptText = prompt
                currentGovernancePlan = PromptRuleEngine.generatePlan(prompt)
                Log.i(TAG, "Accessibility Service rules updated for prompt: \"$prompt\"")
            }
        }

        // Observe uninstall protection toggle
        serviceScope.launch {
            prefsRepo.blockUninstall.collectLatest { enabled ->
                blockUninstallEnabled = enabled
            }
        }

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        // Ignore System UI, Launchers, Dialer & ELVO app itself
        if (packageName == "com.android.systemui" ||
            packageName == "com.android.phone" ||
            packageName == "com.android.dialer" ||
            packageName == applicationContext.packageName ||
            packageName.lowercase().contains("launcher")
        ) {
            return
        }

        // Uninstall Protection check (only when the setting is enabled).
        // Normal APK installs are left alone; only uninstall/device-admin
        // removal screens are intercepted.
        val className = event.className?.toString() ?: ""
        val eventText = event.text?.joinToString(" ")?.lowercase().orEmpty()
        val isUninstallAttempt = className.contains("UninstallerActivity") ||
                className.contains("DeviceAdminAdd") ||
                (packageName.contains("packageinstaller") && eventText.contains("uninstall"))
        if (blockUninstallEnabled && isUninstallAttempt) {
            recordBlock(packageName, "UNINSTALL_GUARD")
            Toast.makeText(this, "ELVO Safeguard: Uninstall guard is active!", Toast.LENGTH_SHORT).show()
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlockTime < BLOCK_COOLDOWN_MS) {
            return
        }

        val eventType = event.eventType
        val isWindowState = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val isContentChange = eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        val isTextChange = eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        val isScroll = eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

        if (!isWindowState && !isContentChange && !isTextChange && !isScroll) {
            return
        }

        val lowerPackage = packageName.lowercase()
        val plan = currentGovernancePlan ?: PromptRuleEngine.generatePlan(activePromptText)
        val withinPromptWindow = plan.activeWindow?.allowsNow() ?: true

        // Browsers are filtered only when the prompt asks for adult filtering,
        // and only within the prompt's time window if one was given.
        if (isBrowserPackage(lowerPackage)) {
            if (isContentChange) return
            if (!plan.filterBrowserAdult || !withinPromptWindow) return
            if (!isWindowState && !isScroll) return
            handleBrowserEvent(packageName, isWindowState, currentTime)
            return
        }

        // App-level checks only need window / text / scroll events.
        if (isContentChange) return

        // Check only the apps named by the prompt (and anything "social media" covers).
        val isSpecificTargetApp = plan.targetAppPackages.contains(packageName) ||
                plan.packageKeywords.any { kw -> kw.length >= 4 && lowerPackage.contains(kw) }

        if (isSpecificTargetApp && withinPromptWindow) {
            Log.w(TAG, "Restricted App Match: $packageName matched prompt policy (\"${plan.rawPrompt}\")! Enforcing $currentBlockMode...")
            lastBlockTime = currentTime
            recordBlock(packageName, "APP_POLICY")
            enforcer.block(this, packageName, currentBlockMode)
        }
    }

    private fun isBrowserPackage(lowerPackage: String): Boolean =
        browserPackageHints.any { lowerPackage.contains(it) }

    /**
     * Window changes are inspected immediately. Content changes and scrolling
     * are throttled, with one guaranteed follow-up scan after the page settles,
     * so a fast-loading page cannot slip through the throttle.
     */
    private fun handleBrowserEvent(packageName: String, isWindowState: Boolean, now: Long) {
        if (isWindowState) {
            browserScanJob?.cancel()
            lastBrowserScanTime = now
            inspectBrowserWindow(packageName)
            return
        }

        val elapsed = now - lastBrowserScanTime
        if (elapsed >= BROWSER_SCAN_INTERVAL_MS) {
            lastBrowserScanTime = now
            inspectBrowserWindow(packageName)
        } else if (browserScanJob?.isActive != true) {
            browserScanJob = serviceScope.launch {
                delay(BROWSER_SCAN_INTERVAL_MS - elapsed)
                lastBrowserScanTime = System.currentTimeMillis()
                inspectBrowserWindow(packageName)
            }
        }
    }

    private fun inspectBrowserWindow(packageName: String) {
        if (System.currentTimeMillis() - lastBlockTime < BLOCK_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return
        val rootPackage = root.packageName?.toString()?.lowercase() ?: packageName.lowercase()
        if (!isBrowserPackage(rootPackage)) return

        val inspection = BrowserPageInspector.inspect(root)
        if (inspection.match.isAdult) {
            Log.w(
                TAG,
                "Unclean page kept from your eyes on $packageName (${inspection.match.reason}: " +
                        "\"${inspection.match.matchedTerm}\") at ${inspection.url}"
            )
            lastBlockTime = System.currentTimeMillis()
            recordBlock(packageName, "ADULT_CONTENT")
            enforcer.block(this, packageName, currentBlockMode)
        }
    }

    private fun recordBlock(packageName: String, verdict: String) {
        Log.d(TAG, "Block event: $verdict on $packageName")
        serviceScope.launch(Dispatchers.IO) {
            runCatching {
                prefsRepo.recordBlockEvent()
            }.onFailure {
                Log.w(TAG, "Unable to record block event: ${it.message}")
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        browserScanJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
