package br.com.watchup.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.watchup.core.data.model.EpisodiosTemporada
import br.com.watchup.core.data.model.Midia

@Database(
    entities = [Midia::class, EpisodiosTemporada::class],
    version = 8,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class WatchUpDatabase : RoomDatabase() {
    abstract fun midiaDao(): MidiaDao
}

/**
 * v4 → v5: coluna `favorito` (item 11). Migração explícita para **preservar a
 * biblioteca** ao atualizar (em vez do wipe destrutivo). Boolean é INTEGER no
 * SQLite (0 = false).
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE midia ADD COLUMN favorito INTEGER NOT NULL DEFAULT 0")
    }
}

/** v5 → v6: coluna `arquivada` (item 9), também preservando a biblioteca. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE midia ADD COLUMN arquivada INTEGER NOT NULL DEFAULT 0")
    }
}

/** v6 → v7: colunas de contagem de novos episódios (item 4). */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE midia ADD COLUMN cadenciaDias INTEGER NOT NULL DEFAULT 7")
        db.execSQL("ALTER TABLE midia ADD COLUMN dataBaseContagem TEXT")
    }
}

/** v7 → v8: coluna `intencao` (item 8), preservando a biblioteca. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE midia ADD COLUMN intencao INTEGER NOT NULL DEFAULT 0")
    }
}
