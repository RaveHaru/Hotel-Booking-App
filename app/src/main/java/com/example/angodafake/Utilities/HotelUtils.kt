package com.example.angodafake.Utilities

import android.icu.util.Calendar
import android.util.Log
import com.example.angodafake.db.Bookmark
import com.example.angodafake.db.Chat_Room
import com.example.angodafake.db.Hotel
import com.example.angodafake.db.Rooms
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Locale

object HotelUtils {
    private lateinit var database: DatabaseReference

    init {
        database = Firebase.database.reference
    }

    fun getHotelByID(ID: String, listener: (Hotel) -> Unit){
        database.child("hotels").child(ID).get().addOnSuccessListener {
            val hotel = it.getValue(Hotel::class.java)
            hotel?.ID = ID
            if(hotel != null) {
                listener(hotel)
            }
        }.addOnFailureListener{
            Log.e("firebase", "Error getting data", it)
        }
    }
    fun getHotelList(listener: (List<Hotel>) -> Unit) {
        val hotelList = mutableListOf<Hotel>()
        database.child("hotels").get().addOnSuccessListener { dataSnapshot ->
            for (hotelSnapshot in dataSnapshot.children) {
                val hotel = hotelSnapshot.getValue(Hotel::class.java)
                hotel?.let {
                    it.ID = hotelSnapshot.key ?: ""
                    hotelList.add(it)
                }
            }
            listener(hotelList)
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error getting hotel list", exception)
            listener(emptyList()) // Trả về danh sách rỗng nếu có lỗi xảy ra
        }
    }
    fun getHotelByOwnerID(ownerID: String, listener: (MutableList<Hotel>) -> Unit) {
        val hotelsQuery = database.child("hotels")
        hotelsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val hotelsList = mutableListOf<Hotel>()
                for (hotelSnapshot in dataSnapshot.children) {
                    val hotel = hotelSnapshot.getValue(Hotel::class.java)
                    if (hotel?.ID_Owner == ownerID) {
                        hotel.ID = hotelSnapshot.key
                        hotel.let { hotelsList.add(it) }
                    }
                }
                listener(hotelsList)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun getHotelNamesByOwnerID(ownerID: String, listener: (MutableList<String>) -> Unit) {
        val hotelsQuery = database.child("hotels")
        hotelsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val hotelNameList = mutableListOf<String>()
                for (hotelSnapshot in dataSnapshot.children) {
                    val hotel = hotelSnapshot.getValue(Hotel::class.java)
                    if (hotel?.ID_Owner == ownerID) {
                        hotelNameList.add(hotel.name!!)
                    }
                }
                listener(hotelNameList)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun addHotel(hotel: Hotel, listener: (String) -> Unit){
        val key = database.child("hotels").push().key
        if (key != null){
            database.child("hotels").child(key).setValue(hotel)
            listener(key)
        } else{
            Log.e("firebase","Counldn't get push key for hotel")
        }
    }

    fun updateHotel(ID: String, hotel: Hotel, listener: (String?) -> Unit){
        database.child("hotels").child(ID).setValue(hotel)
            .addOnSuccessListener {
                // Xử lý khi cập nhật thông tin khách sạn thành công
                listener(ID)
            }
            .addOnFailureListener { exception ->
                // Xử lý khi xảy ra lỗi
                Log.e("firebase", "Couldn't update hotel information: ${exception.message}")
                listener(null)
            }
    }

    fun updateRating(hotelID: String, point: Double, money_rating: Double, location: Double, clean: Double, service: Double, convenience: Double, callback: (String) -> Unit) {
        val ratingUpdate = hashMapOf<String, Any>(
            "/hotels/$hotelID/point" to point,
            "/hotels/$hotelID/money_rating" to money_rating,
            "/hotels/$hotelID/location" to location,
            "/hotels/$hotelID/clean" to clean,
            "/hotels/$hotelID/service" to service,
            "/hotels/$hotelID/convenience" to convenience,
        )

        database.updateChildren(ratingUpdate)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.invoke("success")
                } else {
                    callback.invoke("failure")
                }
            }
    }

    fun deleteHotel(ID_hotel: String, listener: (Boolean) -> Unit) {
        var allRoomsValid = true

        database.child("rooms").child(ID_hotel).get().addOnSuccessListener { dataSnapshot ->
            dataSnapshot.children.forEach { roomSnapshot ->
                val room = roomSnapshot.getValue(Rooms::class.java)
                room?.let { room ->
                    room.ID = roomSnapshot.key
                    room.ID_Hotel = ID_hotel
                    PurchaseUtils.getPurchaseByRoom(room.ID_Hotel!!, room.ID!!, SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)) { isDateAfterGoDate ->
                        if (!isDateAfterGoDate) {
                            allRoomsValid = false
                        }
                    }
                }
            }

            // Nếu tất cả các phòng đều hợp lệ, thực hiện xóa khách sạn và các phòng
            if (allRoomsValid) {
                dataSnapshot.children.forEach { roomSnapshot ->
                    val room = roomSnapshot.getValue(Rooms::class.java)
                    room?.let { room ->
                        room.ID = roomSnapshot.key
                        room.ID_Hotel = ID_hotel
                        // Xóa phòng, ảnh, bình luận và các giao dịch liên quan
                        RoomUtils.deleteRoom(room.ID_Hotel!!, room.ID!!)
                        CommentUtils.deleteCommentByIDHotel(room.ID_Hotel!!)
                        PictureUtils.deleteRoomPictues(room.ID_Hotel!!, room.ID!!)
                        PurchaseUtils.deletePurchaseByRoomID(room.ID_Hotel!!, room.ID!!)
                    }
                }

                // Xóa khách sạn
                database.child("hotels").child(ID_hotel).setValue(null)
                    .addOnSuccessListener { listener(true) }
                    .addOnFailureListener { listener(false) }
            } else {
                listener(false)
            }
        }
    }

    fun getHotelsByNameAndIdOwner(hotelName: String, ID_owner: String, listener: (MutableList<Hotel>) -> Unit){
        val hotelsQuery = database.child("hotels")
        hotelsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val hotelsList = mutableListOf<Hotel>()
                for (hotelSnapshot in dataSnapshot.children) {
                    val hotel = hotelSnapshot.getValue(Hotel::class.java)
                    if (hotel?.name!!.toLowerCase(Locale.getDefault()).contains(hotelName.toLowerCase(Locale.getDefault()))
                        && hotel.ID_Owner == ID_owner) {
                        hotel.ID = hotelSnapshot.key
                        hotel.let { hotelsList.add(it) }
                    }
                }
                listener(hotelsList)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    fun getIDHotelByNameAndIdOwner(hotelName: String, ID_owner: String, listener: (String?) -> Unit){
        val hotelsQuery = database.child("hotels")
        hotelsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var firstHotel: String? = null
                for (hotelSnapshot in dataSnapshot.children) {
                    val hotel = hotelSnapshot.getValue(Hotel::class.java)
                    if (hotel?.name!!.equals(hotelName, ignoreCase = true)
                        && hotel.ID_Owner == ID_owner) {
                        firstHotel = hotelSnapshot.key
                        break
                    }
                }
                listener(firstHotel)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                listener(null) // Truyền giá trị null nếu có lỗi
            }
        })
    }


}