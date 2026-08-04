package br.com.lit.busca.material.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject

// ---------------------------------------------------------------------------
// Card genérico para exibir um objeto de resultado da API.
// Lê dinamicamente todas as chaves/valores do JsonObject sem assumir campos
// fixos — garante que qualquer campo retornado pelo SAP seja exibido.
// ---------------------------------------------------------------------------

/**
 * Card que exibe todos os campos de um [JsonObject] em pares chave → valor.
 * Sem campos fixos: itera todas as entradas do objeto dinamicamente.
 *
 * @param objeto    objeto JSON com os campos do material.
 * @param indice    posição na lista — exibida como rótulo do card (ex: "Item 1").
 * @param modifier  modificador opcional para personalização externa.
 */
@Composable
fun ResultCard(
    objeto: JsonObject,
    indice: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Cabeçalho do card com o número do item
            Text(
                text  = "Item ${indice + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Spacer(modifier = Modifier.height(8.dp))

            // Itera dinamicamente todas as entradas do JsonObject
            objeto.entrySet().forEach { (chave, valor) ->
                // Formata o valor: remove aspas extras do JsonPrimitive
                val valorTexto = when {
                    valor.isJsonNull    -> "—"
                    valor.isJsonPrimitive -> valor.asString
                    else                -> valor.toString()
                }

                // Linha: Chave → Valor
                CampoLinha(chave = chave, valor = valorTexto)

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Componente auxiliar — linha de um campo com rótulo e valor.
// ---------------------------------------------------------------------------

/**
 * Linha simples "chave: valor" para exibição de campos do resultado.
 *
 * @param chave nome do campo (retornado pela API).
 * @param valor texto do valor a ser exibido.
 */
@Composable
private fun CampoLinha(chave: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Rótulo do campo (chave)
        Text(
            text     = "$chave:",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(140.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Valor do campo
        Text(
            text  = valor,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
