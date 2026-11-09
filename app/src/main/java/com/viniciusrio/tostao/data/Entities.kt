package com.viniciusrio.tostao.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.viniciusrio.tostao.domain.Account
import com.viniciusrio.tostao.domain.Budget
import com.viniciusrio.tostao.domain.Category
import com.viniciusrio.tostao.domain.CategoryType
import com.viniciusrio.tostao.domain.PlannedRule
import com.viniciusrio.tostao.domain.RecurrenceUnit
import com.viniciusrio.tostao.domain.Transaction
import com.viniciusrio.tostao.domain.TxType
import java.time.LocalDate

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Long,
    val icon: String,
    val initialBalanceCents: Long,
    val includeInTotal: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val colorArgb: Long,
    val icon: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("toAccountId"), Index("categoryId"), Index("dateTimeMillis")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    val accountId: String,
    val toAccountId: String?,
    val categoryId: String?,
    val title: String,
    val note: String,
    val dateTimeMillis: Long,
    val plannedRuleId: String?,
    val isAdjustment: Boolean,
)

@Entity(tableName = "planned_rules", indices = [Index("accountId")])
data class PlannedRuleEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    val accountId: String,
    val categoryId: String?,
    val title: String,
    val startEpochDay: Long,
    val unit: String?,
    val interval: Int,
    val endEpochDay: Long?,
    val consumedOccurrences: Int,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val limitCents: Long,
    val allExpenses: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "budget_categories",
    primaryKeys = ["budgetId", "categoryId"],
    indices = [Index("categoryId")],
    foreignKeys = [
        ForeignKey(BudgetEntity::class, ["id"], ["budgetId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class BudgetCategoryEntity(val budgetId: String, val categoryId: String)

fun AccountEntity.toDomain() = Account(id, name, colorArgb, icon, initialBalanceCents, includeInTotal, sortOrder)
fun Account.toEntity() = AccountEntity(id, name, colorArgb, icon, initialBalanceCents, includeInTotal, order)

fun CategoryEntity.toDomain() = Category(id, name, CategoryType.valueOf(type), colorArgb, icon, sortOrder)
fun Category.toEntity() = CategoryEntity(id, name, type.name, colorArgb, icon, order)

fun TransactionEntity.toDomain() = Transaction(
    id, TxType.valueOf(type), amountCents, accountId, toAccountId, categoryId, title, note, dateTimeMillis, plannedRuleId, isAdjustment,
)
fun Transaction.toEntity() = TransactionEntity(
    id, type.name, amountCents, accountId, toAccountId, categoryId, title, note, dateTimeMillis, plannedRuleId, isAdjustment,
)

fun PlannedRuleEntity.toDomain() = PlannedRule(
    id, TxType.valueOf(type), amountCents, accountId, categoryId, title, LocalDate.ofEpochDay(startEpochDay),
    unit?.let(RecurrenceUnit::valueOf), interval, endEpochDay?.let(LocalDate::ofEpochDay), consumedOccurrences,
)
fun PlannedRule.toEntity() = PlannedRuleEntity(
    id, type.name, amountCents, accountId, categoryId, title, startDate.toEpochDay(), unit?.name, interval,
    endDate?.toEpochDay(), consumedOccurrences,
)

fun Budget.toEntity() = BudgetEntity(id, name, limitCents, allExpenses, order)
