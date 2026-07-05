package br.com.watchup.feature.search

/**
 * Dicionário de gêneros do TMDB (movie + tv unificados) em pt-BR. Os nomes batem
 * com [br.com.watchup.core.data.model.GENEROS_DISPONIVEIS] para que os chips
 * do cadastro sejam pré-selecionados a partir do resultado da busca.
 *
 * Alguns IDs também guiam a inferência do tipo do app (Anime, Documentário etc.).
 */
object TmdbGenres {

    const val DOCUMENTARIO = 99
    const val ANIMACAO = 16
    const val REALITY = 10764
    const val TALK = 10767
    const val NEWS = 10763

    private val NOMES: Map<Int, String> = mapOf(
        28 to "Ação",
        12 to "Aventura",
        16 to "Animação",
        35 to "Comédia",
        80 to "Crime",
        99 to "Documentário",
        18 to "Drama",
        10751 to "Família",
        14 to "Fantasia",
        36 to "História",
        27 to "Terror",
        10402 to "Música",
        9648 to "Mistério",
        10749 to "Romance",
        878 to "Ficção científica",
        10770 to "Cinema TV",
        53 to "Thriller",
        10752 to "Guerra",
        37 to "Faroeste",
        // Exclusivos de TV
        10759 to "Ação e Aventura",
        10762 to "Kids",
        10763 to "Notícias",
        10764 to "Reality",
        10765 to "Ficção científica e Fantasia",
        10766 to "Novela",
        10767 to "Talk",
        10768 to "Guerra e Política",
    )

    fun nomes(ids: List<Int>): List<String> = ids.mapNotNull { NOMES[it] }
}
