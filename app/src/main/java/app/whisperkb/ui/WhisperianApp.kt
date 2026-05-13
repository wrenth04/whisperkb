package app.whisperkb.ui

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView

object WhisperkbApp {
    fun build(context: Context): ScrollView = ScrollView(context).apply {
        addView(LinearLayout(context))
    }
}
