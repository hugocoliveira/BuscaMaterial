package br.com.lit.busca.material.ui

import android.Manifest
import android.media.MediaPlayer
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.lit.busca.material.R
import br.com.lit.busca.material.ui.theme.BuscaMaterialTheme
import com.google.gson.JsonObject
import br.com.lit.busca.material.scanner.iniciarScanner
import br.com.lit.busca.material.ui.components.ResultCard
import br.com.lit.busca.material.ui.components.ScannerField
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService

private const val TAG = "BuscaMaterialScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BuscaMaterialScreen(
    viewModel: BuscaMaterialViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissaoCamera = rememberPermissionState(Manifest.permission.CAMERA)

    Scaffold(
        topBar = {
            // ponytail: titleMedium (16sp) em vez de headlineMedium (22sp) — libera ~30dp de área útil
            TopAppBar(
                title = {
                    Text(
                        text  = stringResource(R.string.titulo_tela),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ConteudoPrincipal(
                uiState         = uiState,
                onCampoAlterado = viewModel::onCampoAlterado,
                onBuscar        = viewModel::onBuscar,
                onLimpar        = viewModel::onLimpar,
                onRetentar      = viewModel::onBuscar,
                onAbrirScanner  = {
                    if (permissaoCamera.status.isGranted) {
                        viewModel.onAbrirScanner()
                    } else {
                        permissaoCamera.launchPermissionRequest()
                    }
                }
            )

            AnimatedVisibility(
                visible = uiState.scannerAberto,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                ScannerOverlay(
                    onCodigoLido   = viewModel::onCodigoEscaneado,
                    onFechar       = viewModel::onFecharScanner,
                    lifecycleOwner = lifecycleOwner,
                    context        = context
                )
            }

            // Banner de permissão negada — persistente (estado permanente, não Snackbar)
            if (!permissaoCamera.status.isGranted) {
                androidx.compose.runtime.LaunchedEffect(Unit) { /* permissão negada tratada inline */ }
            }
        }
    }
}

@Composable
private fun ConteudoPrincipal(
    uiState: BuscaUiState,
    onCampoAlterado: (String) -> Unit,
    onBuscar: () -> Unit,
    onLimpar: () -> Unit,
    onRetentar: () -> Unit,
    onAbrirScanner: () -> Unit
) {
    val context = LocalContext.current
    val nenhumResultado = !uiState.carregando
        && uiState.resultados.isEmpty()
        && uiState.campoBusca.isNotBlank()
        && uiState.erro == null

    androidx.compose.runtime.LaunchedEffect(nenhumResultado) {
        if (nenhumResultado) {
            val mp = MediaPlayer.create(context, R.raw.error)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        }
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Campo de busca + botão
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // ponytail: 8dp (era 16dp) — libera ~16dp de área útil vertical
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                ScannerField(
                    valor          = uiState.campoBusca,
                    onValorChange  = onCampoAlterado,
                    onBuscar       = onBuscar,
                    onAbrirScanner = onAbrirScanner,
                    onLimpar       = onLimpar,
                    modifier       = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick  = onBuscar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = uiState.campoBusca.isNotBlank() && !uiState.carregando,
                    shape    = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text  = if (uiState.carregando)
                                    stringResource(R.string.buscando)
                                else
                                    stringResource(R.string.botao_buscar),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Banner de erro inline — persistente até sucesso ou nova digitação
        if (uiState.erro != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    shape  = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = uiState.erro,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onRetentar) {
                            Text(
                                text  = stringResource(R.string.tentar_novamente),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Loading
        if (uiState.carregando) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Nenhum resultado
        if (!uiState.carregando && uiState.resultados.isEmpty()
            && uiState.campoBusca.isNotBlank() && uiState.erro == null
        ) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = stringResource(R.string.nenhum_resultado),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Resultados
        itemsIndexed(
            items = uiState.resultados,
            key   = { indice, _ -> indice }
        ) { indice, objeto ->
            ResultCard(objeto = objeto, indice = indice)
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Preview(showBackground = true, name = "Tela vazia")
@Composable
private fun PreviewTelaVazia() {
    BuscaMaterialTheme {
        ConteudoPrincipal(
            uiState         = BuscaUiState(),
            onCampoAlterado = {}, onBuscar = {}, onLimpar = {}, onRetentar = {}, onAbrirScanner = {}
        )
    }
}

@Preview(showBackground = true, name = "Tela com resultados")
@Composable
private fun PreviewComResultados() {
    val item = JsonObject().apply {
        addProperty("Material", "MAT-001234")
        addProperty("Descrição", "Parafuso M6 x 20mm Inox")
        addProperty("Unidade", "PC")
        addProperty("Estoque", "250")
    }
    BuscaMaterialTheme {
        ConteudoPrincipal(
            uiState         = BuscaUiState(campoBusca = "MAT-001234", resultados = listOf(item, item)),
            onCampoAlterado = {}, onBuscar = {}, onLimpar = {}, onRetentar = {}, onAbrirScanner = {}
        )
    }
}

@Preview(showBackground = true, name = "Tela com erro")
@Composable
private fun PreviewComErro() {
    BuscaMaterialTheme {
        ConteudoPrincipal(
            uiState         = BuscaUiState(campoBusca = "XYZ", erro = "Material não encontrado no sistema SAP."),
            onCampoAlterado = {}, onBuscar = {}, onLimpar = {}, onRetentar = {}, onAbrirScanner = {}
        )
    }
}

@Composable
private fun ScannerOverlay(
    onCodigoLido: (String) -> Unit,
    onFechar: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: android.content.Context
) {
    val executorRef = remember { mutableListOf<ExecutorService>() }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            executorRef.firstOrNull()?.shutdown()
            executorRef.clear()
            Log.d(TAG, "Executor da câmera encerrado.")
        }
    }

    val jaLeu = remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = iniciarScanner(
                    context        = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView    = previewView,
                    onCodigoLido   = { codigo ->
                        if (!jaLeu.value) {
                            jaLeu.value = true
                            onCodigoLido(codigo)
                        }
                    }
                )
                executorRef.add(executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick  = onFechar,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = stringResource(R.string.fechar_scanner),
                tint               = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
