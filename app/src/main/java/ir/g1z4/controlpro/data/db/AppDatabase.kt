package ir.g1z4.controlpro.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DeviceEntity::class,
        ZoneEntity::class,
        OutputEntity::class,
        RemoteEntity::class,
        EventEntity::class,
        AlertEntity::class,
        CommandEntity::class,
        SnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun devices(): DeviceDao
    abstract fun zones(): ZoneDao
    abstract fun outputs(): OutputDao
    abstract fun remotes(): RemoteDao
    abstract fun events(): EventDao
    abstract fun alerts(): AlertDao
    abstract fun commands(): CommandDao
    abstract fun snapshots(): SnapshotDao
}
