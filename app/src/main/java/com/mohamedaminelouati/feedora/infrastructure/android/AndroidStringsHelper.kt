package com.mohamedaminelouati.feedora.infrastructure.android

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.mohamedaminelouati.feedora.ui.ext.formatAsString
import java.util.*
import javax.inject.Inject

class AndroidStringsHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {

    fun getString(resId: Int, vararg formatArgs: Any) = context.getString(resId, *formatArgs)

    fun getQuantityString(resId: Int, quantity: Int, vararg formatArgs: Any) =
        context.resources.getQuantityString(resId, quantity, *formatArgs)

    fun formatAsString(
        date: Date?,
        onlyHourMinute: Boolean? = false,
        atHourMinute: Boolean? = false,
    ) = date?.formatAsString(context, onlyHourMinute, atHourMinute)
}
