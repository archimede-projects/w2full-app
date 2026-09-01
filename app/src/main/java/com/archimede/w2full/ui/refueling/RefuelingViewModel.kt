package com.archimede.w2full.ui.refueling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.archimede.w2full.data.repository.RefuelingRepository
import com.archimede.w2full.domain.calculation.FuelMetrics
import com.archimede.w2full.domain.calculation.FuelMetricsCalculator
import com.archimede.w2full.domain.model.Rifornimento
import com.archimede.w2full.domain.model.RifornimentoDraft
import com.archimede.w2full.domain.model.VehicleConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val EmptyMetrics = FuelMetrics(
    averageConsumptionLPer100Km = null,
    costPerKmEuro = null,
    estimatedRemainingLiters = null,
    estimatedRangeKm = null,
)

data class RefuelingUiState(
    val refuelings: List<Rifornimento> = emptyList(),
    val vehicle: VehicleConfig? = null,
    val metrics: FuelMetrics = EmptyMetrics,
    val errorMessage: String? = null,
)

class RefuelingViewModel(
    private val repository: RefuelingRepository,
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.observeRefuelings(),
        repository.observeVehicle(),
        errorMessage,
    ) { refuelings, vehicle, error ->
        RefuelingUiState(
            refuelings = refuelings,
            vehicle = vehicle,
            metrics = FuelMetricsCalculator.calculate(
                records = refuelings,
                tankCapacityMilliliters = vehicle?.tankCapacityMilliliters,
            ),
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RefuelingUiState(),
    )

    init {
        launchRepositoryAction { repository.ensureDefaultVehicle() }
    }

    fun insert(draft: RifornimentoDraft) {
        launchRepositoryAction { repository.insert(draft) }
    }

    fun update(existing: Rifornimento, draft: RifornimentoDraft) {
        launchRepositoryAction {
            repository.update(
                existing.copy(
                    timestampEpochMillis = draft.timestampEpochMillis,
                    odometerKm = draft.odometerKm,
                    litersMilliliters = draft.litersMilliliters,
                    totalCostCents = draft.totalCostCents,
                    fuelType = draft.fuelType,
                    isFullTank = draft.isFullTank,
                ),
            )
        }
    }

    fun delete(id: Long) {
        launchRepositoryAction { repository.delete(id) }
    }

    fun updateTankCapacity(capacityMilliliters: Long) {
        launchRepositoryAction {
            repository.updateTankCapacityMilliliters(capacityMilliliters)
        }
    }

    fun clearError() {
        errorMessage.value = null
    }

    private fun launchRepositoryAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { errorMessage.value = null }
                .onFailure { errorMessage.value = it.message ?: "Operazione non riuscita." }
        }
    }

    class Factory(
        private val repository: RefuelingRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RefuelingViewModel::class.java))
            return RefuelingViewModel(repository) as T
        }
    }
}
