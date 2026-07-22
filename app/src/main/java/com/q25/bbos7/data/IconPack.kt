package com.q25.bbos7.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.q25.bbos7.R

/**
 * Bien icon Android thanh icon BBOS7.
 *
 * Hai duong: app nao co icon BBOS7 ve rieng thi dung thang icon do; app nao khong co thi lay icon
 * goc dat len de BBOS7 (mieng gradient xam xanh trich tu chinh theme goc). Khong the "bien doi"
 * icon Android thanh chat BBOS7 bang thuat toan — chung khac han ve ngon ngu thiet ke — nen viec
 * de lam la khien chung dong bo khi dung canh nhau trong luoi.
 */
object IconPack {

    /**
     * Anh xa package -> icon BBOS7.
     *
     * Chi thay icon cho app CHUC NANG HE THONG ma BBOS7 co thu tuong duong mot doi mot. Moi app
     * co logo nhan dien rieng thi giu icon goc dat len de — ke ca app Google (Chrome, Gmail,
     * Maps, Photos, Keep, Play Games) chu khong rieng gi Zalo hay Facebook: trong luoi 6 cot
     * khong nhan, mau va hinh logo la thu duy nhat de phan biet chung.
     */
    private val BY_PACKAGE: Map<String, Int> = mapOf(
        "com.google.android.dialer" to R.drawable.bb_phone,
        "com.google.android.apps.messaging" to R.drawable.bb_messages,
        "com.google.android.contacts" to R.drawable.bb_contacts,
        "com.android.settings" to R.drawable.bb_settings,
        "com.mediatek.camera" to R.drawable.bb_camera,
        "com.google.android.deskclock" to R.drawable.bb_clock,
        "com.google.android.calculator" to R.drawable.bb_calculator,
        "com.google.android.calendar" to R.drawable.bb_calendar,
        "com.android.soundrecorder" to R.drawable.bb_recorder,
        "com.android.fmradio" to R.drawable.bb_radio,
        "com.q25.unlockhelper" to R.drawable.bb_blackberry,
        "com.android.stk" to R.drawable.bb_sim,
    )

    /** Cho app co nhieu launcher activity, khi tung activity can icon khac nhau. */
    private val BY_COMPONENT: Map<String, Int> = emptyMap()

    /** Icon cua ca may cong lai van nho hon 4MB, nhung gioi han cho chac. */
    private val cache = LruCache<String, Drawable>(200)

    fun iconFor(context: Context, component: ComponentName, original: Drawable): Drawable {
        val key = component.flattenToShortString()
        cache.get(key)?.let { return it }

        val mapped = BY_COMPONENT[key] ?: BY_PACKAGE[component.packageName]
        val icon = if (mapped != null) {
            context.getDrawable(mapped) ?: original
        } else {
            onTile(context, original)
        }
        cache.put(key, icon)
        return icon
    }

    /** Xoa cache khi danh sach app doi (go/cai lai app co the doi icon). */
    fun clear() = cache.evictAll()

    // --- De fallback -------------------------------------------------------

    /**
     * Bien icon goc thanh mot khoi BBOS7, theo mot trong hai cach tuy hinh dang icon:
     *
     * - Icon co nen trong suot (Chrome, Dien thoai, Tin nhan): dat len de gradient. Day la cach
     *   BBOS7 that lam voi app ben thu ba.
     * - Icon nen dac kin khung (nhieu app may anh): dat len de thi no phu gan kin, de chi con la
     *   vien mong va mat het chieu sau. Voi loai nay thi cat theo dung hinh bo goc cua BBOS7 roi
     *   phu mot lop gloss nhe — giu mau nhan dien cua app nhung dang khoi thi giong het cac icon
     *   con lai.
     *
     * Mask (o 2 trong bo asset) va phan dac cua de (o 14) trung khit nhau — cung la khoi
     * (16,15)-(77,77) trong khung 92 — nen ca hai duong deu ra khoi cung kich thuoc, cung vi tri.
     */
    private fun onTile(context: Context, original: Drawable): Drawable {
        val base = context.getDrawable(R.drawable.bb_tile_base) as? BitmapDrawable
            ?: return original
        val size = base.bitmap.width
        val content = renderContent(original, size) ?: return base

        val inner = if (hasOpaqueCorners(content)) {
            // Icon nen dac ma cat mask cho chiem ca khoi thi mau sang phu kin, trong khi moi o
            // khac la khoi toi co hinh nho o giua — mot cai nhu the la nhay han ra khoi nhip.
            // Bo goc no roi dat len de nhu cac app khac: moi o cung mot khoi, chi khac phan ruot.
            roundedBlock(context, content, size) ?: content
        } else {
            content
        }
        return onGradientTile(context, base.bitmap, inner, size)
    }

    private fun onGradientTile(
        context: Context,
        base: Bitmap,
        content: Bitmap,
        size: Int,
    ): Drawable {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(base, 0f, 0f, null)

        val inner = (size * INNER_RATIO).toInt()
        val offset = (size - inner) / 2
        canvas.drawBitmap(
            content,
            null,
            Rect(offset, offset, offset + inner, offset + inner),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
        )
        return BitmapDrawable(context.resources, output)
    }

    /**
     * Cat icon theo hinh bo goc BBOS7 va tra ve dung phan khoi do (khong con vien trong xung
     * quanh), de [onGradientTile] dat no vao giua de o dung ty le nhu moi icon khac.
     */
    private fun roundedBlock(context: Context, content: Bitmap, size: Int): Bitmap? {
        val mask = (context.getDrawable(R.drawable.bb_tile_mask) as? BitmapDrawable)?.bitmap
            ?: return null

        val block = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val blockCanvas = Canvas(block)
        blockCanvas.drawBitmap(content, null, Rect(0, 0, size, size), FILTERING)
        blockCanvas.drawBitmap(mask, 0f, 0f, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })
        // Khoi nen trang tinh (Maps, CH Play) van choi hon han phan con lai cua luoi du da thu
        // nho. Phu mot lop toi mong, manh dan theo do sang — mau nhan dien con nguyen, chi bot
        // gat. Icon von da tram (Ky Vuong) thi khong dung toi.
        val excess = averageLuminance(block) - DIM_THRESHOLD
        if (excess > 0f) {
            val alpha = (excess * DIM_STRENGTH).coerceAtMost(DIM_MAX)
            blockCanvas.drawRect(
                0f, 0f, size.toFloat(), size.toFloat(),
                Paint().apply {
                    color = Color.argb((alpha * 255).toInt(), 0, 0, 0)
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                },
            )
        }

        // Gloss chi bam vao cho da co pixel, nen khong can cat lai lan nua.
        blockCanvas.drawRect(
            0f, 0f, size.toFloat(), size * GLOSS_HEIGHT,
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, size * GLOSS_HEIGHT,
                    GLOSS_COLOR, 0x00FFFFFF, Shader.TileMode.CLAMP,
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            },
        )

        // Mask chi chiem khoi (16,15)-(77,77) trong khung 92, xung quanh la vien trong danh cho
        // bong cua de. Cat bo vien do di, neu khong khi thu nho lai thi phan hinh con be ti.
        return Bitmap.createBitmap(block, MASK_LEFT, MASK_TOP, MASK_WIDTH, MASK_HEIGHT)
    }

    /**
     * Voi adaptive icon thi chi lay lop foreground: giu ca hai lop thi Chrome hoa khoi trang, may
     * anh hoa khoi mau — mat sach phan hinh nhan dien. Foreground co vien an rong (vung an toan
     * chi chiem 72/108 khung) nen phai phong [ADAPTIVE_ZOOM] lan, khong thi hinh be tin hin.
     */
    private fun renderContent(original: Drawable, size: Int): Bitmap? = try {
        if (original is AdaptiveIconDrawable) {
            val foreground = original.foreground ?: original
            val zoomed = (size * ADAPTIVE_ZOOM).toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val offset = (size - zoomed) / 2
            foreground.setBounds(offset, offset, offset + zoomed, offset + zoomed)
            foreground.draw(canvas)
            bitmap
        } else {
            original.toBitmap(size, size)
        }
    } catch (e: Exception) {
        // Icon hong hoac drawable la mot thu khong ve duoc ra bitmap.
        null
    }

    /** Do sang trung binh cua phan khoi, bo qua pixel trong suot. 0 = den, 1 = trang. */
    private fun averageLuminance(block: Bitmap): Float {
        var sum = 0f
        var count = 0
        var x = MASK_LEFT
        while (x < MASK_LEFT + MASK_WIDTH) {
            var y = MASK_TOP
            while (y < MASK_TOP + MASK_HEIGHT) {
                val pixel = block.getPixel(x, y)
                if ((pixel ushr 24) > 128) {
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    sum += (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
                    count++
                }
                y += LUMINANCE_STEP
            }
            x += LUMINANCE_STEP
        }
        return if (count == 0) 0f else sum / count
    }

    /** Bon goc deu dac nghia la icon phu kin khung, khong phai hinh tu do tren nen trong. */
    private fun hasOpaqueCorners(bitmap: Bitmap): Boolean {
        val near = (bitmap.width * CORNER_PROBE).toInt()
        val far = bitmap.width - 1 - near
        return listOf(near to near, far to near, near to far, far to far)
            .all { (x, y) -> (bitmap.getPixel(x, y) ushr 24) > OPAQUE_THRESHOLD }
    }

    private val FILTERING = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private const val INNER_RATIO = 0.58f
    private const val ADAPTIVE_ZOOM = 1.5f
    private const val CORNER_PROBE = 0.15f

    /** Khoi dac cua mask trong khung 92 — do duoc tu chinh file asset. */
    private const val MASK_LEFT = 16
    private const val MASK_TOP = 15
    private const val MASK_WIDTH = 62
    private const val MASK_HEIGHT = 63
    private const val OPAQUE_THRESHOLD = 200
    private const val GLOSS_HEIGHT = 0.45f
    private const val GLOSS_COLOR = 0x30FFFFFF

    /** Sang hon nguong nay thi bat dau lam tram; toi da [DIM_MAX] cho khoi trang tinh. */
    private const val DIM_THRESHOLD = 0.55f
    private const val DIM_STRENGTH = 0.6f
    private const val DIM_MAX = 0.18f
    private const val LUMINANCE_STEP = 3
}
