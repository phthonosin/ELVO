package com.elvo.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.ui.theme.TextPrimary
import com.elvo.app.ui.theme.TextSecondary

/** Setup uses tighter type; Settings uses the larger in-app reading size. */
enum class TermsTextStyle { Setup, Settings }

private data class TermsEntry(val title: String, val body: String)

private val termsEntries = listOf(
    TermsEntry(
        title = "1. How it works",
        body = "ELVO watches app and browser activity to enforce the rules you set."
    ),
    TermsEntry(
        title = "2. Hardcore mode",
        body = "When Hardcore mode is on, ELVO resists being disabled or uninstalled without your master code."
    ),
    TermsEntry(
        title = "3. Privacy",
        body = "Your prompt, rules, and block history stay on your phone."
    ),
    TermsEntry(
        title = "4. Your code",
        body = "Keep your master code safe. You'll need it to change the prompt or turn off Hardcore mode."
    )
)

/** The same short terms shown in setup and in Settings. */
@Composable
fun TermsContent(style: TermsTextStyle = TermsTextStyle.Setup) {
    Column {
        termsEntries.forEachIndexed { index, entry ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(if (style == TermsTextStyle.Setup) 16.dp else 18.dp))
            }
            TermsParagraph(entry.title, entry.body, style)
        }
    }
}

@Composable
private fun TermsParagraph(title: String, body: String, style: TermsTextStyle) {
    Column {
        Text(
            text = title,
            fontSize = if (style == TermsTextStyle.Setup) 14.5.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = body,
            fontSize = if (style == TermsTextStyle.Setup) 13.5.sp else 14.sp,
            color = TextSecondary,
            lineHeight = if (style == TermsTextStyle.Setup) 20.sp else 21.sp
        )
    }
}
