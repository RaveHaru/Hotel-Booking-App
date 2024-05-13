package com.example.angodafake

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.angodafake.Adapter.BillAdapter
import com.example.angodafake.Utilities.HotelUtils
import com.example.angodafake.Utilities.PurchaseUtils
import com.example.angodafake.db.Purchase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

/**
 * A simple [Fragment] subclass.
 * Use the [BillFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BillFragment(private var idUser: String) : Fragment() {
    // TODO: Rename and change types of parameters
    private var fromFrag: String? = null
    private lateinit var recyclerView : RecyclerView
    private lateinit var adapter : BillAdapter
    private lateinit var bill_list : ArrayList<Purchase>
    private var bill_list_tmp = ArrayList<Purchase>()
    private lateinit var btn_back : ImageButton
    private lateinit var tv_total : TextView
    private lateinit var lBillFilter: TextInputLayout
    private lateinit var et_billFilter: TextInputEditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var addFab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fromFrag = it.getString("from")
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bill, container, false)
        initUI(view)

        adapter = BillAdapter(requireContext(), bill_list)
        recyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager

        if (fromFrag == "edit" || fromFrag == "edit_room" || fromFrag == "myHotel"){
            val list = arguments?.getStringArrayList("bills")
            var total = 0.0
            for (bill in list!!){
                PurchaseUtils.getPurchaseByID(bill){
                    bill_list.add(it)
                    bill_list_tmp.add(it)
                    adapter.notifyDataSetChanged()
                    if (it.time_cancel == "" || it.time_cancel == null){
                        total += it.total_purchase!!
                        tv_total.text = "${formatMoney(total.toInt())} VND"
                    }
                }
            }
        }

        addFab.setOnClickListener {
            val arg = Bundle()
            arg.putString("date", arguments?.getString("date"))
            arg.putString("dateType", arguments?.getString("dateType"))
            arg.putString("searchStr", arguments?.getString("searchStr"))
            arg.putStringArrayList("bills", arguments?.getStringArrayList("bills"))

            val addBillFrag = AddBillFragment(idUser)
            addBillFrag.arguments = arg

            val mainActivity = requireActivity() as MainActivity
            mainActivity.replaceFragment(addBillFrag)
        }

        et_billFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Không cần thực hiện gì trong trường hợp này
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Không cần thực hiện gì trong trường hợp này
            }

            override fun afterTextChanged(s: Editable?) {
                // Phương thức này được gọi sau khi văn bản đã thay đổi
                val enteredText = s.toString()
                if (enteredText == ""){
                    bill_list.clear()
                    bill_list.addAll(bill_list_tmp)
                    adapter.notifyDataSetChanged()
                    var total = 0.0
                    for (bill in bill_list){
                        total += bill.total_purchase!!
                        tv_total.text = "${formatMoney(total.toInt())} VND"
                    }
                } else{
                    bill_list.clear()
                    var total = 0.0
                    for (bill in bill_list_tmp){
                        HotelUtils.getHotelByID(bill.ID_Hotel!!){
                            if (bill.ID?.toLowerCase(Locale.ROOT)!!.contains(enteredText.toLowerCase(Locale.ROOT))
                                || it.name?.toLowerCase(Locale.ROOT)!!.contains(enteredText.toLowerCase(Locale.ROOT))
                                || SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                                    SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).parse(bill.time_booking!!)!!
                                ).contains(enteredText)){
                                bill_list.add(bill)
                                adapter.notifyDataSetChanged()
                                if (bill.time_cancel == "" || bill.time_cancel == null){
                                    total += bill.total_purchase!!
                                    tv_total.text = "${formatMoney(total.toInt())} VND"
                                }
                            }
                        }
                    }
                }

            }
        })

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val radioButton: RadioButton = view.findViewById(checkedId)
            if (radioButton.text == "Gần nhất"){
                bill_list.sortByDescending {
                    val bookingDate = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).parse(it.time_booking!!)
                    bookingDate
                }
                adapter.notifyDataSetChanged()
                bill_list_tmp.sortByDescending {
                    val bookingDate = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).parse(it.time_booking!!)
                    bookingDate
                }
            } else{
                bill_list.sortBy {
                    val bookingDate =
                        SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).parse(
                            it.time_booking!!
                        )
                    bookingDate
                }
                adapter.notifyDataSetChanged()
                bill_list_tmp.sortBy {
                    val bookingDate = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).parse(it.time_booking!!)
                    bookingDate
                }
            }
            adapter.notifyDataSetChanged()
        }

        btn_back.setOnClickListener {
            if (fromFrag == "edit" || fromFrag == "myHotel"){
                val arg = Bundle()
                arg.putString("date", arguments?.getString("date"))
                arg.putString("dateType", arguments?.getString("dateType"))
                arg.putString("searchStr", arguments?.getString("searchStr"))

                val hotelManageFrg = MyHotel(idUser)
                hotelManageFrg.arguments = arg

                val mainActivity = requireActivity() as MainActivity
                mainActivity.replaceFragment(hotelManageFrg)
            } else if (fromFrag == "edit_room"){
                val arg = Bundle()
                arg.putString("date", arguments?.getString("date"))
                arg.putString("idHotel", arguments?.getString("idHotel"))
                arg.putString("dateType", arguments?.getString("dateType"))
                arg.putString("searchStr", arguments?.getString("searchStr"))
                arg.putString("searchRoomStr", arguments?.getString("searchRoomStr"))

                val roomManageFrg = ManageRoomsFragment(idUser)
                roomManageFrg.arguments = arg

                val mainActivity = requireActivity() as MainActivity
                mainActivity.replaceFragment(roomManageFrg)
            }
        }

        return view
    }

    fun formatMoney(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat
        formatter.applyPattern("#,###")
        return formatter.format(amount.toLong())
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initUI(view: View){
        recyclerView = view.findViewById(R.id.recyclerView)
        btn_back = view.findViewById(R.id.btn_back)
        tv_total = view.findViewById(R.id.tv_total)
        lBillFilter = view.findViewById(R.id.lBillFilter)
        et_billFilter = lBillFilter.editText as TextInputEditText
        radioGroup = view.findViewById(R.id.radioGroup)
        bill_list = ArrayList()
        addFab = view.findViewById(R.id.addFab)
        if (fromFrag == "myHotel"){
            addFab.visibility = View.VISIBLE
        } else{
            addFab.visibility = View.GONE
        }
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BillFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(fromFrag: String, idUser: String) =
            BillFragment(idUser).apply {
                arguments = Bundle().apply {
                    putString("from", fromFrag)
                }
            }
    }

}