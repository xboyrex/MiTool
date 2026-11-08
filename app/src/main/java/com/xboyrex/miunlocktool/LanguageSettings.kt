package com.xboyrex.miunlocktool

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xboyrex.miunlocktool.data.TranslationManager
import java.util.Locale

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
)

object LanguageSettings {
    private var prefs: SharedPreferences? = null

    var currentCode by mutableStateOf("auto")
        private set

    var version by mutableIntStateOf(0)
        private set

    val ALL_LANGUAGES: List<LanguageItem> = listOf(
        LanguageItem("auto", "Auto (System)", "Auto (System default)"),
        LanguageItem("en", "English", "English"),
        LanguageItem("hi", "Hindi", "हिन्दी"),
        LanguageItem("ru", "Russian", "Русский"),
        LanguageItem("uk", "Ukrainian", "Українська"),
        LanguageItem("es", "Spanish", "Español"),
        LanguageItem("pt", "Portuguese", "Português"),
        LanguageItem("id", "Indonesian", "Bahasa Indonesia"),
        LanguageItem("zh-CN", "Chinese (Simplified)", "简体中文"),
        LanguageItem("zh-TW", "Chinese (Traditional)", "繁體中文"),
        LanguageItem("ja", "Japanese", "日本語"),
        LanguageItem("ko", "Korean", "한국어"),
        LanguageItem("fr", "French", "Français"),
        LanguageItem("de", "German", "Deutsch"),
        LanguageItem("it", "Italian", "Italiano"),
        LanguageItem("tr", "Turkish", "Türkçe"),
        LanguageItem("vi", "Vietnamese", "Tiếng Việt"),
        LanguageItem("ar", "Arabic", "العربية"),
        LanguageItem("th", "Thai", "ไทย"),
        LanguageItem("pl", "Polish", "Polski"),
        LanguageItem("nl", "Dutch", "Nederlands"),
        LanguageItem("fa", "Persian", "فارسی"),
        LanguageItem("bn", "Bengali", "বাংলা"),
        LanguageItem("mr", "Marathi", "मराठी"),
        LanguageItem("te", "Telugu", "తెలుగు"),
        LanguageItem("ta", "Tamil", "தமிழ்"),
        LanguageItem("gu", "Gujarati", "ગુજરાતી"),
        LanguageItem("kn", "Kannada", "ಕನ್ನಡ"),
        LanguageItem("ml", "Malayalam", "മലയാളം"),
        LanguageItem("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        LanguageItem("ur", "Urdu", "اردو"),
    )

    fun init(context: Context) {
        val p = context.getSharedPreferences("language_settings_xboyrex", Context.MODE_PRIVATE).also { prefs = it }
        currentCode = p.getString("language_code", "auto") ?: "auto"
        applyLocale(context, currentCode)
        TranslationManager.init(context, effectiveLanguageCode())
    }

    fun choose(context: Context, code: String) {
        currentCode = code
        prefs?.edit()?.putString("language_code", code)?.apply()
        applyLocale(context, code)
        TranslationManager.init(context, effectiveLanguageCode())
        version++
    }

    fun currentDisplay(): String {
        if (currentCode == "auto") return "Auto (System)"
        val item = ALL_LANGUAGES.firstOrNull { it.code.equals(currentCode, ignoreCase = true) }
        return item?.let { "${it.nativeName} (${it.name})" } ?: currentCode
    }

    fun effectiveLanguageCode(): String {
        return if (currentCode == "auto") {
            Locale.getDefault().language.lowercase()
        } else {
            currentCode.substringBefore('-').lowercase()
        }
    }

    private fun applyLocale(context: Context, code: String) {
        runCatching {
            val locale = if (code == "auto") {
                LocaleList.getDefault().get(0) ?: Locale.ENGLISH
            } else {
                Locale.forLanguageTag(code)
            }
            Locale.setDefault(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val lm = context.getSystemService(LocaleManager::class.java)
                lm?.applicationLocales = if (code == "auto") LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(code)
            }
        }
    }
}

object I18n {
    private val HI_STRINGS = mapOf(
        "tab_apply" to "Apply",
        "tab_fastboot" to "Fastboot",
        "tab_guide" to "गाइड",
        "tab_settings" to "सेटिंग्स",
        "settings" to "सेटिंग्स",
        "customization" to "कस्टमाइज़ेशन",
        "appearance_theme" to "थीम और दिखावट",
        "appearance_sub" to "AMOLED डार्क मोड, येलो पैलेट और Liquid Glass",
        "general_modification" to "जनरल और अनलॉक सेटिंग्स",
        "general_modification_sub" to "फ़ॉन्ट, NTP सर्वर, टाइमिंग प्रोफाइल और बर्स्ट मोड",
        "language_section" to "भाषा (Language)",
        "language" to "ऐप की भाषा",
        "data_storage" to "डेटा और स्टोरेज",
        "storage_usage" to "स्टोरेज उपयोग",
        "storage_sub" to "फ़ोन स्टोरेज देखें और कैश/लॉग्स साफ़ करें",
        "about" to "के बारे में (About)",
        "version" to "वर्ज़न",
        "up_to_date" to "अप टू डेट",
        "check_updates" to "अपडेट चेक करें",
        "check_updates_sub" to "GitHub से सीधे नया अपडेट इंस्टॉल करें",
        "open_source" to "ओपन सोर्स (GitHub)",
        "open_source_sub" to "GitHub पर पूरा सोर्स कोड देखें",
        "developer" to "डेवलपर",
    )

    private val RU_STRINGS = mapOf(
        "tab_apply" to "Заявка",
        "tab_fastboot" to "Fastboot",
        "tab_guide" to "Гайд",
        "tab_settings" to "Настройки",
        "settings" to "Настройки",
        "customization" to "Персонализация",
        "appearance_theme" to "Тема и внешний вид",
        "appearance_sub" to "Режим темы, палитра цветов и Liquid Glass",
        "general_modification" to "Параметры разблокировки",
        "general_modification_sub" to "Шрифт, NTP-сервер, профили таймингов и серия запросов",
        "language_section" to "Язык",
        "language" to "Язык приложения",
        "data_storage" to "Данные и память",
        "storage_usage" to "Использование памяти",
        "storage_sub" to "Память устройства и очистка кэша/логов",
        "about" to "О приложении",
        "version" to "Версия",
        "up_to_date" to "Актуально",
        "check_updates" to "Проверить обновления",
        "check_updates_sub" to "Установка новых релизов с GitHub внутри приложения",
        "open_source" to "Исходный код",
        "open_source_sub" to "Открыть репозиторий на GitHub",
        "developer" to "Разработчик",
    )

    fun t(key: String, fallback: String): String {
        @Suppress("UNUSED_VARIABLE")
        val v = LanguageSettings.version
        val lang = LanguageSettings.effectiveLanguageCode()
        if (lang == "hi") {
            HI_STRINGS[key]?.let { return it }
        } else if (lang == "ru" || lang == "uk") {
            RU_STRINGS[key]?.let { return it }
        }
        TranslationManager.trOrNull(key)?.let { return it }
        return fallback
    }
}
