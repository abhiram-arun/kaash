package dev.expensetracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Txn::class, Category::class, MerchantRule::class, Budget::class, RecurringBill::class, Unparsed::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
