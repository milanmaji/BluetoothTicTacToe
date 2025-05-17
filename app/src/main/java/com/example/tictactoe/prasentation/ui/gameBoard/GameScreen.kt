package com.example.tictactoe.prasentation.ui.gameBoard

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tictactoe.R
import com.example.tictactoe.common.Helper.vibrate
import com.example.tictactoe.prasentation.component.TopAppBar
import com.example.tictactoe.prasentation.theme.TicTacToeTheme
import com.example.tictactoe.prasentation.theme.oColor
import com.example.tictactoe.prasentation.theme.xColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel,
    backToHome: () -> Unit
) {

    val context = LocalContext.current
    val gameState = gameViewModel.gameUiState.collectAsStateWithLifecycle()

    BackHandler(gameState.value is GameUiState.GameStarted) {
        Log.d("TAG", "OnBackPressed")
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar()

        Spacer(modifier = Modifier.height(20.dp))

        when (val state = gameState.value) {
            is GameUiState.GameStarted -> {

                LaunchedEffect(Unit) {
                    context.vibrate()
                }
                GameStarted(
                    modifier = modifier,
                    state = state,
                    onCellClicked = { index -> gameViewModel.onCellClicked(index) },
                    restartGame = { gameViewModel.restartButtonClick() },
                    closeGame = { gameViewModel.disconnectFromDevice() }
                )

            }

            is GameUiState.Connecting -> {
                Connecting(isServer = state.isServer)
            }

            is GameUiState.Connected -> {
                Connected(
                    localPlayerInfo = state.localPlayerInfo,
                    opponentPlayerInfo = state.opponentPlayerInfo,
                    isServer = state.isServer,
                    startGame = {
                        gameViewModel.startButtonClick()
                    }
                )
            }

            GameUiState.Disconnected -> {
                backToHome()
            }

            is GameUiState.Error -> {
                Error(message = state.errorMessage) {
                    backToHome()
                }
            }

        }
    }


}

@Composable
fun GameStarted(
    modifier: Modifier = Modifier,
    state: GameUiState.GameStarted,
    onCellClicked: (Int) -> Unit,
    restartGame: () -> Unit,
    closeGame: () -> Unit
) {

    val isDarkMode = isSystemInDarkTheme()
    val borderColor = if (isDarkMode) Color.White else Color.Black

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current
        val isLocalPlayerTurn = state.currentPlayer == state.localPlayerInfo.player
        val currentPlayerInfo =
            if (isLocalPlayerTurn) state.localPlayerInfo else state.opponentPlayerInfo

        LaunchedEffect(isLocalPlayerTurn) {
            if (isLocalPlayerTurn) {
                context.vibrate()
            }
        }

        Text("Current Player: ${currentPlayerInfo.name}", fontSize = 16.sp)

        PlayerProfile(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 20.dp),
            name = state.opponentPlayerInfo.name,
            isEnable = state.opponentPlayerInfo.player == state.currentPlayer
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .border(1.dp, borderColor)
                .height(300.dp)
                .width(300.dp),
            userScrollEnabled = false // Disable scrolling for a fixed 3x3 grid
        ) {
            itemsIndexed(state.board) { index, symbol ->
                val row = index / 3
                val col = index % 3

                val symbolColor = when (symbol) {
                    "X" -> xColor
                    "O" -> oColor
                    else -> Color.Unspecified
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()

                            if (col < 2) {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(size.width, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = strokeWidth
                                )
                            }
                            if (row < 2) {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                        .clickable {
                            onCellClicked(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = symbol,
                        fontSize = 32.sp,
                        color = symbolColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PlayerProfile(
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 20.dp),
            name = "You",
            isEnable = state.localPlayerInfo.player == state.currentPlayer
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (state.result) {
            GameResult.XWins -> {
                val text = if (state.localPlayerInfo.player == Player.X)
                    "You Win!"
                else
                    "Player ${state.opponentPlayerInfo.name} Wins!"
                Text(text)
            }

            GameResult.OWins -> {
                val text = if (state.localPlayerInfo.player == Player.O)
                    "You Win!"
                else
                    "Player ${state.opponentPlayerInfo.name} Wins!"
                Text(text)
            }

            GameResult.Draw -> {
                Text("It's a Draw!")
            }

            else -> {}
        }

        if (state.result != GameResult.InProgress && state.localPlayerInfo.isHost) {
            Button(onClick = { restartGame() }) {
                Text("Restart Game", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = { closeGame() }) {
            Text("Close Game", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun PlayerProfile(modifier: Modifier = Modifier, name: String?, isEnable: Boolean) {

    Column(modifier = modifier.alpha(if (isEnable) 1f else 0.5f)) {

        Image(
            painter = painterResource(R.drawable.person),
            contentDescription = "Person icon",
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        )

        Text(text = name ?: "", fontSize = 12.sp)
    }
}

@Composable
fun Connecting(modifier: Modifier = Modifier, isServer: Boolean) {

    val discoverableBluetoothLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            /* Not needed */
        }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if(isServer){
            coroutineScope.launch {
                delay(1000)
                discoverableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                )
            }

        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val message = if (isServer) "Waiting for opponent.." else "Connecting..."

        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(20.dp))
        Text(message, fontSize = 20.sp)
    }
}

@Composable
fun Connected(
    modifier: Modifier = Modifier,
    localPlayerInfo: PlayerInfo?,
    opponentPlayerInfo: PlayerInfo?,
    isServer: Boolean,
    startGame: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .border(1.dp, color = Color.Gray, shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 30.dp, vertical = 15.dp)
        ) {
            Text(
                text = localPlayerInfo?.name ?: "",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center
                ),
            )
            if (opponentPlayerInfo?.name.isNullOrBlank().not()) {
                val symbolColor = when (localPlayerInfo?.player?.name) {
                    "X" -> xColor
                    "O" -> oColor
                    else -> Color.Unspecified
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = localPlayerInfo?.player?.name ?: "",
                    fontSize = 32.sp,
                    color = symbolColor,
                    fontWeight = FontWeight.ExtraBold
                )
            }

        }


        Spacer(modifier = Modifier.height(10.dp))

        Image(
            modifier = Modifier.size(80.dp),
            painter = painterResource(R.drawable.vs_icon),
            contentDescription = "Versus"
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .border(1.dp, color = Color.Gray, shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 30.dp, vertical = 15.dp)
        ) {
            Text(
                text = opponentPlayerInfo?.name?.takeIf { it.isNotBlank() } ?: "?",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center
                )
            )
            if (opponentPlayerInfo?.name.isNullOrBlank().not()) {
                val symbolColor = when (opponentPlayerInfo.player.name) {
                    "X" -> xColor
                    "O" -> oColor
                    else -> Color.Unspecified
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = opponentPlayerInfo.player.name,
                    fontSize = 32.sp,
                    color = symbolColor,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (opponentPlayerInfo == null) return

        Spacer(modifier = Modifier.height(50.dp))

        if (isServer) {
            Button(onClick = { startGame() }) {
                Text("Start Game", fontSize = 20.sp)
            }
        } else {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Waiting for your friend ${opponentPlayerInfo.name} to start the match..",
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }


}


@Composable
fun Error(modifier: Modifier = Modifier, message: String?, onReturnClick: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(message ?: "Something went wrong.", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { onReturnClick() }) {
            Text("Return")
        }
    }
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun PreviewGameScreen() {
    TicTacToeTheme {
        GameStarted(
            state = GameUiState.GameStarted(
                localPlayerInfo = PlayerInfo(
                    name = "Milan",
                    player = Player.X,
                    isHost = true
                ),
                opponentPlayerInfo = PlayerInfo(
                    name = "Alex",
                    player = Player.O,
                    isHost = false
                )
            ),
            onCellClicked = { index -> },
            restartGame = {},
            closeGame = {}
        )
    }
}