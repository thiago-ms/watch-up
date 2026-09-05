package br.com.watchup.core.tmdb

import br.com.watchup.core.data.model.TipoMidia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Um resultado da busca no TMDB, já normalizado para o vocabulário do app. */
data class TmdbResultado(
    val id: Int, // id no TMDB — chave para buscar os detalhes (item 17)
    val tipoTmdb: String, // "movie" ou "tv", como o TMDB devolve; define o endpoint de detalhes
    val titulo: String,
    val ano: String?, // só o ano (YYYY) ou null
    val tipo: TipoMidia,
    val generos: List<String>,
    val posterUrl: String?,
)

/** Erros de busca com mensagem amigável para a UI. */
class TmdbException(mensagem: String) : Exception(mensagem)

/**
 * Cliente mínimo do TMDB (`/search/multi` + detalhes de `/tv|movie/{id}`) via
 * [HttpURLConnection] + `org.json`, sem lib de rede extra. Só leitura de catálogo
 * público. Resultados `person` são ignorados.
 */
object TmdbClient {

    private const val BASE = "https://api.themoviedb.org/3"
    private const val IMG_BASE = "https://image.tmdb.org/t/p/w342"

    suspend fun buscar(apiKey: String, termo: String): List<TmdbResultado> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw TmdbException("Configure a chave do TMDB para buscar.")
        val q = URLEncoder.encode(termo.trim(), "UTF-8")
        val url = URL("$BASE/search/multi?api_key=$apiKey&language=pt-BR&include_adult=false&query=$q")
        val corpo = baixar(url)
        runCatching { parsear(corpo) }.getOrElse { throw TmdbException("Resposta inesperada do TMDB.") }
    }

    /**
     * Item 17 — detalhes de uma obra para autopreencher o cadastro. `next_episode_to_air`
     * e `watch/providers` só existem aqui (o `/search/multi` não os traz), o que é a
     * razão de uma 2ª chamada ao entrar no form.
     *
     * **Falha em silêncio**: devolve null se não houver chave, se a rede cair ou se a
     * resposta vier estranha. O autopreenchimento é um bônus — nunca pode impedir o
     * cadastro manual, então aqui não se lança exceção.
     */
    suspend fun detalhar(apiKey: String, id: Int, tipoTmdb: String): TmdbDetalhes? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || id <= 0) return@withContext null
        val episodico = tipoTmdb == "tv"
        val caminho = if (episodico) "tv" else "movie"
        val url = URL("$BASE/$caminho/$id?api_key=$apiKey&language=pt-BR&append_to_response=watch/providers")
        runCatching { parsearDetalhes(baixar(url), episodico) }.getOrNull()
    }

    /** GET cru com timeout curto; qualquer tropeço vira [TmdbException]. */
    private fun baixar(url: URL): String {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            val codigo = conn.responseCode
            if (codigo == 401) throw TmdbException("Chave do TMDB inválida.")
            if (codigo !in 200..299) throw TmdbException("Falha na busca (HTTP $codigo).")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: TmdbException) {
            throw e
        } catch (e: Exception) {
            throw TmdbException("Sem conexão com o TMDB.")
        } finally {
            conn.disconnect()
        }
    }

    private fun parsear(json: String): List<TmdbResultado> {
        val arr = JSONObject(json).optJSONArray("results") ?: return emptyList()
        val out = ArrayList<TmdbResultado>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val media = o.optString("media_type")
            if (media != "movie" && media != "tv") continue // ignora "person" etc.
            val titulo = o.optStringOrNull("title") ?: o.optStringOrNull("name") ?: continue
            val data = o.optStringOrNull("release_date") ?: o.optStringOrNull("first_air_date")
            val ano = data?.take(4)?.takeIf { it.length == 4 }
            val poster = o.optStringOrNull("poster_path")?.let { IMG_BASE + it }
            val genreIds = o.optJSONArray("genre_ids")?.let { g ->
                (0 until g.length()).map { g.getInt(it) }
            }.orEmpty()
            val idioma = o.optStringOrNull("original_language")
            out += TmdbResultado(
                id = o.optInt("id"),
                tipoTmdb = media,
                titulo = titulo,
                ano = ano,
                tipo = inferirTipo(media, genreIds, idioma),
                generos = TmdbGenres.nomes(genreIds),
                posterUrl = poster,
            )
        }
        return out
    }

    /**
     * Extrai do JSON de detalhes só os campos crus e delega a tradução ao [TmdbMapa]
     * (que é puro e testável). Cada campo é independente: o que faltar vira null e o
     * form continua pedindo ao usuário.
     */
    private fun parsearDetalhes(json: String, episodico: Boolean): TmdbDetalhes {
        val o = JSONObject(json)
        val estreia = o.optStringOrNull(if (episodico) "first_air_date" else "release_date")
        val status = o.optStringOrNull("status")

        val generos = o.optJSONArray("genres")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optStringOrNull("name") }
        }.orEmpty()

        // Temporadas: a lista `seasons` inclui os "specials" (season_number 0), que não
        // contam como temporada no app — daí filtrar em vez de usar number_of_seasons.
        val temporadas = o.optJSONArray("seasons")
        val regulares = temporadas?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                .filter { it.optInt("season_number", 0) > 0 }
        }.orEmpty()

        // Episódios da temporada que está no ar (a do último episódio exibido). Não
        // mexemos em `temporadaAtual`: essa é progresso pessoal, não disponibilidade.
        val ultimoEp = o.optJSONObject("last_episode_to_air")
        val tempMaisRecente = ultimoEp?.optInt("season_number", 0)?.takeIf { it > 0 }
        val episodiosTemp = tempMaisRecente
            ?.let { n -> regulares.firstOrNull { it.optInt("season_number", 0) == n } }
            ?.optInt("episode_count", 0)?.takeIf { it > 0 }

        // Dia da semana: o próximo episódio é a melhor fonte; sem ele, o último exibido.
        val dataEpisodio = o.optJSONObject("next_episode_to_air")?.optStringOrNull("air_date")
            ?: ultimoEp?.optStringOrNull("air_date")

        val provedores = o.optJSONObject("watch/providers")
            ?.optJSONObject("results")
            ?.optJSONObject("BR")
            ?.optJSONArray("flatrate")
            ?.let { arr -> (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optStringOrNull("provider_name") } }
            .orEmpty()

        return TmdbDetalhes(
            ano = TmdbMapa.ano(estreia),
            dataDigitos = TmdbMapa.digitosData(estreia),
            statusLancEpisodico = if (episodico) TmdbMapa.statusEpisodico(status) else null,
            cancelada = !episodico && TmdbMapa.canceladoNaoEpisodico(status),
            generos = generos,
            temporadasDisponiveis = regulares.size.takeIf { it > 0 },
            episodiosTempMaisRecente = episodiosTemp,
            diaLancamento = TmdbMapa.diaDaSemana(dataEpisodio),
            streamings = TmdbMapa.streamings(provedores),
        )
    }

    /**
     * Mapeia (media_type + gêneros + idioma) para o tipo do app. TMDB só distingue
     * movie/tv; o resto é heurística: Documentário pelo gênero, Anime por animação
     * japonesa (só em séries, pra não confundir com filme episódico), Reality e
     * Programa por gêneros de TV. Erros são corrigíveis no cadastro.
     */
    private fun inferirTipo(media: String, generos: List<Int>, idioma: String?): TipoMidia = when {
        TmdbGenres.DOCUMENTARIO in generos -> TipoMidia.DOCUMENTARIO
        media == "tv" && TmdbGenres.ANIMACAO in generos && idioma == "ja" -> TipoMidia.ANIME
        media == "tv" && TmdbGenres.REALITY in generos -> TipoMidia.REALITY
        media == "tv" && (TmdbGenres.TALK in generos || TmdbGenres.NEWS in generos) -> TipoMidia.PROGRAMA
        media == "tv" -> TipoMidia.SERIE
        else -> TipoMidia.FILME
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).ifBlank { null }
}
