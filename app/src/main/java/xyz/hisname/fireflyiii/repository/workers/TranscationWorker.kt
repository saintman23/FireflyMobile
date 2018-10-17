package xyz.hisname.fireflyiii.repository.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.google.gson.Gson
import xyz.hisname.fireflyiii.repository.RetrofitBuilder
import xyz.hisname.fireflyiii.repository.api.TransactionService
import xyz.hisname.fireflyiii.repository.models.transaction.ErrorModel
import xyz.hisname.fireflyiii.ui.notifications.NotificationUtils
import xyz.hisname.fireflyiii.util.retrofitCallback

class TranscationWorker(private val context: Context, workerParameters: WorkerParameters): BaseWorker(context, workerParameters) {


    override fun doWork(): Result {
        val notif = NotificationUtils(context)
        val transactionType = inputData.getString("transactionType") ?: ""
        val transactionDescription = inputData.getString("description") ?: ""
        val transactionDate = inputData.getString("date") ?: ""
        val transactionAmount = inputData.getString("amount") ?: ""
        val transactionCurrency = inputData.getString("currency") ?: ""
        val destinationName = inputData.getString("destinationName") ?: ""
        val sourceName = inputData.getString("sourceName") ?: ""
        val piggyBank = inputData.getString("piggyBankName")
        val billName = inputData.getString("billName")
        val transactionService = RetrofitBuilder.getClient(baseUrl, accessToken)?.
                create(TransactionService::class.java)
        transactionService?.addTransaction(transactionType, transactionDescription, transactionDate, piggyBank,
                billName, transactionAmount,sourceName, destinationName, transactionCurrency)?.enqueue(
                retrofitCallback({ response ->
                    var errorBody = ""
                    if (response.errorBody() != null) {
                        errorBody = String(response.errorBody()?.bytes()!!)
                    }
                    val gson = Gson().fromJson(errorBody, ErrorModel::class.java)
                    var error = ""
                    if(response.isSuccessful){
                        notif.showTransactionNotification("Transaction added successfully!", "Transaction")
                        Result.SUCCESS
                    } else {
                        when {
                            gson.errors.transactions_destination_name != null -> {
                                error = if(gson.errors.transactions_destination_name.contains("is required")){
                                    "Destination Account Required"
                                } else {
                                    "Invalid Destination Account"
                                }
                            }
                            gson.errors.transactions_currency != null -> {
                                error = if(gson.errors.transactions_currency.contains("is required")){
                                    "Currency Code Required"
                                } else {
                                    "Invalid Currency Code"
                                }
                            }
                            gson.errors.transactions_source_name != null  -> {
                                error = if(gson.errors.transactions_source_name.contains("is required")){
                                    "Source Account Required"
                                } else {
                                    "Invalid Source Account"
                                }
                            }
                        }
                        notif.showTransactionNotification(error, "Error Adding $transactionType")
                        Result.FAILURE
                    }
                })
                { throwable ->
                    notif.showTransactionNotification(throwable.message.toString(), "Error adding Transaction")
                    Result.FAILURE
                })
        return Result.SUCCESS
    }
}