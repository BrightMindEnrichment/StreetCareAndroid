package org.brightmindenrichment.street_care.util

import org.brightmindenrichment.street_care.R

object Constants {
    const val EVENTS_NOTIFICATION_CHANNEL_ID = "events_notification_channel_id" // Provide a unique channel ID
    const val EVENT_TABLE = "event_table"
    const val EVENT_DATABASE = "event_database"
    const val INTENT_TYPE_NOTIFICATION = "intent_type_notification"
    const val NOTIFICATION_WORKER = "notification_worker"
    const val ROOM_DB_IS_INITIALIZED = "room_db_is_initialized"
    const val IS_APP_ON_BACKGROUND = "is_app_on_background"
    const val EVENTS_NOTIFICATION = "events_notification"
    const val DEFAULT_CAPACITY = 1000
    const val IL_DRAFT_JSON = "il_draft_json"

    // Interaction Log fragment destination IDs for progress bar navigation
    val INTERACTION_LOG_DEST_IDS = listOf(
        R.id.interactionQ1Fragment,
        R.id.interactionQ2Fragment,
        R.id.interactionQ3Fragment,
        R.id.interactionQ4Fragment,
        R.id.interactionQ5Fragment,
        R.id.interactionQ6Fragment,
        R.id.interactionQ7Fragment
    )
}