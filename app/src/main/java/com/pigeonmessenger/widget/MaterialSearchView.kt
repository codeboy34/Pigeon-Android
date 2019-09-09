package com.pigeonmessenger.widget

import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.FrameLayout
import com.pigeonmessenger.R
import kotlinx.android.synthetic.main.view_search.view.*


class MaterialSearchView : FrameLayout {
    var isOpen = false
        private set
    private var mClearingFocus: Boolean = false

    private var mCurrentQuery: CharSequence? = null
    var mOnQueryTextListener: OnQueryTextListener? = null
    private var mSearchViewListener: SearchViewListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_search, this, true)
        initStyle(attrs, defStyleAttr)
        initSearchView()
    }

    @Suppress("unused")
    val currentQuery: String
        get() = if (!TextUtils.isEmpty(mCurrentQuery)) {
            mCurrentQuery.toString()
        } else ""

    private fun initStyle(attributeSet: AttributeSet?, defStyleAttribute: Int) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        setSearchBarHeight(getActionBarHeight())

    }

    fun getActionBarHeight(): Int {
        val ta = context.theme.obtainStyledAttributes(
                intArrayOf(android.R.attr.actionBarSize))
        return ta.getDimension(0, 0f).toInt()
    }

    private fun initSearchView() {
        left_ib.setOnClickListener { closeSearch() }
        search_et.setOnEditorActionListener { _, _, _ ->
            onSubmitQuery()
            true
        }

        search_et.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // When the text changes, filter
                this@MaterialSearchView.onTextChanged(s)
                if (search_et.text.isEmpty()) {
                    right_clear.visibility = View.GONE
                } else {
                    right_clear.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        search_et.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                search_et.post { search_et.showKeyboard() }
            }
        }

        right_clear.setOnClickListener {
            if (search_et.text.isNotEmpty()) {
                search_et.setText("")
            }
        }

        search_tv.setOnClickListener {
            openSearch()
        }

    }

    var oldLeftX = 0f
    var oldSearchWidth = 0
    var oldSearchTvWidth = 0
    fun openSearch() {
        synchronized(this) {
            if (isOpen) {
                return
            }
            search_et.visibility = View.VISIBLE
          //  search_tv.visibility = View.INVISIBLE
            showKeyboard()

            left_ib.visibility = View.GONE
            back_ib.visibility = View.VISIBLE
            right_clear.visibility = View.GONE

            search_et.requestFocus()
            search_et.setText("")
            oldLeftX = left_ib.x
            oldSearchWidth = search_et.measuredWidth
            oldSearchTvWidth = search_tv.measuredWidth
            search_tv.hint=""
            right_ib.translationX(context.dpToPx(42f).toFloat())
            search_et.animateWidth(oldSearchWidth, oldSearchWidth + context.dpToPx(50f))
            search_tv.animateWidth(oldSearchTvWidth,oldSearchTvWidth+context.dpToPx(50f))
            mSearchViewListener?.onSearchViewOpened()
            isOpen = true
            search_et.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0F, 0F, 0))
            search_et.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0F, 0F, 0))
        }
    }

    fun Context.dpToPx(dp: Float): Int {
        return if (dp == 0f) {
            0
        } else {
            Math.ceil((this.resources.displayMetrics.density * dp).toDouble()).toInt()
        }
    }

     fun closeSearch() {
        synchronized(this) {
            if (!isOpen) {
                return
            }

            search_et.visibility = View.INVISIBLE
            search_tv.visibility = View.VISIBLE
            left_ib.visibility = View.VISIBLE
            back_ib.visibility = View.GONE
            right_clear.visibility = View.GONE

            right_ib.translationX(0f)
            search_et.animateWidth(oldSearchWidth + context.dpToPx(50f), oldSearchWidth)
            search_tv.animateWidth(oldSearchTvWidth + context.dpToPx(50f), oldSearchTvWidth)

            clearFocus()
            search_et.hideKeyboard()
            search_et.hint =""
            search_tv.hint= context.getString(R.string.search_hint)
            mSearchViewListener?.onSearchViewClosed()
            isOpen = false
        }
    }

    private fun onTextChanged(newText: CharSequence) {
        mCurrentQuery = search_et.text

        mOnQueryTextListener?.onQueryTextChange(newText.toString())
    }

    private fun onSubmitQuery() {
        val query = search_et.text

        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryTextListener == null || !mOnQueryTextListener!!.onQueryTextSubmit(query.toString())) {
                closeSearch()
                search_et.setText("")
            }
        }
    }

    fun setOnQueryTextListener(mOnQueryTextListener: OnQueryTextListener) {
        this.mOnQueryTextListener = mOnQueryTextListener
    }

    fun setSearchViewListener(mSearchViewListener: SearchViewListener) {
        this.mSearchViewListener = mSearchViewListener
    }

    fun setQuery(query: CharSequence?, submit: Boolean) {
        search_et.setText(query)

        if (query != null) {
            search_et.setSelection(search_et.length())
            mCurrentQuery = query
        }

        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery()
        }
    }

    fun setSearchBarColor(color: Int) {
        search_et.setBackgroundColor(color)
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        if (factor < 0) return color

        val alpha = Math.round(Color.alpha(color) * factor)

        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun setTextColor(color: Int) {
        search_et.setTextColor(color)
    }

    private fun setHintTextColor(color: Int) {
        search_et.setHintTextColor(color)
    }

    private fun setHint(hint: CharSequence?) {
        search_et.hint = hint
    }

    private fun setCancelIcon(resourceId: Int) {
        back_ib.setImageResource(resourceId)
    }


    private fun setInputType(inputType: Int) {
        search_et.inputType = inputType
    }

    private fun setSearchBarHeight(height: Int) {
        search_view.minimumHeight = height
        search_view.layoutParams.height = height
    }

    fun setOnRightClickListener(onClickListener: OnClickListener) {
        right_ib.setOnClickListener(onClickListener)
    }

    fun setOnLeftClickListener(onClickListener: OnClickListener) {
        left_ib.setOnClickListener(onClickListener)
    }

    fun setOnBackClickListener(onClickListener: OnClickListener) {
        back_ib.setOnClickListener(onClickListener)
    }

    override fun clearFocus() {
        this.mClearingFocus = true
        hideKeyboard()
        super.clearFocus()
        search_et.clearFocus()
        this.mClearingFocus = false
    }

    interface OnQueryTextListener {
        fun onQueryTextSubmit(query: String): Boolean

        fun onQueryTextChange(newText: String): Boolean
    }

    interface SearchViewListener {
        fun onSearchViewOpened()

        fun onSearchViewClosed()
    }
}
