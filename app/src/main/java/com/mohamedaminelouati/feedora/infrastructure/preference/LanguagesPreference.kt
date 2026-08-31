package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.languages
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put
import java.util.Locale

val LocalLanguages = compositionLocalOf<LanguagesPreference> { LanguagesPreference.default }

sealed class LanguagesPreference(val value: Int) : Preference() {
    data object UseDeviceLanguages : LanguagesPreference(0)
    data object English : LanguagesPreference(1)
    data object ChineseSimplified : LanguagesPreference(2)
    data object German : LanguagesPreference(3)
    data object French : LanguagesPreference(4)
    data object Czech : LanguagesPreference(5)
    data object Italian : LanguagesPreference(6)
    data object Hindi : LanguagesPreference(7)
    data object Spanish : LanguagesPreference(8)
    data object Polish : LanguagesPreference(9)
    data object Russian : LanguagesPreference(10)
    data object Basque : LanguagesPreference(11)
    data object Indonesian : LanguagesPreference(12)
    data object ChineseTraditional : LanguagesPreference(13)
    data object Arabic : LanguagesPreference(14)
    data object Bulgarian : LanguagesPreference(15)
    data object Catalan : LanguagesPreference(16)
    data object Danish : LanguagesPreference(17)
    data object Dutch : LanguagesPreference(18)
    data object Esperanto : LanguagesPreference(19)
    data object Filipino : LanguagesPreference(20)
    data object Hebrew : LanguagesPreference(21)
    data object Hungarian : LanguagesPreference(22)
    data object Japanese : LanguagesPreference(23)
    data object Kannada : LanguagesPreference(24)
    data object NorwegianBokmal : LanguagesPreference(25)
    data object Persian : LanguagesPreference(26)
    data object Portuguese : LanguagesPreference(27)
    data object PortugueseBrazil : LanguagesPreference(28)
    data object Romanian : LanguagesPreference(29)
    data object Serbian : LanguagesPreference(30)
    data object Slovenian : LanguagesPreference(31)
    data object Swedish : LanguagesPreference(32)
    data object Turkish : LanguagesPreference(33)
    data object Ukrainian : LanguagesPreference(34)
    data object Vietnamese : LanguagesPreference(35)
    data object ArabicNorthLevantine : LanguagesPreference(36)
    data object Estonian : LanguagesPreference(37)
    data object Galician : LanguagesPreference(38)
    data object Slovak : LanguagesPreference(39)
    data object Tamil : LanguagesPreference(40)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.languages, value
            )
            scope.launch(Dispatchers.Main) { setLocale(this@LanguagesPreference) }
        }
    }

    @Composable
    fun toDesc(): String {
        val name = when (this) {
            ChineseTraditional -> stringResource(id = R.string.chinese_traditional)
            ChineseSimplified -> stringResource(id = R.string.chinese_simplified)
            else -> {
                this.toLocale().toDisplayName()
            }
        }
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    fun toLocale(): Locale? = when (this) {
        UseDeviceLanguages -> null
        English -> Locale("en")
        ChineseSimplified -> Locale.forLanguageTag("zh-Hans")
        German -> Locale("de")
        French -> Locale("fr")
        Czech -> Locale("cs")
        Italian -> Locale("it")
        Hindi -> Locale("hi")
        Spanish -> Locale("es")
        Polish -> Locale("pl")
        Russian -> Locale("ru")
        Basque -> Locale("eu")
        Indonesian -> Locale("in")
        ChineseTraditional -> Locale.forLanguageTag("zh-Hant")
        Arabic -> Locale("ar")
        Bulgarian -> Locale("bg")
        Catalan -> Locale("ca")
        Danish -> Locale("da")
        Dutch -> Locale("nl")
        Esperanto -> Locale("eo")
        Filipino -> Locale("fil")
        Hebrew -> Locale("he")
        Hungarian -> Locale("hu")
        Japanese -> Locale("ja")
        Kannada -> Locale("kn")
        NorwegianBokmal -> Locale("nb")
        Persian -> Locale("fa")
        Portuguese -> Locale("pt")
        PortugueseBrazil -> Locale("pt", "BR")
        Romanian -> Locale("ro")
        Serbian -> Locale("sr")
        Slovenian -> Locale("sl")
        Swedish -> Locale("sv")
        Turkish -> Locale("tr")
        Ukrainian -> Locale("uk")
        Vietnamese -> Locale("vi")
        ArabicNorthLevantine -> Locale.forLanguageTag("apc")
        Estonian -> Locale("et")
        Galician -> Locale("gl")
        Slovak -> Locale("sk")
        Tamil -> Locale("ta")
    }

    private fun toLocaleList(): LocaleListCompat =
        toLocale()?.let { LocaleListCompat.create(it) } ?: LocaleListCompat.getEmptyLocaleList()

    companion object {
        val default = UseDeviceLanguages

        private val allLanguages = listOf(
            English,
            ChineseSimplified,
            German,
            French,
            Czech,
            Italian,
            Hindi,
            Spanish,
            Polish,
            Russian,
            Basque,
            Indonesian,
            ChineseTraditional,
            Arabic,
            Bulgarian,
            Catalan,
            Danish,
            Dutch,
            Esperanto,
            Filipino,
            Hebrew,
            Hungarian,
            Japanese,
            Kannada,
            NorwegianBokmal,
            Persian,
            Portuguese,
            PortugueseBrazil,
            Romanian,
            Serbian,
            Slovenian,
            Swedish,
            Turkish,
            Ukrainian,
            Vietnamese,
            ArabicNorthLevantine,
            Estonian,
            Galician,
            Slovak,
            Tamil,
        )

        val values: List<LanguagesPreference> by lazy {
            listOf(UseDeviceLanguages) + allLanguages.sortedBy { pref ->
                val loc = pref.toLocale()
                loc?.getDisplayName(loc)?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                } ?: ""
            }
        }

        fun fromPreferences(preferences: Preferences): LanguagesPreference =
            fromValue(preferences[DataStoreKey.keys[languages]?.key as Preferences.Key<Int>] ?: 0)

        fun fromValue(value: Int): LanguagesPreference = when (value) {
            0 -> UseDeviceLanguages
            1 -> English
            2 -> ChineseSimplified
            3 -> German
            4 -> French
            5 -> Czech
            6 -> Italian
            7 -> Hindi
            8 -> Spanish
            9 -> Polish
            10 -> Russian
            11 -> Basque
            12 -> Indonesian
            13 -> ChineseTraditional
            14 -> Arabic
            15 -> Bulgarian
            16 -> Catalan
            17 -> Danish
            18 -> Dutch
            19 -> Esperanto
            20 -> Filipino
            21 -> Hebrew
            22 -> Hungarian
            23 -> Japanese
            24 -> Kannada
            25 -> NorwegianBokmal
            26 -> Persian
            27 -> Portuguese
            28 -> PortugueseBrazil
            29 -> Romanian
            30 -> Serbian
            31 -> Slovenian
            32 -> Swedish
            33 -> Turkish
            34 -> Ukrainian
            35 -> Vietnamese
            36 -> ArabicNorthLevantine
            37 -> Estonian
            38 -> Galician
            39 -> Slovak
            40 -> Tamil
            else -> default
        }

        fun setLocale(preference: LanguagesPreference) {
            AppCompatDelegate.setApplicationLocales(preference.toLocaleList())
        }
    }
}

@Composable
fun Locale?.toDisplayName(): String {
    val name = this?.getDisplayName(this) ?: stringResource(id = R.string.use_device_languages)
    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
