package br.com.watchup.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.watchup.core.data.domain.FiltroLancamento
import br.com.watchup.core.data.domain.JanelaData
import br.com.watchup.core.data.domain.combinaFiltro
import br.com.watchup.core.data.domain.deriveStatusMidia
import br.com.watchup.core.data.domain.emAndamento
import br.com.watchup.core.data.domain.episodiosFaltantes
import br.com.watchup.core.data.domain.estaEmDia
import br.com.watchup.core.data.domain.fracaoProgresso
import br.com.watchup.core.data.domain.janelaData
import br.com.watchup.core.data.domain.novosEpisodiosEstimados
import br.com.watchup.core.data.model.Midia
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.repo.MidiaRepository
import java.time.LocalDate
import br.com.watchup.core.ui.component.EmptyState
import br.com.watchup.core.ui.component.JanelaTag
import br.com.watchup.core.ui.component.LabeledProgressBar
import br.com.watchup.core.ui.component.MediaPoster
import br.com.watchup.core.ui.component.SectionHeader
import br.com.watchup.core.ui.component.StatusMidiaChip
import br.com.watchup.core.ui.component.formatarData

/**
 * S001 — Início/Home. Reúne "Continuar assistindo" (mídias episódicas em
 * andamento) e o radar de Lançamentos: filtros por tipo de data + duas seções —
 * "Em cartaz" (já estreou / no ar) e "Próximas datas" (unificada, cada item com a
 * tag da sua janela temporal: esta semana, semana que vem, este mês, … , sem data).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDetail: (Long) -> Unit,
    onAdicionar: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    // null = ainda carregando (evita piscar o estado vazio antes da 1ª emissão do Room).
    val todas by repo.observarTodas().collectAsStateWithLifecycle(null)

    var filtro by remember { mutableStateOf(FiltroLancamento.TODOS) }

    // Itens 9 e 8: arquivadas e intenções de assistir não aparecem na Home.
    val ativas = remember(todas) { todas.orEmpty().filterNot { it.arquivada || it.intencao } }
    val emAndamento = remember(ativas) { emAndamento(ativas) }
    // Radar: cada mídia com sua janela temporal (item 3). "Em cartaz" é seção própria;
    // as demais entram numa única seção "Próximas datas", cada uma com sua tag.
    val radar = remember(ativas, filtro) {
        // Item 1: mídias já vistas não entram no radar da Home.
        ativas
            .filter { it.statusUsuario != StatusUsuario.VISTO && combinaFiltro(it, filtro) }
            .map { it to janelaData(it) }
    }
    val emCartaz = remember(radar) {
        radar.filter { it.second == JanelaData.EM_CARTAZ }
            .sortedWith(compareBy(nullsLast<LocalDate>()) { it.first.dataPrincipal })
            .map { it.first }
    }
    // Item 2: ordenar por janela (mais próxima primeiro) e, dentro dela, por data;
    // itens sem data fixa vão ao fim.
    val proximas = remember(radar) {
        radar.filter { it.second != JanelaData.EM_CARTAZ }
            .sortedWith(
                compareBy<Pair<Midia, JanelaData>> { it.second.prioridade }
                    .thenComparing(compareBy(nullsLast<LocalDate>()) { it.first.dataPrincipal }),
            )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("WatchUp") }) },
    ) { innerPadding ->
        if (todas == null) return@Scaffold // carregando: nada a exibir ainda
        if (todas.orEmpty().isEmpty()) {
            EmptyState(
                mensagem = "Sua biblioteca está vazia. Que tal adicionar a primeira mídia?",
                modifier = Modifier.padding(innerPadding),
                cta = {
                    Button(onClick = onAdicionar) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar mídia")
                    }
                },
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // "Continuar assistindo" — grade vertical de 2 por linha (sem scroll horizontal).
            if (emAndamento.isNotEmpty()) {
                SectionHeader("Continuar assistindo")
                Spacer(Modifier.height(12.dp))
                emAndamento.chunked(2).forEach { linha ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        linha.forEach { midia ->
                            ContinuarCard(
                                midia = midia,
                                onClick = { onOpenDetail(midia.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (linha.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            // Radar de Lançamentos.
            SectionHeader("Lançamentos")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroLancamento.entries.forEach { f ->
                    FilterChip(
                        selected = filtro == f,
                        onClick = { filtro = f },
                        label = { Text(f.rotulo) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Seção "Em cartaz" — o que já estreou / está no ar.
            if (emCartaz.isNotEmpty()) {
                SectionHeader("Em cartaz", trailing = "${emCartaz.size}")
                Spacer(Modifier.height(8.dp))
                emCartaz.forEach { midia ->
                    LancamentoRow(midia = midia, onClick = { onOpenDetail(midia.id) })
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            // Seção unificada "Próximas datas" — cada item com a tag da sua janela.
            SectionHeader("Próximas datas", trailing = "${proximas.size}")
            Spacer(Modifier.height(8.dp))
            if (proximas.isEmpty()) {
                Text(
                    "Nenhum item neste filtro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                proximas.forEach { (midia, janela) ->
                    LancamentoRow(
                        midia = midia,
                        janela = janela,
                        onClick = { onOpenDetail(midia.id) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ContinuarCard(midia: Midia, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp)) {
            MediaPoster(
                titulo = midia.titulo,
                posterUrl = midia.posterUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                midia.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            LabeledProgressBar(
                fracao = fracaoProgresso(midia),
                label = if (estaEmDia(midia)) "Em dia" else "Faltam ${episodiosFaltantes(midia)} ep.",
            )
            // Item 4: indicação sutil de novos episódios estimados.
            val novos = novosEpisodiosEstimados(midia)
            if (novos > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "+$novos novo(s) ep.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Subtítulo da linha de lançamento: para episódica "lançando" com dia/horário
 * definidos mostra a cadência ("Toda Quinta · 20:00"); senão, a data formatada.
 */
private fun subtituloLancamento(m: Midia): String {
    val cadencia = listOfNotNull(
        m.diaLancamento?.takeIf { it.isNotBlank() }?.let { "Toda $it" },
        m.horarioLancamento?.takeIf { it.isNotBlank() },
    )
    return if (cadencia.isNotEmpty()) cadencia.joinToString(" · ") else formatarData(m.dataPrincipal)
}

@Composable
private fun LancamentoRow(midia: Midia, onClick: () -> Unit, janela: JanelaData? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaPoster(titulo = midia.titulo, posterUrl = midia.posterUrl, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(midia.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtituloLancamento(midia),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // "Próximas datas" mostra a tag da janela; "Em cartaz" mostra o status.
            Box { if (janela != null) JanelaTag(janela) else StatusMidiaChip(deriveStatusMidia(midia)) }
        }
    }
}
