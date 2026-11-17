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
    version = 5,
    exportSchema = true, // app/schemas/*.json: lets MigrationTestHelper check future migrations
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}

/**
 * v5: SMS text is unique. Keeps the earliest copy of any repeated text (a copy's later timestamp can only come from
 * re-injection, never from the bank), so a lone injected copy gets its real date back on the next inbox rescan.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM Txn WHERE EXISTS (SELECT 1 FROM Txn o WHERE o.raw = Txn.raw " +
                "AND (o.timestamp < Txn.timestamp OR (o.timestamp = Txn.timestamp AND o.id < Txn.id)))"
        )
        db.execSQL("DELETE FROM Split WHERE txnId NOT IN (SELECT id FROM Txn)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Txn_raw` ON `Txn` (`raw`)")
    }
}

/**
 * v4: a UPI ref is unique per account, not globally — a transfer between own accounts carries the same ref in both
 * banks' SMS. Also drops rows whose SMS text duplicates an earlier row (only the old debug sample button made those;
 * real bank SMS never repeat verbatim) and the OTP/declined noise that used to be logged as unparsed.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_Txn_ref`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Txn_ref_account` ON `Txn` (`ref`, `account`)")
        db.execSQL("DELETE FROM Txn WHERE id NOT IN (SELECT MIN(id) FROM Txn GROUP BY raw)")
        db.execSQL("DELETE FROM Split WHERE txnId NOT IN (SELECT id FROM Txn)")
        db.execSQL("DELETE FROM Unparsed WHERE body LIKE '%OTP%' OR body LIKE '%declined%'")
    }
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
