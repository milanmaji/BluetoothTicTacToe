package com.example.tictactoe.prasentation

import com.example.tictactoe.domain.model.BluetoothDeviceDomain
import kotlinx.serialization.Serializable

sealed class MainDest {

    @Serializable
    data object Home : MainDest()

    @Serializable
    data class DeviceScan(val name:String?) : MainDest()

    @Serializable
    data class GameBoard(val name:String?,val deviceName:String?,val deviceAddress:String?, val selectedPlayer: String?) : MainDest()


}