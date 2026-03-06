package org.brightmindenrichment.street_care.util

import org.brightmindenrichment.street_care.BuildConfig

object FirestoreCollections {
    val INTERACTION_LOG: String get() = BuildConfig.INTERACTION_LOG_COLLECTION
    val HELP_REQUEST: String get() = BuildConfig.HELP_REQUEST_COLLECTION
}
