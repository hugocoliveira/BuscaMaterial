package br.com.lit.busca.material.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.lit.busca.material.data.repository.MaterialRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// ViewModel da tela principal de busca de material.
// Concentra todo o estado e lógica de negócio — os Composables são stateless.
// ---------------------------------------------------------------------------

/**
 * Estado imutável da tela de busca.
 *
 * @property campoBusca     texto atual do campo de entrada.
 * @property carregando     true enquanto aguarda resposta da API.
 * @property erro           mensagem de erro ou null se não há erro.
 * @property resultados     lista de objetos JSON retornados pela API.
 * @property scannerAberto  true quando a tela de câmera está visível.
 */
data class BuscaUiState(
    val campoBusca: String      = "",
    val carregando: Boolean     = false,
    val erro: String?           = null,
    val resultados: List<JsonObject> = emptyList(),
    val scannerAberto: Boolean  = false
)

/**
 * ViewModel que gerencia o estado e os efeitos da tela [BuscaMaterialScreen].
 * Usa [StateFlow] para emitir atualizações reativas à UI.
 */
class BuscaMaterialViewModel : ViewModel() {

    /** Repositório de acesso aos dados remotos. */
    private val repositorio = MaterialRepository.instance

    /** Estado interno mutável — exposto como imutável via [uiState]. */
    private val _uiState = MutableStateFlow(BuscaUiState())

    /** Estado observável pelos Composables. */
    val uiState: StateFlow<BuscaUiState> = _uiState.asStateFlow()

    // -----------------------------------------------------------------------
    // Eventos de UI
    // -----------------------------------------------------------------------

    /**
     * Atualiza o texto do campo de busca a cada tecla digitada.
     * Limpa qualquer erro anterior ao digitar.
     *
     * @param valor novo texto do campo.
     */
    fun onCampoAlterado(valor: String) {
        _uiState.update { it.copy(campoBusca = valor, erro = null) }
    }

    /**
     * Dispara a busca com o valor atual do campo.
     * Não faz nada se o campo estiver vazio.
     */
    fun onBuscar() {
        val valor = _uiState.value.campoBusca.trim()
        if (valor.isBlank()) return
        buscar(valor)
    }

    /**
     * Chamado quando a câmera detecta um código.
     * Fecha o scanner, preenche o campo e dispara a busca automaticamente.
     *
     * @param codigo valor bruto lido pela câmera.
     */
    fun onCodigoEscaneado(codigo: String) {
        _uiState.update { it.copy(campoBusca = codigo, scannerAberto = false) }
        buscar(codigo)
    }

    /** Abre a tela de câmera. */
    fun onAbrirScanner() {
        _uiState.update { it.copy(scannerAberto = true, erro = null) }
    }

    /** Fecha a tela de câmera sem buscar. */
    fun onFecharScanner() {
        _uiState.update { it.copy(scannerAberto = false) }
    }

    /** Limpa resultados, campo e erros — retorna ao estado inicial. */
    fun onLimpar() {
        _uiState.value = BuscaUiState()
    }

    // -----------------------------------------------------------------------
    // Lógica interna
    // -----------------------------------------------------------------------

    /**
     * Executa a chamada à API no [Dispatchers.IO] e atualiza o estado conforme
     * o resultado. Encapsula sucesso e falha sem lançar exceções ao chamador.
     *
     * @param valor texto a ser enviado no filtro OData.
     */
    private fun buscar(valor: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Indica loading e limpa estado anterior
            _uiState.update { it.copy(carregando = true, erro = null, resultados = emptyList()) }

            repositorio.buscar(valor).fold(
                onSuccess = { lista ->
                    _uiState.update {
                        it.copy(carregando = false, resultados = lista)
                    }
                },
                onFailure = { excecao ->
                    _uiState.update {
                        it.copy(
                            carregando = false,
                            erro = excecao.message ?: "Erro desconhecido"
                        )
                    }
                }
            )
        }
    }
}
