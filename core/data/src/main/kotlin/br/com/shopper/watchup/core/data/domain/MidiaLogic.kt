package br.com.shopper.watchup.core.data.domain

import br.com.shopper.watchup.core.data.model.Midia
import br.com.shopper.watchup.core.data.model.Modalidade
import br.com.shopper.watchup.core.data.model.StatusData
import br.com.shopper.watchup.core.data.model.StatusLancEpisodico
import br.com.shopper.watchup.core.data.model.StatusMidia
import br.com.shopper.watchup.core.data.model.StatusUsuario
import br.com.shopper.watchup.core.data.model.TipoMidia
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

/** Tem card de progresso no Detalhe (S016) quando há progresso a exibir. */
fun temCardProgresso(m: Midia): Boolean =
    progressoVisivel(m.tipo, m.statusLancEpisodico, m.statusUsuario)

// ---------------------------------------------------------------------------
// Lançamentos (S006) — agrupamento por status de data
// ---------------------------------------------------------------------------

/** Seções fixas do radar de lançamentos, em ordem de exibição. */
enum class SecaoLancamento(val titulo: String) {
    ESTA_SEMANA("Esta semana"),
    PROXIMAS_DATAS("Próximas datas"),
    SEM_DATA("Sem data definida"),
}

/** Filtros por tipo de data (S006). */
enum class FiltroLancamento(val rotulo: String) {
    TODOS("Todos"),
    ESTREIA_CINEMA("Estreia cinema"),
    ESTREIA_STREAMING("Estreia streaming"),
    NOVO_EPISODIO("Novo episódio"),
}

/** Classifica a mídia numa seção do radar conforme a data principal. */
fun secaoLancamento(m: Midia, hoje: LocalDate = LocalDate.now()): SecaoLancamento {
    val data = m.dataPrincipal
    if (m.statusData != StatusData.DEFINIDA || data == null) return SecaoLancamento.SEM_DATA
    val dias = ChronoUnit.DAYS.between(hoje, data)
    return if (dias in -7..7) SecaoLancamento.ESTA_SEMANA else SecaoLancamento.PROXIMAS_DATAS
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
