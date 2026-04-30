package com.merkost.drawablepreview

fun String.getDigits() = try {
    this.replace(Regex("\\D+"), "").toInt()
} catch (exception: Exception) {
    null
}