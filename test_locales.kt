import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration

fun getLocalizedContext(context: android.content.Context): android.content.Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    val config = Configuration(context.resources.configuration)
    if (!locales.isEmpty) {
        config.setLocales(locales)
    }
    return context.createConfigurationContext(config)
}
