package com.merkost.drawablepreview

fun String.getDigits() = try {
    this.replace("\\D+", "").toInt()
} catch (exception: Exception) {
    null
}