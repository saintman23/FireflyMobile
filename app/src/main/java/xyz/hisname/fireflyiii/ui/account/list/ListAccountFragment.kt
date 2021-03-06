package xyz.hisname.fireflyiii.ui.account.list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.asLiveData
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import kotlinx.android.synthetic.main.account_list_item.view.*
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.android.synthetic.main.base_swipe_layout.*
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.models.accounts.AccountData
import xyz.hisname.fireflyiii.ui.account.AccountRecyclerAdapter
import xyz.hisname.fireflyiii.ui.account.AddAccountFragment
import xyz.hisname.fireflyiii.ui.account.details.AccountDetailFragment
import xyz.hisname.fireflyiii.ui.base.BaseFragment
import xyz.hisname.fireflyiii.util.extension.*
import java.util.*

class ListAccountFragment: BaseFragment() {

    private val accountType by lazy { arguments?.getString("accountType") ?: "" }
    private val noAccountImage by lazy { requireActivity().findViewById<ImageView>(R.id.listImage) }
    private val noAccountText by lazy { requireActivity().findViewById<TextView>(R.id.listText) }
    private val accountVm by lazy { getImprovedViewModel(ListAccountViewModel::class.java) }
    private val accountAdapter by lazy { AccountRecyclerAdapter { data: AccountData -> itemClicked(data) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_base_list, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerView()
        displayView()
        pullToRefresh()
        initFab()
        enableDragAndDrop()
    }

    private fun enableDragAndDrop(){
        recycler_view.enableDragDrop(extendedFab) { viewHolder, isCurrentlyActive ->
            if (viewHolder.itemView.accountList.isOverlapping(extendedFab)){
                extendedFab.dropToRemove()
                if(!isCurrentlyActive){
                    val accountName = viewHolder.itemView.accountNameText.text.toString()
                    val accountId = viewHolder.itemView.accountId.text.toString()
                    accountVm.deleteAccountByName(accountId).observe(viewLifecycleOwner){ isDeleted ->
                        accountAdapter.refresh()
                        if(isDeleted){
                            when (accountType){
                                "asset" -> toastSuccess(resources.getString(R.string.asset_account_deleted, accountName))
                                "expense" -> toastSuccess(resources.getString(R.string.expense_account_deleted, accountName))
                                "revenue" -> toastSuccess(resources.getString(R.string.revenue_account_deleted, accountName))
                                "liability" -> toastSuccess(resources.getString(R.string.liability_account_deleted, accountName))
                            }
                        } else {
                            toastOffline(resources.getString(R.string.data_will_be_deleted_later, accountName), Toast.LENGTH_LONG)
                        }
                    }
                }
            }
        }
    }

    private fun displayView(){
        accountVm.getAccountList(accountType).observe(viewLifecycleOwner){ pagingData ->
            accountAdapter.submitData(lifecycle, pagingData)
        }
    }


    private fun setRecyclerView(){
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recycler_view.adapter = accountAdapter
        accountAdapter.loadStateFlow.asLiveData().observe(viewLifecycleOwner){ loadStates ->
            swipeContainer.isRefreshing = loadStates.refresh is LoadState.Loading
            if(loadStates.refresh !is LoadState.Loading) {
                if(accountAdapter.itemCount < 1){
                    recycler_view.isGone = true
                    noAccountImage.isVisible = true
                    noAccountText.isVisible = true
                    when (accountType) {
                        "asset" -> {
                            noAccountImage.setImageDrawable(IconicsDrawable(requireContext(), FontAwesome.Icon.faw_money_bill))
                            noAccountText.text = resources.getString(R.string.no_account_found, resources.getString(R.string.asset_account))
                        }
                        "expense" -> {
                            noAccountImage.setImageDrawable(IconicsDrawable(requireContext(), FontAwesome.Icon.faw_shopping_cart))
                            noAccountText.text = resources.getString(R.string.no_account_found, resources.getString(R.string.expense_account))
                        }
                        "revenue" -> {
                            noAccountImage.setImageDrawable(IconicsDrawable(requireContext(), FontAwesome.Icon.faw_download))
                            noAccountText.text = resources.getString(R.string.no_account_found, resources.getString(R.string.revenue_account))
                        }
                        "liabilities" -> {
                            noAccountImage.setImageDrawable(IconicsDrawable(requireContext(), FontAwesome.Icon.faw_ticket_alt))
                            noAccountText.text = resources.getString(R.string.no_account_found, resources.getString(R.string.liability_account))
                        }
                    }
                } else {
                    recycler_view.isVisible = true
                    noAccountImage.isGone = true
                    noAccountText.isGone = true
                }
            }
        }
    }

    private fun itemClicked(data: AccountData){
        val bundle = bundleOf("accountId" to data.accountId, "accountType" to accountType)
        parentFragmentManager.commit {
            addToBackStack(null)
            replace(R.id.fragment_container, AccountDetailFragment().apply { arguments = bundle })
        }
    }

    private fun pullToRefresh(){
        swipeContainer.setOnRefreshListener {
            displayView()
        }
    }

    private fun initFab(){
        extendedFab.display {
            extendedFab.isClickable = false
            parentFragmentManager.commit {
                replace(R.id.bigger_fragment_container, AddAccountFragment().apply{
                    arguments = bundleOf("revealX" to extendedFab.width / 2, "revealY" to extendedFab.height / 2, "accountType" to accountType)
                })
                addToBackStack(null)
            }
            extendedFab.isClickable = true
        }
    }

    private fun convertString(): String{
        return when {
            Objects.equals(accountType, "asset") -> resources.getString(R.string.asset_account)
            Objects.equals(accountType, "expense") -> resources.getString(R.string.expense_account)
            Objects.equals(accountType, "revenue") -> resources.getString(R.string.revenue_account)
            Objects.equals(accountType, "liabilities") -> resources.getString(R.string.liability_account)
            else -> "Accounts"
        }
    }

    override fun onAttach(context: Context){
        super.onAttach(context)
        activity?.activity_toolbar?.title = convertString()
    }

    override fun onResume() {
        super.onResume()
        activity?.activity_toolbar?.title = convertString()
    }

}