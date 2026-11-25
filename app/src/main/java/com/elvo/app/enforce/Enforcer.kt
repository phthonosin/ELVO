package com.elvo.app.enforce

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.widget.Toast
import com.elvo.app.domain.model.BlockMode

class Enforcer(private val context: Context) {
    private val overlayController = OverlayController(context)

    fun block(service: AccessibilityService, packageName: String, mode: BlockMode) {
        // Perform instant background home action (0ms latency)
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

        when (mode) {
            BlockMode.NORMAL -> {
                Toast.makeText(context, "ELVO: $packageName was kept from your eyes", Toast.LENGTH_SHORT).show()
            }
            BlockMode.VIDEO -> {
                overlayController.showRickrollVideoOverlay {
                    // Video finished or double-tapped
                }
            }
            BlockMode.REFLECTION -> {
                overlayController.showReflectionOverlay {
                    // Reflection finished
                }
            }
        }
    }
}
