package br.com.lit.busca.material.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ---------------------------------------------------------------------------
// MaterialTheme customizado para o BuscaMaterial.
// Usa apenas light scheme — sem suporte a dark mode por ora (operadores de
// armazém usam dispositivos em ambientes com iluminação controlada).
// ---------------------------------------------------------------------------

/** Esquema de cores baseado na paleta LIT. */
private val LitColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = PrimaryContainer,
    surface          = Surface,
    surfaceVariant   = SurfaceVariant,
    onSurface        = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error            = ErrorColor,
    outline          = Outline
)

/**
 * Tema raiz do aplicativo. Deve envolver toda a hierarquia de Composables
 * que precisam de acesso às cores e tipografia do projeto.
 *
 * @param content conteúdo filho a ser renderizado dentro do tema.
 */
@Composable
fun BuscaMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LitColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
