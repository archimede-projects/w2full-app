package com.archimede.w2full.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alert_rule WHERE id = 1 LIMIT 1")
    fun observeRule(): Flow<PriceAlertRuleEntity?>

    @Query("SELECT * FROM price_alert_rule WHERE id = 1 LIMIT 1")
    suspend fun getRule(): PriceAlertRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: PriceAlertRuleEntity)
}
