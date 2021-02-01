/*
 * Copyright (c)  2018 - 2021 Daniel Quah
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.hisname.fireflyiii.data.remote.firefly.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.hisname.fireflyiii.Constants
import xyz.hisname.fireflyiii.Constants.Companion.AVAILABLE_BUDGET_API_ENDPOINT
import xyz.hisname.fireflyiii.Constants.Companion.BUDGET_API_ENDPOINT
import xyz.hisname.fireflyiii.repository.models.autocomplete.BudgetItems
import xyz.hisname.fireflyiii.repository.models.budget.BudgetModel
import xyz.hisname.fireflyiii.repository.models.budget.budgetList.BudgetListModel
import xyz.hisname.fireflyiii.repository.models.budget.limits.BudgetLimitModel
import xyz.hisname.fireflyiii.repository.models.transaction.TransactionModel

interface BudgetService {

    @GET(AVAILABLE_BUDGET_API_ENDPOINT)
    suspend fun getAllBudget(): Response<BudgetModel>

    @GET(AVAILABLE_BUDGET_API_ENDPOINT)
    fun getPaginatedBudget(@Query("page") page: Int): Call<BudgetModel>

    @GET("${Constants.AUTOCOMPLETE_API_ENDPOINT}/budgets")
    suspend fun searchBudget(@Query("query") queryString: String): Response<List<BudgetItems>>

    @GET(AVAILABLE_BUDGET_API_ENDPOINT)
    suspend fun getAvailableBudget(@Query("page") page: Int,
                                   @Query("start") start: String,
                                   @Query("end") end: String): Response<BudgetModel>

    @GET(BUDGET_API_ENDPOINT)
    suspend fun getPaginatedSpentBudget(@Query("page") page: Int, @Query("start") start: String,
                                @Query("end") end: String): Response<BudgetListModel>

    @GET(BUDGET_API_ENDPOINT)
    suspend fun getPaginatedSpentBudget(@Query("page") page: Int): Response<BudgetListModel>

    @GET("$BUDGET_API_ENDPOINT/{id}/limits")
    suspend fun getBudgetLimit(@Path("id") budgetId: Long, @Query("start") start: String,
                               @Query("end") end: String): Response<BudgetLimitModel>

    @GET("$BUDGET_API_ENDPOINT/{id}/transactions")
    suspend fun getPaginatedTransactionByBudget(
            @Path("id") budgetId: Long,
            @Query("page") page: Int,
            @Query("start") start: String,
            @Query("end") end: String,
            @Query("type") transactionType: String): Response<TransactionModel>


}