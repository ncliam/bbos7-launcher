package com.q25.bbos7.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.q25.bbos7.R
import kotlin.math.abs

/**
 * Tray keo len ba nac kieu BBOS7.
 *
 * Layout: view nay phu toan man hinh va co dung mot child (panel). Panel duoc
 * dich chuyen bang translationY; vung phia tren panel trong suot va — khi tray
 * dang o PEEK — khong tieu thu touch, de luoi icon phia duoi nhan duoc.
 *
 * Tu viet thay vi dung BottomSheetBehavior: khong muon keo ca Material Components
 * vao chi de lay mot hanh vi keo, va bottom sheet chi co hai nac.
 */
class TrayLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class State { PEEK, HALF, FULL }

    var state: State = State.PEEK
        private set

    /** Goi khi tray da dung yen han o mot nac. */
    var onStateSettled: ((State) -> Unit)? = null

    /**
     * Goi ngay khi panel bat dau mo rong (true) hoac vua thu gon xong (false).
     * Tach khoi [onStateSettled] de thanh tab kip hien ra tu luc bat dau keo,
     * nhung chi bien mat khi panel da dong han.
     */
    var onExpandingChanged: ((Boolean) -> Unit)? = null

    private var expanding = false

    /**
     * Tra ve luoi dang hien trong panel. Tray chi gianh quyen keo xuong khi
     * luoi da o dinh, neu khong nguoi dung se khong cuon duoc danh sach app.
     * Dung lambda vi luoi doi moi khi vuot sang tab khac.
     */
    var scrollableChild: (() -> View?)? = null

    private lateinit var panel: View

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
    private val peekHeight = resources.getDimensionPixelSize(R.dimen.tray_peek_height)

    private var velocityTracker: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var downInPanel = false
    private var dragging = false
    private var dragStartTranslation = 0f
    private var animator: ValueAnimator? = null
    private var laidOut = false

    override fun onFinishInflate() {
        super.onFinishInflate()
        check(childCount == 1) { "TrayLayout can dung mot child lam panel" }
        panel = getChildAt(0)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!laidOut || changed) {
            laidOut = true
            if (animator == null && !dragging) {
                panel.translationY = translationFor(state)
            }
        }
    }

    private fun translationFor(target: State): Float = when (target) {
        State.FULL -> 0f
        State.HALF -> height * HALF_FRACTION
        State.PEEK -> (height - peekHeight).toFloat()
    }

    private fun isInPanel(ev: MotionEvent): Boolean = ev.y >= panel.top + panel.translationY

    // --- Touch ------------------------------------------------------------

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                downInPanel = isInPanel(ev)
                dragging = false
                animator?.cancel()
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!downInPanel) return false
                velocityTracker?.addMovement(ev)
                val dy = ev.y - downY
                if (abs(dy) > touchSlop && abs(dy) > abs(ev.x - downX) && canDrag(dy)) {
                    beginDrag()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> releaseTracker()
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Cham vao vung trong suot phia tren panel.
        if (!downInPanel) {
            // O PEEK thi nhuong su kien cho luoi icon phia duoi.
            if (state == State.PEEK) return false
            if (ev.actionMasked == MotionEvent.ACTION_UP) animateTo(State.PEEK)
            return true
        }

        velocityTracker?.addMovement(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true

            MotionEvent.ACTION_MOVE -> {
                val dy = ev.y - downY
                if (!dragging) {
                    if (abs(dy) > touchSlop && abs(dy) > abs(ev.x - downX) && canDrag(dy)) {
                        beginDrag()
                    } else {
                        return true
                    }
                }
                val max = (height - peekHeight).toFloat()
                panel.translationY = (dragStartTranslation + (ev.y - downY)).coerceIn(0f, max)
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    val tracker = velocityTracker
                    tracker?.computeCurrentVelocity(1000, maxFlingVelocity)
                    settle(tracker?.yVelocity ?: 0f)
                } else if (abs(ev.y - downY) < touchSlop && state == State.PEEK) {
                    // Cham nhe vao tay cam khi dang thu gon => mo tray.
                    animateTo(State.HALF)
                }
                releaseTracker()
                dragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (dragging) settle(0f)
                releaseTracker()
                dragging = false
                return true
            }
        }
        return true
    }

    /**
     * dy > 0 la keo xuong (dong tray), dy < 0 la keo len (mo tray).
     * Khi da FULL, keo len phai nhuong cho danh sach cuon.
     */
    private fun canDrag(dy: Float): Boolean {
        val atTop = scrollableChild?.invoke()?.canScrollVertically(-1) != true
        return when {
            dy > 0 -> atTop
            else -> state != State.FULL
        }
    }

    private fun beginDrag() {
        dragging = true
        dragStartTranslation = panel.translationY
        parent?.requestDisallowInterceptTouchEvent(true)
        notifyExpanding(true)
    }

    private fun notifyExpanding(value: Boolean) {
        if (expanding == value) return
        expanding = value
        onExpandingChanged?.invoke(value)
    }

    private fun releaseTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun settle(yVelocity: Float) {
        val current = panel.translationY
        val nearest = nearestState(current)
        val target = when {
            yVelocity < -FLING_THRESHOLD -> nextUp(nearest, current)
            yVelocity > FLING_THRESHOLD -> nextDown(nearest, current)
            else -> nearest
        }
        animateTo(target)
    }

    private fun nearestState(translation: Float): State =
        State.entries.minBy { abs(translationFor(it) - translation) }

    private fun nextUp(nearest: State, current: Float): State {
        // Vuot len: nhay len mot nac so voi nac dang o gan nhat phia duoi.
        val above = State.entries
            .filter { translationFor(it) < current - 1f }
            .maxByOrNull { translationFor(it) }
        return above ?: nearest
    }

    private fun nextDown(nearest: State, current: Float): State {
        val below = State.entries
            .filter { translationFor(it) > current + 1f }
            .minByOrNull { translationFor(it) }
        return below ?: nearest
    }

    fun animateTo(target: State) {
        animator?.cancel()
        val from = panel.translationY
        val to = translationFor(target)
        state = target
        // Mo rong thi bao ngay; thu gon thi doi den khi dong han, neu khong
        // thanh tab bien mat giua chung lam noi dung nhay len.
        if (target != State.PEEK) notifyExpanding(true)
        if (from == to) {
            if (target == State.PEEK) notifyExpanding(false)
            onStateSettled?.invoke(target)
            return
        }
        animator = ValueAnimator.ofFloat(from, to).apply {
            duration = SETTLE_DURATION
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { panel.translationY = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animator = null
                    if (target == State.PEEK) notifyExpanding(false)
                    onStateSettled?.invoke(target)
                }
            })
            start()
        }
    }

    /** Gap lai neu dang mo, mo het neu dang thu gon. */
    fun toggle() {
        animateTo(if (state == State.PEEK) State.FULL else State.PEEK)
    }

    /** Tra ve true neu da xu ly (tray dang mo va vua duoc thu gon). */
    fun collapseIfOpen(): Boolean {
        if (state == State.PEEK) return false
        animateTo(State.PEEK)
        return true
    }

    private companion object {
        const val HALF_FRACTION = 0.45f
        const val FLING_THRESHOLD = 800f
        const val SETTLE_DURATION = 220L
    }
}
