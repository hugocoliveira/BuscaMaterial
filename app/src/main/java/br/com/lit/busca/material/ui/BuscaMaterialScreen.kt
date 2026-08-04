package br.com.lit.busca.material.ui

import android.Manifest
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.lit.busca.material.R
import br.com.lit.busca.material.scanner.iniciarScanner
import br.com.lit.busca.material.ui.components.ResultCard
import br.com.lit.busca.material.ui.components.ScannerField
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService

// ---------------------------------------------------------------------------
// Tela principal do app — campo de busca, scanner e lista de resultados.
// Stateless: todo o estado vive no BuscaMaterialViewModel.
// ---------------------------------------------------------------------------

private const val TAG = "BuscaMaterialScreen"

/**
 * Tela única do BuscaMaterial.
 * Gerencia a permissão de câmera, exibe o campo de busca/scanner e a lista
 * de resultados retornados pela API SAP OData.
 *
 * @param viewModel instância do ViewModel fornecida pelo Compose (padrão: criada automaticamente).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BuscaMaterialScreen(
    viewModel: BuscaMaterialViewModel = viewModel()
) {
    // Estado atual da UI — coleta o Flow de forma ciclo-de-vida consciente
    val uiState by viewModel.uiState.collectAsState()

    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope          = rememberCoroutineScope()
    val snackbarHost   = remember { SnackbarHostState() }

    // Gerenciamento de permissão de câmera via Accompanist
    val permissaoCamera = rememberPermissionState(Manifest.permission.CAMERA)

    // Exibe erro no Snackbar sempre que o estado de erro mudar
    LaunchedEffect(uiState.erro) {
        uiState.erro?.let { mensagem ->
            scope.launch {
                snackbarHost.showSnackbar(mensagem)
            }
        }
    }

    Scaffold(
        // TopAppBar azul com título do app
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = stringResource(R.string.titulo_tela),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHost) { dados ->
                Snackbar(
                    snackbarData     = dados,
                    containerColor   = MaterialTheme.colorScheme.error,
                    contentColor     = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Conteúdo principal: campo + resultados
            ConteudoPrincipal(
                uiState        = uiState,
                onCampoAlterado = viewModel::onCampoAlterado,
                onBuscar        = viewModel::onBuscar,
                onLimpar        = viewModel::onLimpar,
                onAbrirScanner  = {
                    // Solicita permissão antes de abrir o scanner
                    if (permissaoCamera.status.isGranted) {
                        viewModel.onAbrirScanner()
                    } else {
                        permissaoCamera.launchPermissionRequest()
                    }
                }
            )

            // Overlay do scanner — exibido sobre o conteúdo quando scannerAberto = true
            AnimatedVisibility(
                visible = uiState.scannerAberto,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                ScannerOverlay(
                    onCodigoLido  = viewModel::onCodigoEscaneado,
                    onFechar      = viewModel::onFecharScanner,
                    lifecycleOwner = lifecycleOwner,
                    context       = context
                )
            }

            // Mensagem de permissão negada
            if (!permissaoCamera.status.isGranted && permissaoCamera.status.shouldShowRationale) {
                LaunchedEffect(Unit) {
                    snackbarHost.showSnackbar(
                        context.getString(R.string.permissao_camera_negada)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Conteúdo principal: campo de busca + estado de loading + resultados
// ---------------------------------------------------------------------------

/**
 * Área principal da tela com campo de busca, botão buscar e lista de resultados.
 * Stateless — todos os valores vêm do [BuscaUiState].
 *
 * @param uiState         estado atual da UI.
 * @param onCampoAlterado callback ao digitar no campo.
 * @param onBuscar        callback ao pressionar buscar.
 * @param onLimpar        callback ao limpar o campo.
 * @param onAbrirScanner  callback ao tocar no ícone de câmera.
 */
@Composable
private fun ConteudoPrincipal(
    uiState: BuscaUiState,
    onCampoAlterado: (String) -> Unit,
    onBuscar: () -> Unit,
    onLimpar: () -> Unit,
    onAbrirScanner: () -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Cabeçalho: campo de busca + botão buscar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Campo com ícone de scanner
                ScannerField(
                    valor          = uiState.campoBusca,
                    onValorChange  = onCampoAlterado,
                    onBuscar       = onBuscar,
                    onAbrirScanner = onAbrirScanner,
                    onLimpar       = onLimpar,
                    modifier       = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Botão buscar
                Button(
                    onClick  = onBuscar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = uiState.campoBusca.isNotBlank() && !uiState.carregando,
                    shape    = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text  = stringResource(R.string.botao_buscar),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Estado de loading
        if (uiState.carregando) {
            item {
                Box(
                    modifier        = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Nenhum resultado (após busca concluída sem resultados e sem erro)
        if (!uiState.carregando && uiState.resultados.isEmpty()
            && uiState.campoBusca.isNotBlank() && uiState.erro == null
        ) {
            item {
                Box(
                    modifier        = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = stringResource(R.string.nenhum_resultado),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Lista de resultados — um card por objeto JSON
        itemsIndexed(
            items = uiState.resultados,
            // key estável para evitar recomposições incorretas
            key   = { indice, _ -> indice }
        ) { indice, objeto ->
            ResultCard(objeto = objeto, indice = indice)
        }

        // Espaço final para não ficar colado na navigation bar
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Overlay de câmera — cobre toda a tela durante o scan
// ---------------------------------------------------------------------------

/**
 * Overlay de tela cheia que exibe o preview da câmera para leitura de código.
 * Inicia e encerra a câmera conforme o ciclo de vida do Composable.
 *
 * @param onCodigoLido   callback chamado com o código detectado.
 * @param onFechar       callback para fechar o overlay sem escanear.
 * @param lifecycleOwner dono do ciclo de vida para o CameraX.
 * @param context        contexto Android.
 */
@Composable
private fun ScannerOverlay(
    onCodigoLido: (String) -> Unit,
    onFechar: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: android.content.Context
) {
    // Referência ao ExecutorService — encerrado quando o Composable sai da composição
    val executorRef = remember { mutableListOf<ExecutorService>() }

    // Garante encerramento do executor ao sair do scanner
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            executorRef.firstOrNull()?.shutdown()
            executorRef.clear()
            Log.d(TAG, "Executor da câmera encerrado.")
        }
    }

    // Flag para disparar onCodigoLido apenas uma vez por sessão de scan
    val jaLeu = remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Preview da câmera via AndroidView (CameraX usa View clássica)
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = iniciarScanner(
                    context        = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView    = previewView,
                    onCodigoLido   = { codigo ->
                        // Evita múltiplos disparos do mesmo código
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

        // Botão fechar no canto superior direito
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
