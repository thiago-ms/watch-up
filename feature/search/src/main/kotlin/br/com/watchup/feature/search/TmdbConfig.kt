package br.com.watchup.feature.search

import android.content.Context

/**
 * Guarda a API key do TMDB informada pelo usuário (engrenagem na busca). Se estiver
 * vazia, cai no [BuildConfig.TMDB_API_KEY] (chave opcional embutida no build).
 */
object TmdbConfig {
    private const val NOME = "watchup_tmdb"
    private const val K_API_KEY = "api_key"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NOME, Context.MODE_PRIVATE)

    /** Chave em uso: a informada no app tem prioridade sobre a do build. */
    fun getApiKey(context: Context): String {
        val salva = prefs(context).getString(K_API_KEY, null)?.trim().orEmpty()
        return salva.ifBlank { BuildConfig.TMDB_API_KEY }
    }

    fun setApiKey(context: Context, key: String) {
        prefs(context).edit().putString(K_API_KEY, key.trim()).apply()
    }

    fun temChave(context: Context): Boolean = getApiKey(context).isNotBlank()
}
