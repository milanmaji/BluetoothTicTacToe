package com.example.tictactoe.prasentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tictactoe.domain.BluetoothController
import com.example.tictactoe.prasentation.ui.gameBoard.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() :
    ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()


    fun onSelectPlayerChange(player: Player) = viewModelScope.launch {
        _uiState.update {
            it.copy(selectedPlayer = player)
        }
    }

    fun showDialog() {
        _uiState.update {
            it.copy(isShowDialog = true)
        }
    }

    fun dismissDialog() {
        _uiState.update {
            it.copy(isShowDialog = false)
        }
    }

    fun onNameChanged(name: String) = viewModelScope.launch {
        _uiState.update {
            it.copy(name = name)
        }
    }

}