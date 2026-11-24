package com.oryno.piggy_ledger.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object OnboardingAnswerFormatter {

    fun formatLanguage(langCode: String?): String {
        val normalized = langCode?.trim().orEmpty()
        if (normalized.isBlank()) return "Unknown"
        return when (normalized.lowercase(Locale.US)) {
            "en", "en-us" -> "English ($normalized)"
            "ar", "ar-sa" -> "Arabic ($normalized)"
            "ar-eg" -> "Egyptian Arabic ($normalized)"
            else -> normalized
        }
    }

    fun formatCompletedAt(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(timestampMillis))
    }

    fun sanitizeIdSlug(key: String): String {
        val slug = key.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(32)
        return slug.ifBlank { "answer" }
    }

    fun buildAnswersMap(
        intentQuestion: String,
        intentAnswer: String,
        intensityQuestion: String,
        intensityAnswer: String,
        completedAtMillis: Long,
        relatesToLoans: Boolean? = null,
        loansQuestion: String = "",
        relatesToAccounts: Boolean? = null,
        accountsQuestion: String = "",
        relatesToEmergency: Boolean? = null,
        emergencyQuestion: String = "",
        yesLabel: String = "Yes",
        noLabel: String = "No"
    ): Map<String, String> {
        val map = linkedMapOf(
            "q1: $intentQuestion" to intentAnswer,
            "q2: $intensityQuestion" to intensityAnswer,
            "completed_at" to formatCompletedAt(completedAtMillis)
        )
        relatesToLoans?.let {
            map["q3: $loansQuestion"] = if (it) yesLabel else noLabel
        }
        relatesToAccounts?.let {
            map["q4: $accountsQuestion"] = if (it) yesLabel else noLabel
        }
        relatesToEmergency?.let {
            map["q5: $emergencyQuestion"] = if (it) yesLabel else noLabel
        }
        return map
    }
}
