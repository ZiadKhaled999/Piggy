@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.oryno.piggy_ledger.ai

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class SovereignAiResponse(
    @SerialName("thinking_process") val thinkingProcess: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("current_archetype") val currentArchetype: String = "",
    @SerialName("archetype_rationale") val archetypeRationale: String = "",
    @SerialName("ui_blocks") val uiBlocks: List<UiBlock> = emptyList()
)

@Serializable
@JsonClassDiscriminator("type")
sealed class UiBlock {
    @Serializable
    @SerialName("KPI_CARD")
    data class KpiCardBlock(
        val title: String = "",
        @SerialName("primary_value") val primaryValue: String = "",
        @SerialName("sub_value") val subValue: String = "",
        val trend: String = ""
    ) : UiBlock()

    @Serializable
    @SerialName("STREAK_STATUS")
    data class StreakStatusBlock(
        @SerialName("current_streak") val currentStreak: Int = 0,
        @SerialName("streak_freezes_available") val streakFreezesAvailable: Int = 0,
        @SerialName("status_message") val statusMessage: String = ""
    ) : UiBlock()

    @Serializable
    @SerialName("METRIC_GRID")
    data class MetricGridBlock(
        val title: String = "",
        val metrics: List<Metric> = emptyList()
    ) : UiBlock() {
        @Serializable
        data class Metric(val label: String = "", val value: String = "", val status: String = "")
    }

    @Serializable
    @SerialName("INTERACTIVE_CHART")
    data class InteractiveChartBlock(
        @SerialName("chart_type") val chartType: String = "",
        val title: String = "",
        @SerialName("x_axis_label") val xAxisLabel: String = "",
        @SerialName("y_axis_label") val yAxisLabel: String = "",
        @SerialName("data_points") val dataPoints: List<DataPoint> = emptyList()
    ) : UiBlock() {
        @Serializable
        data class DataPoint(val label: String = "", val value: Double = 0.0)
    }

    @Serializable
    @SerialName("REFLECTIVE_POLL")
    data class ReflectivePollBlock(
        @SerialName("target_transaction_id") val targetTransactionId: String = "",
        @SerialName("cool_down_days") val coolDownDays: Int = 0,
        @SerialName("prompt_message") val promptMessage: String = "",
        val options: List<String> = emptyList()
    ) : UiBlock()

    @Serializable
    @SerialName("LEDGER_ITEM")
    data class LedgerItemBlock(
        @SerialName("transaction_id") val transactionId: String = "",
        val merchant: String = "",
        val amount: String = "",
        @SerialName("impact_on_runway") val impactOnRunway: String = "",
        @SerialName("flag_reason") val flagReason: String = ""
    ) : UiBlock()

    @Serializable
    @SerialName("ACTION_BANNER")
    data class ActionBannerBlock(
        val message: String = "",
        @SerialName("action_payload") val actionPayload: String = ""
    ) : UiBlock()

    @Serializable
    @SerialName("HIGHLIGHT_TEXT")
    data class HighlightTextBlock(
        val text: String = "",
        val color: String = "PINK"
    ) : UiBlock()

    @Serializable
    @SerialName("GROUP_BLOCK")
    data class GroupBlock(
        val title: String = "",
        val type: String = "CARD", // CARD or CIRCLE
        val items: List<GroupItem> = emptyList()
    ) : UiBlock() {
        @Serializable
        data class GroupItem(val title: String = "", val value: String = "")
    }
}

// Groq API Models
@Serializable
data class ChatMessageRequest(
    val role: String,
    val content: String
)

@Serializable
data class GroqRequest(
    val model: String = "qwen/qwen3.6-27b",
    val messages: List<ChatMessageRequest>,
    val temperature: Double? = 0.6,
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int? = 2048,
    @SerialName("top_p") val topP: Double? = 0.95,
    val stream: Boolean = true,
    @SerialName("reasoning_effort") val reasoningEffort: String? = "default",
    val stop: String? = null
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class GroqResponse(
    val id: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: ChatMessageResponse
)

@Serializable
data class ChatMessageResponse(
    val role: String,
    val content: String
)
