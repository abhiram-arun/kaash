package dev.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    /** Returns -1 when a unique index says it's a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTxn(t: Txn): Long

    @Query(
        """SELECT * FROM Txn WHERE timestamp BETWEEN :from AND :to
        AND (:q = '' OR counterparty LIKE '%' || :q || '%')
        AND (:cat IS NULL OR categoryId = :cat)
        AND (:untagged = 0 OR categoryId IS NULL)
        AND (:dir IS NULL OR direction = :dir)
        AND (:acct IS NULL OR account = :acct)
        ORDER BY timestamp DESC"""
    )
    fun txns(
        from: Long, to: Long, q: String = "", cat: Long? = null, untagged: Boolean = false,
        dir: String? = null, acct: String? = null,
    ): Flow<List<Txn>>

    @Query("SELECT DISTINCT account FROM Txn ORDER BY account")
    fun accounts(): Flow<List<String>>

    @Query("UPDATE Txn SET categoryId = :cat WHERE id = :id")
    suspend fun setCategory(id: Long, cat: Long?)

    @Query("UPDATE Txn SET categoryId = :cat WHERE merchantKey = :key AND categoryId IS NULL")
    suspend fun tagUntagged(key: String, cat: Long)

    @Query(
        """SELECT categoryId, SUM(amountPaise) AS total FROM Txn
        WHERE direction = 'DEBIT' AND timestamp BETWEEN :from AND :to
        GROUP BY categoryId ORDER BY total DESC"""
    )
    fun spendByCategory(from: Long, to: Long): Flow<List<CatTotal>>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM Txn WHERE direction = :dir AND timestamp BETWEEN :from AND :to")
    fun total(dir: String, from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM Txn WHERE direction = 'DEBIT' AND categoryId = :cat AND timestamp BETWEEN :from AND :to")
    suspend fun spent(cat: Long, from: Long, to: Long): Long

    // Categories & rules
    @Query("SELECT * FROM Category ORDER BY name")
    fun categories(): Flow<List<Category>>

    @Query("SELECT name FROM Category WHERE id = :id")
    suspend fun categoryName(id: Long): String?

    @Query("SELECT COUNT(*) FROM Category")
    suspend fun categoryCount(): Int

    @Insert
    suspend fun insertCategories(c: List<Category>)

    @Query("SELECT categoryId FROM MerchantRule WHERE merchantKey = :key")
    suspend fun ruleFor(key: String): Long?

    @Upsert
    suspend fun upsertRule(r: MerchantRule)

    // Budgets
    @Query("SELECT * FROM Budget")
    fun budgets(): Flow<List<Budget>>

    @Query("SELECT * FROM Budget WHERE categoryId = :cat")
    suspend fun budget(cat: Long): Budget?

    @Upsert
    suspend fun upsertBudget(b: Budget)

    @Query("DELETE FROM Budget WHERE categoryId = :cat")
    suspend fun deleteBudget(cat: Long)

    // Bills
    @Query("SELECT * FROM RecurringBill ORDER BY name")
    fun bills(): Flow<List<RecurringBill>>

    @Query("SELECT * FROM RecurringBill")
    suspend fun allBills(): List<RecurringBill>

    @Upsert
    suspend fun upsertBill(b: RecurringBill)

    @Delete
    suspend fun deleteBill(b: RecurringBill)

    // Unparsed
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnparsed(u: Unparsed)

    @Query("SELECT * FROM Unparsed ORDER BY timestamp DESC")
    fun unparsed(): Flow<List<Unparsed>>
}
