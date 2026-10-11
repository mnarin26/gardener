package com.mustafa.gardener.modules.vision

import android.content.Context

/**
 * Session + SharedPreferences knobs for the YOLOv8n smoke-trial UI
 * (confidence threshold + which COCO classes to keep).
 */
object DetectTrialSettings {
    const val DEFAULT_CONF = 0.25f
    const val MIN_CONF = 0.15f
    const val MAX_CONF = 0.70f

    /** Compact checklist for the trial panel (COCO English names). */
    val CHECKLIST: List<String> = listOf(
        "person", "car", "bicycle", "dog", "cat", "chair", "bottle",
    )

    /** Common outdoor / street preset — same set as [CHECKLIST] for this trial. */
    val OUTDOOR_PRESET: Set<String> = CHECKLIST.toSet()

    private const val PREFS = "yolo_detect_trial"
    private const val KEY_CONF = "conf_threshold"
    private const val KEY_MODE = "class_mode"
    private const val KEY_CUSTOM = "custom_classes"

    private const val MODE_ALL = "all"
    private const val MODE_OUTDOOR = "outdoor"
    private const val MODE_CUSTOM = "custom"

    sealed class ClassFilter {
        data object All : ClassFilter()
        data object Outdoor : ClassFilter()
        data class Custom(val names: Set<String>) : ClassFilter()

        /** null = show every COCO class; otherwise keep only these indices. */
        fun allowedIndices(): Set<Int>? = when (this) {
            All -> null
            Outdoor -> OUTDOOR_PRESET.mapNotNull { CocoLabels.ALL.indexOf(it).takeIf { i -> i >= 0 } }
                .toSet()
            is Custom -> names.mapNotNull { CocoLabels.ALL.indexOf(it).takeIf { i -> i >= 0 } }
                .toSet()
        }
    }

    fun clampConf(value: Float): Float = value.coerceIn(MIN_CONF, MAX_CONF)

    fun loadConf(context: Context): Float {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_CONF, DEFAULT_CONF)
        return clampConf(raw)
    }

    fun saveConf(context: Context, value: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_CONF, clampConf(value))
            .apply()
    }

    fun loadClassFilter(context: Context): ClassFilter {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return when (prefs.getString(KEY_MODE, MODE_OUTDOOR) ?: MODE_OUTDOOR) {
            MODE_ALL -> ClassFilter.All
            MODE_CUSTOM -> {
                val raw = prefs.getString(KEY_CUSTOM, "") ?: ""
                val names = raw.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it in CocoLabels.ALL }
                    .toSet()
                if (names.isEmpty()) ClassFilter.Outdoor else ClassFilter.Custom(names)
            }
            else -> ClassFilter.Outdoor
        }
    }

    fun saveClassFilter(context: Context, filter: ClassFilter) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        when (filter) {
            ClassFilter.All -> {
                prefs.putString(KEY_MODE, MODE_ALL)
                prefs.remove(KEY_CUSTOM)
            }
            ClassFilter.Outdoor -> {
                prefs.putString(KEY_MODE, MODE_OUTDOOR)
                prefs.remove(KEY_CUSTOM)
            }
            is ClassFilter.Custom -> {
                prefs.putString(KEY_MODE, MODE_CUSTOM)
                prefs.putString(KEY_CUSTOM, filter.names.sorted().joinToString(","))
            }
        }
        prefs.apply()
    }

    fun turkishChecklistLabel(english: String): String {
        val tr = when (english) {
            "person" -> "kişi"
            "car" -> "araba"
            "bicycle" -> "bisiklet"
            "dog" -> "köpek"
            "cat" -> "kedi"
            "chair" -> "sandalye"
            "bottle" -> "şişe"
            else -> english
        }
        return "$tr ($english)"
    }
}
