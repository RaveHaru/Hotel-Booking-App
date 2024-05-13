package com.example.angodafake.Utilities

import android.util.Log
import com.example.angodafake.db.Hotel
import com.example.angodafake.db.Purchase
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object PurchaseUtils {
    private lateinit var database: DatabaseReference

    init {
        database = Firebase.database.reference
    }
    private fun getTimePurchase(): String {
        val currentTime = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(currentTime.time)
    }

    fun updatePurchaseStatus(purchaseID: String, callback: (String) -> Unit) {
        getPurchaseByID(purchaseID){
            if (it.time_purchase == ""){
                val purchaseUpdate = hashMapOf<String, Any>(
                    "/purchases/$purchaseID/detail" to "HOAN_TAT",
                    "/purchases/$purchaseID/status_purchase" to "DA_THANH_TOAN",
                    "/purchases/$purchaseID/time_purchase" to getTimePurchase()
                )
                database.updateChildren(purchaseUpdate)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback.invoke("success")
                        } else {
                            callback.invoke("failure")
                        }
                    }
            } else {
                val purchaseUpdate = hashMapOf<String, Any>(
                    "/purchases/$purchaseID/detail" to "HOAN_TAT",
                    "/purchases/$purchaseID/status_purchase" to "DA_THANH_TOAN"
                )
                database.updateChildren(purchaseUpdate)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback.invoke("success")
                        } else {
                            callback.invoke("failure")
                        }
                    }
            }
        }
    }

    fun getAllPurchases(ownerID: String, listener: (List<Purchase>) -> Unit) {
        val purchasesQuery = database.child("purchases")

        purchasesQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val purchasesList = mutableListOf<Purchase>()
                for (purchaseSnapshot in dataSnapshot.children) {
                    val purchase = purchaseSnapshot.getValue(Purchase::class.java)
                    if (purchase?.ID_Owner == ownerID) {
                        purchase.ID = purchaseSnapshot.key
                        purchase.let { purchasesList.add(it) }
                    }
                }
                listener(purchasesList)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
    fun getAllPurchasesByHotelID(ownerID: String, listener: (List<Purchase>) -> Unit) {
        val purchasesQuery = database.child("purchases")

        purchasesQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val purchasesList = mutableListOf<Purchase>()
                for (purchaseSnapshot in dataSnapshot.children) {
                    val purchase = purchaseSnapshot.getValue(Purchase::class.java)
                    if (purchase != null && purchase.ID_Hotel == ownerID) {
                        purchase.ID = purchaseSnapshot.key
                        purchasesList.add(purchase)
                    }
                }
                listener(purchasesList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
            }
        })
    }

    fun getPurchaseByID(ID: String, listener: (Purchase) -> Unit){
        database.child("purchases").child(ID).get().addOnSuccessListener {
            val purchase = it.getValue(Purchase::class.java)
            purchase?.ID = ID
            if(purchase != null) {
                listener(purchase)
            }
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }

    fun cancelPurchase(purchaseID: String, detail: String, reason: String, status_purchase: String, time_cancel: String, callback: (String) -> Unit) {
        val purchaseUpdate = hashMapOf<String, Any>(
            "/purchases/$purchaseID/detail" to detail,
            "/purchases/$purchaseID/reason" to reason,
            "/purchases/$purchaseID/status_purchase" to status_purchase,
            "/purchases/$purchaseID/time_cancel" to time_cancel,
        )

        database.updateChildren(purchaseUpdate)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.invoke("success")
                } else {
                    callback.invoke("failure")
                }
            }
    }

    fun getBookedRoomBillsByHotelID(ID_Hotel: String, date: String, listener: (List<Purchase>?, Int) -> Unit){
        val billQuery = database.child("purchases")
        billQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val purchaseList = mutableListOf<Purchase>()
                var count = 0
                for (purchaseSnapshort in dataSnapshot.children){
                    val purchase = purchaseSnapshort.getValue(Purchase::class.java)
                    if (purchase?.ID_Hotel == ID_Hotel
                        && (purchase.time_cancel == "" || purchase.time_cancel == null)
                        && isDateInRange(date, purchase.date_come!!, purchase.date_go!!)
                    ) {
                        purchase.ID = purchaseSnapshort.key
                        purchaseList.add(purchase)
                        count += purchase.quantity ?: 0
                    }
                }
                listener(purchaseList, count)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error getting room count: ${databaseError.message}")
                listener(null, 0)
            }
        })
    }

    fun getBookedRoomBillsByHotelIDAndBookedDate(ID_Hotel: String, date: String, listener: (List<Purchase>?, Int) -> Unit){
        val billQuery = database.child("purchases")
        billQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val purchaseList = mutableListOf<Purchase>()
                var count = 0
                for (purchaseSnapshort in dataSnapshot.children){
                    val purchase = purchaseSnapshort.getValue(Purchase::class.java)
                    if (purchase?.ID_Hotel == ID_Hotel
                        && (purchase.time_cancel == "" || purchase.time_cancel == null)
                        && SimpleDateFormat("dd/MM/yyyy",
                            Locale.getDefault()).format(SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).parse(purchase.time_booking!!)!!)
                        == SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(date)!!)
                    ) {
                        purchase.ID = purchaseSnapshort.key
                        purchaseList.add(purchase)
                        count += purchase.quantity ?: 0
                    }
                }
                listener(purchaseList, count)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error getting room count: ${databaseError.message}")
                listener(null, 0)
            }
        })
    }
    fun getBookedRoomBillsByRoomIDAndBookedDate(ID_Hotel: String, ID_Room: String, date: String, listener: (List<Purchase>?, Int) -> Unit){
        val billQuery = database.child("purchases")
        billQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val purchaseList = mutableListOf<Purchase>()
                var count = 0
                for (purchaseSnapshort in dataSnapshot.children){
                    val purchase = purchaseSnapshort.getValue(Purchase::class.java)
                    if (purchase?.ID_Hotel == ID_Hotel
                        && (purchase.time_cancel == "" || purchase.time_cancel == null)
                        && purchase.ID_Room == ID_Room
                        && SimpleDateFormat("dd/MM/yyyy",
                            Locale.getDefault()).format(SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).parse(purchase.time_booking!!)!!)
                        == SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(date)!!)
                    ) {
                        purchase.ID = purchaseSnapshort.key
                        purchaseList.add(purchase)
                        count += purchase.quantity ?: 0
                    }
                }
                listener(purchaseList, count)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error getting room count: ${databaseError.message}")
                listener(null, 0)
            }
        })
    }

    fun getBookedRoomBillsByRoomIDAndDate(ID_Hotel: String, ID_Room: String, dateCome: String, dateGo: String, listener: (Int) -> Unit){
        val billQuery = database.child("purchases")
        billQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var count = 0
                for (purchaseSnapshort in dataSnapshot.children){
                    val purchase = purchaseSnapshort.getValue(Purchase::class.java)
                    if (purchase?.ID_Hotel == ID_Hotel
                        && (purchase.time_cancel == "" || purchase.time_cancel == null)
                        && purchase.ID_Room == ID_Room
                        && (isDateInRange(purchase.date_come!!, dateCome, dateGo)
                                || isDateInRange(purchase.date_go!!, dateCome, dateGo))
                    ) {
                        purchase.ID = purchaseSnapshort.key
                        count += purchase.quantity ?: 0
                    }
                }
                listener(count)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error getting room count: ${databaseError.message}")
                listener(0)
            }
        })
    }

    fun getBookedRoomBillsByRoomID(ID_Hotel: String, ID_Room: String, date: String, listener: (List<Purchase>?, Int) -> Unit){
        val billQuery = database.child("purchases")
        billQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val purchaseList = mutableListOf<Purchase>()
                var count = 0
                for (purchaseSnapshort in dataSnapshot.children){
                    val purchase = purchaseSnapshort.getValue(Purchase::class.java)
                    if (purchase?.ID_Hotel == ID_Hotel
                        && (purchase.time_cancel == "" || purchase.time_cancel == null)
                        && purchase.ID_Room == ID_Room
                        && isDateInRange(date, purchase.date_come!!, purchase.date_go!!)
                    ) {
                        purchase.ID = purchaseSnapshort.key
                        purchaseList.add(purchase)
                        count += purchase.quantity ?: 0
                    }
                }
                listener(purchaseList, count)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error getting room count: ${databaseError.message}")
                listener(null, 0)
            }
        })
    }

    fun isDateInRange(date: String, startDate: String, endDate: String): Boolean {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateObj = format.parse(date)
        val startDateObj = format.parse(startDate)
        val endDateObj = format.parse(endDate)
        return dateObj in startDateObj..endDateObj
    }

    fun getAllPurchaseByIDHotelOwner(hotelOwner: String, listener: (List<Purchase>?) -> Unit){
        val billQuery = database.child("purchases")
        billQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val purchaseList = mutableListOf<Purchase>()
                val hotelMap = mutableMapOf<String, Hotel>()
                var completedCount = 0 // Số lượng yêu cầu hoàn thành

                HotelUtils.getHotelByOwnerID(hotelOwner) { hotels ->
                    hotels.forEach { hotel ->
                        hotelMap[hotel.ID!!] = hotel
                    }

                    for (purchaseSnapshort in dataSnapshot.children){
                        val purchase = purchaseSnapshort.getValue(Purchase::class.java)
                        val hotelID = purchase?.ID_Hotel

                        if (hotelID == null || !hotelMap.containsKey(hotelID)) {
                            completedCount++

                            if (completedCount == dataSnapshot.childrenCount.toInt()) {
                                listener(purchaseList)
                            }
                            continue
                        }

                        if (hotelMap[hotelID]!!.ID_Owner == hotelOwner){
                            purchase.ID = purchaseSnapshort.key
                            purchaseList.add(purchase)
                        }

                        completedCount++

                        if (completedCount == dataSnapshot.childrenCount.toInt()) {
                            listener(purchaseList)
                        }
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error getting room count: ${databaseError.message}")
                listener(null)
            }
        })
    }

    fun getPurchaseByRoom(ID_hotel: String, ID_Room : String, date: String, listener: (Boolean) -> Unit){
        val billQuery = database.child("purchases")
        billQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var isDateAfterGoDate = true
                for (purchaseSnapshort in dataSnapshot.children) {
                    val purchase = purchaseSnapshort.getValue(Purchase::class.java)
                    if (purchase?.ID_Hotel == ID_hotel
                        && (purchase.time_cancel == "" || purchase.time_cancel == null)
                        && purchase.ID_Room == ID_Room
                    ) {
                        // Kiểm tra xem ngày `date` có lớn hơn hoặc bằng ngày `date_go` không
                        if (!isDateAfter(purchase.date_go!!, date)) {
                            isDateAfterGoDate = false
                            break
                        }
                    }
                }
                listener(isDateAfterGoDate)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error getting room count: ${databaseError.message}")
                listener(false)
            }
        })
    }
    fun isDateAfter(compareDate: String, date: String): Boolean {
        val compareDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val compareDateTime = compareDateFormat.parse(compareDate)
        val dateTime = compareDateFormat.parse(date)
        return dateTime?.after(compareDateTime) ?: false
    }

    fun deletePurchaseByRoomID(ID_Hotel: String, ID_Room: String){
        val billQuery = database.child("purchases")
        billQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (purchaseSnapshort in dataSnapshot.children){
                    val purchase = purchaseSnapshort.getValue(Purchase::class.java)
                    if (purchase?.ID_Hotel == ID_Hotel
                        && purchase.ID_Room == ID_Room
                    ) {
                        database.child("purchases").child(purchaseSnapshort.key!!).setValue(null)
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error delete purchase: ${databaseError.message}")
            }
        })
    }

    fun addPurchase(bill: Purchase, listener: (String) -> Unit){
        val key = database.child("purchases").push().key
        if (key != null){
            database.child("purchases").child(key).setValue(bill)
            listener(key)
        } else{
            Log.e("firebase","Counldn't get push key for user")
        }
    }

}