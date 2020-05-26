package au.gov.health.covidsafe.streetpass.persistence

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "record_table")
class StreetPassRecord(
        @ColumnInfo(name = "v")
        var v: Int,

        @ColumnInfo(name = "org")
        var org: String,

        @ColumnInfo(name = "localBlob")
        val localBlob: String,

        @ColumnInfo(name = "remoteBlob")
        val remoteBlob: String

) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Long = System.currentTimeMillis()

    override fun toString(): String {
        return "StreetPassRecord(v=$v, , org='$org', id=$id, timestamp=$timestamp,localBlob=$localBlob, remoteBlob=$remoteBlob)"
    }

}
