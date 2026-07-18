package br.com.watchup.core.data.domain

import br.com.watchup.core.data.model.Midia
import br.com.watchup.core.data.model.Modalidade
import br.com.watchup.core.data.model.StatusData
import br.com.watchup.core.data.model.StatusLancEpisodico
import br.com.watchup.core.data.model.StatusMidia
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.model.TipoMidia
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Regras de negócio derivadas (§5). Nada aqui é persistido: são funções puras
 * sobre uma [Midia], testáveis sem Android (ver MidiaLogicTest).
 */

/** §5.2 — derivação de statusMidia a partir do tipo/estado da mídia. */
fun deriveStatusMidia(m: Midia, hoje: LocalDate = LocalDate.now()): StatusMidia = when {
    m.tipo.episodica -> when (m.statusLancEpisodico) {
        StatusLancEpisodico.COMPLETA -> StatusMidia.FINALIZADA
        StatusLancEpisodico.CANCELADA -> StatusMidia.CANCELADA
        StatusLancEpisodico.LANCANDO -> StatusMidia.LANCANDO
        StatusLancEpisodico.VAI_LANCAR -> StatusMidia.EM_BREVE
        null -> StatusMidia.EM_CARTAZ
    }

    // Não-episódica cancelada (filme etc.).
    m.cancelada -> StatusMidia.CANCELADA

    m.statusData == StatusData.DEFINIDA && m.dataPrincipal != null && m.dataPrincipal.isAfter(hoje) ->
        StatusMidia.EM_BREVE

    else -> StatusMidia.EM_CARTAZ
}

/** Séries "terminadas" onde faz sentido marcar "Visto" (§5.5) — Completa ou Cancelada. */
fun permiteVisto(tipo: TipoMidia, statusLancEp: StatusLancEpisodico?): Boolean =
    !tipo.episodica ||
        statusLancEp == StatusLancEpisodico.COMPLETA ||
        statusLancEp == StatusLancEpisodico.CANCELADA

/**
 * §5.3 — rótulo de exibição do status do usuário. "Em dia" **não** é uma opção
 * do usuário: é derivado quando a série assistida está com o último episódio
 * visto igual (ou acima) do disponível.
 */
fun statusExibicao(m: Midia): String = when {
    m.tipo.episodica &&
        m.statusUsuario == StatusUsuario.ASSISTINDO &&
        m.episodiosDispTempAtual > 0 &&
        m.ultimoEpisodioVisto >= m.episodiosDispTempAtual -> "Em dia"

    else -> m.statusUsuario.rotulo
}

/** Verdadeiro quando a mídia está no estado derivado "Em dia". */
fun estaEmDia(m: Midia): Boolean =
    m.tipo.episodica &&
        m.statusUsuario == StatusUsuario.ASSISTINDO &&
        m.episodiosDispTempAtual > 0 &&
        m.ultimoEpisodioVisto >= m.episodiosDispTempAtual

/** Episódios que faltam para ficar em dia na temporada atual. */
fun episodiosFaltantes(m: Midia): Int =
    (m.episodiosDispTempAtual - m.ultimoEpisodioVisto).coerceAtLeast(0)

/** Fração de progresso [0f, 1f] na temporada atual. */
fun fracaoProgresso(m: Midia): Float =
    if (m.episodiosDispTempAtual <= 0) 0f
    else (m.ultimoEpisodioVisto.toFloat() / m.episodiosDispTempAtual).coerceIn(0f, 1f)

/** §5.1 — o bloco Disponibilidade só aparece p/ episódica que não é "vai lançar". */
fun disponibilidadeVisivel(tipo: TipoMidia, statusLancEp: StatusLancEpisodico?): Boolean =
    tipo.episodica && statusLancEp != null && statusLancEp != StatusLancEpisodico.VAI_LANCAR

/** §5.1 — o bloco Progresso só aparece p/ ASSISTINDO com Disponibilidade visível. */
fun progressoVisivel(
    tipo: TipoMidia,
    statusLancEp: StatusLancEpisodico?,
    statusUsuario: StatusUsuario,
): Boolean =
    disponibilidadeVisivel(tipo, statusLancEp) && statusUsuario == StatusUsuario.ASSISTINDO

/**
 * Progresso **acessível** no Detalhe/tela de progresso: toda episódica sendo
 * assistida com ao menos uma temporada disponível — inclusive quando uma nova
 * temporada está por vir (VAI_LANCAR "temporada nova"). Diferente de
 * [progressoVisivel], que rege apenas os campos exibidos no **cadastro** (§5.1) e
 * segue ocultando o progresso para "vai lançar".
 */
fun progressoAcessivel(m: Midia): Boolean =
    m.tipo.episodica &&
        m.statusUsuario == StatusUsuario.ASSISTINDO &&
        m.temporadasDisponiveis > 0

/** Tem card de progresso no Detalhe (S016) quando há progresso a exibir. */
fun temCardProgresso(m: Midia): Boolean = progressoAcessivel(m)

// ---------------------------------------------------------------------------
// Novos episódios (item 4) — estimativa por cadência
// ---------------------------------------------------------------------------

/** Cadências oferecidas no Detalhe (dias entre novos episódios). */
enum class Cadencia(val dias: Int, val rotulo: String) {
    DIARIA(1, "Diária"),
    SEMANAL(7, "Semanal"),
    QUINZENAL(14, "Quinzenal"),
    MENSAL(30, "Mensal"),
}

/** Cadência atual da mídia (aproxima para a opção mais próxima, default semanal). */
fun cadenciaDe(m: Midia): Cadencia =
    Cadencia.entries.minByOrNull { kotlin.math.abs(it.dias - m.cadenciaDias) } ?: Cadencia.SEMANAL

/**
 * Item 4 — nº de **novos episódios estimados** desde a última atualização de
 * quantidade. Usa a cadência (dias entre episódios) e a data-base da contagem;
 * quando a data-base é nula (ainda "vai lançar"), cai na data de lançamento e conta
 * a partir do episódio 1. Vale só p/ episódica lançando (ou "vai lançar" cuja
 * estreia já passou). Devolve 0 quando não há base, a base é futura ou não há
 * novidade — é uma **sugestão sutil**, nunca altera dado sozinho.
 */
fun novosEpisodiosEstimados(m: Midia, hoje: LocalDate = LocalDate.now()): Int {
    if (!m.tipo.episodica) return 0
    val lancando = m.statusLancEpisodico == StatusLancEpisodico.LANCANDO
    val vaiLancarEstreou = m.statusLancEpisodico == StatusLancEpisodico.VAI_LANCAR &&
        m.dataBaseContagem == null && m.dataPrincipal != null
    if (!lancando && !vaiLancarEstreou) return 0

    val base = m.dataBaseContagem ?: m.dataPrincipal ?: return 0
    if (base.isAfter(hoje)) return 0

    val cad = m.cadenciaDias.coerceAtLeast(1)
    val intervalos = (ChronoUnit.DAYS.between(base, hoje) / cad).toInt()
    return if (m.dataBaseContagem != null) {
        intervalos // a data-base já contava os episódios de então; só os novos contam
    } else {
        (intervalos + 1 - m.episodiosDispTempAtual).coerceAtLeast(0) // conta a partir do ep 1
    }
}

// ---------------------------------------------------------------------------
// Lançamentos (S006) — agrupamento por status de data
// ---------------------------------------------------------------------------

/**
 * Janela temporal de um lançamento em relação a hoje (item 3). `EM_CARTAZ` agrupa o
 * que já estreou / está no ar; as demais são janelas de **calendário** (semana, mês
 * e ano correntes e seguintes). A [prioridade] ordena da mais próxima (0) à mais
 * distante e guia a ênfase de cor da tag — `SEM_DATA` é a mais neutra.
 */
enum class JanelaData(val rotulo: String, val prioridade: Int) {
    EM_CARTAZ("Em cartaz", 0),
    ESTA_SEMANA("Esta semana", 1),
    SEMANA_QUE_VEM("Semana que vem", 2),
    ESTE_MES("Este mês", 3),
    PROXIMO_MES("Próximo mês", 4),
    ESTE_ANO("Este ano", 5),
    PROXIMO_ANO("Próximo ano", 6),
    FUTURO_DISTANTE("Futuro distante", 7),
    SEM_DATA("Sem data", 8),
}

/** Filtros por tipo de data (S006). */
enum class FiltroLancamento(val rotulo: String) {
    TODOS("Todos"),
    ESTREIA_CINEMA("Estreia cinema"),
    ESTREIA_STREAMING("Estreia streaming"),
    NOVO_EPISODIO("Novo episódio"),
}

/**
 * Classifica a mídia numa [JanelaData] por **calendário** (semana começando no
 * domingo, mês e ano correntes/seguintes). Episódica "lançando" com cadência
 * semanal e estreias já passadas contam como "Em cartaz".
 */
fun janelaData(m: Midia, hoje: LocalDate = LocalDate.now()): JanelaData {
    // Episódica "lançando" com cadência semanal (dia da semana definido) tem novo
    // episódio toda semana → está no ar (bug: caíam em "sem data" na Home).
    if (m.tipo.episodica &&
        m.statusLancEpisodico == StatusLancEpisodico.LANCANDO &&
        !m.diaLancamento.isNullOrBlank()
    ) {
        return JanelaData.EM_CARTAZ
    }
    val data = m.dataPrincipal
    if (m.statusData != StatusData.DEFINIDA || data == null) return JanelaData.SEM_DATA
    if (data.isBefore(hoje)) return JanelaData.EM_CARTAZ // estreia no passado

    // Semana-calendário começando no domingo (Dom=0 … Sáb=6).
    val inicioSemana = hoje.minusDays((hoje.dayOfWeek.value % 7).toLong())
    val fimSemana = inicioSemana.plusDays(6)
    val fimProxSemana = fimSemana.plusDays(7)
    val proxMes = hoje.plusMonths(1)

    return when {
        !data.isAfter(fimSemana) -> JanelaData.ESTA_SEMANA
        !data.isAfter(fimProxSemana) -> JanelaData.SEMANA_QUE_VEM
        data.year == hoje.year && data.monthValue == hoje.monthValue -> JanelaData.ESTE_MES
        data.year == proxMes.year && data.monthValue == proxMes.monthValue -> JanelaData.PROXIMO_MES
        data.year == hoje.year -> JanelaData.ESTE_ANO
        data.year == hoje.year + 1 -> JanelaData.PROXIMO_ANO
        else -> JanelaData.FUTURO_DISTANTE
    }
}

/** Aplica o filtro por tipo de data do radar. */
fun combinaFiltro(m: Midia, filtro: FiltroLancamento): Boolean = when (filtro) {
    FiltroLancamento.TODOS -> true
    FiltroLancamento.ESTREIA_CINEMA -> m.modalidade == Modalidade.CINEMA
    FiltroLancamento.ESTREIA_STREAMING -> m.modalidade == Modalidade.STREAMING
    FiltroLancamento.NOVO_EPISODIO ->
        m.tipo.episodica && m.statusLancEpisodico == StatusLancEpisodico.LANCANDO
}

// ---------------------------------------------------------------------------
// Biblioteca — filtros por tipo
// ---------------------------------------------------------------------------

enum class FiltroBiblioteca(val rotulo: String, val tipos: Set<TipoMidia>?) {
    TODOS("Todos", null),
    FILMES("Filmes", setOf(TipoMidia.FILME)),
    SERIES("Séries", setOf(TipoMidia.SERIE, TipoMidia.PROGRAMA, TipoMidia.REALITY)),
    ANIMES("Animes", setOf(TipoMidia.ANIME)),
    DOCS("Docs", setOf(TipoMidia.DOCUMENTARIO)),
}

fun combinaFiltroBiblioteca(m: Midia, filtro: FiltroBiblioteca): Boolean =
    filtro.tipos?.contains(m.tipo) ?: true

/** Mídias episódicas em andamento (carrossel "Continuar assistindo" da Home). */
fun emAndamento(midias: List<Midia>): List<Midia> =
    midias.filter { it.tipo.episodica && it.statusUsuario == StatusUsuario.ASSISTINDO }
