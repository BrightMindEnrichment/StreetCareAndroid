package org.brightmindenrichment.street_care.util

fun String.isValidZip(): Boolean = matches(Regex("^\\d{5}$"))

/** True if the string is non-empty AND not a valid 5-digit ZIP. */
fun String.isInvalidZip(): Boolean = isNotEmpty() && !isValidZip()

fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

/** True if the string is non-empty AND not a valid e-mail address. */
fun String.isInvalidEmail(): Boolean = isNotEmpty() && !isValidEmail()

fun String.isValidPhone(): Boolean = matches(Regex("^\\+[0-9]{7,15}$"))

/** True if the string is non-empty AND not a valid phone. */
fun String.isInvalidPhone(): Boolean = isNotEmpty() && !isValidPhone()
