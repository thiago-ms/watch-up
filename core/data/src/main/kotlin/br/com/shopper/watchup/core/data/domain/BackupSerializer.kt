package br.com.shopper.watchup.core.data.domain

import br.com.shopper.watchup.core.data.model.Contexto
import br.com.shopper.watchup.core.data.model.EpisodiosTemporada
import br.com.shopper.watchup.core.data.model.Midia
import br.com.shopper.watchup.core.data.model.Modalidade
import br.com.shopper.watchup.core.data.model.StatusData
import br.com.shopper.watchup.core.data.model.StatusLancEpisodico
import br.com.shopper.watchup.core.data.model.StatusUsuario
import br.com.shopper.watchup.core.data.model.TipoMidia
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * (De)serialização do backup da biblioteca em JSON (via `org.json`, sem lib extra).
 * Usado pelo backup via SAF — grava/le um único `backup.json`. Enums viram `name`,
 * `LocalDate` vira ISO-8601 e a lista de streamings vira um array.
 */
object BackupSerializer {

    private const val VERSION = 1

    fun toJson(midias: List<Midia>, episodios: List<EpisodiosTemporada>): String {
        val root = JSONObject()
        root.put("version", VERSION)

        val arrMidias = JSONArray()
        midias.forEach { arrMidias.put(midiaToJson(it)) }
        root.put("midias", arrMidias)

        val arrEps = JSONArray()
        episodios.forEach {
            arrEps.put(
                JSONObject()
                    .put("midiaId", it.midiaId)
                    .put("temporada", it.temporada)
                    .put("quantidade", it.quantidade),
            )
        }
        root.put("episodios", arrEps)
        return root.toString(2)
    }

    fun fromJson(json: String): Pair<List<Midia>, List<EpisodiosTemporada>> {
        val root = JSONObject(json)

        val midias = root.optJSONArray("midias")?.let { arr ->
            (0 until arr.length()).map { midiaFromJson(arr.getJSONObject(it)) }
        }.orEmpty()

        val episodios = root.optJSONArray("episodios")?.let { arr ->
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                EpisodiosTemporada(
                    midiaId = o.getLong("midiaId"),
                    temporada = o.getInt("temporada"),
                    quantidade = o.getInt("quantidade"),
                )
            }
        }.orEmpty()

        return midias to episodios
    }

    private fun midiaToJson(m: Midia): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("tipo", m.tipo.name)
        put("titulo", m.titulo)
        putN("ano", m.ano)
        put("genero", m.genero)
        put("contexto", m.contexto.name)
        put("modalidade", m.modalidade.name)
        put("streamings", JSONArray(m.streamings))
        putN("streamingPrincipal", m.streamingPrincipal)
        putN("cinemaRede", m.cinemaRede)
        putN("statusLancEpisodico", m.statusLancEpisodico?.name)
        put("cancelada", m.cancelada)
        put("statusData", m.statusData.name)
        putN("dataPrincipal", m.dataPrincipal?.toString())
        putN("diaLancamento", m.diaLancamento)
        putN("horarioLancamento", m.horarioLancamento)
        put("temporadasDisponiveis", m.temporadasDisponiveis)
        put("temporadaAtual", m.temporadaAtual)
        put("episodiosDispTempAtual", m.episodiosDispTempAtual)
        put("ultimoEpisodioVisto", m.ultimoEpisodioVisto)
        put("statusUsuario", m.statusUsuario.name)
    }

    private fun midiaFromJson(o: JSONObject): Midia = Midia(
        id = o.getLong("id"),
        tipo = TipoMidia.valueOf(o.getString("tipo")),
        titulo = o.getString("titulo"),
        ano = o.intOrNull("ano"),
        genero = o.getString("genero"),
        contexto = Contexto.valueOf(o.getString("contexto")),
        modalidade = Modalidade.valueOf(o.getString("modalidade")),
        streamings = o.optJSONArray("streamings")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }.orEmpty(),
        streamingPrincipal = o.strOrNull("streamingPrincipal"),
        cinemaRede = o.strOrNull("cinemaRede"),
        statusLancEpisodico = o.strOrNull("statusLancEpisodico")?.let { StatusLancEpisodico.valueOf(it) },
        cancelada = o.optBoolean("cancelada", false),
        statusData = StatusData.valueOf(o.getString("statusData")),
        dataPrincipal = o.strOrNull("dataPrincipal")?.let(LocalDate::parse),
        diaLancamento = o.strOrNull("diaLancamento"),
        horarioLancamento = o.strOrNull("horarioLancamento"),
        temporadasDisponiveis = o.optInt("temporadasDisponiveis", 0),
        temporadaAtual = o.optInt("temporadaAtual", 0),
        episodiosDispTempAtual = o.optInt("episodiosDispTempAtual", 0),
        ultimoEpisodioVisto = o.optInt("ultimoEpisodioVisto", 0),
        statusUsuario = StatusUsuario.valueOf(o.getString("statusUsuario")),
    )

    private fun JSONObject.putN(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.strOrNull(key: String): String? =
        if (isNull(key)) null else optString(key, null)

    private fun JSONObject.intOrNull(key: String): Int? =
        if (isNull(key)) null else getInt(key)
}
