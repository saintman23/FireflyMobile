package xyz.hisname.fireflyiii.repository.models.budget

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "budget")
data class BudgetData(
        @Embedded
        @Json(name ="attributes")
        val budgetAttributes: BudgetAttributes,
        @PrimaryKey(autoGenerate = false)
        @Json(name ="id")
        val budgetId: Long
)