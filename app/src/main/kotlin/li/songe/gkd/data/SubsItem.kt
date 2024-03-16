package li.songe.gkd.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import li.songe.gkd.db.DbSet
import li.songe.gkd.util.deleteSubscription
import li.songe.gkd.util.isSafeUrl

@Entity(
    tableName = "subs_item",
)
data class SubsItem(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "ctime") val ctime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "mtime") val mtime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "enable") val enable: Boolean = true,
    @ColumnInfo(name = "enable_update") val enableUpdate: Boolean = true,
    @ColumnInfo(name = "order") val order: Int,
    @ColumnInfo(name = "update_url") val updateUrl: String? = null,

    ) {

    val isSafeRemote by lazy {
        if (updateUrl != null) {
            isSafeUrl(updateUrl)
        } else {
            false
        }
    }

    suspend fun removeAssets() {
        deleteSubscription(id)
        DbSet.subsItemDao.delete(this)
        DbSet.subsConfigDao.delete(id)
        DbSet.clickLogDao.deleteBySubsId(id)
        DbSet.categoryConfigDao.deleteBySubsItemId(id)
    }

    @Dao
    interface SubsItemDao {
        @Update
        suspend fun update(vararg objects: SubsItem): Int

        @Query("UPDATE subs_item SET enable=:enable WHERE id=:id")
        suspend fun updateEnable(id: Long, enable: Boolean): Int

        @Query("UPDATE subs_item SET `order`=:order WHERE id=:id")
        suspend fun updateOrder(id: Long, order: Int): Int

        @Transaction
        suspend fun batchUpdateOrder(subsItems: List<SubsItem>) {
            subsItems.forEach { subsItem ->
                updateOrder(subsItem.id, subsItem.order)
            }
        }

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insert(vararg users: SubsItem): List<Long>

        @Delete
        suspend fun delete(vararg users: SubsItem): Int

        @Query("UPDATE subs_item SET mtime=:mtime WHERE id=:id")
        suspend fun updateMtime(id: Long, mtime: Long = System.currentTimeMillis()): Int

        @Query("SELECT * FROM subs_item ORDER BY `order`")
        fun query(): Flow<List<SubsItem>>

        @Query("SELECT * FROM subs_item WHERE id=:id")
        fun queryById(id: Long): Flow<SubsItem?>
    }

}
