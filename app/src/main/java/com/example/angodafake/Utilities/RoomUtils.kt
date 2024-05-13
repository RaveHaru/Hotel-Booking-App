package com.example.angodafake.Utilities


import android.util.Log
import com.example.angodafake.db.Hotel
import com.example.angodafake.db.Purchase
import com.example.angodafake.db.Rooms
import com.example.angodafake.db.User
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

object RoomUtils {
    private var database: DatabaseReference = Firebase.database.reference

    fun getRoomByHotelID(ID_Hotel: String, listener: (List<Rooms>) -> Unit) {
        val roomList = mutableListOf<Rooms>()
        database.child("rooms").child(ID_Hotel).get().addOnSuccessListener { dataSnapshot ->
            for (roomSnapshot in dataSnapshot.children) {
                val room = roomSnapshot.getValue(Rooms::class.java)
                room?.let {
                    it.ID = roomSnapshot.key
                    it.ID_Hotel = ID_Hotel
                    roomList.add(it)
                }
            }
            listener(roomList)
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error getting room list", exception)
            listener(emptyList()) // Trả về danh sách rỗng nếu có lỗi xảy ra
        }
    }

    fun getRoomByID(ID_Hotel: String, roomID: String, listener: (Rooms) -> Unit) {
        database.child("rooms").child(ID_Hotel).child(roomID).get().addOnSuccessListener { dataSnapshot ->
            val room = dataSnapshot.getValue(Rooms::class.java)
            if (room != null) {
                listener(room)
            }
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error getting room by ID", exception)
        }
    }


    fun getRoomList(listener: (ArrayList<Rooms>) -> Unit) {
        val roomList = ArrayList<Rooms>()
        database.child("rooms").get().addOnSuccessListener { dataSnapshot ->
            for (roomSnapshot in dataSnapshot.children) {
                val roomArrayList = roomSnapshot.getValue(object : GenericTypeIndicator<ArrayList<Rooms>>() {})
                roomArrayList?.let {
                    for (room in it) {
                        roomList.add(room)
                    }
                }
            }
            listener(roomList)
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error getting hotel list", exception)
            listener(ArrayList()) // Trả về danh sách rỗng nếu có lỗi xảy ra
        }
    }

    fun getRoomsFromDatabase(listener: (List<Rooms>) -> Unit) {
        val roomsRef = database.child("rooms")

        roomsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val roomList = mutableListOf<Rooms>()

                for (roomSnapshot in dataSnapshot.children) {
                    for (roomDataSnapshot in roomSnapshot.children) {
                        val room = roomDataSnapshot.getValue(Rooms::class.java)
                        room?.let {
                            it.ID_Hotel = roomSnapshot.key
                            it.ID = roomDataSnapshot.key
                            roomList.add(it)
                        }
                    }
                }

                // Gửi danh sách phòng tới listener
                listener(roomList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
            }
        })
    }

    fun addRoom(room: Rooms, idHotel: String, listener: (String) -> Unit){
        val key = database.child("rooms").child(idHotel).push().key
        if (key != null){
            database.child("rooms").child(idHotel).child(key).setValue(room)
            listener(key)
        } else{
            Log.e("firebase","Counldn't get push key for hotel")
        }
    }

    fun getRoomQtyByHotelID(ID_Hotel: String, listener: (Int?) -> Unit){
        val roomQuery = database.child("rooms").child(ID_Hotel)
        roomQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var count = 0
                for (roomSnapshort in dataSnapshot.children){
                    val room = roomSnapshort.getValue(Rooms::class.java)
                    count += room?.quantity ?: 0
                }
                listener(count)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                Log.e("firebase", "Error getting room count: ${databaseError.message}")
                listener(null)
            }
        })
    }

    fun updateRoom(ID_Hotel: String, ID_Room: String, room: Rooms, listener: (String?) -> Unit){
        database.child("rooms").child(ID_Hotel).child(ID_Room).setValue(room)
            .addOnSuccessListener {
                // Xử lý khi cập nhật thông tin khách sạn thành công
                listener(ID_Room)
            }
            .addOnFailureListener { exception ->
                // Xử lý khi xảy ra lỗi
                Log.e("firebase", "Couldn't update hotel information: ${exception.message}")
                listener(null)
            }
    }

    fun deleteRoom(ID_Hotel: String, ID_Room: String){
        database.child("rooms").child(ID_Hotel).child(ID_Room).setValue(null)
    }

    fun getRoomsByType(ID_Hotel: String, roomType: String, listener: (List<Rooms>) -> Unit){
        val roomList = mutableListOf<Rooms>()
        database.child("rooms").child(ID_Hotel).get().addOnSuccessListener { dataSnapshot ->
            for (roomSnapshot in dataSnapshot.children) {
                val room = roomSnapshot.getValue(Rooms::class.java)
                if (room?.type!!.toLowerCase(Locale.getDefault()).contains(roomType.toLowerCase(Locale.getDefault()))){
                    room.let {
                        it.ID = roomSnapshot.key
                        it.ID_Hotel = ID_Hotel
                        roomList.add(it)
                    }
                }
            }
            listener(roomList)
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error getting room list", exception)
            listener(emptyList()) // Trả về danh sách rỗng nếu có lỗi xảy ra
        }
    }

    fun getTypeByHotelID(ID_Hotel: String, listener: (List<String>) -> Unit){
        val typeList = mutableListOf<String>()
        database.child("rooms").child(ID_Hotel).get().addOnSuccessListener { dataSnapshot ->
            for (roomSnapshot in dataSnapshot.children) {
                val room = roomSnapshot.getValue(Rooms::class.java)
                typeList.add(room?.type!!)
            }
            listener(typeList)
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error getting room list", exception)
            listener(emptyList()) // Trả về danh sách rỗng nếu có lỗi xảy ra
        }
    }

    fun getQuantityAndPriceByType(ID_Hotel: String, type: String, listener: (String, Int, Int) -> Unit){
        database.child("rooms").child(ID_Hotel).get().addOnSuccessListener { dataSnapshot ->
            var id = ""
            var quantity = 0
            var price = 0
            for (roomSnapshot in dataSnapshot.children) {
                val room = roomSnapshot.getValue(Rooms::class.java)
                if (room?.type == type){
                    id = roomSnapshot.key!!
                    quantity = room.quantity!!
                    price = room.price!!
                    break
                }
            }
            listener(id, quantity, price)
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error getting room list", exception)
            listener("", 0, 0) // Trả về danh sách rỗng nếu có lỗi xảy ra
        }
    }

}