package com.example.vendingmanager

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class DashboardActivity : AppCompatActivity() {
    private lateinit var vendingId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // 전달받은 자판기 ID 정보 가져오기
        vendingId = intent.getStringExtra("VENDING_ID") ?: "Unknown"

        // 자판기 ID를 화면에 표시
        val tvVendingId = findViewById<TextView>(R.id.tvVendingId)
        tvVendingId.text = vendingId

        // 카테고리 스피너 설정
        setupCategorySpinner()

        // 기본 화면으로 분석 페이지 로드
        loadFragment(AnalysisFragment())
    }

    private fun setupCategorySpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        val categories = arrayOf("분석", "재고관리", "유지보수")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadFragment(AnalysisFragment())
                    1 -> loadFragment(InventoryFragment())
                    2 -> loadFragment(MaintenanceFragment())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 아무것도 선택되지 않았을 때는 아무 작업도 하지 않음
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}