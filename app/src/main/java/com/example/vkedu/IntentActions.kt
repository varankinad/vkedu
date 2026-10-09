package com.example.vkedu

import android.content.Intent
import android.net.Uri

private const val RU_PHONE_DIGITS = 11

fun normalizePhone(input: String): String {
    val trimmed = input.trim()
    val hasLeadingPlus = trimmed.startsWith("+")
    val digits = trimmed.filter { it.isDigit() }
    return if (hasLeadingPlus) "+$digits" else digits
}

fun isValidRuPhone(input: String): Boolean {
    val digits = normalizePhone(input).removePrefix("+")
    return digits.length == RU_PHONE_DIGITS &&
        (digits.first() == '7' || digits.first() == '8')
}

fun dialIntent(phone: String): Intent {
    return Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + normalizePhone(phone)))
}

fun shareTextIntent(text: String): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return Intent.createChooser(sendIntent, "Поделиться через…")
}
