package dev.polek.episodetracker.common.utils

fun allNotNull(vararg values: Any?): Boolean {
    for (value in values) {
        value ?: return false
    }
    return true
}

fun anyNotNull(vararg values: Any?): Boolean {
    for (value in values) {
        if (value != null) return true
    }
    return false
}
