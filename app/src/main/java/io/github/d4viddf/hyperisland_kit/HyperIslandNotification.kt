package io.github.d4viddf.hyperisland_kit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import io.github.d4viddf.hyperisland_kit.models.*
import org.json.JSONObject

private const val PIC_PREFIX = "miui.focus.pic_"

data class HyperPicture(
    val key: String,
    val icon: Icon
) {
    constructor(key: String, context: Context, drawableRes: Int) : this(key, Icon.createWithResource(context, drawableRes))
    constructor(key: String, bitmap: Bitmap) : this(key, Icon.createWithBitmap(bitmap))
}

class HyperIslandNotification private constructor(
    private val context: Context,
    private val businessName: String,
    private val ticker: String
) {
    private var chatInfo: ChatInfo? = null
    private var paramIsland: ParamIsland? = null
    private var islandPriority: Int = 2
    private var dismissIsland: Boolean = false
    private var enableFloat: Boolean = true
    private var isShowNotification: Boolean = true
    private var islandFirstFloat: Boolean = false
    private val pictures = mutableListOf<HyperPicture>()

    fun addPicture(picture: HyperPicture) = apply { this.pictures.add(picture) }

    fun setChatInfo(
        title: String,
        content: String? = null,
        pictureKey: String? = null,
        timer: TimerInfo? = null
    ) = apply {
        this.chatInfo = ChatInfo(
            title = title,
            content = content,
            picFunction = pictureKey?.let { PIC_PREFIX + it },
            timerInfo = timer
        )
    }

    fun setSmallIsland(picKey: String) = apply {
        val current = this.paramIsland ?: ParamIsland()
        this.paramIsland = current.copy(
            islandProperty = 1,
            smallIslandArea = SmallIslandArea(picInfo = PicInfo(type = 1, pic = PIC_PREFIX + picKey))
        )
    }

    fun setBigIslandInfo(left: ImageTextInfoLeft? = null) = apply {
        val current = this.paramIsland ?: ParamIsland()
        val fixedLeft = left?.copy(
            picInfo = left.picInfo?.let { it.copy(pic = PIC_PREFIX + it.pic) }
        )
        this.paramIsland = current.copy(
            islandProperty = 1,
            bigIslandArea = BigIslandArea(imageTextInfoLeft = fixedLeft)
        )
    }

    fun setIslandConfig(priority: Int = 2, dismissible: Boolean = false) = apply {
        this.islandPriority = priority
        this.dismissIsland = dismissible
    }

    fun setIslandFirstFloat(enable: Boolean) = apply { this.islandFirstFloat = enable }
    fun setEnableFloat(enable: Boolean) = apply { this.enableFloat = enable }
    fun setShowNotification(show: Boolean) = apply { this.isShowNotification = show }

    fun buildResourceBundle(): Bundle {
        val bundle = Bundle()
        bundle.putBundle("miui.focus.actions", Bundle())
        val picsBundle = Bundle()
        pictures.forEach { picsBundle.putParcelable(PIC_PREFIX + it.key, it.icon) }
        bundle.putBundle("miui.focus.pics", picsBundle)
        return bundle
    }

    fun buildJsonParam(): String {
        val finalIsland = (this.paramIsland ?: ParamIsland()).copy(
            islandPriority = this.islandPriority,
            dismissIsland = this.dismissIsland
        )
        val paramV2 = JSONObject().apply {
            put("protocol", 3)
            put("business", businessName)
            put("updatable", true)
            put("ticker", ticker)
            put("enableFloat", enableFloat)
            put("isShowNotification", isShowNotification)
            put("islandFirstFloat", islandFirstFloat)
            put("param_island", finalIsland.toJson())
            chatInfo?.let { put("chatInfo", it.toJson()) }
        }
        return JSONObject().apply {
            put("param_v2", paramV2)
            put("isShowNotification", isShowNotification)
        }.toString()
    }

    companion object {
        fun Builder(context: Context, businessName: String, ticker: String): HyperIslandNotification =
            HyperIslandNotification(context, businessName, ticker)
    }
}
