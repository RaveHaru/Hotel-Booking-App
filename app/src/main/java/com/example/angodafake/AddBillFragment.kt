package com.example.angodafake

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.angodafake.Utilities.HotelUtils
import com.example.angodafake.Utilities.PurchaseUtils
import com.example.angodafake.Utilities.RoomUtils
import com.example.angodafake.Utilities.UserUtils
import com.example.angodafake.db.Hotel
import com.example.angodafake.db.Purchase
import com.example.angodafake.db.User
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [AddBillFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddBillFragment(private var idUser: String) : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var lName: TextInputLayout
    private lateinit var et_name: TextInputEditText
    private lateinit var lPhoneN: TextInputLayout
    private lateinit var et_phoneN: TextInputEditText
    private lateinit var sp_hotelName: Spinner
    private lateinit var sp_typeRoom: Spinner
    private lateinit var lDateCome: TextInputLayout
    private lateinit var et_dateCome: TextInputEditText
    private lateinit var lDateGo: TextInputLayout
    private lateinit var et_dateGo: TextInputEditText
    private lateinit var errorText: TextView
    private lateinit var lMethod: TextInputLayout
    private lateinit var sp_method: Spinner
    private lateinit var lTotal: TextInputLayout
    private lateinit var et_total: TextInputEditText

    private lateinit var btn_back: ImageButton
    private lateinit var btn_addBill: Button
    private lateinit var minus: ImageView
    private lateinit var plus: ImageView
    private lateinit var countRoom: TextView
    private var quantity = 1
    private var price = 0
    private var hotel_ID: String? = null
    private var room_ID: String? = null
    private var method: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_bill, container, false)
        initUI(view)

        et_name.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                lName.error = null
            }
        }
        et_phoneN.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                lPhoneN.error = null
            }
        }
        et_dateCome.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                lDateCome.error = null
                errorText.visibility = View.GONE
            }
        }
        et_dateGo.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                lDateGo.error = null
                errorText.visibility = View.GONE
            }
        }
        et_dateGo.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                lDateGo.error = null
            }
        }

        //spinner khach san
        HotelUtils.getHotelNamesByOwnerID(idUser) { hotelNames ->
            var items = hotelNames
            items = items.sorted().toMutableList()
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sp_hotelName.adapter = adapter

            // Đặt giá trị mặc định cho Spinner sp_hotelName
            sp_hotelName.setSelection(0)

            // Lắng nghe sự kiện khi người dùng chọn một khách sạn
            sp_hotelName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedHotelName = sp_hotelName.selectedItem.toString()
                    HotelUtils.getIDHotelByNameAndIdOwner(selectedHotelName, idUser) { hotelID ->
                        if (hotelID != null) {
                            hotel_ID = hotelID
                            // Lấy danh sách loại phòng của khách sạn
                            RoomUtils.getTypeByHotelID(hotelID) { typeList ->
                                val typeItems = typeList
                                val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeItems)
                                typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                sp_typeRoom.adapter = typeAdapter
                                // Lắng nghe sự kiện khi người dùng chọn một loại phòng
                                sp_typeRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long
                                    ) {
                                        RoomUtils.getQuantityAndPriceByType(hotelID, sp_typeRoom.selectedItem.toString()){i, q, p ->
                                            room_ID = i
                                            quantity = q
                                            price = p
                                            countRoom.text = "Số phòng: 1"
                                            et_total.text = Editable.Factory.getInstance().newEditable("${formatMoney(price)} VND")
                                        }
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {
                                        TODO("Not yet implemented")
                                    }

                                }
                            }
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }

        //chon so luong phong
        minus.setOnClickListener {
            val countRoomText = countRoom.text.toString()
            var roomCount = countRoomText.substringAfterLast(":").trim().toIntOrNull()
            if (roomCount != null) {
                if (roomCount > 1) {
                    roomCount -= 1
                    countRoom.text = "Số phòng: ${roomCount}"
                    et_total.text = Editable.Factory.getInstance().newEditable("${formatMoney(price * roomCount)} VND")
                }
            }
        }
        plus.setOnClickListener {
            val countRoomText = countRoom.text.toString()
            var roomCount = countRoomText.substringAfterLast(":").trim().toIntOrNull()
            if (roomCount != null) {
                if (roomCount < quantity) {
                    roomCount += 1
                    countRoom.text = "Số phòng: ${roomCount}"
                    et_total.text = Editable.Factory.getInstance().newEditable("${formatMoney(price * roomCount)} VND")
                }
            }
        }


        //ngay den ngay di
        et_dateCome.setOnClickListener {
            et_dateCome.error = null
            et_dateGo.error = null
            errorText.visibility  = View.GONE
            hideKeyboard(requireActivity())
            val year: Int
            val month: Int
            val day: Int
            if (et_dateCome.text.toString() == ""){
                // Lấy ngày hiện tại
                val calendar = Calendar.getInstance()
                year = calendar.get(Calendar.YEAR)
                month = calendar.get(Calendar.MONTH)
                day = calendar.get(Calendar.DAY_OF_MONTH)
            }
            else{
                val date = et_dateCome.text.toString().split("/")
                year = date[2].toInt()
                month = date[1].toInt() - 1
                day = date[0].toInt()
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { view: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                    // Xử lý khi người dùng chọn ngày
                    val selectedDate = "$selectedDayOfMonth/${selectedMonth + 1}/$selectedYear"
                    et_dateCome.setText(selectedDate)
                    if (et_dateGo.text.toString() != ""){
                        RoomUtils.getRoomByID(hotel_ID!!, room_ID!!){
                            PurchaseUtils.getBookedRoomBillsByRoomIDAndDate(hotel_ID!!, room_ID!!, et_dateCome.text.toString(), et_dateGo.text.toString()){bookedRoomQty->
                                quantity = it.quantity?.minus(bookedRoomQty) ?: 0
                                val countRoomText = countRoom.text.toString()
                                val roomCount = countRoomText.substringAfterLast(":").trim().toIntOrNull()
                                if (roomCount != null) {
                                    if (roomCount > quantity){
                                        countRoom.text = "Số phòng: ${quantity}"
                                        et_total.text = Editable.Factory.getInstance().newEditable("${formatMoney(price * quantity)} VND")
                                    }
                                }

                            }
                        }

                    }
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        et_dateGo.setOnClickListener {
            et_dateCome.error = null
            et_dateGo.error = null
            errorText.visibility  = View.GONE
            hideKeyboard(requireActivity())
            val year: Int
            val month: Int
            val day: Int
            if (et_dateGo.text.toString() == ""){
                // Lấy ngày hiện tại
                val calendar = Calendar.getInstance()
                year = calendar.get(Calendar.YEAR)
                month = calendar.get(Calendar.MONTH)
                day = calendar.get(Calendar.DAY_OF_MONTH)
            }
            else{
                val date = et_dateGo.text.toString().split("/")
                year = date[2].toInt()
                month = date[1].toInt() - 1
                day = date[0].toInt()
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { view: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                    // Xử lý khi người dùng chọn ngày
                    val selectedDate = "$selectedDayOfMonth/${selectedMonth + 1}/$selectedYear"
                    et_dateGo.setText(selectedDate)
                    if (et_dateCome.text.toString() != ""){
                        RoomUtils.getRoomByID(hotel_ID!!, room_ID!!){
                            PurchaseUtils.getBookedRoomBillsByRoomIDAndDate(hotel_ID!!, room_ID!!, et_dateCome.text.toString(), et_dateGo.text.toString()){bookedRoomQty->
                                quantity = it.quantity?.minus(bookedRoomQty) ?: 0
                                val countRoomText = countRoom.text.toString()
                                val roomCount = countRoomText.substringAfterLast(":").trim().toIntOrNull()
                                if (roomCount != null) {
                                    if (roomCount > quantity){
                                        countRoom.text = "Số phòng: ${quantity}"
                                        et_total.text = Editable.Factory.getInstance().newEditable("${formatMoney(price * quantity)} VND")
                                    }
                                }

                            }
                        }

                    }
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        //spinner phuong thuc thanh toan
        val methodItems = arrayOf("Cash", "Momo")
        val methodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, methodItems)
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sp_method.adapter = methodAdapter

        btn_addBill.setOnClickListener {
            if (checkAll()){
                val name = et_name.text.toString()
                val phoneN = et_phoneN.text.toString()

                val user = User(null, name, null, null, phoneN)
                UserUtils.addUser(user){idCustomer->
                    val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
                    val currentTime = Date()
                    val formattedTime = dateFormat.format(currentTime)

                    val ID_Owner = idCustomer
                    val ID_Hotel = hotel_ID
                    val ID_Room = room_ID
                    val quantity = countRoom.text.toString().substringAfterLast(":").trim().toIntOrNull()
                    val method = sp_method.selectedItem.toString()
                    val time_booking = formattedTime
                    val total_purchase = quantity?.times(price)?.toDouble()
                    val detail = "HOAN_TAT"
                    val date_come = et_dateCome.text.toString()
                    val date_go = et_dateGo.text.toString()

                    val purchase = Purchase(null, ID_Owner, ID_Hotel, ID_Room, quantity, method, time_booking, time_booking, "", "", total_purchase, "", detail, date_come, date_go)
                    PurchaseUtils.addPurchase(purchase){
                        val arg = Bundle()
                        arg.putString("from", "myHotel")
                        arg.putString("date", arguments?.getString("date"))
                        arg.putString("dateType", arguments?.getString("dateType"))
                        arg.putString("searchStr", arguments?.getString("searchStr"))
                        val bills = arguments?.getStringArrayList("bills")
                        bills?.add(it)
                        arg.putStringArrayList("bills", bills)

                        val billFrag = BillFragment(idUser)
                        billFrag.arguments = arg

                        val mainActivity = requireActivity() as MainActivity
                        mainActivity.replaceFragment(billFrag)
                    }
                }
            }
        }

        btn_back.setOnClickListener {
            val arg = Bundle()
            arg.putString("from", "myHotel")
            arg.putString("date", arguments?.getString("date"))
            arg.putString("dateType", arguments?.getString("dateType"))
            arg.putString("searchStr", arguments?.getString("searchStr"))
            arg.putStringArrayList("bills", arguments?.getStringArrayList("bills"))

            val billFrag = BillFragment(idUser)
            billFrag.arguments = arg

            val mainActivity = requireActivity() as MainActivity
            mainActivity.replaceFragment(billFrag)
        }

        return view
    }


    private fun checkAll(): Boolean{
        hideKeyboard(requireActivity())
        var check = true
        if (isCheckEmpty(lName, et_name, "Tên khách hàng"))
            check = false
        if (isCheckEmpty(lPhoneN, et_phoneN, "Số di động"))
            check = false
        if (isCheckEmpty(lDateCome, et_dateCome, "!") || isCheckEmpty(lDateGo, et_dateGo, "!"))
            check = false
        if (!validateDate())
            check = false
        return check
    }

    private fun isCheckEmpty(textInputLayout: TextInputLayout, editText: EditText, name: String) : Boolean{
        return if (editText.text.toString().trim() == ""){
            textInputLayout.error = "$name không được để trống."
            if (name == "!"){
                errorText.text = "Ngày đến, ngày đi không được để trống."
                errorText.visibility = View.VISIBLE
            }
            true
        } else{
            textInputLayout.error = null
            false
        }
    }

    private fun validateDate(): Boolean{
        val timeFormat = SimpleDateFormat("dd/mm/yyyy", Locale.getDefault())

        if (et_dateCome.text.toString() != "" && et_dateGo.text.toString() != ""){
            try {
                val dateCome = timeFormat.parse(et_dateCome.text.toString())
                val dateGo = timeFormat.parse(et_dateGo.text.toString())

                if (dateGo != null) {
                    if (dateGo.after(dateCome)) {
                        et_dateCome.error = null
                        et_dateGo.error = null
                        errorText.visibility  = View.GONE
                        return true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            et_dateCome.error = "!"
            et_dateGo.error = "!"
            errorText.text = "Ngày đến phải sớm hơn Ngày đi"
            errorText.visibility  = View.VISIBLE
        }
        return false
    }

    fun formatMoney(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat
        formatter.applyPattern("#,###")
        return formatter.format(amount.toLong())
    }

    private fun initUI(view: View){
        lName = view.findViewById(R.id.lName)
        et_name = lName.editText as TextInputEditText
        lPhoneN = view.findViewById(R.id.lPhoneN)
        et_phoneN = lPhoneN.editText as TextInputEditText
        sp_hotelName = view.findViewById(R.id.sp_hotelName)
        sp_typeRoom = view.findViewById(R.id.sp_typeRoom)
        lDateCome = view.findViewById(R.id.lDateCome)
        et_dateCome = lDateCome.editText as TextInputEditText
        lDateGo = view.findViewById(R.id.lDateGo)
        et_dateGo = lDateGo.editText as TextInputEditText
        errorText = view.findViewById(R.id.errorText)
        lMethod = view.findViewById(R.id.lMethod)
        sp_method = view.findViewById(R.id.sp_method)
        lTotal = view.findViewById(R.id.lTotal)
        et_total = lTotal.editText as TextInputEditText
        btn_back = view.findViewById(R.id.btn_back)
        btn_addBill = view.findViewById(R.id.btn_addBill)
        minus = view.findViewById(R.id.minus)
        plus = view.findViewById(R.id.plus)
        countRoom = view.findViewById(R.id.countRoom)
    }

    fun hideKeyboard(activity: Activity) {
        val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        // Kiểm tra xem view nào đang có focus
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        // Sử dụng InputMethodManager để ẩn bàn phím
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddBillFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(idUser: String) =
            AddBillFragment(idUser).apply {
                arguments = Bundle().apply {
                }
            }
    }
}