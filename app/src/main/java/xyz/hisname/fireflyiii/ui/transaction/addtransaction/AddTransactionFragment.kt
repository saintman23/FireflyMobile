package xyz.hisname.fireflyiii.ui.transaction.addtransaction

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.commit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.hootsuite.nachos.ChipConfiguration
import com.hootsuite.nachos.chip.ChipCreator
import com.hootsuite.nachos.chip.ChipSpan
import com.hootsuite.nachos.terminator.ChipTerminatorHandler
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer
import com.mikepenz.iconics.IconicsColor.Companion.colorList
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.*
import kotlinx.android.synthetic.main.fragment_add_transaction.*
import kotlinx.android.synthetic.main.fragment_add_transaction.currency_edittext
import kotlinx.android.synthetic.main.fragment_add_transaction.description_edittext
import kotlinx.android.synthetic.main.fragment_add_transaction.expansionLayout
import kotlinx.android.synthetic.main.fragment_add_transaction.placeHolderToolbar
import me.toptas.fancyshowcase.FancyShowCaseQueue
import net.dinglisch.android.tasker.TaskerPlugin
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.repository.MarkdownViewModel
import xyz.hisname.fireflyiii.repository.models.attachment.AttachmentData
import xyz.hisname.fireflyiii.repository.models.attachment.Attributes
import xyz.hisname.fireflyiii.ui.ProgressBar
import xyz.hisname.fireflyiii.ui.base.BaseFragment
import xyz.hisname.fireflyiii.ui.budget.BudgetSearchDialog
import xyz.hisname.fireflyiii.ui.budget.BudgetSearchViewModel
import xyz.hisname.fireflyiii.ui.categories.CategoriesDialog
import xyz.hisname.fireflyiii.ui.categories.CategoriesDialogViewModel
import xyz.hisname.fireflyiii.ui.currency.CurrencyBottomSheetViewModel
import xyz.hisname.fireflyiii.ui.currency.CurrencyListBottomSheet
import xyz.hisname.fireflyiii.ui.markdown.MarkdownFragment
import xyz.hisname.fireflyiii.ui.piggybank.SearchPiggyDialog
import xyz.hisname.fireflyiii.ui.piggybank.SearchPiggyViewModel
import xyz.hisname.fireflyiii.ui.transaction.details.TransactionAttachmentRecyclerAdapter
import xyz.hisname.fireflyiii.util.DateTimeUtil
import xyz.hisname.fireflyiii.util.FileUtils
import xyz.hisname.fireflyiii.util.extension.*
import java.io.File
import java.util.*

// This code sucks :(
class AddTransactionFragment: BaseFragment() {

    private val transactionJournalId by lazy { arguments?.getLong("transactionJournalId") ?: 0 }
    private val transactionActivity by lazy { arguments?.getBoolean("FROM_TRANSACTION_ACTIVITY") }

    private val addTransactionViewModel by lazy { getImprovedViewModel(AddTransactionViewModel::class.java) }
    private val currencyViewModel by lazy { getViewModel(CurrencyBottomSheetViewModel::class.java) }
    private val markdownViewModel by lazy { getViewModel(MarkdownViewModel::class.java) }
    private val budgetSearch by lazy { getViewModel(BudgetSearchViewModel::class.java) }
    private val piggySearch by lazy { getViewModel(SearchPiggyViewModel::class.java) }
    private val categorySearch by lazy { getViewModel(CategoriesDialogViewModel::class.java) }

    private val isTasker by lazy { arguments?.getBoolean("isTasker") ?: false }
    private val isFromNotification by lazy { requireActivity().intent.extras?.getBoolean("isFromNotification") ?: false }
    private val isFromFragment by lazy { arguments?.getBoolean("SHOULD_HIDE") ?: false }
    private val transactionType by lazy { arguments?.getString("transactionType") ?: "" }

    private lateinit var fileUri: Uri
    private var selectedTime = ""
    private lateinit var takePicture: ActivityResultLauncher<Uri>
    private lateinit var chooseDocument: ActivityResultLauncher<Array<String>>
    private val attachmentDataAdapter by lazy { arrayListOf<AttachmentData>() }
    private val attachmentItemAdapter by lazy { arrayListOf<Uri>() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.create(R.layout.fragment_add_transaction, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ProgressBar.animateView(progressLayout, View.VISIBLE, 0.4f, 200)
        if(isTasker){
            // Disable file upload for now
            add_attachment_button.isVisible = false
            addTransactionViewModel.transactionBundle.observe(viewLifecycleOwner) { bundle ->
                addTransactionViewModel.parseBundle(bundle)
            }
            setTaskerIcons()
            addTransactionViewModel.isFromTasker.postValue(true)
        }
        if(isFromNotification){
            requireActivity().intent.extras?.let {
                addTransactionViewModel.parseBundle(it)
            }
        }
        setIcons()
        setWidgets()
        if(transactionJournalId != 0L){
            addTransactionViewModel.getTransactionFromJournalId(transactionJournalId)
        }
        setFab()
        setCalculator()
        contextSwitch()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                attachment_information.isVisible = true
                attachmentDataAdapter.add(AttachmentData(Attributes(0, "",
                        "", "", FileUtils.getFileName(requireContext(), fileUri) ?: "",
                        "", "", "", 0, "", "", ""), 0, ""))
                attachmentItemAdapter.add(fileUri)
                attachment_information.adapter?.notifyDataSetChanged()
            }
        }
        chooseDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()){ fileChoosen ->
            attachment_information.isVisible = true
            if(fileChoosen != null){
                attachmentDataAdapter.add(AttachmentData(Attributes(0, "",
                        "", "", FileUtils.getFileName(requireContext(), fileChoosen) ?: "",
                        "", "", "", 0, "", "", ""), 0, ""))
                attachmentItemAdapter.add(fileChoosen)
                attachment_information.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun setTaskerImageDrawable(vararg view: ImageView){
        view.forEach { image ->
            image.setImageDrawable(
                    IconicsDrawable(requireContext()).apply {
                        icon(GoogleMaterial.Icon.gmd_label)
                        colorInt = getCompatColor(R.color.colorAccent)
                    })
        }
    }

    private fun setTaskerText(imageView: ImageView, editText: EditText){
        val variablesFromHost = TaskerPlugin.getRelevantVariableList(requireActivity().intent.extras)
        val arrayAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_item)
        variablesFromHost.forEach { variables ->
            arrayAdapter.add(variables)
        }
        val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Select Variable")
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
        imageView.setOnClickListener {
            dialog.setAdapter(arrayAdapter){ _, which ->
                val itemClicked = arrayAdapter.getItem(which)
                editText.setText(itemClicked)
                if(editText == currency_edittext){
                    // Yeah... we need to figure out how to make `currency` a non-global variable
                    addTransactionViewModel.currency = itemClicked ?: ""
                }
            }
            dialog.show()

        }
    }

    private fun setTaskerIcons(){
        setTaskerImageDrawable(descriptionTasker, transactionAmountTasker,
                currencyTasker, dateTasker, sourceTextInputTasker, sourceExposedTasker,
                destinationTextInputTasker, destinationExposedTasker, timeTasker, piggyTasker,
                categoryTasker, tagsTasker, budgetTasker, noteTasker)
        setTaskerText(descriptionTasker, description_edittext)
        setTaskerText(transactionAmountTasker, transaction_amount_edittext)
        setTaskerText(dateTasker, transaction_date_edittext)
        setTaskerText(sourceTextInputTasker, source_edittext)
        setTaskerText(sourceExposedTasker, source_exposed_dropdown)
        setTaskerText(destinationTextInputTasker, destination_edittext)
        setTaskerText(destinationExposedTasker, destination_exposed_dropdown)
        setTaskerText(timeTasker, time_edittext)
        setTaskerText(piggyTasker, piggy_edittext)
        setTaskerText(categoryTasker, category_edittext)
        setTaskerText(tagsTasker, tags_chip)
        setTaskerText(budgetTasker, budget_edittext)
        setTaskerText(noteTasker, note_edittext)
    }

    private fun setUi() {
        addTransactionViewModel.transactionCurrency.observe(viewLifecycleOwner) { transactionCurrency ->
            currency_edittext.setText(transactionCurrency)
        }

        addTransactionViewModel.transactionDescription.observe(viewLifecycleOwner) { transactionDescription ->
            description_edittext.setText(transactionDescription)
        }

        addTransactionViewModel.transactionAmount.observe(viewLifecycleOwner) { transactionAmount ->
            transaction_amount_edittext.setText(transactionAmount)
        }

        addTransactionViewModel.transactionDate.observe(viewLifecycleOwner) { transactionDate ->
            transaction_date_edittext.setText(transactionDate)
        }

        addTransactionViewModel.transactionTime.observe(viewLifecycleOwner) { transactionTime ->
            time_edittext.setText(transactionTime)
        }

        if (piggy_layout.isVisible) {
            addTransactionViewModel.transactionPiggyBank.observe(viewLifecycleOwner) { transactionPiggyBank ->
                piggy_edittext.setText(transactionPiggyBank)

            }
        }

        addTransactionViewModel.transactionTags.observe(viewLifecycleOwner) { transactionTags ->
            tags_chip.setText(transactionTags)
        }

        addTransactionViewModel.transactionBudget.observe(viewLifecycleOwner) { transactionBudget ->
            budget_edittext.setText(transactionBudget)
        }

        addTransactionViewModel.transactionCategory.observe(viewLifecycleOwner) { transactionCategory ->
            category_edittext.setText(transactionCategory)
        }

        addTransactionViewModel.transactionNote.observe(viewLifecycleOwner) { transactionNote ->
            note_edittext.setText(transactionNote)
        }
    }

    private fun setFab(){
        if(transactionJournalId != 0L) {
            addTransactionFab.setImageDrawable(IconicsDrawable(requireContext()).icon(GoogleMaterial.Icon.gmd_update))
        }
        addTransactionFab.setOnClickListener {
            // Listen to loading only when FAB is clicked
            addTransactionViewModel.isLoading.observe(viewLifecycleOwner){ loading ->
                if(loading){
                    ProgressBar.animateView(progressLayout, View.VISIBLE, 0.4f, 200)
                } else {
                    ProgressBar.animateView(progressLayout, View.GONE, 0f, 200)
                }
            }
            hideKeyboard()
            val piggyBank = if(piggy_edittext.isBlank()){
                null
            } else {
                piggy_edittext.getString()
            }
            val categoryName = if(category_edittext.isBlank()){
                null
            } else {
                category_edittext.getString()
            }
            val transactionTags = if(tags_chip.allChips.isNullOrEmpty()){
                null
            } else {
                // Remove [ and ] from beginning and end of string
                val beforeTags = tags_chip.allChips.toString().substring(1)
                beforeTags.substring(0, beforeTags.length - 1)
            }
            val budgetName = if(budget_edittext.isBlank()){
                null
            } else {
                budget_edittext.getString()
            }
            var sourceAccount = ""
            var destinationAccount = ""
            when {
                Objects.equals("Withdrawal", transactionType) -> {
                    sourceAccount = source_exposed_dropdown.getString()
                    destinationAccount = destination_edittext.getString()
                }
                Objects.equals("Transfer", transactionType) -> {
                    sourceAccount = source_exposed_dropdown.getString()
                    destinationAccount = destination_exposed_dropdown.getString()
                }
                Objects.equals("Deposit", transactionType) -> {
                    sourceAccount = source_edittext.getString()
                    destinationAccount = destination_exposed_dropdown.getString()
                }
            }
            if(transactionJournalId != 0L){
                updateData(piggyBank, sourceAccount, destinationAccount, categoryName, transactionTags, budgetName)
            } else {
                if(isTasker){
                    taskerPlugin(piggyBank, sourceAccount, destinationAccount, categoryName, transactionTags, budgetName)
                } else {
                    submitData(piggyBank, sourceAccount, destinationAccount, categoryName, transactionTags, budgetName)
                }

            }
        }
    }

    private fun taskerPlugin(piggyBank: String?, sourceAccount: String, destinationAccount: String,
                             categoryName: String?, transactionTags: String?, budgetName: String?){
        val currencyText = currency_edittext.getString()
        if(currencyText.startsWith("%")){
            addTransactionViewModel.currency = currency_edittext.getString()
        } else {
            /* Get content between brackets
             * For example: Euro(EUR) becomes (EUR)
             * Then we remove the first and last character and it becomes EUR
             */
            addTransactionViewModel.currency = currency_edittext.getString()
            val regex = "(?<=\\().+?(?=\\))".toRegex()
            val regexReplaced = regex.find(addTransactionViewModel.currency)
            regexReplaced?.value
            addTransactionViewModel.currency = regexReplaced?.value ?: ""
        }
        addTransactionViewModel.transactionType.postValue(transactionType)
        addTransactionViewModel.transactionDescription.postValue(description_edittext.getString())
        addTransactionViewModel.transactionAmount.postValue(transaction_amount_edittext.getString())
        addTransactionViewModel.transactionDate.postValue(transaction_date_edittext.getString())
        addTransactionViewModel.transactionTime.postValue(time_edittext.getString())
        addTransactionViewModel.transactionPiggyBank.postValue(piggyBank)
        addTransactionViewModel.transactionSourceAccount.postValue(sourceAccount)
        addTransactionViewModel.transactionDestinationAccount.postValue(destinationAccount)
        addTransactionViewModel.transactionCurrency.postValue(addTransactionViewModel.currency)
        addTransactionViewModel.transactionTags.postValue(transactionTags)
        addTransactionViewModel.transactionBudget.postValue(budgetName)
        addTransactionViewModel.transactionCategory.postValue(categoryName)
        addTransactionViewModel.transactionNote.postValue(note_edittext.getString())
        addTransactionViewModel.fileUri.postValue(fileUri.toString())
        addTransactionViewModel.removeFragment.postValue(true)
    }

    private fun setIcons(){
        tags_layout.startIconDrawable = IconicsDrawable(requireContext()).apply{
            icon = FontAwesome.Icon.faw_tags
            colorRes = R.color.md_blue_900
            sizeDp = 24
        }
        currency_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_money_bill
                    colorRes = R.color.md_green_400
                    sizeDp = 24
                },null, null, null)
        transaction_amount_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_calculator
                    colorRes = R.color.md_blue_grey_400
                    sizeDp = 16
                }, null, null, null)
        transaction_date_edittext.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(requireContext()).apply {
            icon = FontAwesome.Icon.faw_calendar
            color = colorList(ColorStateList.valueOf(Color.rgb(18, 122, 190)))
            sizeDp = 24
        },null, null, null)
        source_edittext.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(requireContext()).apply {
            icon = FontAwesome.Icon.faw_exchange_alt
            colorRes = R.color.md_green_500
            sizeDp = 24
        },null, null, null)
        val bankTransferIconColorWrap = DrawableCompat.wrap(getCompatDrawable(R.drawable.ic_bank_transfer)!!).mutate()
        DrawableCompat.setTint(bankTransferIconColorWrap, Color.parseColor("#e67a15"))
        destination_edittext.setCompoundDrawablesWithIntrinsicBounds(
                bankTransferIconColorWrap,null, null, null)
        category_edittext.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(requireContext()).apply{
            icon = FontAwesome.Icon.faw_chart_bar
            colorRes = R.color.md_deep_purple_400
            sizeDp = 24
        }, null, null, null)
        piggy_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply{
                    icon = FontAwesome.Icon.faw_piggy_bank
                    colorRes = R.color.md_pink_200
                    sizeDp = 24
                },null, null, null)
        time_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_clock
                    colorRes = R.color.md_red_400
                    sizeDp = 24
                },null, null, null)
        tags_chip.chipTokenizer = SpanChipTokenizer(requireContext(), object : ChipCreator<ChipSpan> {
            override fun configureChip(chip: ChipSpan, chipConfiguration: ChipConfiguration) {
            }

            override fun createChip(context: Context, text: CharSequence, data: Any?): ChipSpan {
                return ChipSpan(requireContext(), text,
                        IconicsDrawable(requireContext()).apply {
                            icon = FontAwesome.Icon.faw_tag
                            sizeDp = 12
                        }, data)
            }

            override fun createChip(context: Context, existingChip: ChipSpan): ChipSpan {
                return ChipSpan(requireContext(), existingChip)
            }
        }, ChipSpan::class.java)
        addTransactionFab.setImageDrawable(IconicsDrawable(requireContext()).apply{
            icon = FontAwesome.Icon.faw_plus
            colorRes = R.color.md_black_1000
            sizeDp = 24
        })
        budget_edittext.setCompoundDrawablesWithIntrinsicBounds(
                IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_gratipay
                    colorRes = R.color.md_amber_300
                    sizeDp = 24
                },null, null, null)
    }

    private fun setCalculator(){
        transaction_amount_layout.setOnTouchListener { v, event ->
            if(event.action == MotionEvent.ACTION_UP){
                transaction_amount_layout.performClick()
                addTransactionViewModel.transactionAmount.value = if(transaction_amount_edittext.getString().isEmpty()){
                    "0.0"
                } else {
                    transaction_amount_edittext.getString()
                }
                val calculatorDialog = TransactionCalculatorDialog()
                calculatorDialog.show(parentFragmentManager, "calculatorDialog")
            }
            false
        }
        transaction_amount_edittext.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if(event.action == MotionEvent.ACTION_UP){
                    if(event.x <= transaction_amount_edittext.compoundDrawables[0].bounds.width() + 30){
                        addTransactionViewModel.transactionAmount.value = if(transaction_amount_edittext.getString().isEmpty()){
                            "0.0"
                        } else {
                            transaction_amount_edittext.getString()
                        }
                        val calculatorDialog = TransactionCalculatorDialog()
                        calculatorDialog.show(parentFragmentManager, "calculatorDialog")
                        return true
                    }
                }
                return false
            }

        })
        addTransactionViewModel.transactionAmount.observe(viewLifecycleOwner){ amount ->
            transaction_amount_edittext.setText(amount)
        }
    }

    private fun setWidgets(){
        val queue = FancyShowCaseQueue()
        if(transactionActivity == true){
            queue.add(showCase(R.string.transactions_create_switch_box, "bottomNavigationShowCase",
                           requireActivity().findViewById(R.id.transactionBottomView), false))
        }
        queue.add(showCase(R.string.urge_users_to_click_icons, "transactionIcons",
                transaction_amount_placeholder_view))
        queue.show()
        add_attachment_button.setOnClickListener {
            attachmentDialog()
        }
        transaction_date_edittext.setText(DateTimeUtil.getTodayDate())
        transaction_date_edittext.setOnClickListener {
            val materialDatePicker = MaterialDatePicker.Builder.datePicker()
            val picker = materialDatePicker.build()
            picker.show(parentFragmentManager, picker.toString())
            picker.addOnPositiveButtonClickListener { time ->
                transaction_date_edittext.setText(DateTimeUtil.getCalToString(time.toString()))
            }
        }
        category_edittext.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if(event.action == MotionEvent.ACTION_UP){
                    category_edittext.compoundDrawables[0].bounds.width()
                    if(event.x <= category_edittext.compoundDrawables[0].bounds.width() + 30){
                        val catDialog = CategoriesDialog()
                        catDialog.show(parentFragmentManager, "categoryDialog")
                        return true
                    }
                }
                return false
            }
        })
        categorySearch.categoryName.observe(viewLifecycleOwner) {
            category_edittext.setText(it)
        }
        currencyViewModel.currencyCode.observe(viewLifecycleOwner) { currency ->
            addTransactionViewModel.currency = currency
        }
        currencyViewModel.currencyFullDetails.observe(viewLifecycleOwner) {
            currency_edittext.setText(it)
        }
        currency_edittext.setOnClickListener{
            CurrencyListBottomSheet().show(parentFragmentManager, "currencyList" )
        }
        tags_chip.addChipTerminator('\n' ,ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL)
        tags_chip.addChipTerminator(',', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR)
        tags_chip.enableEditChipOnTouch(false, true)
        tags_chip.doAfterTextChanged { editable ->
            addTransactionViewModel.getTags(editable.toString()).observe(viewLifecycleOwner){ tags ->
                val tagsAdapter = ArrayAdapter(requireContext(), android.R.layout.select_dialog_item, tags)
                tags_chip.threshold = 1
                tags_chip.setAdapter(tagsAdapter)
            }
        }
        addTransactionViewModel.getDefaultCurrency().observe(viewLifecycleOwner) { defaultCurrency ->
            if(!isTasker){
                currency_edittext.setText(defaultCurrency)
            }
        }
        piggy_edittext.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if(event.action == MotionEvent.ACTION_UP){
                    piggy_edittext.compoundDrawables[0].bounds.width()
                    if(event.x <= piggy_edittext.compoundDrawables[0].bounds.width() + 30){
                        val piggyBankDialog = SearchPiggyDialog()
                        piggyBankDialog.show(parentFragmentManager, "piggyDialog")

                        return true
                    }
                }
                return false
            }
        })
        piggy_edittext.doAfterTextChanged { editable ->
            addTransactionViewModel.getPiggyBank(editable.toString()).observe(viewLifecycleOwner){ dataToDisplay ->
                val autocompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.select_dialog_item, dataToDisplay)
                piggy_edittext.setAdapter(autocompleteAdapter)
            }
        }
        piggySearch.piggyName.observe(viewLifecycleOwner){ piggy ->
            piggy_edittext.setText(piggy)
        }
        budget_edittext.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if(event.action == MotionEvent.ACTION_UP){
                    budget_edittext.compoundDrawables[0].bounds.width()
                    if(event.x <= budget_edittext.compoundDrawables[0].bounds.width() + 30){
                        val budgetDialog = BudgetSearchDialog()
                        budgetDialog.show(parentFragmentManager, "budgetDialog")
                        return true
                    }
                }
                return false
            }
        })
        budgetSearch.budgetName.observe(viewLifecycleOwner) { name ->
            budget_edittext.setText(name)
        }
        expansionLayout.addListener { _, expanded ->
            if(expanded){
                if (piggy_layout.isVisible){
                    dialog_add_transaction_layout.post {
                        dialog_add_transaction_layout.smoothScrollTo(0, piggy_layout.bottom)
                    }
                    dialog_add_transaction_layout.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                        if(scrollY == piggy_layout.bottom){
                            showCase(R.string.transactions_create_transfer_ffInput_piggy_bank_id,
                                    "transactionPiggyShowCase", piggy_layout, false).show()
                        }
                    }
                }
            }
        }
        placeHolderToolbar.navigationIcon = getCompatDrawable(R.drawable.abc_ic_clear_material)
        placeHolderToolbar.setNavigationOnClickListener {
            handleBack()
        }
        time_edittext.setOnClickListener {
            val materialTimePicker = MaterialTimePicker()
            materialTimePicker.setTimeFormat(TimeFormat.CLOCK_24H)
            materialTimePicker.setListener { materialTimePickerListener ->
                val min = if(materialTimePickerListener.minute < 10){
                    "0${materialTimePickerListener.minute}"
                } else {
                    materialTimePickerListener.minute.toString()
                }
                val hour = if(materialTimePickerListener.hour < 10){
                    "0${materialTimePickerListener.hour}"
                } else {
                    materialTimePickerListener.hour.toString()
                }
                selectedTime = "${hour}:${min}"
                time_edittext.setText(selectedTime)
            }
            materialTimePicker.show(parentFragmentManager, "timePickerDialog")
        }
        description_edittext.doAfterTextChanged { editable ->
            addTransactionViewModel.getTransactionByDescription(editable.toString()).observe(viewLifecycleOwner){ list ->
                val adapter = ArrayAdapter(requireContext(), android.R.layout.select_dialog_item, list)
                description_edittext.setAdapter(adapter)
            }
        }
        destination_edittext.doAfterTextChanged { editable ->
            addTransactionViewModel.getAccountByNameAndType("expense", editable.toString()).observe(viewLifecycleOwner){ list ->
                val autocompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.select_dialog_item, list)
                destination_edittext.setAdapter(autocompleteAdapter)
            }
        }
        category_edittext.doAfterTextChanged { editable ->
            addTransactionViewModel.getCategory(editable.toString()).observe(viewLifecycleOwner) { dataToDisplay ->
                val autocompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.select_dialog_item, dataToDisplay)
                category_edittext.setAdapter(autocompleteAdapter)
            }
        }
        budget_edittext.doAfterTextChanged { editable ->
            addTransactionViewModel.getBudgetByName(editable.toString()).observe(viewLifecycleOwner){ list ->
                val autocompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.select_dialog_item, list)
                budget_edittext.setAdapter(autocompleteAdapter)
            }
        }
        source_edittext.doAfterTextChanged { editable ->
            addTransactionViewModel.getAccountByNameAndType("revenue", editable.toString()).observe(viewLifecycleOwner){ list ->
                val autocompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.select_dialog_item, list)
                source_edittext.setAdapter(autocompleteAdapter)
            }
        }
        markdownViewModel.markdownText.observe(viewLifecycleOwner){ markdownText ->
            note_edittext.setText(markdownText)
        }
        note_edittext.setOnClickListener {
            markdownViewModel.markdownText.postValue(note_edittext.getString())
            parentFragmentManager.commit {
                replace(R.id.bigger_fragment_container, MarkdownFragment())
                addToBackStack(null)
            }
        }
        attachment_information.layoutManager = LinearLayoutManager(requireContext())
        attachment_information.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        attachment_information.adapter = TransactionAttachmentRecyclerAdapter(attachmentDataAdapter,
                false, { data: AttachmentData ->
            attachmentDataAdapter.remove(data)
            attachment_information.adapter?.notifyDataSetChanged()
        }) { another: Int -> }
        setUi()
    }

    private fun contextSwitch(){
        addTransactionViewModel.getAccounts().observe(viewLifecycleOwner){ accounts ->
            if(transactionType.contentEquals("Transfer")){
                source_exposed_menu.isVisible = true
                sourceExposedTasker.isVisible = true
                source_layout.isGone = true
                sourceTextInputTasker.isGone = true
                destination_layout.isGone = true
                destinationTextInputTasker.isGone = true
                destination_exposed_menu.isVisible = true
                destinationExposedTasker.isVisible = true
                piggy_layout.isVisible = true
                piggyTasker.isVisible = true
                val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.cat_exposed_dropdown_popup_item, accounts)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                destination_exposed_dropdown.setAdapter(spinnerAdapter)
                source_exposed_dropdown.setAdapter(spinnerAdapter)
                addTransactionViewModel.transactionDestinationAccount.observe(viewLifecycleOwner){ transactionDestinationAccount ->
                    destination_exposed_dropdown.setText(transactionDestinationAccount)
                }
                addTransactionViewModel.transactionSourceAccount.observe(viewLifecycleOwner){ transactionSourceAccount ->
                    source_exposed_dropdown.setText(transactionSourceAccount)
                }
                addTransactionViewModel.isFromTasker.observe(viewLifecycleOwner){ isTask ->
                    if(isTask){
                        destinationTextInputTasker.isVisible = false
                        sourceTextInputTasker.isVisible = false
                        piggyTasker.isVisible = true
                    }
                }
            } else if(transactionType.contentEquals("Deposit")){
                val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.cat_exposed_dropdown_popup_item, accounts)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                destination_exposed_dropdown.setAdapter(spinnerAdapter)
                destination_layout.isVisible = false
                destinationTextInputTasker.isVisible = false
                source_exposed_menu.isVisible = false
                sourceExposedTasker.isVisible = false
                addTransactionViewModel.transactionDestinationAccount.observe(viewLifecycleOwner){ transactionDestinationAccount ->
                    destination_exposed_dropdown.setText(transactionDestinationAccount)
                }
                addTransactionViewModel.transactionSourceAccount.observe(viewLifecycleOwner){ transactionSourceAccount ->
                    source_edittext.setText(transactionSourceAccount)
                }
                addTransactionViewModel.isFromTasker.observe(viewLifecycleOwner){ isTask ->
                    if(isTask){
                        sourceExposedTasker.isVisible = false
                        destinationTextInputTasker.isVisible = false
                    }
                }
            } else if(transactionType.contentEquals("Withdrawal")){
                source_layout.isVisible = false
                destination_exposed_menu.isVisible = false
                sourceTextInputTasker.isVisible = false
                destinationExposedTasker.isVisible = false
                // Spinner for source account
                val spinnerAdapter = ArrayAdapter(requireContext(),
                        R.layout.cat_exposed_dropdown_popup_item, accounts)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                source_exposed_dropdown.setAdapter(spinnerAdapter)
                addTransactionViewModel.transactionDestinationAccount.observe(viewLifecycleOwner){ transactionDestinationAccount ->
                    destination_edittext.setText(transactionDestinationAccount)
                }
                addTransactionViewModel.transactionSourceAccount.observe(viewLifecycleOwner){ transactionSourceAccount ->
                    source_exposed_dropdown.setText(transactionSourceAccount)
                }
                addTransactionViewModel.isFromTasker.observe(viewLifecycleOwner){ isTask ->
                    if(isTask){
                        destinationExposedTasker.isVisible = false
                        sourceTextInputTasker.isVisible = false
                    }
                }
            }
            ProgressBar.animateView(progressLayout, View.GONE, 0f, 200)
        }
    }

    private fun submitData(piggyBank: String?, sourceAccount: String, destinationAccount: String,
                           categoryName: String?, transactionTags: String?, budgetName: String?) {
        addTransactionViewModel.addTransaction(transactionType, description_edittext.getString(),
                transaction_date_edittext.getString(), selectedTime, piggyBank, transaction_amount_edittext.getString(),
                sourceAccount, destinationAccount, categoryName, transactionTags, budgetName, attachmentItemAdapter,
                note_edittext.getString()).observe(viewLifecycleOwner){ response ->
            if(response.first){
                toastSuccess(response.second)
                handleBack()
            } else {
                toastInfo(response.second)
            }
        }
    }

    private fun updateData(piggyBank: String?, sourceAccount: String, destinationAccount: String,
                           categoryName: String?, transactionTags: String?, budgetName: String?){
        addTransactionViewModel.updateTransaction(transactionJournalId, transactionType,
                description_edittext.getString(), transaction_date_edittext.getString(),
                selectedTime, piggyBank, transaction_amount_edittext.getString(),
                sourceAccount, destinationAccount, categoryName,
                transactionTags, budgetName, note_edittext.getString()).observe(viewLifecycleOwner){ response ->
            if(response.first){
                toastSuccess(response.second)
                handleBack()
            } else {
                toastInfo(response.second)
            }
        }
    }


    private fun attachmentDialog(){
        val listItems = arrayOf("Capture image from camera", "Choose File")
        AlertDialog.Builder(requireContext())
                .setItems(listItems) { dialog, which ->
                    when (which) {
                        0 -> {
                            val createTempDir = File(requireContext().getExternalFilesDir(null).toString() +
                                    File.separator + "temp")
                            if(!createTempDir.exists()){
                                createTempDir.mkdir()
                            }
                            val randomId = UUID.randomUUID().toString().substring(0, 7)
                            val fileToOpen = File(requireContext().getExternalFilesDir(null).toString() +
                                    File.separator + "temp" + File.separator + "${randomId}-firefly.png")
                            if(fileToOpen.exists()){
                                fileToOpen.delete()
                            }
                            fileUri = FileProvider.getUriForFile(requireContext(),
                                    requireContext().packageName + ".provider", fileToOpen)
                            takePicture.launch(fileUri)
                        }
                        1 -> {
                            chooseDocument.launch(arrayOf("*/*"))
                        }
                    }
                }
                .show()
    }

    override fun handleBack() {
        if(isFromFragment){
            parentFragmentManager.popBackStack()
            extendedFab.isVisible = true
        } else {
            if(isTasker){
                requireActivity().onBackPressed()
            } else {
                requireActivity().finish()
            }
        }
    }
}