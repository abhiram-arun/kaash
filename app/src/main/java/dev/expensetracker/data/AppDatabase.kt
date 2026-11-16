package dev.expensetracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Txn::class, Category::class, MerchantRule::class, Budget::class, RecurringBill::class, Unparsed::class,
        Person::class, Split::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}

/** v3: people and splits. SQL mirrors Room's generated schema exactly, or Room rejects the DB on open. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Person` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Person_name` ON `Person` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `Split` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `txnId` INTEGER NOT NULL, " +
                "`personId` INTEGER NOT NULL, `amountPaise` INTEGER NOT NULL, `settled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`settledAt` INTEGER, `lastReminded` INTEGER)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_Split_txnId` ON `Split` (`txnId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_Split_personId` ON `Split` (`personId`)")
    }
}

/** v2: category hierarchy. Old flat categories become topics, renamed to match TAXONOMY so tags survive. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Category ADD COLUMN parentId INTEGER")
        db.execSQL("ALTER TABLE Category ADD COLUMN icon TEXT")
        listOf("Food" to "Food & Dining", "Rent" to "Housing", "Bills" to "Bills & Utilities").forEach { (old, new) ->
            db.execSQL("UPDATE Category SET name = ? WHERE name = ?", arrayOf(new, old))
        }
    }
}
