package br.com.watchup.core.data.repo

import android.content.Context

/**
 * Guarda a API key do TMDB informada pelo usuário (engrenagem na busca). Aqui a
 * chave é crua: quem decide o fallback para a chave embutida no build é o
 * `TmdbConfig` do `:core:tmdb` — este módulo não tem `BuildConfig`.
 *
 * O nome do arquivo e da chave não mudam: são os mesmos de quando isso morava no
 * `:feature:search`, senão a chave já configurada no aparelho se perderia.
 */
object TmdbPrefs {
    private const val NOME = "watchup_tmdb"
    private const val K_API_KEY = "api_key"
    private const val K_AUTOPREENCHER = "autopreencher"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NOME, Context.MODE_PRIVATE)

    /** Chave salva pelo usuário, já sem espaços; vazia quando nunca foi informada. */
    fun getApiKey(context: Context): String =
        prefs(context).getString(K_API_KEY, null)?.trim().orEmpty()

    fun setApiKey(context: Context, key: String) {
        prefs(context).edit().putString(K_API_KEY, key.trim()).apply()
    }

    /**
     * Item 17 — autopreencher o cadastro com os detalhes do TMDB quando ele vem da
     * busca. **Ligado por padrão**: o toggle existe para desligar; desligado, o form
     * se comporta exatamente como antes (nem chama a API).
     */
    fun getAutoPreencher(context: Context): Boolean = prefs(context).getBoolean(K_AUTOPREENCHER, true)

    fun setAutoPreencher(context: Context, ativo: Boolean) {
        prefs(context).edit().putBoolean(K_AUTOPREENCHER, ativo).apply()
    }
}
