package br.com.lit.busca.material.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.lit.busca.material.ui.theme.BuscaMaterialTheme

// ---------------------------------------------------------------------------
// Entry point do aplicativo.
// Responsabilidade única: inicializar o tema e exibir a tela raiz.
// Toda lógica de estado e negócio fica no BuscaMaterialViewModel.
// ---------------------------------------------------------------------------

/**
 * Activity principal e única do BuscaMaterial.
 * Não possui intent-filter de LAUNCHER — é iniciada via Intent explícita
 * pelo aplicativo MenuAutomatico (launcher controlado do armazém).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita layout edge-to-edge (conteúdo vai até as bordas da tela)
        enableEdgeToEdge()

        setContent {
            // Aplica o tema LIT em toda a hierarquia de Composables
            BuscaMaterialTheme {
                // Tela única do app
                BuscaMaterialScreen()
            }
        }
    }
}
