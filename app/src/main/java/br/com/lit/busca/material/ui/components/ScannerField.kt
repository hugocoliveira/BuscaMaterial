package br.com.lit.busca.material.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.lit.busca.material.R

// ---------------------------------------------------------------------------
// Campo de busca com botão de scanner integrado.
// Stateless — recebe estado e emite eventos via lambdas.
// ---------------------------------------------------------------------------

/**
 * Campo de texto para digitar ou escanear o código do material.
 *
 * Exibe:
 * - Ícone de scanner (câmera) à direita — abre o scanner ao clicar.
 * - Ícone de limpar (X) quando há texto — limpa o campo ao clicar.
 * - Ação de teclado "Buscar" (ImeAction.Search) para disparar busca pelo teclado.
 *
 * @param valor          texto atual do campo.
 * @param onValorChange  callback chamado a cada alteração do texto.
 * @param onBuscar       callback chamado ao pressionar "Buscar" no teclado.
 * @param onAbrirScanner callback chamado ao tocar no ícone da câmera.
 * @param onLimpar       callback chamado ao tocar no ícone de limpar.
 * @param modifier       modificador opcional.
 */
@Composable
fun ScannerField(
    valor: String,
    onValorChange: (String) -> Unit,
    onBuscar: () -> Unit,
    onAbrirScanner: () -> Unit,
    onLimpar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value         = valor,
        onValueChange = onValorChange,
        // onKeyEvent captura eventos do scanner físico Zebra (infravermelho) e fecha o teclado virtual.
        // Toque do usuário usa InputConnection e não passa por aqui, então o teclado ainda abre ao digitar.
        modifier      = modifier
            .onFocusChanged { if (it.isFocused) keyboardController?.hide() }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) keyboardController?.hide()
                false
            },
        label         = { Text(stringResource(R.string.label_campo_busca)) },
        placeholder   = { Text(stringResource(R.string.placeholder_campo_busca)) },
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),

        // Ícone de ação à direita: limpar (quando há texto) ou scanner (quando vazio)
        trailingIcon = {
            if (valor.isNotEmpty()) {
                // Botão de limpar — remove o texto atual
                IconButton(onClick = onLimpar) {
                    Icon(
                        imageVector        = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.botao_limpar),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Botão de scanner — abre a câmera para leitura de código
                IconButton(onClick = onAbrirScanner) {
                    Icon(
                        imageVector        = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.descricao_icone_scanner),
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },

        // Teclado alfanumérico — cobre EAN numérico e códigos alfanuméricos SAP
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction    = ImeAction.Search
        ),

        // Ação do botão "Buscar" no teclado virtual
        keyboardActions = KeyboardActions(
            onSearch = { onBuscar() }
        )
    )
}
