package br.com.shopper.watchup.core.data.model

/**
 * Enums do domínio WatchUp (§3.1 da spec). Os `rotulo` são os rótulos visíveis na
 * UI; os nomes técnicos (identidade dos enums) seguem o domínio, não a UI.
 *
 * [StatusMidia] e o estado "Em dia" são **derivados** (§5.2/§5.3) e vivem em
 * [br.com.shopper.watchup.core.data.domain.MidiaLogic] — não são escolhidos pelo
 * usuário nem persistidos como fonte de verdade.
 */
enum class TipoMidia(val episodica: Boolean, val rotulo: String) {
    FILME(false, "Filme"),
    DOCUMENTARIO(false, "Documentário"),
    SHOW(false, "Show"),
    OUTRO(false, "Outro"),
    SERIE(true, "Série"),
    ANIME(true, "Anime"),
    REALITY(true, "Reality"),
    PROGRAMA(true, "Programa"),
}

enum class Contexto(val rotulo: String) {
    SOZINHO("Sozinho"),
    ACOMPANHADO("Com alguém"),
    TANTO_FAZ("Tanto faz"),
    NAO_DEFINIDO("Não definido"),
}

enum class Modalidade(val rotulo: String) {
    STREAMING("Streaming"),
    CINEMA("Cinema"),
    OUTRO("Outro"),
    NAO_SEI("Não sei ainda"),
}

enum class StatusLancEpisodico(val rotulo: String) {
    COMPLETA("Completa"),
    LANCANDO("Lançando"),
    VAI_LANCAR("Vai lançar"),
}

enum class StatusData(val rotulo: String) {
    DEFINIDA("Com data definida"),
    SEM_DATA("Sem data"),
    NAO_INFORMADO("Não informado"),
    NAO_APLICA("Não se aplica"),
}

/** Derivado (§5.2) — nunca escolhido pelo usuário. */
enum class StatusMidia(val rotulo: String) {
    LANCANDO("Lançando"),
    EM_CARTAZ("Em cartaz"),
    EM_BREVE("Em breve"),
    FINALIZADA("Finalizada"),
}

enum class StatusUsuario(val rotulo: String) {
    QUERO_ASSISTIR("Quero assistir"),
    ASSISTINDO("Assistindo"),
    VISTO("Visto"),
}

/** Streamings oferecidos na etapa "Onde assistir" (§4), com escapes "Outro"/"Não sei". */
val STREAMINGS_DISPONIVEIS: List<String> = listOf(
    "Netflix", "Prime", "Max", "Disney+", "Apple TV+", "Crunchyroll", "Globoplay", "Paramount+",
    "Outro", "Não sei",
)

/** Redes de cinema oferecidas na etapa "Onde assistir" (§4). */
val REDES_CINEMA: List<String> = listOf(
    "Cinemark", "UCI", "Kinoplex", "Cinépolis", "Cinesystem", "Outro", "Não definido",
)
