package com.ace.mobile.presentation.diagnostic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WearDiagnosticScreen(
    viewModel: WearDiagnosticViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === TITULO ===
        Text(
            text = "A.C.E Wear Diagnostic",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // === ESTADO DE CONEXION ===
        ConnectionStatusCard(state)

        Spacer(modifier = Modifier.height(8.dp))

        // === BOTONES DE ACCION ===
        ActionButtons(
            onRefresh = { viewModel.refreshStatus() },
            onTestMessage = { viewModel.sendTestMessage() },
            onTestDataItem = { viewModel.sendTestDataItem() },
            onStart = { viewModel.sendStartCommand() },
            onStop = { viewModel.sendStopCommand() },
            isLoading = state.isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        // === RESULTADO ULTIMO TEST ===
        if (state.lastTestResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Text(
                    text = "Resultado: ${state.lastTestResult}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // === LOGS EN TIEMPO REAL ===
        LogPanel(logs = state.logs)

        Spacer(modifier = Modifier.height(8.dp))

        // === BOTON CERRAR ===
        Button(onClick = onClose) {
            Text("Cerrar Diagnostico")
        }
    }
}

@Composable
private fun ConnectionStatusCard(state: WearDiagnosticUiState) {
    val (bgColor, statusText) = when {
        state.isLoading -> Pair(Color(0xFFFFF9C4), "Verificando...")
        state.isConnected -> Pair(Color(0xFFC8E6C9), "CONECTADO")
        else -> Pair(Color(0xFFFFCDD2), "DESCONECTADO")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
            )

            if (state.nodeCount > 0) {
                Text(
                    text = "Nodos: ${state.nodeCount}",
                    style = MaterialTheme.typography.bodySmall
                )
                state.nodeNames.forEach { name ->
                    Text(
                        text = "  $name",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (state.lastError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Error: ${state.lastError}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onRefresh: () -> Unit,
    onTestMessage: () -> Unit,
    onTestDataItem: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
                } else {
                    Text("Refresh", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = onTestMessage,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Test Msg", style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = onTestDataItem,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Test Data", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("START", style = MaterialTheme.typography.bodySmall)
            }
        }

        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
        ) {
            Text("STOP", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LogPanel(logs: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "LOGS (ultimos ${logs.size}):",
                color = Color(0xFF00FF00),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            logs.forEach { log ->
                Text(
                    text = log,
                    color = Color(0xFF00FF00),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}