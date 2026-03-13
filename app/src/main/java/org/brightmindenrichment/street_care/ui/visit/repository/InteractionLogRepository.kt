package org.brightmindenrichment.street_care.ui.visit.repository

import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog

interface InteractionLogRepository {

    fun saveInteractionLog(
        interactionLog: InteractionLog,
        onComplete: (Boolean, String?) -> Unit
    )

    fun loadInteractionLogByDocumentId(
        documentId: String,
        onComplete: (InteractionLog?) -> Unit
    )

    fun loadPublicInteractionLogs(
        onComplete: (List<InteractionLog>) -> Unit
    )
}