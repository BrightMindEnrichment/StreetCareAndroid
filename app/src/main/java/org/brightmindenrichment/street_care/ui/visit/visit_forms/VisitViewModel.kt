package org.brightmindenrichment.street_care.ui.visit.visit_forms

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.brightmindenrichment.street_care.ui.visit.data.VisitLog
import org.brightmindenrichment.street_care.ui.visit.repository.VisitLogRepository
import org.brightmindenrichment.street_care.ui.visit.repository.VisitLogRepositoryImp
import java.util.*

class VisitViewModel : ViewModel() {
    private val repository: VisitLogRepository = VisitLogRepositoryImp()
    var visitLog: VisitLog = VisitLog()

    // Interaction Number Tracking
    private val _interactionIndex = MutableLiveData(1)
    val interactionIndex: LiveData<Int> = _interactionIndex

    fun nextInteraction() {
        val cur = _interactionIndex.value ?: 1
        _interactionIndex.value = cur + 1
    }

    fun resetInteractions() {
        _interactionIndex.value = 1
    }

    init {
        resetVisitLogPage()
    }
    fun saveVisitLog(){
        repository.saveVisitLog(visitLog)
    }

//    fun resetVisitLogPage() {
//         visitLog = VisitLog()
//    }
    fun resetVisitLogPage(forceReset: Boolean = true) {
        if (forceReset) {
            visitLog = VisitLog()
        }
    }

    fun validateLocation(location: String): Boolean {
        // Most of the cases user will fill the location, so it won't be empty
        return !(location.isNullOrEmpty())
    }
    fun validateDate(visitedDate: Date): Boolean {
        val currentDate: Date = Calendar.getInstance().time
        return visitedDate <= currentDate
    }

    fun increment(totalPeopleCount: Int): Int {
        return totalPeopleCount.inc()
    }

    fun decrement(totalPeopleCount: Int): Int {
        return if (totalPeopleCount > 0) {
            totalPeopleCount.dec()

        } else
            0
    }
}