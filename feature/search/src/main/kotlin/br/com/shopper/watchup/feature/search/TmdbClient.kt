package br.com.shopper.watchup.feature.search

import br.com.shopper.watchup.core.data.model.TipoMidia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Um resultado da busca no TMDB, já normalizado para o vocabulário do app. */
data class TmdbResultado(
    val titulo: String,
    val ano: String?, // só o ano (YYYY) ou null
    val tipo: TipoMidia,
    val posterUrl: String?,
)

/** Erros de busca com mensagem amigável para a UI. */
class TmdbException(mensagem: String) : Exception(mensagem)

/**
 * Cliente mínimo do TMDB (`/search/multi`) via [HttpURLConnection] + `org.json`,
 * sem lib de rede extra. Só leitura de catálogo público — traz título, ano, tipo
 * e URL do pôster. Resultados `person` são ignorados.
 */
object TmdbClient {

    private const val BASE = "https://api.themoviedb.org/3"
    private const val IMG_BASE = "https://image.tmdb.org/t/p/w342"

    suspend fun buscar(apiKey: String, termo: String): List<TmdbResultado> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw TmdbException("Configure a chave do TMDB para buscar.")
        val q = URLEncoder.encode(termo.trim(), "UTF-8")
        val url = URL("$BASE/search/multi?api_key=$apiKey&language=pt-BR&include_adult=false&query=$q")

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            val codigo = conn.responseCode
            if (codigo == 401) throw TmdbException("Chave do TMDB inválida.")
            if (codigo !in 200..299) throw TmdbException("Falha na busca (HTTP $codigo).")
            val corpo = conn.inputStream.bufferedReader().use { it.readText() }
            parsear(corpo)
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
            val tipo = when (o.optString("media_type")) {
                "movie" -> TipoMidia.FILME
                "tv" -> TipoMidia.SERIE
                else -> continue // ignora "person" e o que não for filme/série
            }
            val titulo = o.optStringOrNull("title") ?: o.optStringOrNull("name") ?: continue
            val data = o.optStringOrNull("release_date") ?: o.optStringOrNull("first_air_date")
            val ano = data?.take(4)?.takeIf { it.length == 4 }
            val poster = o.optStringOrNull("poster_path")?.let { IMG_BASE + it }
            out += TmdbResultado(titulo = titulo, ano = ano, tipo = tipo, posterUrl = poster)
        }
        return out
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).ifBlank { null }
}
