package dev.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
        AND (:cat IS NULL OR categoryId IN (SELECT id FROM Category WHERE id = :cat OR parentId = :cat))
        AND (:untagged = 0 OR categoryId IS NULL)
        AND (:dir IS NULL OR direction = :dir)
        AND (:acct IS NULL OR account = :acct)
        ORDER BY timestamp DESC"""
    )
    fun txns(
        from: Long, to: Long, q: String = "", cat: Long? = null, untagged: Boolean = false,
        dir: String? = null, acct: String? = null,
    ): Flow<List<Txn>>

    /** Lets a rescan fill balances the parser learned to read after the row was first saved. */
    @Query("UPDATE Txn SET balancePaise = :bal WHERE account = :account AND amountPaise = :amount AND timestamp = :ts AND balancePaise IS NULL")
    suspend fun fillBalance(account: String, amount: Long, ts: Long, bal: Long)

    /** The earliest sighting of an SMS is its real time (inbox date_sent); later copies only move it earlier. */
    @Query("UPDATE Txn SET timestamp = :ts WHERE raw = :raw AND timestamp > :ts")
    suspend fun keepEarliest(raw: String, ts: Long)

    @Query("SELECT * FROM Txn WHERE id = :id")
    suspend fun txn(id: Long): Txn?

    @Query(
        """SELECT account,
        SUM(CASE WHEN direction = 'DEBIT' THEN amountPaise ELSE 0 END) AS spent,
        SUM(CASE WHEN direction = 'CREDIT' THEN amountPaise ELSE 0 END) AS received,
        MAX(raw) AS sample
        FROM Txn WHERE timestamp BETWEEN :from AND :to GROUP BY account ORDER BY spent DESC"""
    )
    fun accountTotals(from: Long, to: Long): Flow<List<AccountTotal>>

    /** The most recent balance-bearing txn per account (for cards, balance = available limit). */
    @Query(
        """SELECT t.* FROM Txn t JOIN (
            SELECT account, MAX(timestamp) AS ts FROM Txn WHERE balancePaise IS NOT NULL GROUP BY account
        ) m ON t.account = m.account AND t.timestamp = m.ts
        WHERE t.balancePaise IS NOT NULL"""
    )
    fun latestBalances(): Flow<List<Txn>>

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

    /** Debit totals rolled up to the topic level; categoryId null = untagged. */
    @Query(
        """SELECT COALESCE(c.parentId, c.id) AS categoryId, SUM(t.amountPaise) AS total
        FROM Txn t LEFT JOIN Category c ON c.id = t.categoryId
        WHERE t.direction = 'DEBIT' AND t.timestamp BETWEEN :from AND :to
        GROUP BY COALESCE(c.parentId, c.id) ORDER BY total DESC"""
    )
    fun spendByTopic(from: Long, to: Long): Flow<List<CatTotal>>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM Txn WHERE direction = :dir AND timestamp BETWEEN :from AND :to")
    fun total(dir: String, from: Long, to: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM Txn WHERE categoryId IS NULL AND timestamp BETWEEN :from AND :to")
    fun untaggedCount(from: Long, to: Long): Flow<Int>

    /** Spend for a topic including all its subtopics. */
    @Query(
        """SELECT COALESCE(SUM(amountPaise), 0) FROM Txn WHERE direction = 'DEBIT' AND timestamp BETWEEN :from AND :to
        AND categoryId IN (SELECT id FROM Category WHERE id = :topic OR parentId = :topic)"""
    )
    suspend fun spent(topic: Long, from: Long, to: Long): Long

    // Categories & rules
    @Query("SELECT * FROM Category ORDER BY id")
    fun categories(): Flow<List<Category>>

    @Query("SELECT * FROM Category")
    suspend fun allCategories(): List<Category>

    @Query("SELECT name FROM Category WHERE id = :id")
    suspend fun categoryName(id: Long): String?

    @Query("SELECT COALESCE(parentId, id) FROM Category WHERE id = :id")
    suspend fun topicOf(id: Long): Long?

    @Insert
    suspend fun insertCategory(c: Category): Long

    @Update
    suspend fun updateCategory(c: Category)

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

    // People & splits
    @Query("SELECT * FROM Person ORDER BY name COLLATE NOCASE")
    fun people(): Flow<List<Person>>

    @Query("SELECT id FROM Person WHERE name = :name COLLATE NOCASE")
    suspend fun personId(name: String): Long?

    @Insert
    suspend fun insertPerson(p: Person): Long

    @Query(
        """SELECT s.*, p.name AS personName, t.counterparty AS payee, t.timestamp AS txnTime
        FROM Split s JOIN Person p ON p.id = s.personId JOIN Txn t ON t.id = s.txnId
        WHERE s.txnId = :txnId ORDER BY s.id"""
    )
    fun splitsFor(txnId: Long): Flow<List<SplitDetail>>

    @Query(
        """SELECT s.*, p.name AS personName, t.counterparty AS payee, t.timestamp AS txnTime
        FROM Split s JOIN Person p ON p.id = s.personId JOIN Txn t ON t.id = s.txnId
        WHERE s.settled = 0 ORDER BY p.name COLLATE NOCASE, t.timestamp"""
    )
    fun unsettled(): Flow<List<SplitDetail>>

    @Query(
        """SELECT s.*, p.name AS personName, t.counterparty AS payee, t.timestamp AS txnTime
        FROM Split s JOIN Person p ON p.id = s.personId JOIN Txn t ON t.id = s.txnId
        WHERE s.settled = 0"""
    )
    suspend fun unsettledList(): List<SplitDetail>

    @Query("SELECT * FROM Split WHERE txnId = :txnId AND personId = :personId")
    suspend fun split(txnId: Long, personId: Long): Split?

    @Upsert
    suspend fun upsertSplit(s: Split)

    @Query("UPDATE Split SET settled = :settled, settledAt = :at WHERE id = :id")
    suspend fun setSettled(id: Long, settled: Boolean, at: Long?)

    @Delete
    suspend fun deleteSplit(s: Split)

    @Query("UPDATE Split SET lastReminded = :at WHERE id IN (:ids)")
    suspend fun markReminded(ids: List<Long>, at: Long)

    // Unparsed
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnparsed(u: Unparsed)

    @Query("DELETE FROM Unparsed WHERE sender = :sender AND timestamp = :ts")
    suspend fun deleteUnparsed(sender: String, ts: Long)

    @Query("SELECT * FROM Unparsed ORDER BY timestamp DESC")
    fun unparsed(): Flow<List<Unparsed>>
}
