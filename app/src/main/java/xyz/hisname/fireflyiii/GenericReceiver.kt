package xyz.hisname.fireflyiii

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat.startActivity
import androidx.work.*
import xyz.hisname.fireflyiii.repository.workers.BillWorker
import xyz.hisname.fireflyiii.repository.workers.PiggyBankWorker
import xyz.hisname.fireflyiii.repository.workers.TranscationWorker
import xyz.hisname.fireflyiii.ui.HomeActivity
import xyz.hisname.fireflyiii.ui.notifications.NotificationUtils

class GenericReceiver: BroadcastReceiver(){

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPref: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
        val baseUrl: String by lazy { sharedPref.getString("fireflyUrl", "") }
        val accessToken: String by lazy { sharedPref.getString("access_token","") }
        if(baseUrl.isBlank() || accessToken.isBlank()){
            val notif = NotificationUtils(context)
            notif.showNotSignedIn()
        } else {
            when {
                intent.action == "firefly.hisname.ADD_PIGGY_BANK" -> {
                    val piggyData = Data.Builder()
                            .putString("name", intent.getStringExtra("name"))
                            .putString("accountId", intent.getStringExtra("accountId"))
                            .putString("targetAmount", intent.getStringExtra("targetAmount"))
                            .putString("currentAmount", intent.getStringExtra("currentAmount"))
                            .putString("startDate", intent.getStringExtra("startDate"))
                            .putString("endDate", intent.getStringExtra("endDate"))
                            .putString("notes", intent.getStringExtra("notes"))
                            .build()
                    val piggybankWork = OneTimeWorkRequest.Builder(PiggyBankWorker::class.java)
                            .setInputData(piggyData)
                            .setConstraints(Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build())
                            .build()
                    WorkManager.getInstance().enqueue(piggybankWork)
                }
                intent.action == "firefly.hisname.ADD_BILL" -> {
                    val billData = Data.Builder()
                            .putString("name", intent.getStringExtra("name"))
                            .putString("billMatch", intent.getStringExtra("billMatch"))
                            .putString("minAmount", intent.getStringExtra("minAmount"))
                            .putString("maxAmount", intent.getStringExtra("maxAmount"))
                            .putString("billDate", intent.getStringExtra("billDate"))
                            .putString("repeatFreq", intent.getStringExtra("repeatFreq"))
                            .putString("skip", intent.getStringExtra("skip"))
                            .putString("currencyCode", intent.getStringExtra("currencyCode"))
                            .putString("notes", intent.getStringExtra("notes"))
                            .build()
                    val billWork = OneTimeWorkRequest.Builder(BillWorker::class.java)
                            .setInputData(billData)
                            .setConstraints(Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build())
                            .build()
                    WorkManager.getInstance().enqueue(billWork)
                }
                intent.action == "firefly.hisname.ADD_DEPOSIT" -> {
                    val depositData = Data.Builder()
                            .putString("description", intent.getStringExtra("description"))
                            .putString("date", intent.getStringExtra("date"))
                            .putString("amount", intent.getStringExtra("amount"))
                            .putString("currency", intent.getStringExtra("currency"))
                            .putString("destinationName", intent.getStringExtra("destinationName"))
                            .putString("billName", intent.getStringExtra("billName"))
                    transactionWork(depositData, "deposit")
                }
                intent.action == "firefly.hisname.ADD_WITHDRAW" -> {
                    val withdrawData = Data.Builder()
                            .putString("description", intent.getStringExtra("description"))
                            .putString("date", intent.getStringExtra("date"))
                            .putString("amount", intent.getStringExtra("amount"))
                            .putString("currency", intent.getStringExtra("currency"))
                            .putString("sourceName", intent.getStringExtra("sourceName"))
                    transactionWork(withdrawData, "withdrawal")
                }
                intent.action == "firefly.hisname.ADD_TRANSFER" -> {
                    val transferData = Data.Builder()
                            .putString("description", intent.getStringExtra("description"))
                            .putString("date", intent.getStringExtra("date"))
                            .putString("amount", intent.getStringExtra("amount"))
                            .putString("currency", intent.getStringExtra("currency"))
                            .putString("sourceName", intent.getStringExtra("sourceName"))
                            .putString("destinationName", intent.getStringExtra("destinationName"))
                            .putString("piggyBankName", intent.getStringExtra("piggyBankName"))
                    transactionWork(transferData, "transfer")
                }
            }
        }
        val action = intent.getStringExtra("transaction_notif")
        if(action != null){
            // close the notification tray
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            val intent = Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            when (action) {
                "expense" -> intent.putExtra("transaction", "expense")
                "income" -> intent.putExtra("transaction", "income")
                "transfer" -> intent.putExtra("transaction", "transfer")
            }
            context.startActivity(intent)
        }

    }

    private fun transactionWork(data: Data.Builder, type: String){
        val transactionWork = OneTimeWorkRequest.Builder(TranscationWorker::class.java)
                .setInputData(data.putString("transactionType" ,type).build())
                .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build()
        WorkManager.getInstance().enqueue(transactionWork)
    }

}