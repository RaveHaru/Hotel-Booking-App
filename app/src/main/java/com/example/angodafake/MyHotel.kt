package com.example.angodafake

import android.app.Dialog
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Spinner
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.angodafake.Adapter.HotelManageAdapter
import com.example.angodafake.Adapter.OnHotelDeleteListener
import com.example.angodafake.Utilities.HotelUtils
import com.example.angodafake.Utilities.PurchaseUtils
import com.example.angodafake.db.Hotel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [MyHotel.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyHotel(private var idUser: String) : Fragment(), OnHotelDeleteListener {
    // TODO: Rename and change types of parameters

    private lateinit var addLL : View
    private lateinit var addFab : FloatingActionButton
    private lateinit var voucherLL : View
    private lateinit var voucherFab : FloatingActionButton
    private lateinit var billLL : View
    private lateinit var billFab : FloatingActionButton
    private lateinit var chatLL : View
    private lateinit var chatFab : FloatingActionButton
    private lateinit var qrLL : View
    private lateinit var qrFab : FloatingActionButton
    private lateinit var menuFab : FloatingActionButton
    private var rotate = false

    private lateinit var filterSpinner: Spinner
    private lateinit var lDate : TextInputLayout
    private lateinit var et_date : TextInputEditText
    private lateinit var recyclerView : RecyclerView
    private lateinit var hotel_list : ArrayList<Hotel>
    private lateinit var date: String
    private var dateType = 0
    private var searchStr = ""
    private lateinit var adapter: HotelManageAdapter

    private lateinit var lHotelName: TextInputLayout
    private lateinit var et_hotelName: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_hotel, container, false)
        initUI(view)
        initShowout(addLL)
        initShowout(voucherLL)
        initShowout(billLL)
        initShowout(qrLL)
        initShowout(chatLL)

        hotel_list = ArrayList()

        //khởi tạo gt cho date
        if (et_date.text.toString() == ""){
            // Lấy ngày hiện tại
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val formattedDay = if (day < 10) "0$day" else "$day"
            val formattedMonth = if (month < 10) "0$month" else "$month"
            et_date.text = Editable.Factory.getInstance().newEditable("$formattedDay/$formattedMonth/$year")
        }

        if (arguments?.getString("date") != null){
            et_date.text = Editable.Factory.getInstance().newEditable(arguments?.getString("date"))
        }

        date = et_date.text.toString()
        dateType = 0
        adapter = HotelManageAdapter(requireContext(), hotel_list, date, dateType, searchStr)
        adapter.setOnDeleteListener(this)
        adapter.onItemClick = {
            val arg = Bundle()
            arg.putString("date", adapter.date)
            arg.putString("idHotel", it.ID)
            arg.putString("dateType", adapter.dateType.toString())
            arg.putString("searchStr", adapter.searchStr)

            val manageRoomFrg = ManageRoomsFragment(idUser)
            manageRoomFrg.arguments = arg
            val mainActivity = requireActivity() as MainActivity
            mainActivity.replaceFragment(manageRoomFrg)
        }
        recyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        HotelUtils.getHotelByOwnerID(idUser){hotelL->
            hotel_list.clear()
            hotel_list.addAll(hotelL)
            adapter.notifyDataSetChanged()
        }

        val items = listOf("Ngày đặt phòng", "Ngày đến - Ngày đi")
        val spAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spAdapter

        if (arguments?.getString("dateType") != null){
            dateType = arguments?.getString("dateType")!!.toInt()
            filterSpinner.setSelection(dateType)
            adapter.dateType = dateType
            adapter.notifyDataSetChanged()
        }
        if (arguments?.getString("searchStr") != null){
            et_hotelName.text = Editable.Factory.getInstance().newEditable(arguments?.getString("searchStr"))
            HotelUtils.getHotelsByNameAndIdOwner(et_hotelName.text.toString(), idUser){hotelL->
                hotel_list.clear()
                hotel_list.addAll(hotelL)
                adapter.searchStr = arguments?.getString("searchStr")!!
                adapter.notifyDataSetChanged()
            }
        }

        et_hotelName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Không cần thực hiện gì trong trường hợp này
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Không cần thực hiện gì trong trường hợp này
            }

            override fun afterTextChanged(s: Editable?) {
                // Phương thức này được gọi sau khi văn bản đã thay đổi
                val enteredText = s.toString()
                HotelUtils.getHotelsByNameAndIdOwner(enteredText, idUser){hotelL->
                    hotel_list.clear()
                    hotel_list.addAll(hotelL)
                    adapter.searchStr = enteredText
                    adapter.notifyDataSetChanged()
                }
            }
        })


        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = items[position]
                if (selectedItem == "Ngày đặt phòng"){
                    adapter.dateType = 0
                    adapter.notifyDataSetChanged()
                } else{
                    adapter.dateType = 1
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Xử lý khi không có mục nào được chọn
            }
        }

        et_date.setOnClickListener {
            it.clearFocus()
            val year: Int
            val month: Int
            val day: Int
            if (et_date.text.toString() == ""){
                // Lấy ngày hiện tại
                val calendar = Calendar.getInstance()
                year = calendar.get(Calendar.YEAR)
                month = calendar.get(Calendar.MONTH)
                day = calendar.get(Calendar.DAY_OF_MONTH)
            }
            else{
                val dateParts  = et_date.text.toString().split("/")
                year = dateParts[2].toInt()
                month = dateParts[1].toInt() - 1
                day = dateParts[0].toInt()
            }
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { view: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                    // Xử lý khi người dùng chọn ngày
                    val selectedDate = "$selectedDayOfMonth/${selectedMonth + 1}/$selectedYear"
                    et_date.setText(selectedDate)
                    adapter.date = et_date.text.toString()
                    adapter.notifyDataSetChanged()
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        addFab.setOnClickListener {
            val mainActivity = requireActivity() as MainActivity
            mainActivity.replaceFragment(AddHotelFragment(idUser))
        }

        voucherFab.setOnClickListener {
            val intent = Intent(context, HotelOfManagementVoucher::class.java)
            intent.putExtra("id_user", idUser)
            startActivity(intent)
        }

        chatFab.setOnClickListener {
                val intent = Intent(context, ChatList::class.java)
                intent.putExtra("id_user", idUser)
                startActivity(intent)
        }

        billFab.setOnClickListener {
            PurchaseUtils.getAllPurchaseByIDHotelOwner(idUser){
                val list = ArrayList<String>()
                for (bill in it!!){
                    list.add(bill.ID!!)
                }

                val arg = Bundle()
                arg.putString("from", "myHotel")
                arg.putString("date", et_date.text.toString())
                arg.putString("dateType", dateType.toString())
                arg.putString("searchStr", searchStr)
                arg.putStringArrayList("bills", list)

                val billFrag = BillFragment(idUser)
                billFrag.arguments = arg

                val mainActivity = requireActivity() as MainActivity
                mainActivity.replaceFragment(billFrag)
            }
        }

        qrFab.setOnClickListener {
            val intent = Intent(context, ScanQRCodeActivity::class.java)
            intent.putExtra("idUser", idUser)
            startActivity(intent)
        }

        menuFab.setOnClickListener {
            toggleFabMode(it)
        }

        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onHotelDeleted(hotel: Hotel){
        // Xử lý sự kiện khi người dùng xóa khách sạn
        HotelUtils.deleteHotel(hotel.ID!!){
            if (it){
                hotel_list.remove(hotel)
                adapter.notifyDataSetChanged()
                showSuccessSnackBar("Đã xóa phòng.", requireView())
            } else{
                showSnackBar("Xóa thất bại! Phòng này hiện vẫn đang được giao dịch. Xin kiểm tra lại hóa đơn.", requireView())
            }
        }
    }

    private fun toggleFabMode(v: View) {
        rotate = rotateFab(v, !rotate)
        if (rotate){
            showIn(addLL)
            showIn(voucherLL)
            showIn(billLL)
            showIn(qrLL)
            showIn(chatLL)
            requireView().findViewById<View>(R.id.backgroundOverlay).visibility = View.VISIBLE
        } else{
            showOut(addLL)
            showOut(voucherLL)
            showOut(billLL)
            showOut(qrLL)
            showOut(chatLL)
            requireView().findViewById<View>(R.id.backgroundOverlay).visibility = View.GONE
        }
    }

    private fun initShowout(v: View){
        v.apply {
            visibility = View.GONE
            translationY = height.toFloat()
            alpha = 0f
        }
    }
    private fun showOut(view: View) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 1f
            translationY = 0f
            animate()
                .setDuration(200)
                .translationY(height.toFloat())
                .setListener(object : AnimatorListenerAdapter(){
                })
                .alpha(0f)
                .start()
        }
    }

    private fun showIn(view: View) {
        view.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = height.toFloat()
            animate()
                .setDuration(200)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter(){})
                .alpha(1f)
                .start()
        }
    }

    private fun rotateFab(v: View, rotate: Boolean): Boolean {
        v.animate()
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter(){})
            .rotation(if (rotate) 180f else 0f)
        return rotate
    }

    private fun showSnackBar(msg: String, view: View) {
        val snackbar = Snackbar.make(view.rootView, msg, Snackbar.LENGTH_LONG)
        // Đổi màu background của Snackbar
        snackbar.view.backgroundTintList = ColorStateList.valueOf(Color.RED)
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }
    private fun showSuccessSnackBar(msg: String, view: View) {
        val snackbar = Snackbar.make(view.rootView, msg, Snackbar.LENGTH_LONG)
        snackbar.view.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3193FF"))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initUI(view: View){
        addLL = view.findViewById(R.id.addLL)
        addFab = view.findViewById(R.id.addFab)
        voucherLL = view.findViewById(R.id.voucherLL)
        voucherFab = view.findViewById(R.id.voucherFab)
        billLL = view.findViewById(R.id.billLL)
        billFab = view.findViewById(R.id.billFab)
        qrLL = view.findViewById(R.id.qrLL)
        qrFab = view.findViewById(R.id.qrFab)
        chatLL = view.findViewById(R.id.chatLL)
        chatFab = view.findViewById(R.id.chatFab)
        menuFab = view.findViewById(R.id.menuFab)

        lHotelName = view.findViewById(R.id.lHotelName)
        et_hotelName = lHotelName.editText as TextInputEditText
        recyclerView = view.findViewById(R.id.recyclerView)
        filterSpinner = view.findViewById(R.id.filterSpinner)
        lDate = view.findViewById(R.id.lDate)
        et_date = lDate.editText as TextInputEditText
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MyHotel.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(idUser: String) =
            MyHotel(idUser).apply {
                arguments = Bundle().apply {
                }
            }
    }
}