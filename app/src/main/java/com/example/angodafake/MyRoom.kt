package com.example.angodafake

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.angodafake.Adapter.ActivePurchaseAdapter
import com.example.angodafake.Adapter.PastCancelPurchaseAdapter
import com.example.angodafake.Utilities.HotelUtils
import com.example.angodafake.Utilities.PictureUtils
import com.example.angodafake.Utilities.PurchaseUtils
import com.example.angodafake.db.Purchase
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private val TAB_TITLE = arrayOf("Sắp tới","Hoàn tất","Đã hủy")
private const val ARG_OBJECT = "title_tab"
private const val IS_AVAILABLE= "have_content"
private var HAVE_CONTENT: MutableList<Boolean> = mutableListOf(false,false,false)
private var upcomingList: MutableList<PurchaseExtra> = mutableListOf()
private var completedList: MutableList<PurchaseExtra> = mutableListOf()
private var canceledList: MutableList<PurchaseExtra> = mutableListOf()

data class PurchaseExtra(
    var Purchase: Purchase? = null,
    var nameHotel: String? = null,
    var imageHotel: String? = null
)

/**
 * A simple [Fragment] subclass.
 * Use the [MyRoom.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyRoom : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var collectionAdapter: CollectionAdapter
    private fun loadData() {
        collectionAdapter = CollectionAdapter(this)
        val tabHandler = requireView().findViewById<TabLayout>(R.id.tab_handler)
        val viewPager = requireView().findViewById<ViewPager2>(R.id.pager)

        viewPager.adapter = collectionAdapter
        TabLayoutMediator(tabHandler, viewPager) { tab, position ->
            tab.text = TAB_TITLE[position]
        }.attach()
    }
    private suspend fun createData() {
        upcomingList.clear()
        completedList.clear()
        canceledList.clear()

        // Suspend execution and wait for the callback to complete
        val allPurchases = suspendCoroutine<List<Purchase>> { continuation ->
            PurchaseUtils.getAllPurchases("tYw0x3oVS7gAd9wOdOszzvJMOEM2") { allPurchases ->
                continuation.resume(allPurchases)
            }
        }

        for (purchase in allPurchases) {
            var temp: PurchaseExtra?

            val hotel = suspendCoroutine { continuation ->
                HotelUtils.getHotelByID(purchase.ID_Hotel!!) { hotel ->
                    println(hotel.name)
                    continuation.resume(hotel)
                }
            }

            val picture = suspendCoroutine { continuation ->
                PictureUtils.getPictureByHotelID(purchase.ID_Hotel!!) { picture ->
                    println(picture.picture)
                    continuation.resume(picture)
                }
            }

            temp = PurchaseExtra(purchase,hotel.name,picture.picture)
            when (purchase.detail) {
                "SAP_TOI" -> upcomingList.add(temp)
                "HOAN_TAT" -> completedList.add(temp)
                "DA_HUY" -> canceledList.add(temp)
            }
        }

        HAVE_CONTENT = mutableListOf(
            upcomingList.isNotEmpty(),
            completedList.isNotEmpty(),
            canceledList.isNotEmpty()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            createData()
            loadData()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_room, container, false)
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MyRoom.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MyRoom().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
class CollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int).
        val fragment = ObjectFragment()
        fragment.arguments = Bundle().apply {
            // The object is just an integer.
            putInt(ARG_OBJECT, position)
            putBoolean(IS_AVAILABLE, HAVE_CONTENT[position])
        }
        return fragment
    }
}

// Instances of this class are fragments representing a single
// object in the collection.
class ObjectFragment : Fragment() {
    private lateinit var activePurchase: RecyclerView
    private lateinit var pastPurchase: RecyclerView
    private lateinit var cancelPurchase: RecyclerView
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var linearAdapter1: ActivePurchaseAdapter
    private lateinit var linearAdapter2: PastCancelPurchaseAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.apply {
            val have_content= getBoolean(IS_AVAILABLE)
            layoutManager = LinearLayoutManager(requireContext())
            return when (getInt(ARG_OBJECT)) {
                0 -> {
                    println(upcomingList)
                    val view1 = if (have_content) R.layout.tab_active_room else R.layout.empty_active_room
                    val layout1 = inflater.inflate(view1, container, false)
                    if (have_content) {
                        activePurchase = layout1.findViewById(R.id.activeList)
                        activePurchase.layoutManager = layoutManager
                        activePurchase.setHasFixedSize(true)

                        linearAdapter1 = ActivePurchaseAdapter(requireContext(), upcomingList)
                        activePurchase.adapter = linearAdapter1

                        linearAdapter1.onItemClick = { contact ->
                            val intent = Intent(requireContext(), ActivePurchaseDetail::class.java)
                            println(contact.Purchase?.status_purchase)
                            intent.putExtra("status_purchase", contact.Purchase?.status_purchase)
                            startActivity(intent)
                        }
                    } else {
                        val btnOrder1: Button = layout1.findViewById(R.id.btn_order1)

                        btnOrder1.setOnClickListener {
                            replaceFragmentToHotel()
                        }
                    }
                    return layout1
                }
                1 -> {
                    val view2 = if (have_content) R.layout.tab_past_room else R.layout.empty_past_room
                    val layout2 = inflater.inflate(view2, container, false)
                    if (have_content) {
                        pastPurchase = layout2.findViewById(R.id.pastList)
                        pastPurchase.layoutManager = layoutManager
                        pastPurchase.setHasFixedSize(true)

                        linearAdapter2 = PastCancelPurchaseAdapter(requireContext(), completedList)
                        pastPurchase.adapter = linearAdapter2

                        linearAdapter2.onItemClick = { contact ->
                            val intent = Intent(requireContext(), PastCancelPurchaseDetail::class.java)
                            println(contact.Purchase?.status_purchase)
                            intent.putExtra("status_purchase", contact.Purchase?.status_purchase)
                            startActivity(intent)
                        }
                    } else {
                        val btnOrder2: Button = layout2.findViewById(R.id.btn_order2)

                        btnOrder2.setOnClickListener {
                            replaceFragmentToHotel()
                        }
                    }
                    return layout2
                }
                else -> {
                    val view3 = if (have_content) R.layout.tab_canceled_room else R.layout.empty_canceled_room
                    val layout3 =  inflater.inflate(view3, container, false)
                    if (have_content) {
                        cancelPurchase = layout3.findViewById(R.id.canceledList)
                        cancelPurchase.layoutManager = layoutManager
                        cancelPurchase.setHasFixedSize(true)

                        linearAdapter2 = PastCancelPurchaseAdapter(requireContext(), canceledList)
                        cancelPurchase.adapter = linearAdapter2

                        linearAdapter2.onItemClick = { contact ->
                            val intent = Intent(requireContext(), PastCancelPurchaseDetail::class.java)
                            println(contact.Purchase?.status_purchase)
                            intent.putExtra("status_purchase", contact.Purchase?.status_purchase)
                            startActivity(intent)
                        }
                    } else {
                        val btnOrder3: Button = layout3.findViewById(R.id.btn_order3)

                        btnOrder3.setOnClickListener {
                            replaceFragmentToHotel()
                        }
                    }
                    return layout3
                }
            }
        }
        return inflater.inflate(R.layout.empty_active_room, container, false)
    }

    private fun replaceFragmentToHotel() {
        val mainActivity = activity as MainActivity
        mainActivity.replaceFragment(MyHotel())
    }
}