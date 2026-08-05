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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.lit.busca.material.ui.theme.BuscaMaterialTheme
import com.google.gson.JsonObject

@Composable
fun ResultCard(
    objeto: JsonObject,
    indice: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            // ponytail: 4dp (era 6dp) — menos espaço entre cards em tela 16×20
            .padding(horizontal = 16.dp, vertical = 4.dp),
        // ponytail: 6dp (era 12dp) — cantos menos arredondados ficam mais densos em coletor
        shape     = RoundedCornerShape(6.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            // ponytail: 10dp (era 16dp) — padding interno comprimido para coletor 16×20
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text       = "Item ${indice + 1}",
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Spacer(modifier = Modifier.height(4.dp))

            val campos = listOf(
                "Material"            to "Material",
                "MaterialDescription" to "Descrição material",
                "Ean"                 to "EAN",
                "NetWeight"           to "Peso líquido",
                "WeightUnit"          to "Unidade de peso",
                "Volume"              to "Volume",
                "VolumeUnit"          to "Unidade do volume",
                "MaterialGroup"       to "Grupo de mercadoria"
            )
            campos.forEach { (chaveApi, rotulo) ->
                val elemento = objeto.get(chaveApi)
                val valorTexto = when {
                    elemento == null || elemento.isJsonNull -> "—"
                    elemento.isJsonPrimitive               -> elemento.asString
                    else                                   -> elemento.toString()
                }
                CampoLinha(chave = rotulo, valor = valorTexto)
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultCardPreview() {
    BuscaMaterialTheme {
        val objeto = JsonObject().apply {
            addProperty("Material", "MAT-001234")
            addProperty("Descrição", "Parafuso M6 x 20mm Inox")
            addProperty("Unidade", "PC")
            addProperty("Estoque", "250")
            addProperty("Depósito", "0001")
        }
        ResultCard(objeto = objeto, indice = 0)
    }
}

@Composable
private fun CampoLinha(chave: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Rótulo (chave) — secundário, bodySmall
        Text(
            text       = "$chave:",
            style      = MaterialTheme.typography.bodySmall,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            // ponytail: 120dp (era 140dp) — chave mais compacta, valor ganha mais espaço
            modifier   = Modifier.width(120.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Valor — dado primário, bodyMedium (14sp) para leitura com luvas em coletor
        Text(
            text     = valor,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
