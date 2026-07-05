package br.com.watchup.core.data.db

import androidx.room.TypeConverter
import br.com.watchup.core.data.model.Contexto
import br.com.watchup.core.data.model.Modalidade
import br.com.watchup.core.data.model.StatusData
import br.com.watchup.core.data.model.StatusLancEpisodico
import br.com.watchup.core.data.model.StatusUsuario
import br.com.watchup.core.data.model.TipoMidia
import java.time.LocalDate

/**
 * Conversores do Room. Enums são persistidos pelo `name` (estável e legível);
 * [List]<String> vira uma string separada por `` (unit separator, que não
 * aparece em nomes de streaming); [LocalDate] vira ISO-8601.
 */
class Converters {

    // --- List<String> (streamings) ---
    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.joinToString(SEP)

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(SEP)

    // --- LocalDate ---
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    // --- Enums ---
    @TypeConverter
    fun fromTipo(v: TipoMidia): String = v.name

    @TypeConverter
    fun toTipo(v: String): TipoMidia = TipoMidia.valueOf(v)

    @TypeConverter
    fun fromContexto(v: Contexto): String = v.name

    @TypeConverter
    fun toContexto(v: String): Contexto = Contexto.valueOf(v)

    @TypeConverter
    fun fromModalidade(v: Modalidade): String = v.name

    @TypeConverter
    fun toModalidade(v: String): Modalidade = Modalidade.valueOf(v)

    @TypeConverter
    fun fromStatusLancEp(v: StatusLancEpisodico?): String? = v?.name

    @TypeConverter
    fun toStatusLancEp(v: String?): StatusLancEpisodico? = v?.let(StatusLancEpisodico::valueOf)

    @TypeConverter
    fun fromStatusData(v: StatusData): String = v.name

    @TypeConverter
    fun toStatusData(v: String): StatusData = StatusData.valueOf(v)

    @TypeConverter
    fun fromStatusUsuario(v: StatusUsuario): String = v.name

    @TypeConverter
    fun toStatusUsuario(v: String): StatusUsuario = StatusUsuario.valueOf(v)

    private companion object {
        const val SEP = ""
    }
}
