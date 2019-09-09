package com.pigeonmessenger.widget

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.pigeonmessenger.R
import com.pigeonmessenger.RxBus
import com.pigeonmessenger.events.ProgressEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.dip
import org.jetbrains.anko.sp

class CircleProgress @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private val bounds = RectF()
    private val fBounds = RectF()

    private var mObjectAnimatorAngle: ObjectAnimator? = null
    private val mPaint: Paint
    private val mTextPaint: Paint
    private val mBackgroundPaint: Paint
    private val mForkPath: Path
    private val mArrowPath: Path

    private var currentGlobalAngle: Float = 0f
        set(currentGlobalAngle) {
            field = currentGlobalAngle
            invalidate()
        }
    private val mBorderWidth: Int
    private var isRunning: Boolean = false

    private var mProgress = 0
    private var mMaxProgress = 100
    private var mTextSize = context.sp(12)
    private val mSize: Int
    private val mShadowColor: Int
    private val mProgressColor: Int
    private var mBindId: String? = null
    private var arcAngle = 10f
    private var mBorder = false

    private var status = STATUS_LOADING

    @Suppress("unused")
    val progress: Float
        get() = mProgress.toFloat()

    private val cornerRadius by lazy {
        context.dip(3f).toFloat()
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleProgress, defStyleAttr, 0)
        mSize = a.getDimensionPixelSize(R.styleable.CircleProgress_size, context.dip(40))
        mBorderWidth = a.getDimensionPixelSize(R.styleable.CircleProgress_progressWidth,
            context.dip(DEFAULT_BORDER_WIDTH))
        mShadowColor = a.getColor(R.styleable.CircleProgress_shadowColor, Color.WHITE)
        mProgressColor = a.getColor(R.styleable.CircleProgress_progressColor, Color.BLUE)
        mBorder = a.getBoolean(R.styleable.CircleProgress_border, false)
        mProgress = a.getInt(R.styleable.CircleProgress_progressValue, mProgress)
        mMaxProgress = a.getInt(R.styleable.CircleProgress_maxProgress, mMaxProgress)
        mTextSize = a.getInt(R.styleable.CircleProgress_stringSize, mTextSize)
        a.recycle()

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Cap.ROUND
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeWidth = mBorderWidth.toFloat()
        mPaint.pathEffect = CornerPathEffect(cornerRadius)
        mPaint.color = mProgressColor
        mBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mBackgroundPaint.color = mShadowColor

        mForkPath = Path()
        mArrowPath = Path()

        mTextPaint = Paint()
        mTextPaint.color = 0xFFACACAC.toInt()
        mTextPaint.textSize = mTextSize.toFloat()
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setupAnimations()
    }

    private fun start() {
        if (isRunning) {
            return
        }
        isRunning = true
        mObjectAnimatorAngle!!.start()
    }

    private fun stop() {
        if (!isRunning) {
            return
        }
        isRunning = false
        mObjectAnimatorAngle!!.cancel()
        arcAngle = 10f
        setProgress(0)
        invalidate()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == VISIBLE && !isRunning) {
            start()
        } else if (visibility == GONE && isRunning) {
            stop()
        }
    }

    private var disposable: Disposable? = null

    override fun onAttachedToWindow() {
        if (disposable == null) {
            disposable = RxBus.listen(ProgressEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.id == mBindId) {
                        when {
                            it.status == STATUS_ERROR && status == STATUS_LOADING -> {
                                val progress = (it.progress * mMaxProgress).toInt().let {
                                    if (it >= mMaxProgress) {
                                        (mMaxProgress * 0.95).toInt()
                                    } else {
                                        it
                                    }
                                }
                                setProgress(progress)
                            }
                            it.status == STATUS_PAUSE -> {
                                setPlay()
                                invalidate()
                            }
                            it.status == STATUS_PLAY -> {
                                setPause()
                                invalidate()
                            }
                        }
                    } else if (it.status != STATUS_ERROR && (this.status == STATUS_PAUSE || this.status == STATUS_PLAY)) {
                        setPlay()
                        invalidate()
                    }
                }
        }
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
                disposable = null
            }
        }
        disposable = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val centerX = w / 2
        val centerY = h / 2
        bounds.set((centerX - mSize / 2).toFloat(), (centerY - mSize / 2).toFloat(),
            (centerX + mSize / 2).toFloat(), (centerY + mSize / 2).toFloat())
        fBounds.left = bounds.left + mBorderWidth
        fBounds.right = bounds.right - mBorderWidth
        fBounds.top = bounds.top + mBorderWidth
        fBounds.bottom = bounds.bottom - mBorderWidth
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2, mBackgroundPaint)
        when (status) {
            STATUS_DOWNLOAD -> {
                mPaint.style = Paint.Style.STROKE
                drawDownArrow(canvas)
                if (mBorder) {
                    canvas.drawArc(fBounds, 0f, 360f, false, mPaint)
                }
            }
            STATUS_UPLOAD -> {
                mPaint.style = Paint.Style.STROKE
                drawUpArrow(canvas)
                if (mBorder) {
                    canvas.drawArc(fBounds, 0f, 360f, false, mPaint)
                }
            }
            STATUS_DONE -> {
                drawDone(canvas)
            }
            STATUS_PLAY -> {
                mPaint.style = Paint.Style.STROKE
                if (mBorder) {
                    canvas.drawArc(fBounds, 0f, 360f, false, mPaint)
                }
                mPaint.style = Paint.Style.FILL
                drawPlay(canvas)
            }
            STATUS_PAUSE -> {
                mPaint.style = Paint.Style.STROKE
                if (mBorder) {
                    canvas.drawArc(fBounds, 0f, 360f, false, mPaint)
                }
                mPaint.style = Paint.Style.FILL
                drawPause(canvas)
            }
            else -> {
                when {
                    mProgress < 100 -> {
                        mPaint.style = Paint.Style.FILL
                        drawStop(canvas)
                        mPaint.style = Paint.Style.STROKE
                        drawArc(canvas)
                    }
                    else -> {
                        mPaint.style = Paint.Style.FILL
                        drawStop(canvas)
                        mPaint.style = Paint.Style.STROKE
                        canvas.drawArc(fBounds, 0f, 360f, false, mPaint)
                    }
                }
            }
        }
    }

    private fun drawStop(canvas: Canvas) {
        canvas.drawRoundRect(RectF(
            fBounds.centerX() - fBounds.width() * 0.16f,
            fBounds.centerY() - fBounds.height() * 0.16f,
            fBounds.centerX() + fBounds.width() * 0.16f,
            fBounds.centerY() + fBounds.height() * 0.16f
        ), cornerRadius, 0f, mPaint)
    }

    private fun drawDownArrow(canvas: Canvas) {
        mForkPath.reset()
        mForkPath.moveTo(fBounds.centerX(), fBounds.centerY() - fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX(), fBounds.centerY() + fBounds.height() * 0.25f - mBorderWidth)
        mForkPath.moveTo(fBounds.centerX() + fBounds.width() * 0.16f, fBounds.centerY())
        mForkPath.lineTo(fBounds.centerX(), fBounds.centerY() + fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX() - fBounds.width() * 0.16f, fBounds.centerY())
        canvas.drawPath(mForkPath, mPaint)
    }

    private fun drawUpArrow(canvas: Canvas) {
        mForkPath.reset()
        mForkPath.moveTo(fBounds.centerX(), fBounds.centerY() + fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX(), fBounds.centerY() - fBounds.height() * 0.25f + mBorderWidth)
        mForkPath.moveTo(fBounds.centerX() - fBounds.width() * 0.16f, fBounds.centerY())
        mForkPath.lineTo(fBounds.centerX(), fBounds.centerY() - fBounds.height() * 0.25f)
        mForkPath.lineTo(fBounds.centerX() + fBounds.width() * 0.16f, fBounds.centerY())
        canvas.drawPath(mForkPath, mPaint)
    }

    private fun drawDone(canvas: Canvas) {
        val fontMetrics = mTextPaint.fontMetricsInt
        val baseline = (bounds.bottom + bounds.top - fontMetrics.bottom - fontMetrics.top) / 2
        canvas.drawText("FILE", bounds.centerX(), baseline, mTextPaint)
    }

    private fun drawPlay(canvas: Canvas) {
        mForkPath.reset()
        mForkPath.moveTo(fBounds.centerX() - fBounds.width() * 0.20f, fBounds.centerY() - fBounds.height() * 0.2f)
        mForkPath.lineTo(fBounds.centerX() + fBounds.width() * 0.2f, fBounds.centerY())
        mForkPath.lineTo(fBounds.centerX() - fBounds.width() * 0.15f, fBounds.centerY() + fBounds.height() * 0.2f)
        mForkPath.lineTo(fBounds.centerX() - fBounds.width() * 0.15f, fBounds.centerY() - fBounds.height() * 0.2f)
        mForkPath.lineTo(fBounds.centerX() + fBounds.width() * 0.2f, fBounds.centerY())
        canvas.drawPath(mForkPath, mPaint)
    }

    private fun drawPause(canvas: Canvas) {
        val height = (fBounds.height() / 8)
        canvas.drawLine((fBounds.centerX() - fBounds.width() * 0.1f), (fBounds.centerY() - height),
            (fBounds.centerX() - fBounds.width() * 0.1f), (fBounds.centerY() + height), mPaint)
        canvas.drawLine((fBounds.centerX() + width * 0.1f), (fBounds.centerY() - height),
            (fBounds.centerX() + fBounds.width() * 0.1f), (fBounds.centerY() + height), mPaint)
    }

    private fun drawArc(canvas: Canvas) {
        val startAngle = currentGlobalAngle
        val endAngle = (350 * mProgress / mMaxProgress + 10)
        when {
            endAngle > arcAngle + 100 -> arcAngle += 5
            endAngle > arcAngle + 50 -> arcAngle += 2
            endAngle > arcAngle + 10 -> arcAngle += 1
            else -> arcAngle = endAngle.toFloat()
        }
        canvas.drawArc(fBounds, startAngle, arcAngle, false, mPaint)
    }

    private fun setupAnimations() {
        mObjectAnimatorAngle = ObjectAnimator.ofFloat(this, "CurrentGlobalAngle", 0f, 360f)
        mObjectAnimatorAngle!!.interpolator = ANGLE_INTERPOLATOR
        mObjectAnimatorAngle!!.duration = ANGLE_ANIMATOR_DURATION.toLong()
        mObjectAnimatorAngle!!.repeatMode = ValueAnimator.RESTART
        mObjectAnimatorAngle!!.repeatCount = ValueAnimator.INFINITE
    }

    fun setProgress(progress: Int) {
        mProgress = if (progress >= mMaxProgress) {
            mMaxProgress
        } else {
            progress
        }
    }

    fun setBindId(id: String?) {
        if (id != mBindId) {
            mBindId = id
            setProgress(0)
        }
    }

    fun setBindOnly(id: String?) {
        if (id != mBindId) {
            mBindId = id
        }
    }

    @Suppress("unused")
    fun getBindId(): String? {
        return mBindId
    }

    private fun setStatus(status: Int) {
        this.status = status
        if (status == STATUS_UPLOAD || status == STATUS_DOWNLOAD) {
            stop()
        } else {
            start()
        }
    }

    fun enableLoading() {
        if (status != STATUS_LOADING) {
            setProgress(0)
            setStatus(STATUS_LOADING)
        }
    }

    fun enableDownload() {
        setStatus(STATUS_DOWNLOAD)
    }

    fun enableUpload() {
        setStatus(STATUS_UPLOAD)
    }

    fun setDone() {
        setStatus(STATUS_DONE)
    }

    fun setPlay() {
        setStatus(STATUS_PLAY)
    }

    fun setPause() {
        setStatus(STATUS_PAUSE)
    }

    companion object {
        private val ANGLE_INTERPOLATOR = LinearInterpolator()
        private const val ANGLE_ANIMATOR_DURATION = 3000
        private const val DEFAULT_BORDER_WIDTH = 3
        private const val STATUS_LOADING = 0
        private const val STATUS_UPLOAD = 1
        private const val STATUS_DOWNLOAD = 2
        private const val STATUS_DONE = 3
        const val STATUS_PLAY = 4
        const val STATUS_PAUSE = 5
        const val STATUS_ERROR = -1
    }
}
