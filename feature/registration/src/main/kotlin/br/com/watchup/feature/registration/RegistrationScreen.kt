package br.com.watchup.feature.registration

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.watchup.core.data.domain.disponibilidadeVisivel
import br.com.watchup.core.data.domain.permiteVisto
import br.com.watchup.core.data.domain.progressoVisivel
import br.com.watchup.core.data.model.Contexto
import br.com.watchup.core.data.model.GENEROS_DISPONIVEIS
import br.com.watchup.core.data.model.Midia
import br.com.watchup.core.data.model.Modalidade
import br.com.watchup.core.data.model.REDES_CINEMA
import br.com.watchup.core.data.model.STREAMINGS_DISPONIVEIS
import br.com.watchup.core.data.model.StatusData
import br.com.watchup.core.data.model.StatusLancEpisodico
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.model.TipoMidia
import br.com.watchup.core.data.repo.MidiaRepository
import br.com.watchup.core.ui.component.PushScreenScaffold
import br.com.watchup.core.ui.component.formatarData
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * S010–S015 (cadastro) e S018 (edição). Wizard de 6 etapas com barra de progresso,
 * validação por etapa (§5.4) e visibilidade condicional (§5.1). Ao confirmar,
 * persiste (Room) com toast de sucesso e erro acionável ("Tentar novamente").
 * A edição pode abrir direto numa etapa via [passoInicial].
 */
@Composable
fun RegistrationScreen(
    midiaId: Long?,
    onBack: () -> Unit,
    onSalvo: (Long) -> Unit,
    passoInicial: Int = 0,
    // Pré-preenchimento vindo da busca no TMDB (cadastro novo).
    prefTitulo: String? = null,
    prefAno: String? = null,
    prefTipo: String? = null,
    prefGeneros: String? = null, // nomes juntos por "|"
    prefPosterUrl: String? = null,
) {
    val context = LocalContext.current
    val repo = remember { MidiaRepository.get(context) }
    val scope = rememberCoroutineScope()
    val edicao = midiaId != null

    var draft by remember { mutableStateOf(FormDraft()) }
    var midiaOriginal by remember { mutableStateOf<Midia?>(null) } // item 4: base p/ reancorar
    var passoIndex by remember { mutableIntStateOf(0) }
    var erro by remember { mutableStateOf<String?>(null) }
    var iniciado by remember { mutableStateOf(false) }

    var salvando by remember { mutableStateOf(false) }
    var erroSalvar by remember { mutableStateOf(false) }

    // Inicialização única: edição carrega a mídia e pode abrir numa etapa específica.
    LaunchedEffect(midiaId) {
        if (iniciado) return@LaunchedEffect
        if (midiaId != null) {
            repo.observarPorId(midiaId).first()?.let { midiaOriginal = it; draft = it.toDraft() }
            passoIndex = passoInicial.coerceIn(0, PassoCadastro.entries.lastIndex)
        } else if (!prefTitulo.isNullOrBlank()) {
            // Cadastro iniciado a partir de um resultado da busca: já traz título,
            // ano, tipo e pôster; o usuário continua o wizard normalmente.
            val generos = prefGeneros?.split("|")?.filter { it.isNotBlank() }?.toSet().orEmpty()
            draft = FormDraft(
                tipo = prefTipo?.let { runCatching { TipoMidia.valueOf(it) }.getOrNull() },
                titulo = prefTitulo,
                ano = prefAno.orEmpty(),
                generos = generos,
                posterUrl = prefPosterUrl?.ifBlank { null },
            )
        }
        iniciado = true
    }

    val passos = PassoCadastro.entries
    val passo = passos[passoIndex]

    fun voltar() {
        if (passoIndex > 0) {
            passoIndex--
            erro = null
        } else {
            onBack()
        }
    }

    fun avancar() {
        val msg = validarPasso(passo, draft)
        if (msg != null) {
            erro = msg
            return
        }
        erro = null
        if (passoIndex < passos.lastIndex) passoIndex++
    }

    fun salvar() {
        scope.launch {
            salvando = true
            erroSalvar = false
            try {
                val nova = draft.toMidia(id = midiaId ?: 0)
                // Item 4: se a quantidade de episódios mudou na edição de uma série
                // lançando, reancora a contagem de novos episódios na data da edição.
                val ajustada = midiaOriginal?.let { orig ->
                    if (nova.statusLancEpisodico == StatusLancEpisodico.LANCANDO &&
                        nova.episodiosDispTempAtual != orig.episodiosDispTempAtual
                    ) {
                        nova.copy(dataBaseContagem = LocalDate.now())
                    } else {
                        nova
                    }
                } ?: nova
                val salvo = repo.salvar(ajustada)
                salvando = false
                Toast.makeText(
                    context,
                    if (edicao) "Alterações salvas" else "Mídia adicionada à sua biblioteca",
                    Toast.LENGTH_SHORT,
                ).show()
                onSalvo(salvo)
            } catch (e: Exception) {
                salvando = false
                erroSalvar = true
            }
        }
    }

    // Item 8: salva o rascunho como "intenção de assistir" (parcial), pulando o resto.
    fun salvarComoIntencao() {
        scope.launch {
            salvando = true
            erroSalvar = false
            try {
                val salvo = repo.salvar(draft.toMidiaIntencao(id = midiaId ?: 0))
                salvando = false
                Toast.makeText(context, "Salvo como intenção de assistir", Toast.LENGTH_SHORT).show()
                onSalvo(salvo)
            } catch (e: Exception) {
                salvando = false
                erroSalvar = true
            }
        }
    }

    // Botão de intenção: no cadastro novo (ou ao editar uma intenção existente),
    // disponível assim que há tipo + título, em qualquer etapa antes da confirmação.
    val podeIntencao = (!edicao || midiaOriginal?.intencao == true) &&
        passo != PassoCadastro.CONFIRMAR &&
        draft.tipo != null && draft.titulo.isNotBlank()

    PushScreenScaffold(title = if (edicao) "Editar mídia" else "Adicionar mídia", onBack = ::voltar) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
        ) {
            LinearProgressIndicator(
                progress = { (passoIndex + 1) / passos.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text(
                    "Etapa ${passoIndex + 1} de ${passos.size} · ${passo.titulo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                when (passo) {
                    PassoCadastro.TIPO -> PassoTipo(draft) { draft = it }
                    PassoCadastro.TITULO -> PassoTitulo(draft) { draft = it }
                    PassoCadastro.DETALHES -> PassoDetalhes(draft) { draft = it }
                    PassoCadastro.ONDE_ASSISTIR -> PassoOndeAssistir(draft) { draft = it }
                    PassoCadastro.DATAS_STATUS -> PassoDatasStatus(draft) { draft = it }
                    PassoCadastro.CONFIRMAR -> PassoConfirmar(draft, erroSalvar)
                }

                erro?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Barra fixa de ação.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = ::voltar, modifier = Modifier.weight(1f)) {
                    Text(if (passoIndex == 0) "Cancelar" else "Voltar")
                }
                if (passo == PassoCadastro.CONFIRMAR) {
                    Button(onClick = ::salvar, enabled = !salvando, modifier = Modifier.weight(1f)) {
                        if (salvando) {
                            CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (erroSalvar) "Tentar novamente" else "Salvar")
                        }
                    }
                } else {
                    Button(onClick = ::avancar, modifier = Modifier.weight(1f)) { Text("Avançar") }
                }
            }

            // Item 8: atalho para salvar como intenção sem completar o wizard.
            if (podeIntencao) {
                TextButton(
                    onClick = ::salvarComoIntencao,
                    enabled = !salvando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                ) { Text("Salvar como intenção de assistir") }
            }
        }
    }
}

// --- Etapas -----------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassoTipo(d: FormDraft, onChange: (FormDraft) -> Unit) {
    Text("Que tipo de mídia é?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TIPOS_ORDEM.forEach { t ->
            FilterChip(
                selected = d.tipo == t,
                onClick = { onChange(d.copy(tipo = t)) },
                label = { Text(t.rotulo) },
            )
        }
    }
}

@Composable
private fun PassoTitulo(d: FormDraft, onChange: (FormDraft) -> Unit) {
    Text("Como se chama?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = d.titulo,
        onValueChange = { onChange(d.copy(titulo = it)) },
        label = { Text("Título") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = d.ano,
        onValueChange = { onChange(d.copy(ano = it.filter(Char::isDigit).take(4))) },
        label = { Text("Ano (opcional)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassoDetalhes(d: FormDraft, onChange: (FormDraft) -> Unit) {
    Text("Gênero", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    // Seleção múltipla. A ordem do vocabulário + os que já vieram (ex.: da busca).
    val opcoes = remember(d.generos) {
        (GENEROS_DISPONIVEIS + d.generos.toList()).distinct()
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { g ->
            FilterChip(
                selected = !d.semGenero && g in d.generos,
                enabled = !d.semGenero,
                onClick = {
                    val novos = if (g in d.generos) d.generos - g else d.generos + g
                    onChange(d.copy(generos = novos))
                },
                label = { Text(g) },
            )
        }
    }
    // Área de toque envolve checkbox + label.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = d.semGenero,
                role = Role.Checkbox,
                onValueChange = { onChange(d.copy(semGenero = it, generos = if (it) emptySet() else d.generos)) },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = d.semGenero, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Text("Sem gênero")
    }
    Spacer(Modifier.height(16.dp))
    Text("Contexto de consumo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Contexto.entries.forEach { c ->
            FilterChip(
                selected = d.contexto == c,
                onClick = { onChange(d.copy(contexto = c)) },
                label = { Text(c.rotulo) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassoOndeAssistir(d: FormDraft, onChange: (FormDraft) -> Unit) {
    Text("Onde vai assistir?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Modalidade.entries.forEach { mod ->
            FilterChip(
                selected = d.modalidade == mod,
                onClick = { onChange(d.copy(modalidade = mod)) },
                label = { Text(mod.rotulo) },
            )
        }
    }

    if (d.modalidade == Modalidade.STREAMING) {
        Spacer(Modifier.height(16.dp))
        Text("Streamings (toque na ★ p/ o principal)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            STREAMINGS_DISPONIVEIS.forEach { s ->
                val marcado = s in d.streamings
                FilterChip(
                    selected = marcado,
                    onClick = {
                        val novos = if (marcado) d.streamings - s else d.streamings + s
                        val principal = d.streamingPrincipal?.takeIf { it in novos }
                        onChange(d.copy(streamings = novos, streamingPrincipal = principal))
                    },
                    label = { Text(s) },
                    trailingIcon = if (marcado) {
                        {
                            IconButton(onClick = { onChange(d.copy(streamingPrincipal = s)) }, modifier = Modifier.height(20.dp)) {
                                Icon(
                                    if (d.streamingPrincipal == s) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Definir como principal",
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }

    if (d.modalidade == Modalidade.CINEMA) {
        Spacer(Modifier.height(16.dp))
        Text("Rede de cinema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            REDES_CINEMA.forEach { rede ->
                FilterChip(
                    selected = d.cinemaRede == rede,
                    onClick = { onChange(d.copy(cinemaRede = rede)) },
                    label = { Text(rede) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PassoDatasStatus(d: FormDraft, onChange: (FormDraft) -> Unit) {
    // Status de lançamento (só episódica). Ao escolher, ajusta o status da data:
    // "vai lançar" pede escolha; os demais viram NAO_APLICA automaticamente.
    if (d.episodica) {
        Text("Status de lançamento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusLancEpisodico.entries.forEach { s ->
                FilterChip(
                    selected = d.statusLancEpisodico == s,
                    onClick = {
                        val novoStatusData =
                            if (s == StatusLancEpisodico.VAI_LANCAR) null else StatusData.NAO_APLICA
                        onChange(
                            d.copy(
                                statusLancEpisodico = s,
                                statusData = novoStatusData,
                                vaiLancarTipo = if (s == StatusLancEpisodico.VAI_LANCAR) d.vaiLancarTipo else null,
                                novaTemporada = if (s == StatusLancEpisodico.VAI_LANCAR) d.novaTemporada else "",
                            ),
                        )
                    },
                    label = { Text(s.rotulo) },
                )
            }
        }

        // "Vai lançar": série nova × temporada nova.
        if (d.statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VaiLancarTipo.entries.forEach { t ->
                    FilterChip(
                        selected = d.vaiLancarTipo == t,
                        onClick = { onChange(d.copy(vaiLancarTipo = t)) },
                        label = { Text(t.rotulo) },
                    )
                }
            }
            if (d.vaiLancarTipo == VaiLancarTipo.TEMPORADA_NOVA) {
                CampoNumero("Número da nova temporada", d.novaTemporada) { onChange(d.copy(novaTemporada = it)) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Filme (não-episódica): opção de cancelado.
    if (!d.episodica) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = d.cancelada,
                    role = Role.Checkbox,
                    onValueChange = { onChange(d.copy(cancelada = it)) },
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = d.cancelada, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Text("Cancelado")
        }
        Spacer(Modifier.height(8.dp))
    }

    // Status da data — só quando não-episódica não cancelada, ou episódica "vai lançar".
    if (statusDataVisivel(d)) {
        Text("Status da data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusData.entries.forEach { s ->
                FilterChip(
                    selected = d.statusData == s,
                    onClick = { onChange(d.copy(statusData = s)) },
                    label = { Text(s.rotulo) },
                )
            }
        }
        if (d.statusData == StatusData.DEFINIDA) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = d.dataTexto,
                onValueChange = { onChange(d.copy(dataTexto = it.filter(Char::isDigit).take(8))) },
                label = { Text("Data principal") },
                placeholder = { Text("dd/mm/aaaa") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = DateMaskTransformation,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    // Dia/horário (episódica lançando)
    if (d.episodica && d.statusLancEpisodico == StatusLancEpisodico.LANCANDO) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiaSemanaDropdown(
                valor = d.diaLancamento,
                onSelecionar = { onChange(d.copy(diaLancamento = it)) },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = d.horarioLancamento,
                onValueChange = { onChange(d.copy(horarioLancamento = it.filter(Char::isDigit).take(4))) },
                label = { Text("Horário") },
                placeholder = { Text("HH:mm") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = TimeMaskTransformation,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    // Disponibilidade
    val dispVisivel = d.tipo != null && disponibilidadeVisivel(d.tipo, d.statusLancEpisodico)
    if (dispVisivel) {
        Text("Disponibilidade", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        CampoNumero("Temporadas disponíveis", d.temporadasDisponiveis) { onChange(d.copy(temporadasDisponiveis = it)) }
        Spacer(Modifier.height(16.dp))
    }

    // Seu status (§5.5)
    Text("Seu status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    val opcoes = if (d.episodica) {
        listOf(StatusUsuario.QUERO_ASSISTIR, StatusUsuario.ASSISTINDO, StatusUsuario.VISTO)
    } else {
        listOf(StatusUsuario.QUERO_ASSISTIR, StatusUsuario.VISTO)
    }
    val vistoBloqueado = d.tipo != null && !permiteVisto(d.tipo, d.statusLancEpisodico)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { s ->
            val bloqueado = s == StatusUsuario.VISTO && vistoBloqueado
            FilterChip(
                selected = d.statusUsuario == s,
                enabled = !bloqueado,
                onClick = { if (!bloqueado) onChange(d.copy(statusUsuario = s)) },
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
        Text("Requer status Completa ou Cancelada", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // Progresso
    val progVisivel = d.tipo != null && d.statusUsuario != null &&
        progressoVisivel(d.tipo, d.statusLancEpisodico, d.statusUsuario)
    if (progVisivel) {
        Spacer(Modifier.height(16.dp))
        Text("Progresso", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        CampoNumero("Temporada atual", d.temporadaAtual) { onChange(d.copy(temporadaAtual = it)) }
        CampoNumero("Episódios disponíveis (temp. atual)", d.episodiosDispTempAtual) { onChange(d.copy(episodiosDispTempAtual = it)) }
        CampoNumero("Último episódio visto", d.ultimoEpisodioVisto) { onChange(d.copy(ultimoEpisodioVisto = it)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaSemanaDropdown(valor: String, onSelecionar: (String) -> Unit, modifier: Modifier = Modifier) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { aberto = !aberto }, modifier = modifier) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text("Dia") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DIAS_SEMANA.forEach { dia ->
                DropdownMenuItem(
                    text = { Text(dia) },
                    onClick = {
                        onSelecionar(dia)
                        aberto = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CampoNumero(label: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun PassoConfirmar(d: FormDraft, erroSalvar: Boolean) {
    Text("Confira antes de salvar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Resumo("Tipo", d.tipo?.rotulo)
            Resumo("Título", d.titulo)
            Resumo("Ano", d.ano.ifBlank { "—" })
            Resumo("Gênero", d.generos.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Sem gênero")
            Resumo("Contexto", d.contexto?.rotulo)
            Resumo("Onde assistir", resumoOnde(d))
            if (d.episodica) {
                Resumo("Status de lançamento", d.statusLancEpisodico?.rotulo)
                if (d.statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR && d.vaiLancarTipo != null) {
                    val detalhe = if (d.vaiLancarTipo == VaiLancarTipo.TEMPORADA_NOVA) {
                        "Temporada nova (nº ${d.novaTemporada})"
                    } else {
                        "Série nova"
                    }
                    Resumo("Vai lançar", detalhe)
                }
            }
            if (!d.episodica && d.cancelada) Resumo("Situação", "Cancelado")
            if (statusDataVisivel(d)) {
                Resumo("Status da data", d.statusData?.rotulo)
                if (d.statusData == StatusData.DEFINIDA) {
                    Resumo("Data", parseData(d.dataTexto)?.let { formatarData(it) } ?: "—")
                }
            }
            Resumo("Seu status", d.statusUsuario?.rotulo)
        }
    }

    if (erroSalvar) {
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Não foi possível salvar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tente novamente tocando em \"Tentar novamente\".",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Resumo(rotulo: String, valor: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(rotulo, Modifier.width(150.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor ?: "—", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun resumoOnde(d: FormDraft): String = when (d.modalidade) {
    Modalidade.STREAMING -> {
        val principal = d.streamingPrincipal?.let { " (★ $it)" }.orEmpty()
        "Streaming: ${d.streamings.joinToString(", ")}$principal"
    }
    Modalidade.CINEMA -> "Cinema: ${d.cinemaRede ?: "—"}"
    Modalidade.OUTRO -> "Outro"
    Modalidade.NAO_SEI -> "Não sei ainda"
    null -> "—"
}

// --- Máscaras ---------------------------------------------------------------

/** Máscara visual dd/MM/yyyy sobre até 8 dígitos. */
private val DateMaskTransformation = VisualTransformation { text ->
    val digitos = text.text.take(8)
    val sb = StringBuilder()
    digitos.forEachIndexed { i, c ->
        sb.append(c)
        if (i == 1 || i == 3) sb.append('/')
    }
    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = when {
            offset <= 1 -> offset
            offset <= 3 -> offset + 1
            else -> offset + 2
        }

        override fun transformedToOriginal(offset: Int): Int = when {
            offset <= 2 -> offset
            offset <= 5 -> offset - 1
            else -> offset - 2
        }.coerceIn(0, digitos.length)
    }
    TransformedText(AnnotatedString(sb.toString()), mapping)
}

/** Máscara visual HH:mm sobre até 4 dígitos. */
private val TimeMaskTransformation = VisualTransformation { text ->
    val digitos = text.text.take(4)
    val sb = StringBuilder()
    digitos.forEachIndexed { i, c ->
        sb.append(c)
        if (i == 1) sb.append(':')
    }
    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int = if (offset <= 1) offset else offset + 1
        override fun transformedToOriginal(offset: Int): Int = (if (offset <= 2) offset else offset - 1).coerceIn(0, digitos.length)
    }
    TransformedText(AnnotatedString(sb.toString()), mapping)
}
