package br.com.watchup.core.tmdb

import br.com.watchup.core.data.domain.rotuloDiaDaSemana
import br.com.watchup.core.data.model.STREAMINGS_DISPONIVEIS
import br.com.watchup.core.data.model.StatusLancEpisodico
import java.time.LocalDate

/**
 * Detalhes de uma obra no TMDB (`/tv/{id}` ou `/movie/{id}`), já traduzidos para o
 * vocabulário do app — é o que alimenta o autopreenchimento do cadastro (item 17).
 *
 * Tudo é opcional: campo que a API não trouxe vira `null`/vazio e o form segue
 * pedindo ao usuário. O horário de exibição **não está aqui de propósito**: o TMDB
 * não expõe horário em endpoint nenhum, então `horarioLancamento` continua manual.
 */
data class TmdbDetalhes(
    val ano: String? = null, // YYYY
    val dataDigitos: String? = null, // ddMMyyyy (formato do FormDraft.dataTexto)
    val statusLancEpisodico: StatusLancEpisodico? = null, // só faz sentido em episódica
    val cancelada: Boolean = false, // só faz sentido em não-episódica
    val generos: List<String> = emptyList(),
    val temporadasDisponiveis: Int? = null, // já sem os "specials" (season_number 0)
    val episodiosTempMaisRecente: Int? = null, // episódios da última temporada que foi ao ar
    val diaLancamento: String? = null, // rótulo pt-BR (ver DIAS_SEMANA)
    val streamings: List<String> = emptyList(), // já normalizados p/ STREAMINGS_DISPONIVEIS
)

/**
 * Traduções do vocabulário do TMDB para o do app. É lógica pura (sem rede e sem
 * `org.json`) justamente para ser testável — o parser do [TmdbClient] só extrai os
 * campos crus e delega a interpretação para cá.
 */
object TmdbMapa {

    /**
     * Provedores do `watch/providers` → nomes de [STREAMINGS_DISPONIVEIS]. O TMDB usa
     * o nome comercial completo ("Amazon Prime Video", "Disney Plus"), o app usa o
     * apelido curto ("Prime", "Disney+"); e os "Amazon Channel"/"Apple TV Channel" são
     * revendas do mesmo serviço, então caem no serviço original.
     *
     * **Este mapa não foi validado contra resposta real da API** — é melhor esforço.
     * Provedor que não casar é descartado (nunca inventamos um streaming), então
     * corrigir é só acrescentar uma linha aqui.
     */
    private val STREAMING_POR_PROVEDOR: Map<String, String> = mapOf(
        "netflix" to "Netflix",
        "netflix basic with ads" to "Netflix",
        "netflix standard with ads" to "Netflix",
        "amazon prime video" to "Prime",
        "amazon prime video with ads" to "Prime",
        "prime video" to "Prime",
        "hbo max" to "Max",
        "hbo max amazon channel" to "Max",
        "max amazon channel" to "Max",
        "disney plus" to "Disney+",
        // Star+ foi incorporado ao Disney+ no Brasil; catálogo antigo ainda aparece assim.
        "star plus" to "Disney+",
        "apple tv plus" to "Apple TV+",
        "apple tv+ amazon channel" to "Apple TV+",
        "apple tv plus amazon channel" to "Apple TV+",
        "crunchyroll amazon channel" to "Crunchyroll",
        "globoplay amazon channel" to "Globoplay",
        "paramount plus" to "Paramount+",
        "paramount plus premium" to "Paramount+",
        "paramount+ amazon channel" to "Paramount+",
        "paramount plus apple tv channel" to "Paramount+",
    )

    /** "Outro"/"Não sei" são escapes do formulário — autopreenchimento nunca os usa. */
    private val ESCAPES = setOf("Outro", "Não sei")

    /**
     * Nome do streaming no vocabulário do app, ou null se o provedor não é nenhum dos
     * oferecidos no cadastro. Casa primeiro pelo mapa e, na ausência dele, pelo próprio
     * nome (cobre "Netflix", "Max", "Crunchyroll", "Globoplay", que são iguais nos dois).
     */
    fun streaming(provedor: String): String? {
        val chave = provedor.trim().lowercase()
        if (chave.isBlank()) return null
        STREAMING_POR_PROVEDOR[chave]?.let { return it }
        return STREAMINGS_DISPONIVEIS.firstOrNull { it !in ESCAPES && it.lowercase() == chave }
    }

    /** Normaliza a lista do `flatrate`, preservando a ordem e sem repetir. */
    fun streamings(provedores: List<String>): List<String> =
        provedores.mapNotNull { streaming(it) }.distinct()

    /**
     * `status` do `/tv/{id}` → status de lançamento do app. "In Production" e "Pilot"
     * entram como LANCANDO por serem obras já em produção com temporada em curso;
     * "Planned" é o único que ainda não tem nada no ar.
     */
    fun statusEpisodico(status: String?): StatusLancEpisodico? = when (status?.trim()) {
        "Ended" -> StatusLancEpisodico.COMPLETA
        "Canceled", "Cancelled" -> StatusLancEpisodico.CANCELADA
        "Returning Series", "In Production", "Pilot" -> StatusLancEpisodico.LANCANDO
        "Planned" -> StatusLancEpisodico.VAI_LANCAR
        else -> null
    }

    /** `status` do `/movie/{id}`: só o cancelamento tem correspondente no app. */
    fun canceladoNaoEpisodico(status: String?): Boolean =
        status?.trim() == "Canceled" || status?.trim() == "Cancelled"

    /** Ano (YYYY) de uma data ISO `yyyy-MM-dd` do TMDB. */
    fun ano(iso: String?): String? = iso?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }

    /** Data ISO `yyyy-MM-dd` → dígitos `ddMMyyyy`, o formato de `FormDraft.dataTexto`. */
    fun digitosData(iso: String?): String? = parseIso(iso)?.let {
        "%02d%02d%04d".format(it.dayOfMonth, it.monthValue, it.year)
    }

    /** Dia-da-semana de uma data ISO, no rótulo canônico do app ("Segunda", ...). */
    fun diaDaSemana(iso: String?): String? = parseIso(iso)?.let { rotuloDiaDaSemana(it.dayOfWeek) }

    private fun parseIso(iso: String?): LocalDate? =
        iso?.takeIf { it.length >= 10 }?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
}
