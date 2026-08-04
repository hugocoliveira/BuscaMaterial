package br.com.lit.busca.material.data.remote

import com.google.gson.JsonObject

// ---------------------------------------------------------------------------
// Modelos de resposta da API SAP OData v4.
// A resposta tem o envelope: { "value": [ { campo: valor, ... }, ... ] }
// Usamos JsonObject para manter a leitura genérica — sem assumir campos fixos,
// pois a estrutura pode variar conforme a consulta ou versão do serviço SAP.
// ---------------------------------------------------------------------------

/**
 * Envelope principal da resposta OData v4.
 * O campo [value] contém a lista de objetos retornados.
 *
 * @property value lista de objetos JSON brutos (campos dinâmicos).
 */
data class ODataResponse(
    val value: List<JsonObject>
)
