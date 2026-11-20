package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items ORDER BY code ASC")
    fun observeAll(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items ORDER BY code ASC")
    suspend fun getAllSnapshot(): List<MenuItemEntity>

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun count(): Int

    @Query("SELECT * FROM menu_items WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): MenuItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MenuItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MenuItemEntity>)

    @Query("UPDATE menu_items SET stock = :newStock, isAvailable = CASE WHEN :newStock > 0 THEN 1 ELSE 0 END WHERE code = :code")
    suspend fun updateStock(code: String, newStock: Int)

    @Query("DELETE FROM menu_items WHERE code = :code")
    suspend fun deleteByCode(code: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateStatus(orderId: Long, status: String)
}

@Dao
interface BotSettingsDao {
    @Query("SELECT * FROM bot_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<BotSettingsEntity?>

    @Query("SELECT * FROM bot_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): BotSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: BotSettingsEntity)
}

@Dao
interface BotContactStateDao {
    @Query("SELECT * FROM bot_contact_state WHERE contactKey = :contactKey LIMIT 1")
    suspend fun getByKey(contactKey: String): BotContactStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: BotContactStateEntity)

    @Query("DELETE FROM bot_contact_state")
    suspend fun clearAll()
}

@Dao
interface BotActivityLogDao {
    @Query("SELECT * FROM bot_activity_logs ORDER BY createdAtMillis DESC LIMIT 40")
    fun observeRecent(): Flow<List<BotActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: BotActivityLogEntity)

    @Query("DELETE FROM bot_activity_logs")
    suspend fun clearAll()
}
