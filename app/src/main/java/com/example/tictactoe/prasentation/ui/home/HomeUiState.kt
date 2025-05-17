package com.example.tictactoe.prasentation.ui.home

import com.example.tictactoe.prasentation.ui.gameBoard.Player

data class HomeUiState(
    var name:String = "",
    var selectedPlayer: Player = Player.X,
    var isShowDialog: Boolean = false
)
