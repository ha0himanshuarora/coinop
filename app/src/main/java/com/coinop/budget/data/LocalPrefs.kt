package com.coinop.budget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.coinop.budget.ui.theme.Appearance
import com.coinop.budget.ui.theme.BackdropStyle
import com.coinop.budget.ui.theme.Face
import com.coinop.budget.ui.theme.FontChoice
import com.coinop.budget.ui.theme.UiStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "coinop_prefs")

private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default

/** On-device cache: appearance (theme etc.), settings mirror and the last FX table. */
class LocalPrefs(private val context: Context) {
    private object K {
        val style = stringPreferencesKey("ui_style")
        val face = stringPreferencesKey("face")
        val theme = stringPreferencesKey("theme")
        val font = stringPreferencesKey("font")
        val textScale = floatPreferencesKey("text_scale")
        val backdrop = stringPreferencesKey("backdrop")
        val backdropStrength = floatPreferencesKey("backdrop_strength")
        val currency = stringPreferencesKey("currency")
        val timezone = stringPreferencesKey("timezone")
        val fxJson = stringPreferencesKey("fx_json")
        val fxTime = longPreferencesKey("fx_time")
    }

    val appearance: Flow<Appearance> = context.store.data.map { p ->
        Appearance(
            uiStyle = enumOr(p[K.style], UiStyle.ARCADIA),
            face = enumOr(p[K.face], Face.SYSTEM),
            themeId = p[K.theme] ?: "arcadia",
            font = enumOr(p[K.font], FontChoice.DEFAULT),
            textScale = p[K.textScale] ?: 1f,
            backdrop = enumOr(p[K.backdrop], BackdropStyle.DOTS),
            backdropStrength = p[K.backdropStrength] ?: 0.4f,
        )
    }

    suspend fun saveAppearance(a: Appearance) {
        context.store.edit {
            it[K.style] = a.uiStyle.name
            it[K.face] = a.face.name
            it[K.theme] = a.themeId
            it[K.font] = a.font.name
            it[K.textScale] = a.textScale
            it[K.backdrop] = a.backdrop.name
            it[K.backdropStrength] = a.backdropStrength
        }
    }

    val settings: Flow<AppSettings> = context.store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            currencyCode = p[K.currency] ?: d.currencyCode,
            timezone = p[K.timezone] ?: d.timezone,
        )
    }

    suspend fun saveSettings(s: AppSettings) {
        context.store.edit {
            it[K.currency] = s.currencyCode
            it[K.timezone] = s.timezone
        }
    }

    suspend fun saveFx(jsonText: String, time: Long) {
        context.store.edit {
            it[K.fxJson] = jsonText
            it[K.fxTime] = time
        }
    }

    suspend fun loadFx(): Pair<String, Long>? {
        val prefs = context.store.data.first()
        val j = prefs[K.fxJson] ?: return null
        return j to (prefs[K.fxTime] ?: 0L)
    }
}
