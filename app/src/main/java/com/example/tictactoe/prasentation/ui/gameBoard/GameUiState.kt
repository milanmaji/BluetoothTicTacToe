package com.example.tictactoe.prasentation.ui.gameBoard

enum class Player {
    X, O;

    companion object {
        fun from(symbol: String): Player? = Player.entries.find { it.name == symbol }
    }
}
fun Player.opponent(): Player = if (this == Player.X) Player.O else Player.X

data class PlayerInfo(
    var name: String,
    var player: Player,
    var isHost: Boolean
)

enum class GameResult { InProgress, Draw, XWins, OWins }

sealed class GameUiState() {

    data class Connecting(val isServer: Boolean) : GameUiState()
    data class Connected(val localPlayerInfo: PlayerInfo?, val opponentPlayerInfo: PlayerInfo?,val isServer: Boolean) : GameUiState()
    data class Error(val errorMessage: String? = null) : GameUiState()

    data class GameStarted(
        val board: List<String> = List(9) { "" },
        val currentPlayer: Player = Player.X,
        val result: GameResult = GameResult.InProgress,
        val localPlayerInfo: PlayerInfo,
        val opponentPlayerInfo: PlayerInfo
    ) : GameUiState(){
        companion object {
            fun create(localPlayerInfo: PlayerInfo, opponentPlayerInfo: PlayerInfo) = GameStarted(
                board = List(9) { "" },
                currentPlayer = Player.X,
                result = GameResult.InProgress,
                localPlayerInfo = localPlayerInfo,
                opponentPlayerInfo = opponentPlayerInfo
            )
        }
    }

    data object Disconnected : GameUiState()
}