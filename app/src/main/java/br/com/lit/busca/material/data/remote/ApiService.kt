package br.com.lit.busca.material.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// ---------------------------------------------------------------------------
// Interface Retrofit que declara os endpoints da API SAP OData v4.
// O Retrofit gera a implementação em tempo de execução via proxy dinâmico.
// ---------------------------------------------------------------------------

/**
 * Serviço de acesso à API SAP OData — busca de materiais por valor escaneado.
 *
 * URL base configurada em [NetworkModule]: http://vm77.4hub.cloud:57700/
 * Caminho completo do endpoint:
 *   sap/opu/odata4/sap/zsb_hco_busca_material/srvd_a2x/sap/zsd_hco_busca_material/0001/Material
 */
interface ApiService {

    /**
     * Busca materiais filtrando pelo valor escaneado/digitado.
     *
     * O parâmetro [filter] deve ser passado já codificado (encoded=true) pois o SAP OData
     * exige que as aspas simples ao redor do valor apareçam como %27 na URL.
     * Exemplo: ScannedValue%20eq%20%27EWMS4-503%27
     *
     * @param filter    filtro OData já URL-encoded
     * @param client    mandante SAP (padrão: 100)
     * @param format    formato de resposta (padrão: json)
     * @return [ODataResponse] com a lista de materiais encontrados.
     */
    @GET(
        "sap/opu/odata4/sap/zsb_hco_busca_material/srvd_a2x/sap/zsd_hco_busca_material/0001/Material"
    )
    suspend fun buscarMaterial(
        @Query(value = "\$filter", encoded = true) filter: String,
        @Query("sap-client")    client: String = "100",
        @Query("\$format")      format: String = "json"
    ): ODataResponse
}
