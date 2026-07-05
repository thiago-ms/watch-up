package br.com.watchup.feature.registration

import br.com.watchup.core.data.domain.disponibilidadeVisivel
import br.com.watchup.core.data.domain.permiteVisto
import br.com.watchup.core.data.domain.progressoVisivel
import br.com.watchup.core.data.model.Contexto
import br.com.watchup.core.data.model.Midia
import br.com.watchup.core.data.model.Modalidade
import br.com.watchup.core.data.model.StatusData
import br.com.watchup.core.data.model.StatusLancEpisodico
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.model.TipoMidia
import java.time.LocalDate

/** Rascunho do cadastro (S010–S015). Números, datas e horário ficam como texto e
 *  são convertidos ao construir a [Midia]. Campos condicionais seguem §5.1. */
data class FormDraft(
    val tipo: TipoMidia? = null,
    val titulo: String = "",
    val ano: String = "",
    val posterUrl: String? = null, // vindo da busca no TMDB
    val generos: Set<String> = emptySet(),
    val semGenero: Boolean = false,
    val contexto: Contexto? = null,
    val modalidade: Modalidade? = null,
    val streamings: Set<String> = emptySet(),
    val streamingPrincipal: String? = null,
    val cinemaRede: String? = null,
    val statusLancEpisodico: StatusLancEpisodico? = null,
    val vaiLancarTipo: VaiLancarTipo? = null, // só quando statusLancEpisodico = VAI_LANCAR
    val novaTemporada: String = "", // nº da nova temporada (quando TEMPORADA_NOVA)
    val cancelada: Boolean = false, // cancelamento de não-episódica (filme etc.)
    val statusData: StatusData? = null,
    val dataTexto: String = "", // dígitos ddMMyyyy (máscara aplicada na UI)
    val diaLancamento: String = "",
    val horarioLancamento: String = "", // dígitos HHmm (máscara aplicada na UI)
    val temporadasDisponiveis: String = "",
    val temporadaAtual: String = "",
    val episodiosDispTempAtual: String = "",
    val ultimoEpisodioVisto: String = "",
    val statusUsuario: StatusUsuario? = null,
) {
    val episodica: Boolean get() = tipo?.episodica == true
}

/** Sub-tipo de "Vai lançar" (episódica): série inteira nova ou uma nova temporada. */
enum class VaiLancarTipo(val rotulo: String) {
    SERIE_NOVA("Série nova"),
    TEMPORADA_NOVA("Temporada nova"),
}

/** Ordem de exibição dos tipos na etapa 1 do cadastro. */
val TIPOS_ORDEM: List<TipoMidia> = listOf(
    TipoMidia.FILME,
    TipoMidia.SERIE,
    TipoMidia.ANIME,
    TipoMidia.DOCUMENTARIO,
    TipoMidia.SHOW,
    TipoMidia.PROGRAMA,
    TipoMidia.REALITY,
    TipoMidia.OUTRO,
)

/** Dias da semana para o seletor de lançamento. */
val DIAS_SEMANA: List<String> = listOf(
    "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado",
)

/** Etapas do cadastro em 6 passos (§6, S010–S015). */
enum class PassoCadastro(val titulo: String) {
    TIPO("Tipo"),
    TITULO("Título"),
    DETALHES("Detalhes"),
    ONDE_ASSISTIR("Onde assistir"),
    DATAS_STATUS("Datas e status"),
    CONFIRMAR("Confirmar"),
}

/**
 * "Status da data" só faz sentido escolher quando não-episódica, ou quando
 * episódica "vai lançar". Nos demais casos episódicos (Completa/Lançando) ele é
 * automaticamente NAO_APLICA e o bloco fica oculto.
 */
fun statusDataVisivel(d: FormDraft): Boolean = when {
    d.episodica -> d.statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR
    else -> !d.cancelada // filme cancelado não tem data
}

// --- Data (dígitos ddMMyyyy) ------------------------------------------------
fun parseData(digitos: String): LocalDate? {
    val s = digitos.filter(Char::isDigit)
    if (s.length != 8) return null
    return runCatching {
        LocalDate.of(s.substring(4).toInt(), s.substring(2, 4).toInt(), s.substring(0, 2).toInt())
    }.getOrNull()
}

private fun LocalDate.paraDigitos(): String = "%02d%02d%04d".format(dayOfMonth, monthValue, year)

// --- Horário (dígitos HHmm) -------------------------------------------------
fun formatarHorario(digitos: String): String? {
    val s = digitos.filter(Char::isDigit)
    if (s.length != 4) return null
    val h = s.substring(0, 2).toInt()
    val m = s.substring(2).toInt()
    if (h > 23 || m > 59) return null
    return "%02d:%02d".format(h, m)
}

private fun horarioParaDigitos(hhmm: String?): String =
    hhmm?.filter(Char::isDigit)?.take(4) ?: ""

/**
 * Valida uma etapa (§5.4). Devolve a mensagem de erro (a 1ª que falhar) ou null se
 * a etapa está válida para avançar.
 */
fun validarPasso(passo: PassoCadastro, d: FormDraft): String? = when (passo) {
    PassoCadastro.TIPO ->
        if (d.tipo == null) "Escolha o tipo da mídia." else null

    PassoCadastro.TITULO ->
        if (d.titulo.isBlank()) "Informe um título." else null

    PassoCadastro.DETALHES -> when {
        !d.semGenero && d.generos.isEmpty() -> "Selecione ao menos um gênero."
        d.contexto == null -> "Escolha o contexto de consumo."
        else -> null
    }

    PassoCadastro.ONDE_ASSISTIR -> when {
        d.modalidade == null -> "Selecione onde vai assistir."
        d.modalidade == Modalidade.STREAMING && d.streamings.isEmpty() -> "Selecione ao menos um streaming."
        d.modalidade == Modalidade.STREAMING && d.streamingPrincipal == null -> "Defina o streaming principal (★)."
        d.modalidade == Modalidade.CINEMA && d.cinemaRede.isNullOrBlank() -> "Informe o cinema/rede."
        else -> null
    }

    PassoCadastro.DATAS_STATUS -> validarDatasStatus(d)

    PassoCadastro.CONFIRMAR -> null
}

private fun validarDatasStatus(d: FormDraft): String? {
    val tipo = d.tipo ?: return "Escolha o tipo da mídia."
    if (d.episodica && d.statusLancEpisodico == null) return "Selecione o status de lançamento."

    // "Vai lançar": exige escolher série nova × temporada nova.
    if (d.episodica && d.statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR) {
        if (d.vaiLancarTipo == null) return "É série nova ou temporada nova?"
        if (d.vaiLancarTipo == VaiLancarTipo.TEMPORADA_NOVA) {
            val n = d.novaTemporada.toIntOrNull()
            if (n == null || n < 2) return "Informe o número da nova temporada (2 ou maior)."
        }
    }

    // Status da data só é exigido quando visível (não-episódica não cancelada, ou "vai lançar").
    if (statusDataVisivel(d)) {
        if (d.statusData == null) return "Selecione o status da data."
        if (d.statusData == StatusData.DEFINIDA && parseData(d.dataTexto) == null) return "Informe a data principal."
    }

    val dispVisivel = disponibilidadeVisivel(tipo, d.statusLancEpisodico)
    if (dispVisivel && d.temporadasDisponiveis.toIntOrNull() == null) return "Obrigatório."

    if (d.statusUsuario == null) return "Escolha seu status."
    if (d.statusUsuario == StatusUsuario.VISTO && !permiteVisto(tipo, d.statusLancEpisodico)) {
        return "Só marque 'Visto' quando a série estiver Completa ou Cancelada."
    }

    val progVisivel = progressoVisivel(d.tipo, d.statusLancEpisodico, d.statusUsuario)
    if (progVisivel) {
        val disp = d.episodiosDispTempAtual.toIntOrNull()
        if (d.temporadaAtual.toIntOrNull() == null || disp == null || d.ultimoEpisodioVisto.toIntOrNull() == null) {
            return "Obrigatório."
        }
        if (d.ultimoEpisodioVisto.toInt() > disp) {
            return "Não pode ser maior que os episódios disponíveis ($disp)."
        }
    }
    return null
}

/** Constrói a [Midia] a partir do rascunho, limpando campos que não se aplicam. */
fun FormDraft.toMidia(id: Long = 0): Midia {
    val tipo = requireNotNull(tipo)
    val statusUsuario = requireNotNull(statusUsuario)
    val streaming = modalidade == Modalidade.STREAMING
    val cinema = modalidade == Modalidade.CINEMA
    val dispVisivel = disponibilidadeVisivel(tipo, statusLancEpisodico)
    val progVisivel = progressoVisivel(tipo, statusLancEpisodico, statusUsuario)
    val lancando = statusLancEpisodico == StatusLancEpisodico.LANCANDO
    val vaiLancar = statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR

    // Campo oculto de status da data → automático NAO_APLICA (§6).
    val statusDataFinal =
        if (statusDataVisivel(this)) requireNotNull(statusData) else StatusData.NAO_APLICA

    // "Temporada nova" → temporadas disponíveis são as anteriores à nova (N - 1).
    val temporadasCalc = when {
        vaiLancar && vaiLancarTipo == VaiLancarTipo.TEMPORADA_NOVA ->
            ((novaTemporada.toIntOrNull() ?: 1) - 1).coerceAtLeast(0)
        dispVisivel -> temporadasDisponiveis.toIntOrNull() ?: 0
        else -> 0
    }

    return Midia(
        id = id,
        tipo = tipo,
        titulo = titulo.trim(),
        ano = ano.toIntOrNull(),
        posterUrl = posterUrl,
        generos = if (semGenero) emptyList() else generos.toList(),
        contexto = requireNotNull(contexto),
        modalidade = requireNotNull(modalidade),
        streamings = if (streaming) streamings.toList() else emptyList(),
        streamingPrincipal = if (streaming) streamingPrincipal else null,
        cinemaRede = if (cinema) cinemaRede else null,
        statusLancEpisodico = if (tipo.episodica) statusLancEpisodico else null,
        cancelada = if (!tipo.episodica) cancelada else false,
        statusData = statusDataFinal,
        dataPrincipal = if (statusDataFinal == StatusData.DEFINIDA) parseData(dataTexto) else null,
        diaLancamento = if (episodica && lancando) diaLancamento.ifBlank { null } else null,
        horarioLancamento = if (episodica && lancando) formatarHorario(horarioLancamento) else null,
        temporadasDisponiveis = temporadasCalc,
        temporadaAtual = if (progVisivel) temporadaAtual.toIntOrNull() ?: 0 else 0,
        episodiosDispTempAtual = if (progVisivel) episodiosDispTempAtual.toIntOrNull() ?: 0 else 0,
        ultimoEpisodioVisto = if (progVisivel) ultimoEpisodioVisto.toIntOrNull() ?: 0 else 0,
        statusUsuario = statusUsuario,
    )
}

/** Preenche o rascunho a partir de uma [Midia] existente (edição, S018). */
fun Midia.toDraft(): FormDraft = FormDraft(
    tipo = tipo,
    titulo = titulo,
    ano = ano?.toString() ?: "",
    posterUrl = posterUrl,
    generos = generos.toSet(),
    semGenero = generos.isEmpty(),
    contexto = contexto,
    modalidade = modalidade,
    streamings = streamings.toSet(),
    streamingPrincipal = streamingPrincipal,
    cinemaRede = cinemaRede,
    statusLancEpisodico = statusLancEpisodico,
    vaiLancarTipo = if (statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR) {
        if (temporadasDisponiveis > 0) VaiLancarTipo.TEMPORADA_NOVA else VaiLancarTipo.SERIE_NOVA
    } else {
        null
    },
    novaTemporada = if (statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR && temporadasDisponiveis > 0) {
        (temporadasDisponiveis + 1).toString()
    } else {
        ""
    },
    cancelada = cancelada,
    statusData = statusData,
    dataTexto = dataPrincipal?.paraDigitos() ?: "",
    diaLancamento = diaLancamento ?: "",
    horarioLancamento = horarioParaDigitos(horarioLancamento),
    temporadasDisponiveis = temporadasDisponiveis.takeIf { it > 0 }?.toString() ?: "",
    temporadaAtual = temporadaAtual.takeIf { it > 0 }?.toString() ?: "",
    episodiosDispTempAtual = episodiosDispTempAtual.takeIf { it > 0 }?.toString() ?: "",
    ultimoEpisodioVisto = ultimoEpisodioVisto.toString(),
    statusUsuario = statusUsuario,
)
