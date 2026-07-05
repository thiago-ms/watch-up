package br.com.watchup.feature.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.repo.MidiaRepository
import br.com.watchup.core.ui.component.PushScreenScaffold
import kotlinx.coroutines.launch

/**
 * S017 — Atualizar progresso (somente Assistindo). Stepper −/+ limitado a
 * 0…disponíveis e grade de episódios com três estados: Visto, Disponível e
 * Bloqueado (episódios ainda não lançados, não clicáveis). Ao alcançar o
 * disponível, exibe o badge "Em dia".
 */
@Composable
fun ProgressScreen(
    midiaId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    val scope = rememberCoroutineScope()

    val midia by repo.observarPorId(midiaId).collectAsStateWithLifecycle(null)
    val episodios by repo.observarEpisodios(midiaId).collectAsStateWithLifecycle(emptyList())

    val m = midia
    PushScreenScaffold(title = "Atualizar progresso", onBack = onBack) { innerPadding ->
        if (m == null) {
            Text("Mídia não encontrada.", modifier = Modifier.padding(innerPadding).padding(16.dp))
            return@PushScreenScaffold
        }

        val disponiveis = m.episodiosDispTempAtual
        val totalTemporada = remember(episodios, disponiveis, m.temporadaAtual) {
            val registrado = episodios.firstOrNull { it.temporada == m.temporadaAtual }?.quantidade ?: 0
            maxOf(disponiveis, registrado)
        }
        var ultimoVisto by remember(m.id) { mutableIntStateOf(m.ultimoEpisodioVisto) }
        val emDia = disponiveis > 0 && ultimoVisto >= disponiveis

        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(m.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Temporada ${m.temporadaAtual} · $disponiveis episódio(s) disponível(is)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            // Stepper −/+
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalIconButton(
                    onClick = { if (ultimoVisto > 0) ultimoVisto-- },
                    enabled = ultimoVisto > 0,
                ) { Icon(Icons.Filled.Remove, contentDescription = "Menos um") }
                Text("$ultimoVisto", style = MaterialTheme.typography.headlineMedium)
                FilledTonalIconButton(
                    onClick = { if (ultimoVisto < disponiveis) ultimoVisto++ },
                    enabled = ultimoVisto < disponiveis,
                ) { Icon(Icons.Filled.Add, contentDescription = "Mais um") }

                if (emDia) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiary) {
                        Text(
                            "Em dia",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Grade de episódios (linhas de 5).
            if (totalTemporada > 0) {
                (1..totalTemporada).chunked(5).forEach { linha ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        linha.forEach { ep ->
                            val bloqueado = ep > disponiveis
                            val visto = ep <= ultimoVisto
                            EpisodeCell(
                                numero = ep,
                                visto = visto,
                                bloqueado = bloqueado,
                                onClick = { if (!bloqueado) ultimoVisto = ep },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        repo.salvar(m.copy(ultimoEpisodioVisto = ultimoVisto, statusUsuario = StatusUsuario.ASSISTINDO))
                        Toast.makeText(context, "Progresso atualizado", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Salvar progresso") }
        }
    }
}

@Composable
private fun EpisodeCell(numero: Int, visto: Boolean, bloqueado: Boolean, onClick: () -> Unit) {
    val cor = MaterialTheme.colorScheme
    when {
        bloqueado -> Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, cor.outlineVariant), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$numero", color = cor.outline, style = MaterialTheme.typography.labelMedium)
        }

        visto -> Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(cor.primary)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("$numero", color = cor.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }

        else -> Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, cor.primary), RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("$numero", color = cor.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}
