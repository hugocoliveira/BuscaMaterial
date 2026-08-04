package br.com.lit.busca.material.scanner

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ---------------------------------------------------------------------------
// Integração CameraX + ML Kit para leitura de QR Code e códigos de barras.
// A câmera é iniciada/encerrada pelo ciclo de vida do Composable chamador.
// ---------------------------------------------------------------------------

private const val TAG = "CameraScanner"

/**
 * Configura e inicia a câmera traseira com análise de imagem em tempo real
 * usando ML Kit Barcode Scanning.
 *
 * Deve ser chamado dentro de um LaunchedEffect ou similar, após a permissão
 * de câmera ter sido concedida.
 *
 * @param context       contexto Android (preferencialmente Activity ou Application).
 * @param lifecycleOwner dono do ciclo de vida que controla a câmera.
 * @param previewView   View onde o preview da câmera será renderizado.
 * @param onCodigoLido  callback chamado com o valor do código detectado.
 *                      Chamado na thread principal via post().
 * @return [ExecutorService] usado para análise — o chamador deve chamar
 *         [ExecutorService.shutdown] ao encerrar o scanner.
 */
fun iniciarScanner(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onCodigoLido: (String) -> Unit
): ExecutorService {
    // Executor dedicado para análise de imagem — fora da Main thread
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // Caso de uso de preview — exibe o feed da câmera no PreviewView
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Caso de uso de análise — processa cada frame para detecção de código
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer { codigo ->
                    // Repassa o resultado para a Main thread via handler do previewView
                    previewView.post { onCodigoLido(codigo) }
                })
            }

        // Câmera traseira — padrão para leitura de códigos em armazém
        val seletor = CameraSelector.DEFAULT_BACK_CAMERA

        runCatching {
            // Desvincula casos de uso anteriores antes de ligar os novos
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, seletor, preview, imageAnalyzer)
        }.onFailure { e ->
            Log.e(TAG, "Falha ao iniciar câmera: ${e.message}", e)
        }

    }, ContextCompat.getMainExecutor(context))

    return cameraExecutor
}

// ---------------------------------------------------------------------------
// Analisador interno — processa cada frame com o ML Kit Barcode Scanner.
// ---------------------------------------------------------------------------

/**
 * Implementação de [ImageAnalysis.Analyzer] que usa ML Kit para detectar
 * QR Codes e códigos de barras em cada frame da câmera.
 *
 * @param onCodigoDetectado callback chamado com o valor bruto do primeiro
 *                          código encontrado no frame.
 */
private class BarcodeAnalyzer(
    private val onCodigoDetectado: (String) -> Unit
) : ImageAnalysis.Analyzer {

    /** Scanner ML Kit — reutilizado entre frames para evitar alocações. */
    private val scanner = BarcodeScanning.getClient()

    /**
     * Analisa um frame da câmera em busca de códigos de barras/QR codes.
     * O frame é fechado ao final, liberando o buffer para o próximo.
     *
     * @param imageProxy frame da câmera encapsulado pelo CameraX.
     */
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val imagemMedia = imageProxy.image

        if (imagemMedia == null) {
            // Frame inválido — fecha e aguarda o próximo
            imageProxy.close()
            return
        }

        // Cria InputImage a partir do frame CameraX com rotação correta
        val imagem = InputImage.fromMediaImage(imagemMedia, imageProxy.imageInfo.rotationDegrees)

        scanner.process(imagem)
            .addOnSuccessListener { codigos ->
                // Pega apenas o primeiro código detectado no frame
                val primeiro = codigos.firstOrNull { !it.rawValue.isNullOrBlank() }
                primeiro?.rawValue?.let { onCodigoDetectado(it) }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Falha na análise do frame: ${e.message}")
            }
            .addOnCompleteListener {
                // Sempre fecha o frame — libera o buffer da câmera
                imageProxy.close()
            }
    }
}
