package xyz.hisname.fireflyiii.ui.account.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.commit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.details_card.*
import kotlinx.android.synthetic.main.fragment_account_detail.*
import kotlinx.android.synthetic.main.fragment_account_detail.attachmentRecyclerView
import kotlinx.android.synthetic.main.fragment_account_detail.notesCard
import kotlinx.android.synthetic.main.fragment_markdown.*
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.models.attachment.AttachmentData
import xyz.hisname.fireflyiii.repository.models.transaction.Transactions
import xyz.hisname.fireflyiii.ui.ProgressBar
import xyz.hisname.fireflyiii.ui.account.AddAccountFragment
import xyz.hisname.fireflyiii.ui.base.AttachmentRecyclerAdapter
import xyz.hisname.fireflyiii.ui.base.BaseDetailFragment
import xyz.hisname.fireflyiii.ui.base.BaseDetailRecyclerAdapter
import xyz.hisname.fireflyiii.ui.transaction.TransactionSeparatorAdapter
import xyz.hisname.fireflyiii.ui.transaction.details.TransactionDetailsFragment
import xyz.hisname.fireflyiii.util.DateTimeUtil
import xyz.hisname.fireflyiii.util.extension.*
import xyz.hisname.fireflyiii.util.openFile
import java.util.ArrayList

class AccountDetailFragment: BaseDetailFragment() {

    private val accountId: Long by lazy { arguments?.getLong("accountId") as Long  }
    private val accountType  by lazy { arguments?.getString("accountType")  }
    private val transactionAdapter by lazy { TransactionSeparatorAdapter{ data -> itemClicked(data) } }
    private val accountDetailViewModel by lazy { getImprovedViewModel(AccountDetailViewModel::class.java) }
    private var attachmentDataAdapter = arrayListOf<AttachmentData>()

    private val coloring = arrayListOf<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_account_detail, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (col in ColorTemplate.COLORFUL_COLORS) {
            coloring.add(col)
        }
        for (col in ColorTemplate.JOYFUL_COLORS){
            coloring.add(col)
        }
        accountDetailViewModel.getAccountById(accountId).observe(viewLifecycleOwner){ accountData ->
            setAccountData()
            setExpensesByCategory()
            setExpensesByBudget()
            setIncomeByCategory()
            getAccountTransaction()
        }
        accountDetailViewModel.isLoading.observe(viewLifecycleOwner){ isLoading ->
            if(isLoading == true){
                ProgressBar.animateView(progressLayout, View.VISIBLE, 0.4f, 200)
            } else {
                ProgressBar.animateView(progressLayout, View.GONE, 0f, 200)
            }
        }

        setDarkMode()
    }

    private fun setAccountData(){
        accountDetailViewModel.accountData.observe(viewLifecycleOwner){ list ->
            detailsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            detailsRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            detailsRecyclerView.adapter = BaseDetailRecyclerAdapter(list){ }
            downloadAttachment()
        }
        accountDetailViewModel.notes.observe(viewLifecycleOwner){ notes ->
            if(notes.isEmpty()){
                notesCard.isGone = true
            } else {
                displayText.text = notes.toMarkDown()
            }
        }
        balanceHistoryCardText.text = resources.getString(R.string.account_chart_description,
                accountDetailViewModel.accountName, DateTimeUtil.getStartOfMonth(), DateTimeUtil.getEndOfMonth())
    }

    private fun downloadAttachment(){
        accountDetailViewModel.accountAttachment.observe(viewLifecycleOwner) { attachment ->
            if (attachment.isNotEmpty()) {
                attachmentDataAdapter = ArrayList(attachment)
                attachmentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                attachmentRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                attachmentRecyclerView.adapter = AttachmentRecyclerAdapter(attachmentDataAdapter,
                        true, { data: AttachmentData ->
                    setDownloadClickListener(data, attachmentDataAdapter)
                }) { another: Int -> }
            }
        }
    }

    private fun setDownloadClickListener(attachmentData: AttachmentData, attachmentAdapter: ArrayList<AttachmentData>){
        accountDetailViewModel.downloadAttachment(attachmentData).observe(viewLifecycleOwner) { downloadedFile ->
            // "Refresh" the icon. From downloading to open file
            attachmentRecyclerView.adapter = AttachmentRecyclerAdapter(attachmentAdapter,
                    true, { data: AttachmentData ->
                setDownloadClickListener(data, attachmentDataAdapter)
            }){ another: Int -> }
            startActivity(requireContext().openFile(downloadedFile))
        }
    }

    private fun setExpensesByCategory(){
        accountDetailViewModel.uniqueExpensesCategoryLiveData.observe(viewLifecycleOwner){ categorySumList ->
            if(categorySumList.isEmpty()){
                categoryPieChart.setNoDataText(resources.getString(R.string.no_data_to_generate_chart))
            } else {
                val pieEntryArray = arrayListOf<PieEntry>()
                categorySumList.forEach { categorySum ->
                    pieEntryArray.add(PieEntry(categorySum.first, categorySum.second, categorySum.third))
                }
                val pieDataSet = PieDataSet(pieEntryArray, "").apply {
                    colors = coloring
                    valueTextSize = 15f
                    valueFormatter = PercentFormatter(categoryPieChart)
                }
                categoryPieChart.description.isEnabled = false
                categoryPieChart.invalidate()
                categoryPieChart.data = PieData(pieDataSet)
                categoryPieChart.setData {
                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(entry: Entry?, h: Highlight?) {
                            val pe = entry as PieEntry
                            val entryLabel = if(entry.label.isBlank()){
                                getString(R.string.expenses_without_category)
                            } else {
                                entry.label
                            }
                            toastInfo(entryLabel + ": " + accountDetailViewModel.currencySymbol + entry.data)
                        }

                        override fun onNothingSelected() {}

                    })
                }
            }
        }
    }

    private fun setExpensesByBudget(){
        accountDetailViewModel.uniqueBudgetLiveData.observe(viewLifecycleOwner){ budgetSumList ->
            if(budgetSumList.isEmpty()){
                budgetPieChart.setNoDataText(resources.getString(R.string.no_data_to_generate_chart))
            } else {
                val pieEntryArray = arrayListOf<PieEntry>()
                budgetSumList.forEach { budgetSum ->
                    pieEntryArray.add(PieEntry(budgetSum.first, budgetSum.second, budgetSum.third))
                }
                val pieDataSet = PieDataSet(pieEntryArray, "").apply {
                    colors = coloring
                    valueTextSize = 15f
                    valueFormatter = PercentFormatter(budgetPieChart)
                }
                budgetPieChart.description.isEnabled = false
                budgetPieChart.invalidate()
                budgetPieChart.data = PieData(pieDataSet)
                budgetPieChart.setData {
                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(entry: Entry?, h: Highlight?) {
                            val pe = entry as PieEntry
                            val entryLabel = if (entry.label.isBlank()) {
                                getString(R.string.expenses_without_budget)
                            } else {
                                entry.label
                            }
                            toastInfo(entryLabel + ": " + accountDetailViewModel.currencySymbol + entry.data)
                        }

                        override fun onNothingSelected() {}
                    })
                }
            }
        }
    }

    private fun setDarkMode(){
        if(isDarkMode()){
            categoryPieChart.legend.textColor = getCompatColor(R.color.md_white_1000)
            budgetPieChart.legend.textColor = getCompatColor(R.color.md_white_1000)
        }
    }

    private fun setIncomeByCategory(){
        accountDetailViewModel.uniqueIncomeCategoryLiveData.observe(viewLifecycleOwner) { categorySumList ->
            if (categorySumList.isEmpty()) {
                incomePieChart.setNoDataText(resources.getString(R.string.no_data_to_generate_chart))
            } else {
                val pieEntryArray = arrayListOf<PieEntry>()
                categorySumList.forEach { categorySum ->
                    pieEntryArray.add(PieEntry(categorySum.first, categorySum.second, categorySum.third))
                }
                val pieDataSet = PieDataSet(pieEntryArray, "").apply {
                    colors = coloring
                    valueTextSize = 15f
                    valueFormatter = PercentFormatter(incomePieChart)
                }
                incomePieChart.description.isEnabled = false
                incomePieChart.invalidate()
                incomePieChart.data = PieData(pieDataSet)
                incomePieChart.setData {
                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(entry: Entry?, h: Highlight?) {
                            val pe = entry as PieEntry
                            val entryLabel = if (entry.label.isBlank()) {
                                getString(R.string.income_without_category)
                            } else {
                                entry.label
                            }
                            toastInfo(entryLabel + ": " + accountDetailViewModel.currencySymbol + entry.data)
                        }

                        override fun onNothingSelected() {}

                    })
                }
            }
        }
    }

    private fun getAccountTransaction(){
        accountTransactionList.layoutManager = LinearLayoutManager(requireContext())
        accountTransactionList.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        accountTransactionList.adapter = transactionAdapter
        accountDetailViewModel.getTransactionList(accountId).observe(viewLifecycleOwner){ list ->
            transactionAdapter.submitData(lifecycle, list)
        }
    }

    private fun itemClicked(data: Transactions){
        parentFragmentManager.commit {
            replace(R.id.fragment_container, TransactionDetailsFragment().apply {
                arguments = bundleOf("transactionId" to data.transaction_journal_id)
            })
            addToBackStack(null)
        }
    }

    override fun deleteItem() {
        AlertDialog.Builder(requireContext())
                .setTitle(resources.getString(R.string.delete_account_title, accountDetailViewModel.accountName))
                .setMessage(resources.getString(R.string.delete_account_message, accountDetailViewModel.accountName))
                .setPositiveButton(R.string.delete_permanently) { _, _ ->
                    accountDetailViewModel.deleteAccountById(accountId).observe(viewLifecycleOwner) { isAccountDeleted ->
                        if(isAccountDeleted){
                            parentFragmentManager.popBackStack()
                            when (accountType) {
                                "asset" -> {
                                    toastSuccess(resources.getString(R.string.asset_account_deleted, accountDetailViewModel.accountName))
                                }
                                "expense" -> {
                                    toastSuccess(resources.getString(R.string.expense_account_deleted, accountDetailViewModel.accountName))
                                }
                                "revenue" -> {
                                    toastSuccess(resources.getString(R.string.revenue_account_deleted, accountDetailViewModel.accountName))
                                }
                                else -> {
                                    toastSuccess("Account Deleted")
                                }
                            }
                        }
                    }
                }
                .setNegativeButton(android.R.string.no){dialog, _ ->
                    dialog.dismiss()
                }
                .show()
    }

    override fun editItem() {
        parentFragmentManager.commit {
            replace(R.id.bigger_fragment_container, AddAccountFragment().apply{
                arguments = bundleOf("accountType" to accountType, "accountId" to accountId)
            })
            addToBackStack(null)
        }
    }
}