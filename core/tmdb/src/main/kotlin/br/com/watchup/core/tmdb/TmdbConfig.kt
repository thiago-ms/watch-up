package br.com.watchup.core.tmdb

import android.content.Context
import br.com.watchup.core.data.repo.TmdbPrefs

/**
 * Resolve qual API key do TMDB usar: a informada pelo usuário (engrenagem na busca,
 * guardada no [TmdbPrefs]) tem prioridade; se estiver vazia, cai no
 * [BuildConfig.TMDB_API_KEY] (chave opcional embutida no build).
 */
object TmdbConfig {

    /** Chave em uso: a informada no app tem prioridade sobre a do build. */
    fun getApiKey(context: Context): String =
        TmdbPrefs.getApiKey(context).ifBlank { BuildConfig.TMDB_API_KEY }

    fun setApiKey(context: Context, key: String) = TmdbPrefs.setApiKey(context, key)

    fun temChave(context: Context): Boolean = getApiKey(context).isNotBlank()
}
