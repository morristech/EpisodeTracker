package dev.polek.episodetracker.common.preferences.delegates

import com.russhwolf.settings.Settings
import dev.polek.episodetracker.common.logging.log
import kotlin.reflect.KProperty

class BooleanPreferenceDelegate(
    private val settings: Settings,
    private val key: String,
    private val defaultValue: Boolean,
    private val valueChangedCallback: ((Boolean) -> Unit)? = null)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return settings.getBoolean(key, defaultValue)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) {
        log { "$key: $value" }

        if (value != null) {
            settings.putBoolean(key, value)
        } else {
            settings.remove(key)
        }

        valueChangedCallback?.invoke(value ?: defaultValue)
    }
}
