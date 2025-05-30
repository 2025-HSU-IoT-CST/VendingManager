package com.example.vendingmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// 워크매니저 import 추가
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val WORK_NAME = "inventory_check"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ─────────────────────────────────────
        // 1) 앱 실행 시 워커 스케줄링 (한 번만 등록)
        // ─────────────────────────────────────
        val workRequest =
            PeriodicWorkRequestBuilder<InventoryCheckWorker>(15, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        // ─────────────────────────────────────

        setContentView(R.layout.activity_main)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            showLoginDialog()
        }

        btnRegister.setOnClickListener {
            showRegisterDialog()
        }
    }

    private fun showLoginDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_login, null)
        val editTextVendingId = dialogView.findViewById<EditText>(R.id.editTextVendingId)
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextPassword)
        val btnLoginConfirm = dialogView.findViewById<Button>(R.id.btnLoginConfirm)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnLoginConfirm.setOnClickListener {
            val vendingId = editTextVendingId.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (vendingId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isValidLogin(vendingId, password)) {
                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()

                // 로그인 정보 저장
                val sharedPref =
                    getSharedPreferences("VendingManagerPref", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("LOGGED_IN_ID", vendingId)
                    apply()
                }

                // 다음 화면으로 이동
                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("VENDING_ID", vendingId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "로그인 실패: 잘못된 아이디 또는 비밀번호", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        alertDialog.show()
    }

    private fun showRegisterDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_register, null)
        val editTextNewVendingId =
            dialogView.findViewById<EditText>(R.id.editTextNewVendingId)
        val editTextNewPassword =
            dialogView.findViewById<EditText>(R.id.editTextNewPassword)
        val editTextNewPasswordConfirm =
            dialogView.findViewById<EditText>(R.id.editTextNewPasswordConfirm)
        val btnRegisterConfirm =
            dialogView.findViewById<Button>(R.id.btnRegisterConfirm)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnRegisterConfirm.setOnClickListener {
            val vendingId = editTextNewVendingId.text.toString().trim()
            val password = editTextNewPassword.text.toString().trim()
            val passwordConfirm =
                editTextNewPasswordConfirm.text.toString().trim()

            if (vendingId.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != passwordConfirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (registerNewVendingMachine(vendingId, password)) {
                Toast.makeText(this, "등록이 완료되었습니다", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "이미 존재하는 자판기 ID입니다", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun isValidLogin(vendingId: String, password: String): Boolean {
        val sharedPref = getSharedPreferences("VendingManagerPref", Context.MODE_PRIVATE)
        val savedPassword = sharedPref.getString(vendingId, null)
        return savedPassword != null && savedPassword == password
    }

    private fun registerNewVendingMachine(vendingId: String, password: String): Boolean {
        val sharedPref = getSharedPreferences("VendingManagerPref", Context.MODE_PRIVATE)
        if (sharedPref.contains(vendingId)) return false
        with(sharedPref.edit()) {
            putString(vendingId, password)
            apply()
        }
        return true
    }
}
