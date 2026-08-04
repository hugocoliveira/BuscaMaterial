package br.com.lit.busca.material.data.repository

import br.com.lit.busca.material.data.remote.NetworkModule
import br.com.lit.busca.material.data.remote.ODataResponse
import com.google.gson.JsonObject

// ---------------------------------------------------------------------------
// Repositório de materiais — ponto central de acesso a dados.
// Orquestra a chamada à API remota e devolve o resultado ao ViewModel
// encapsulado em Result<T> para tratamento de sucesso/erro sem exceções.
// ---------------------------------------------------------------------------

/**
 * Repositório responsável por buscar materiais via API SAP OData.
 *
 * Singleton manual via [instance] — sem Hilt por ora.
 * ponytail: sem cache local; adicionar Room se o app precisar de offline.
 */
class MaterialRepository private constructor() {

    /** Referência ao serviço HTTP configurado com Basic Auth. */
    private val apiService = NetworkModule.apiService

    /**
     * Busca materiais pelo valor informado (digitado ou escaneado).
     *
     * Executa em qualquer coroutine — o chamador é responsável por despachar
     * em [kotlinx.coroutines.Dispatchers.IO].
     *
     * @param valor texto digitado ou lido pela câmera.
     * @return [Result] com lista de [JsonObject] (sucesso) ou exceção (falha).
     */
    suspend fun buscar(valor: String): Result<List<JsonObject>> {
        return runCatching {
            // Monta o filtro OData com aspas codificadas como %27 — exigido pelo SAP OData v4.
            // Exemplo final na URL: ScannedValue%20eq%20%27EWMS4-503%27
            val valorCodificado = java.net.URLEncoder.encode(valor, "UTF-8")
            val filtro = "ScannedValue%20eq%20%27${valorCodificado}%27"
            val resposta: ODataResponse = apiService.buscarMaterial(filter = filtro)
            resposta.value
        }
    }

    companion object {
        /** Instância única do repositório — criada de forma lazy e thread-safe. */
        val instance: MaterialRepository by lazy { MaterialRepository() }
    }
}
