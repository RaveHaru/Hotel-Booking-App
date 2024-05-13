package com.example.angodafake.Utilities

import android.util.Log
import com.example.angodafake.db.User
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await

object UserUtils {
    private var database: DatabaseReference = Firebase.database.reference
    fun getUserByID(ID: String, listener: (User?) -> Unit){
        val usersQuery = database.child("users/$ID")
        usersQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Xử lý khi dữ liệu thay đổi
                val user = dataSnapshot.getValue(User::class.java)
                if (user != null)
                    listener(user)
                else
                    listener(null)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
            }
        })
    }

    fun getNameByID(ID: String, listener: (String) -> Unit){
        val usersQuery = database.child("users").child(ID)
        usersQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Xử lý khi dữ liệu thay đổi
                val user = dataSnapshot.getValue(User::class.java)
                if (user == null){
                    listener("User")
                }
                else{
                    listener(user.name!!)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
            }
        })
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            val dataSnapshot = database.child("users").get().await()
            var user: User? = null
            dataSnapshot.children.forEach { userSnapshot ->
                val currentUser = userSnapshot.getValue(User::class.java)
                if (currentUser?.email == email) {
                    currentUser.ID = userSnapshot.key
                    user = currentUser
                    return@forEach
                }
            }
            user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByPhoneNumber(phoneN: String): User? {
        return try {
            val dataSnapshot = database.child("users").get().await()
            var user: User? = null
            dataSnapshot.children.forEach { userSnapshot ->
                val currentUser = userSnapshot.getValue(User::class.java)
                if (currentUser?.phoneN == phoneN) {
                    currentUser.ID = userSnapshot.key
                    user = currentUser
                    return@forEach
                }
            }
            user
        } catch (e: Exception) {
            null
        }
    }
    fun getUserByPhoneNumber(phoneN: String, listener: (User?) -> Unit){
        val usersQuery = database.child("users")
        usersQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var user : User ?= null
                for (userSnapshort in dataSnapshot.children){
                    val tmp = userSnapshort.getValue(User::class.java)
                    if (tmp!!.phoneN == phoneN){
                        tmp.ID = userSnapshort.key
                        user = tmp
                        break
                    }
                }
                listener(user)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
            }
        })
    }

    fun updateUserByID(ID: String, newUser: User, pw: String, listener: (Boolean) -> Unit){
        val usersQuery = database.child("users/$ID")
        usersQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val oldUser = dataSnapshot.getValue(User::class.java)
                val user = Firebase.auth.currentUser
                val email = if (oldUser!!.email == ""){
                    "${oldUser.phoneN}@gmail.com"
                } else{
                    oldUser.email!!
                }
                if (pw != ""){
                    val credential = EmailAuthProvider.getCredential(email, pw)
                    user!!.reauthenticate(credential).addOnCompleteListener {
                        if (it.isSuccessful){
                            user.updateEmail(newUser.email!!)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful){
                                        database.child("users").child(ID).setValue(newUser)
                                        listener(true)
                                    } else{
                                        listener(false)
                                    }
                                }
                        } else{
                            listener(false)
                        }
                    }
                } else{
                    database.child("users").child(ID).setValue(newUser)
                    listener(true)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
            }
        })
    }

    fun addUser(user: User, listener: (String) -> Unit){
        val key = database.child("users").push().key
        if (key != null){
            database.child("users").child(key).setValue(user)
            listener(key)
        } else{
            Log.e("firebase","Counldn't get push key for user")
        }
    }
}