package com.moneydropper.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun now(): Long = System.currentTimeMillis()

    fun formatForFile(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDisplay(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
