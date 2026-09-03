package com.archimede.w2full.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.archimede.w2full.data.mimit.MimitStationPriceSelector
import com.archimede.w2full.data.repository.VehicleFuelOptions
import com.archimede.w2full.data.repository.VehicleFuelUpdateResult
import com.archimede.w2full.data.repository.VehicleSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VehicleSettingsUiState(
    val isLoading: Boolean = true,
    val vehicleName: String = "Veicolo",
    val selectedFuelType: String = MimitStationPriceSelector.FALLBACK_FUEL_TYPE,
    val fuelOptions: List<String> = VehicleFuelOptions.BASE_FUELS,
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
)

class VehicleSettingsViewModel(
    private val repository: VehicleSettingsRepository,
) : ViewModel() {
    private val isSaving = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        repository.observeSettings(),
        isSaving,
        statusMessage,
    ) { settings, saving, message ->
        VehicleSettingsUiState(
            isLoading = false,
            vehicleName = settings.vehicleName,
            selectedFuelType = settings.selectedFuelType,
            fuelOptions = settings.fuelOptions,
            isSaving = saving,
            statusMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VehicleSettingsUiState(),
    )

    fun selectFuel(fuelType: String) {
        if (VehicleFuelOptions.same(fuelType, uiState.value.selectedFuelType)) return
        if (isSaving.value) return

        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            try {
                statusMessage.value = when (repository.setDefaultFuelType(fuelType)) {
                    is VehicleFuelUpdateResult.Success -> "Carburante aggiornato"
                    VehicleFuelUpdateResult.Failure -> "Impossibile aggiornare il carburante"
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                statusMessage.value = "Impossibile aggiornare il carburante"
            } finally {
                isSaving.value = false
            }
        }
    }

    class Factory(
        private val repository: VehicleSettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(VehicleSettingsViewModel::class.java))
            return VehicleSettingsViewModel(repository) as T
        }
    }
}
