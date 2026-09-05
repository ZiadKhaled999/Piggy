package com.oryno.piggy_ledger.widget

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.getCurrencySymbol
import java.util.Locale

object WidgetUtils {

    const val ACTION_GOAL_CLICK = "com.oryno.piggy_ledger.widget.ACTION_GOAL_CLICK"
    const val ACTION_GOAL_BACK = "com.oryno.piggy_ledger.widget.ACTION_GOAL_BACK"

    fun getLocalizedContext(context: Context): Context {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val locale = when {
            tag.contains("ar-EG") || tag.contains("ar_EG") -> Locale("ar", "EG")
            tag.startsWith("ar") -> Locale("ar")
            tag.startsWith("en") -> Locale("en")
            else -> {
                val defaultLocale = Locale.getDefault()
                if (defaultLocale.language == "ar") {
                    if (defaultLocale.country.equals("EG", ignoreCase = true)) {
                        Locale("ar", "EG")
                    } else {
                        Locale("ar")
                    }
                } else {
                    defaultLocale
                }
            }
        }
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun getWidgetCurrencySymbol(context: Context, currencyCode: String): String {
        if (currencyCode.equals("EGP", ignoreCase = true)) {
            return try {
                getLocalizedContext(context).getString(R.string.currency_egp)
            } catch (e: Exception) {
                "EGP"
            }
        }
        return getCurrencySymbol(currencyCode)
    }

    fun formatWidgetAmount(amount: Double, currencySymbol: String): String {
        val formatted = String.format(Locale.US, "%,.0f", amount)
        return if (currencySymbol.length <= 1) {
            "$currencySymbol$formatted"
        } else {
            "$formatted $currencySymbol"
        }
    }
}
