package com.viniciusrio.tostao.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder") fun accounts(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM categories ORDER BY sortOrder, name") fun categories(): Flow<List<CategoryEntity>>
    // Mais recentemente inseridas primeiro: as telas ordenam por data com sort estável, então
    // num empate de horário a última lançada fica no topo. O upsert de edição preserva o rowid.
    @Query("SELECT * FROM transactions ORDER BY rowid DESC") fun transactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM planned_rules") fun rules(): Flow<List<PlannedRuleEntity>>
    @Query("SELECT * FROM budgets ORDER BY sortOrder") fun budgets(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budget_categories") fun budgetCategories(): Flow<List<BudgetCategoryEntity>>

    @Upsert suspend fun upsertAccount(a: AccountEntity)
    @Upsert suspend fun upsertAccounts(a: List<AccountEntity>)
    @Upsert suspend fun upsertCategory(c: CategoryEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCategories(c: List<CategoryEntity>)
    @Upsert suspend fun upsertTransaction(t: TransactionEntity)
    @Upsert suspend fun upsertRule(r: PlannedRuleEntity)
    @Upsert suspend fun upsertBudget(b: BudgetEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertBudgetCategories(l: List<BudgetCategoryEntity>)

    @Query("SELECT * FROM accounts WHERE id = :id") suspend fun account(id: String): AccountEntity?
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun category(id: String): CategoryEntity?
    @Query("SELECT * FROM planned_rules WHERE id = :id") suspend fun rule(id: String): PlannedRuleEntity?
    @Query("SELECT * FROM transactions WHERE accountId = :id OR toAccountId = :id") suspend fun transactionsOfAccount(id: String): List<TransactionEntity>
    @Query("SELECT * FROM planned_rules WHERE accountId = :id") suspend fun rulesOfAccount(id: String): List<PlannedRuleEntity>
    @Query("SELECT COUNT(*) FROM categories") suspend fun categoryCount(): Int
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM accounts") suspend fun maxAccountOrder(): Int
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM budgets") suspend fun maxBudgetOrder(): Int
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories") suspend fun maxCategoryOrder(): Int

    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteTransaction(id: String)
    @Query("DELETE FROM transactions WHERE accountId = :id OR toAccountId = :id") suspend fun deleteTransactionsOfAccount(id: String)
    @Query("DELETE FROM planned_rules WHERE accountId = :id") suspend fun deleteRulesOfAccount(id: String)
    @Query("DELETE FROM accounts WHERE id = :id") suspend fun deleteAccount(id: String)
    @Query("DELETE FROM planned_rules WHERE id = :id") suspend fun deleteRule(id: String)
    @Query("DELETE FROM budgets WHERE id = :id") suspend fun deleteBudget(id: String)
    @Query("DELETE FROM budget_categories WHERE budgetId = :id") suspend fun clearBudgetCategories(id: String)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun deleteCategory(id: String)
    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :id") suspend fun uncategorizeTransactions(id: String)
    @Query("UPDATE planned_rules SET categoryId = NULL WHERE categoryId = :id") suspend fun uncategorizeRules(id: String)
    @Query(
        """DELETE FROM budgets WHERE allExpenses = 0 AND id IN (SELECT budgetId FROM budget_categories WHERE categoryId = :id)
           AND id NOT IN (SELECT budgetId FROM budget_categories WHERE categoryId != :id)""",
    )
    suspend fun deleteBudgetsOnlyWithCategory(id: String)
    @Query("UPDATE planned_rules SET consumedOccurrences = :value WHERE id = :id") suspend fun setConsumed(id: String, value: Int)

    // Retrato completo (backup) e substituição (restauração).
    @Query("SELECT * FROM accounts ORDER BY sortOrder") suspend fun allAccounts(): List<AccountEntity>
    @Query("SELECT * FROM categories ORDER BY sortOrder, name") suspend fun allCategories(): List<CategoryEntity>
    @Query("SELECT * FROM transactions") suspend fun allTransactions(): List<TransactionEntity>
    @Query("SELECT * FROM planned_rules") suspend fun allRules(): List<PlannedRuleEntity>
    @Query("SELECT * FROM budgets ORDER BY sortOrder") suspend fun allBudgets(): List<BudgetEntity>
    @Query("SELECT * FROM budget_categories") suspend fun allBudgetCategories(): List<BudgetCategoryEntity>

    @Query("DELETE FROM budget_categories") suspend fun clearAllBudgetCategories()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM planned_rules") suspend fun clearRules()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()

    @Insert suspend fun insertAccounts(l: List<AccountEntity>)
    @Insert suspend fun insertCategoriesStrict(l: List<CategoryEntity>)
    @Insert suspend fun insertTransactions(l: List<TransactionEntity>)
    @Insert suspend fun insertRules(l: List<PlannedRuleEntity>)
    @Insert suspend fun insertBudgets(l: List<BudgetEntity>)
    @Insert suspend fun insertBudgetCategoriesStrict(l: List<BudgetCategoryEntity>)
}
