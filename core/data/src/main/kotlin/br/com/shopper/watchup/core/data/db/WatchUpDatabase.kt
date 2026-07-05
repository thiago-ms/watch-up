package br.com.shopper.watchup.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.shopper.watchup.core.data.model.EpisodiosTemporada
import br.com.shopper.watchup.core.data.model.Midia

@Database(
    entities = [Midia::class, EpisodiosTemporada::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class WatchUpDatabase : RoomDatabase() {
    abstract fun midiaDao(): MidiaDao
}
