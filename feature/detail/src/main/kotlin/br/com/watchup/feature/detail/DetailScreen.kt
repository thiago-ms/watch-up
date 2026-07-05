package br.com.watchup.feature.detail

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.watchup.core.data.domain.deriveStatusMidia
import br.com.watchup.core.data.domain.disponibilidadeVisivel
import br.com.watchup.core.data.domain.episodiosFaltantes
import br.com.watchup.core.data.domain.estaEmDia
import br.com.watchup.core.data.domain.fracaoProgresso
import br.com.watchup.core.data.domain.permiteVisto
import br.com.watchup.core.data.domain.temCardProgresso
import br.com.watchup.core.data.model.EpisodiosTemporada
import br.com.watchup.core.data.model.Midia
import br.com.watchup.core.data.model.Modalidade
import br.com.watchup.core.data.model.StatusLancEpisodico
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.repo.MidiaRepository
import br.com.watchup.core.ui.component.LabeledProgressBar
import br.com.watchup.core.ui.component.MediaPoster
import br.com.watchup.core.ui.component.PushScreenScaffold
import br.com.watchup.core.ui.component.StatusMidiaChip
import br.com.watchup.core.ui.component.StatusUsuarioChip
import br.com.watchup.core.ui.component.formatarData
import kotlinx.coroutines.launch

// Índices das etapas do cadastro (espelham PassoCadastro no módulo :feature:registration).
private const val ETAPA_DETALHES = 2
private const val ETAPA_ONDE_ASSISTIR = 3
private const val ETAPA_DATAS_STATUS = 4

/**
 * S016 — Detalhe da mídia. Cabeçalho com dois status (mídia/usuário), seletor de
 * "Seu status" editável, blocos condicionais (progresso, aviso "vai lançar"),
 * ficha (cada linha abre a edição na etapa correspondente) e a lista editável de
 * episódios por temporada (salva no ato).
 */
@Composable
fun DetailScreen(
    midiaId: Long,
    onBack: () -> Unit,
    onEditar: (Long, Int) -> Unit,
    onAtualizarProgresso: (Long) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    val scope = rememberCoroutineScope()

    val midia by repo.observarPorId(midiaId).collectAsStateWithLifecycle(null)
    val episodios by repo.observarEpisodios(midiaId).collectAsStateWithLifecycle(emptyList())

    var confirmarExclusao by remember { mutableStateOf(false) }

    val m = midia

    if (confirmarExclusao && m != null) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Remover mídia?") },
            text = { Text("\"${m.titulo}\" será removida da sua biblioteca. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarExclusao = false
                    scope.launch {
                        repo.remover(m)
                        Toast.makeText(context, "Mídia removida", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
            },
        )
    }

    PushScreenScaffold(
        title = m?.titulo ?: "Detalhe",
        onBack = onBack,
        actions = {
            if (m != null) {
                IconButton(onClick = { onEditar(m.id, 0) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { confirmarExclusao = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remover")
                }
            }
        },
    ) { innerPadding ->
        if (m == null) {
            Text("Mídia não encontrada.", modifier = Modifier.padding(innerPadding).padding(16.dp))
            return@PushScreenScaffold
        }

        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Cabecalho(m)
            Spacer(Modifier.height(16.dp))

            SeuStatusSelector(m) { novo ->
                scope.launch { repo.salvar(m.copy(statusUsuario = novo)) }
            }
            Spacer(Modifier.height(16.dp))

            when {
                m.tipo.episodica && m.statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR ->
                    AvisoCard(avisoVaiLancar(m))

                temCardProgresso(m) ->
                    CardProgresso(m, onAtualizar = { onAtualizarProgresso(m.id) })
            }

            Spacer(Modifier.height(16.dp))
            Ficha(m, onEditar = { etapa -> onEditar(m.id, etapa) })

            // Episódios por temporada — editável (oculto para "vai lançar").
            if (disponibilidadeVisivel(m.tipo, m.statusLancEpisodico) && m.temporadasDisponiveis > 0) {
                Spacer(Modifier.height(16.dp))
                Text("Episódios por temporada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val mapa = remember(episodios) { episodios.associate { it.temporada to it.quantidade } }
                (1..m.temporadasDisponiveis).forEach { temporada ->
                    val atual = mapa[temporada]
                        ?: if (temporada == m.temporadaAtual) m.episodiosDispTempAtual else 0
                    EpisodiosTemporadaRow(
                        temporada = temporada,
                        quantidade = atual,
                        onAlterar = { nova ->
                            scope.launch {
                                repo.salvarEpisodios(EpisodiosTemporada(m.id, temporada, nova))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Cabecalho(m: Midia) {
    Row {
        MediaPoster(titulo = m.titulo, posterUrl = m.posterUrl, modifier = Modifier.size(width = 96.dp, height = 128.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(m.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            val meta = listOfNotNull(
                m.tipo.rotulo,
                m.ano?.toString(),
                m.generos.takeIf { it.isNotEmpty() }?.joinToString(", "),
            ).joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusMidiaChip(deriveStatusMidia(m))
                StatusUsuarioChip(m)
            }
        }
    }
}

/** Seletor de "Seu status" editável (§5.5): persiste na hora. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeuStatusSelector(m: Midia, onSelecionar: (StatusUsuario) -> Unit) {
    Text("Seu status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    val opcoes = if (m.tipo.episodica) {
        listOf(StatusUsuario.QUERO_ASSISTIR, StatusUsuario.ASSISTINDO, StatusUsuario.VISTO)
    } else {
        listOf(StatusUsuario.QUERO_ASSISTIR, StatusUsuario.VISTO)
    }
    val vistoBloqueado = !permiteVisto(m.tipo, m.statusLancEpisodico)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { s ->
            val bloqueado = s == StatusUsuario.VISTO && vistoBloqueado
            FilterChip(
                selected = m.statusUsuario == s,
                enabled = !bloqueado,
                onClick = { if (!bloqueado && m.statusUsuario != s) onSelecionar(s) },
                label = { Text(s.rotulo) },
                trailingIcon = if (bloqueado) {
                    { Icon(Icons.Filled.Lock, contentDescription = "Requer status Completa ou Cancelada", modifier = Modifier.height(16.dp)) }
                } else {
                    null
                },
            )
        }
    }
    if (vistoBloqueado) {
        Text("\"Visto\" requer status Completa ou Cancelada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Texto do aviso para episódica "vai lançar" (série nova × temporada nova). */
private fun avisoVaiLancar(m: Midia): String =
    if (m.temporadasDisponiveis > 0) {
        "Nova temporada ${m.temporadasDisponiveis + 1} a caminho — " +
            "${m.temporadasDisponiveis} temporada(s) já disponível(is). Sem progresso por enquanto."
    } else {
        "Ainda vai lançar — sem disponibilidade ou progresso por enquanto."
    }

@Composable
private fun AvisoCard(texto: String) {
    Card(Modifier.fillMaxWidth()) {
        Text(texto, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CardProgresso(m: Midia, onAtualizar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Temporada ${m.temporadaAtual}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            LabeledProgressBar(
                fracao = fracaoProgresso(m),
                label = "${m.ultimoEpisodioVisto} de ${m.episodiosDispTempAtual} episódios",
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (estaEmDia(m)) "Você está em dia!" else "Faltam ${episodiosFaltantes(m)} episódio(s)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!m.diaLancamento.isNullOrBlank() || !m.horarioLancamento.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                val quando = listOfNotNull(m.diaLancamento, m.horarioLancamento).joinToString(" · ")
                Text(
                    "Próximo episódio: $quando",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(onClick = onAtualizar) { Text("Atualizar progresso") }
        }
    }
}

@Composable
private fun Ficha(m: Midia, onEditar: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                "Ficha",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // Lançamento: episódica usa o status escolhido; não-episódica (filme) usa o derivado.
            val lancamento = m.statusLancEpisodico?.rotulo ?: deriveStatusMidia(m).rotulo
            FichaLinha("Lançamento", lancamento) { onEditar(ETAPA_DATAS_STATUS) }
            FichaLinha("Onde assistir", ondeAssistir(m)) { onEditar(ETAPA_ONDE_ASSISTIR) }
            FichaLinha("Contexto", m.contexto.rotulo) { onEditar(ETAPA_DETALHES) }
            FichaLinha("Status da data", m.statusData.rotulo) { onEditar(ETAPA_DATAS_STATUS) }
            if (m.dataPrincipal != null) FichaLinha("Data", formatarData(m.dataPrincipal)) { onEditar(ETAPA_DATAS_STATUS) }
        }
    }
}

private fun ondeAssistir(m: Midia): String = when (m.modalidade) {
    Modalidade.STREAMING -> {
        val principal = m.streamingPrincipal?.let { " (★ $it)" }.orEmpty()
        "Streaming: ${m.streamings.joinToString(", ")}$principal"
    }
    Modalidade.CINEMA -> "Cinema: ${m.cinemaRede ?: "—"}"
    Modalidade.OUTRO -> "Outro"
    Modalidade.NAO_SEI -> "Não sei ainda"
}

@Composable
private fun FichaLinha(rotulo: String, valor: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(rotulo, Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Editar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EpisodiosTemporadaRow(temporada: Int, quantidade: Int, onAlterar: (Int) -> Unit) {
    var editando by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }

    if (editando) {
        AlertDialog(
            onDismissRequest = { editando = false },
            title = { Text("Episódios da temporada $temporada") },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.filter(Char::isDigit).take(4) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Quantidade") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAlterar(texto.toIntOrNull() ?: quantidade)
                    editando = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editando = false }) { Text("Cancelar") } },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Temporada $temporada", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = { if (quantidade > 0) onAlterar(quantidade - 1) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Menos um episódio")
        }
        // Toque no número abre o campo para digitar a quantidade.
        Text(
            "$quantidade",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable {
                    texto = quantidade.toString()
                    editando = true
                }
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        IconButton(onClick = { onAlterar(quantidade + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Mais um episódio")
        }
    }
    HorizontalDivider()
}
