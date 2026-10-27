package com.jhw.batteryemoji.presentation.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.jhw.batteryemoji.R

/**
 * Shared screen header: back button, title and an optional action icon on the right.
 * Owns its top margin, height and side padding so every screen's header lines up the same.
 *
 * XML: app:headerTitle, app:showBack (default true), app:actionIcon, app:actionTint.
 */
class TopHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val backButton: ImageView
    val titleView: TextView
    val actionButton: ImageView

    var title: CharSequence?
        get() = titleView.text
        set(value) { titleView.text = value }

    var showBack: Boolean
        get() = backButton.visibility == View.VISIBLE
        set(value) { backButton.visibility = if (value) View.VISIBLE else View.GONE }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val sidePadding = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._16sdp)
        // Status bar gap + row height live here, not in each screen's layout
        setPadding(sidePadding, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._28sdp), sidePadding, 0)
        minimumHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._28sdp) + resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._45sdp)

        LayoutInflater.from(context).inflate(R.layout.view_top_header, this, true)
        backButton = findViewById(R.id.btnHeaderBack)
        titleView = findViewById(R.id.tvHeaderTitle)
        actionButton = findViewById(R.id.btnHeaderAction)

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TopHeaderView, defStyleAttr, 0)
            try {
                title = typedArray.getText(R.styleable.TopHeaderView_headerTitle)
                showBack = typedArray.getBoolean(R.styleable.TopHeaderView_showBack, true)
                val actionRes = typedArray.getResourceId(R.styleable.TopHeaderView_actionIcon, 0)
                if (actionRes != 0) {
                    actionButton.setImageResource(actionRes)
                    actionButton.visibility = View.VISIBLE
                }
                if (typedArray.hasValue(R.styleable.TopHeaderView_actionTint)) {
                    actionButton.imageTintList = ColorStateList.valueOf(
                        typedArray.getColor(R.styleable.TopHeaderView_actionTint, 0)
                    )
                }
            } finally {
                typedArray.recycle()
            }
        }
    }
}
