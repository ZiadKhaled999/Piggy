package com.oryno.piggy_ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingAnswerFormatterTest {

    @Test
    fun formatLanguage_producesReadableLanguageLabels() {
        assertEquals("English (en)", OnboardingAnswerFormatter.formatLanguage("en"))
        assertEquals("Arabic (ar)", OnboardingAnswerFormatter.formatLanguage("ar"))
        assertEquals("Egyptian Arabic (ar-EG)", OnboardingAnswerFormatter.formatLanguage("ar-EG"))
        assertEquals("Unknown", OnboardingAnswerFormatter.formatLanguage(""))
        assertEquals("Unknown", OnboardingAnswerFormatter.formatLanguage(null))
    }

    @Test
    fun formatCompletedAt_producesReadableUtcTimestamp() {
        val formatted = OnboardingAnswerFormatter.formatCompletedAt(1_793_363_404_739L)
        assertTrue(
            "Expected yyyy-MM-dd HH:mm:ss UTC, got: $formatted",
            Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} UTC$""").matches(formatted)
        )
    }

    @Test
    fun sanitizeIdSlug_stripsQuotesSpacesAndNonAscii() {
        val englishSlug = OnboardingAnswerFormatter.sanitizeIdSlug(
            """q3: "Whenever I get paid, I feel like none of the money is actually mine""""
        )
        assertFalse(englishSlug.contains('"'))
        assertFalse(englishSlug.contains(' '))
        assertTrue(englishSlug.startsWith("q3_"))

        val arabicSlug = OnboardingAnswerFormatter.sanitizeIdSlug(
            "q3: \"أول ما بقبض، بحس إن الفلوس دي مش بتاعتي\""
        )
        assertEquals("q3", arabicSlug)
    }

    @Test
    fun buildAnswersMap_containsOnlyRealOnboardingQuestionsWithoutPiggyMode() {
        val answers = OnboardingAnswerFormatter.buildAnswersMap(
            intentQuestion = "What's your primary focus?",
            intentAnswer = "Personal Piggy Bank",
            intensityQuestion = "Choose your saving intensity",
            intensityAnswer = "Laser Focus",
            completedAtMillis = 1_793_363_404_739L,
            relatesToLoans = true,
            loansQuestion = "\"Whenever I get paid...\"",
            relatesToAccounts = false,
            accountsQuestion = "\"My money is split...\"",
            relatesToEmergency = true,
            emergencyQuestion = "\"Every time I finally build...\"",
            yesLabel = "Yes",
            noLabel = "No"
        )

        assertEquals(6, answers.size)
        assertEquals("Personal Piggy Bank", answers["q1: What's your primary focus?"])
        assertEquals("Laser Focus", answers["q2: Choose your saving intensity"])
        assertEquals("Yes", answers["q3: \"Whenever I get paid...\""])
        assertEquals("No", answers["q4: \"My money is split...\""])
        assertEquals("Yes", answers["q5: \"Every time I finally build...\""])
        assertTrue(answers["completed_at"]!!.endsWith(" UTC"))
        assertFalse(answers.keys.any { it.contains("Saving mode", ignoreCase = true) })
        assertFalse(answers.values.any { it.contains("piggy ledger", ignoreCase = true) || it.equals("piggy", ignoreCase = true) })
    }
}
