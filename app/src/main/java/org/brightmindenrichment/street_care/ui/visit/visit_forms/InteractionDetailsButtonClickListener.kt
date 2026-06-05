package org.brightmindenrichment.street_care.ui.visit.visit_forms


import org.brightmindenrichment.street_care.ui.visit.data.InteractionLog

interface InteractionDetailsButtonClickListener {
    fun onClick(interactionLog: InteractionLog)
}