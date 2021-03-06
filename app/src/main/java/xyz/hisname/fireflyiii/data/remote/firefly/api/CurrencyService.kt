package xyz.hisname.fireflyiii.data.remote.firefly.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import xyz.hisname.fireflyiii.Constants.Companion.CURRENCY_API_ENDPOINT
import xyz.hisname.fireflyiii.repository.models.currency.CurrencyModel
import xyz.hisname.fireflyiii.repository.models.currency.CurrencySuccessModel
import xyz.hisname.fireflyiii.repository.models.currency.DefaultCurrencyModel
import xyz.hisname.fireflyiii.repository.models.transaction.TransactionModel

// Link to relevant doc: https://firefly-iii.readthedocs.io/en/latest/api/currency.html
interface CurrencyService {

    @GET(CURRENCY_API_ENDPOINT)
    suspend fun getPaginatedCurrency(@Query("page") page: Int): Response<CurrencyModel>

    @GET("$CURRENCY_API_ENDPOINT/default")
    suspend fun getDefaultCurrency(): Response<DefaultCurrencyModel>

    @FormUrlEncoded
    @POST(CURRENCY_API_ENDPOINT)
    suspend fun addCurrency(@Field("name") name: String,
                       @Field("code") code: String,
                       @Field("symbol") symbol: String,
                       @Field("decimal_places") decimalPlaces: String,
                       @Field("enabled") enabled: Boolean,
                       @Field("default") default: Boolean): Response<CurrencySuccessModel>

    @FormUrlEncoded
    @PUT("$CURRENCY_API_ENDPOINT/{currencyCode}")
    suspend fun updateCurrency(@Path("currencyCode") currencyCode: String,
                       @Field("name") name: String,
                       @Field("code") code: String,
                       @Field("symbol") symbol: String,
                       @Field("decimal_places") decimalPlaces: String,
                       @Field("enabled") enabled: Boolean,
                       @Field("default") default: Boolean): Response<CurrencySuccessModel>


    @DELETE("$CURRENCY_API_ENDPOINT/{currencyCode}")
    suspend fun deleteCurrencyByCode(@Path("currencyCode") currencyCode: String): Response<CurrencyModel>

    @GET("$CURRENCY_API_ENDPOINT/{currencyCode}")
    suspend fun getCurrencyByCode(@Query("currencyCode") currencyCode: String): Response<CurrencyModel>

    @GET("$CURRENCY_API_ENDPOINT/{code}")
    suspend fun getTransactionByCurrencyCode(@Path("code") currencyCode: String,
                                             @Query("page") page: Int,
                                             @Query("start_date") startDate: String,
                                             @Query("end_date") endDate: String,
                                             @Query("type") transactionType: String): Response<TransactionModel>

}