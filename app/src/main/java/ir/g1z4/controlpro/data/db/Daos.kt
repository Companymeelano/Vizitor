package ir.g1z4.controlpro.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY createdAt")
    fun observe(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices ORDER BY createdAt")
    suspend fun all(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun get(id: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE phoneKey = :key LIMIT 1")
    suspend fun byPhone(key: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ZoneDao {
    @Query("SELECT * FROM zones WHERE deviceId = :id ORDER BY kind, number")
    fun observe(id: String): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones WHERE deviceId = :id AND key = :key")
    suspend fun one(id: String, key: String): ZoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<ZoneEntity>)

    @Query("DELETE FROM zones WHERE deviceId = :id")
    suspend fun clear(id: String)
}

@Dao
interface OutputDao {
    @Query("SELECT * FROM outputs WHERE deviceId = :id ORDER BY number")
    fun observe(id: String): Flow<List<OutputEntity>>

    @Query("SELECT * FROM outputs WHERE deviceId = :id ORDER BY number")
    suspend fun list(id: String): List<OutputEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<OutputEntity>)

    @Query("DELETE FROM outputs WHERE deviceId = :id")
    suspend fun clear(id: String)
}

@Dao
interface RemoteDao {
    @Query("SELECT * FROM remotes WHERE deviceId = :id ORDER BY slot")
    fun observe(id: String): Flow<List<RemoteEntity>>

    @Query("SELECT * FROM remotes WHERE deviceId = :id ORDER BY slot")
    suspend fun list(id: String): List<RemoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<RemoteEntity>)

    @Query("DELETE FROM remotes WHERE deviceId = :id")
    suspend fun clear(id: String)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE deviceId = :id ORDER BY at DESC LIMIT 400")
    fun observe(id: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY at DESC LIMIT 400")
    fun observeAll(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EventEntity)

    @Query("SELECT COUNT(*) FROM events WHERE deviceId = :id AND rawMasked = :raw AND ABS(at - :at) < 30000")
    suspend fun duplicates(id: String, raw: String, at: Long): Int

    @Query("DELETE FROM events WHERE deviceId = :id")
    suspend fun clear(id: String)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY at DESC LIMIT 300")
    fun observe(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AlertEntity)

    @Query("UPDATE alerts SET read = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE alerts SET read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM alerts WHERE deviceId = :id")
    suspend fun clearDevice(id: String)

    @Query("DELETE FROM alerts")
    suspend fun clear()
}

@Dao
interface CommandDao {
    @Query("SELECT * FROM commands WHERE deviceId = :id ORDER BY COALESCE(sentAt, 0) DESC LIMIT 100")
    fun observe(id: String): Flow<List<CommandEntity>>

    @Query("SELECT * FROM commands WHERE deviceId = :id AND phase IN ('QUEUED','SENDING','WAITING') ORDER BY COALESCE(sentAt, 0) DESC LIMIT 1")
    suspend fun waiting(id: String): CommandEntity?

    @Query("SELECT * FROM commands WHERE phase IN ('QUEUED','SENDING','WAITING')")
    suspend fun allWaiting(): List<CommandEntity>

    @Query("SELECT * FROM commands WHERE id = :id")
    suspend fun byId(id: String): CommandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommandEntity)

    @Query("DELETE FROM commands WHERE deviceId = :id")
    suspend fun clear(id: String)
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots WHERE deviceId = :id")
    fun observe(id: String): Flow<SnapshotEntity?>

    @Query("SELECT * FROM snapshots WHERE deviceId = :id")
    suspend fun get(id: String): SnapshotEntity?

    @Query("SELECT * FROM snapshots")
    suspend fun all(): List<SnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SnapshotEntity)

    @Query("DELETE FROM snapshots WHERE deviceId = :id")
    suspend fun delete(id: String)
}
