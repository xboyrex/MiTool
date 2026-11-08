package io.github.d4viddf.hyperisland_kit.models

import org.json.JSONObject

data class PicInfo(
    val type: Int = 1,
    val pic: String,
    val loop: Boolean = false,
    val autoplay: Boolean = false,
    val number: Int = 0,
    val contentDescription: String? = null,
    val effectColor: String? = null,
    val effectSrc: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        put("pic", pic)
        put("loop", loop)
        put("autoplay", autoplay)
        put("number", number)
        contentDescription?.let { put("contentDescription", it) }
        effectColor?.let { put("effectColor", it) }
        effectSrc?.let { put("effectSrc", it) }
    }
}

data class TextInfo(
    val title: String,
    val content: String? = null,
    val showHighlightColor: Boolean = false,
    val narrowFont: Boolean? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        content?.let { put("content", it) }
        put("showHighlightColor", showHighlightColor)
        narrowFont?.let { put("narrowFont", it) }
    }
}

data class TimerInfo(
    val timerType: Int = -1,
    val timerWhen: Long = 0L,
    val timerTotal: Long = 0L,
    val timerSystemCurrent: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("timerType", timerType)
        put("timerWhen", timerWhen)
        put("timerTotal", timerTotal)
        put("timerSystemCurrent", timerSystemCurrent)
    }
}

data class ImageTextInfoLeft(
    val type: Int = 1,
    val picInfo: PicInfo? = null,
    val textInfo: TextInfo? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        picInfo?.let { put("picInfo", it.toJson()) }
        textInfo?.let { put("textInfo", it.toJson()) }
    }
}

data class SmallIslandArea(
    val picInfo: PicInfo? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        picInfo?.let { put("picInfo", it.toJson()) }
    }
}

data class BigIslandArea(
    val imageTextInfoLeft: ImageTextInfoLeft? = null,
    val textInfo: TextInfo? = null,
    val picInfo: PicInfo? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        imageTextInfoLeft?.let { put("imageTextInfoLeft", it.toJson()) }
        textInfo?.let { put("textInfo", it.toJson()) }
        picInfo?.let { put("picInfo", it.toJson()) }
    }
}

data class ParamIsland(
    val islandProperty: Int = 1,
    val islandPriority: Int = 2,
    val islandTimeout: Int? = null,
    val islandOrder: Boolean = false,
    val dismissIsland: Boolean = false,
    val maxSize: Boolean = false,
    val needCloseAnimation: Boolean = true,
    val expandedTime: Int? = null,
    val highlightColor: String? = null,
    val bigIslandArea: BigIslandArea? = null,
    val smallIslandArea: SmallIslandArea? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("islandProperty", islandProperty)
        put("islandPriority", islandPriority)
        islandTimeout?.let { put("islandTimeout", it) }
        put("islandOrder", islandOrder)
        put("dismissIsland", dismissIsland)
        put("maxSize", maxSize)
        put("needCloseAnimation", needCloseAnimation)
        expandedTime?.let { put("expandedTime", it) }
        highlightColor?.let { put("highlightColor", it) }
        bigIslandArea?.let { put("bigIslandArea", it.toJson()) }
        smallIslandArea?.let { put("smallIslandArea", it.toJson()) }
    }
}

data class ChatInfo(
    val type: Int = 1,
    val title: String,
    val content: String? = null,
    val picFunction: String? = null,
    val picFunctionDark: String? = null,
    val timerInfo: TimerInfo? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        put("title", title)
        content?.let { put("content", it) }
        picFunction?.let { put("picProfile", it) }
        picFunctionDark?.let { put("picProfileDark", it) }
        timerInfo?.let { put("timerInfo", it.toJson()) }
    }
}
