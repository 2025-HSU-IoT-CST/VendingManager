package com.example.vendingmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class MaintenanceFragment : Fragment() {

    private lateinit var tvLastCleaning: TextView
    private lateinit var tvLastMaintenance: TextView
    private lateinit var tvNextMaintenance: TextView
    private lateinit var tvMachineStatus: TextView
    private lateinit var spinnerMonth: Spinner
    private lateinit var btnSaveReport: Button

    // 라디오 그룹들
    private lateinit var radioGroupExterior: RadioGroup
    private lateinit var radioGroupDisplay: RadioGroup
    private lateinit var radioGroupDelivery: RadioGroup
    private lateinit var radioGroupPower: RadioGroup
    private lateinit var radioGroupHygiene: RadioGroup

    // 일자 포맷
    private val dateFormat = SimpleDateFormat("yyyy. MM. dd", Locale.KOREA)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maintenance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 텍스트뷰 초기화
        tvLastCleaning = view.findViewById(R.id.tvLastCleaning)
        tvLastMaintenance = view.findViewById(R.id.tvLastMaintenance)
        tvNextMaintenance = view.findViewById(R.id.tvNextMaintenance)
        tvMachineStatus = view.findViewById(R.id.tvMachineStatus)

        // 라디오 그룹 초기화
        radioGroupExterior = view.findViewById(R.id.radioGroupExterior)
        radioGroupDisplay = view.findViewById(R.id.radioGroupDisplay)
        radioGroupDelivery = view.findViewById(R.id.radioGroupDelivery)
        radioGroupPower = view.findViewById(R.id.radioGroupPower)
        radioGroupHygiene = view.findViewById(R.id.radioGroupHygiene)

        // 버튼 초기화
        btnSaveReport = view.findViewById(R.id.btnSaveReport)

        // 월 선택 스피너 설정
        setupMonthSpinner(view)

        // 초기 데이터 로드
        loadDummyData()

        // 저장 버튼 클릭 리스너
        btnSaveReport.setOnClickListener {
            saveMaintenanceReport()
        }
    }

    private fun setupMonthSpinner(view: View) {
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        val months = arrayOf(
            "1월", "2월", "3월", "4월", "5월", "6월",
            "7월", "8월", "9월", "10월", "11월", "12월"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            months
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = adapter

        // 현재 월을 기본값으로 설정
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        spinnerMonth.setSelection(currentMonth)

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadMonthlyReport(position + 1) // 1월부터 시작하도록 +1
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 아무것도 선택되지 않았을 때는 아무 작업도 하지 않음
            }
        }
    }

    private fun loadDummyData() {
        // 실제 앱에서는 DB나 서버에서 데이터를 가져옴
        val calendar = Calendar.getInstance()

        // 마지막 청소 일자 (예: 1주일 전)
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        tvLastCleaning.text = dateFormat.format(calendar.time)

        // 마지막 점검 일자 (예: 3주일 전)
        calendar.add(Calendar.DAY_OF_MONTH, -14)
        val lastMaintenanceDate = calendar.time
        tvLastMaintenance.text = dateFormat.format(lastMaintenanceDate)

        // 다음 점검 일자 계산 (마지막 점검 + 1개월)
        calendar.add(Calendar.MONTH, 1)
        tvNextMaintenance.text = dateFormat.format(calendar.time)

        // the_dark
        // 자판기 상태
        tvMachineStatus.text = "정상"
        tvMachineStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))

        // 현재 월의 점검 보고서 로드
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 0-based to 1-based
        loadMonthlyReport(currentMonth)
    }

    private fun loadMonthlyReport(month: Int) {
        // 실제 앱에서는 DB에서 해당 월의 보고서를 로드
        // 여기서는 모든 항목을 '정상'으로 기본 설정

        val radioGroups = listOf(
            radioGroupExterior,
            radioGroupDisplay,
            radioGroupDelivery,
            radioGroupPower,
            radioGroupHygiene
        )

        for (radioGroup in radioGroups) {
            // 각 라디오 그룹의 첫 번째 라디오 버튼(정상)을 체크
            val normalRadioButton = radioGroup.getChildAt(0) as RadioButton
            normalRadioButton.isChecked = true
        }

        // 저장된 보고서가 있으면 해당 데이터로 UI 업데이트
        val sharedPref = requireActivity().getSharedPreferences(
            "MaintenanceReports",
            android.content.Context.MODE_PRIVATE
        )

        val reportKey = "report_${month}"
        val reportData = sharedPref.getString(reportKey, null)

        if (reportData != null) {
            // 저장된 보고서 데이터가 있으면 파싱하여 UI에 표시
            val reportItems = reportData.split(",")

            if (reportItems.size >= 5) {
                updateRadioGroup(radioGroupExterior, reportItems[0])
                updateRadioGroup(radioGroupDisplay, reportItems[1])
                updateRadioGroup(radioGroupDelivery, reportItems[2])
                updateRadioGroup(radioGroupPower, reportItems[3])
                updateRadioGroup(radioGroupHygiene, reportItems[4])
            }
        }
    }

    private fun updateRadioGroup(radioGroup: RadioGroup, value: String) {
        val normalRadioButton = radioGroup.getChildAt(0) as RadioButton
        val badRadioButton = radioGroup.getChildAt(1) as RadioButton

        if (value == "불량") {
            badRadioButton.isChecked = true
        } else {
            normalRadioButton.isChecked = true
        }
    }

    private fun saveMaintenanceReport() {
        val month = spinnerMonth.selectedItemPosition + 1

        // 각 라디오 그룹에서 선택된 값 가져오기
        val exteriorStatus = getRadioGroupSelectedValue(radioGroupExterior)
        val displayStatus = getRadioGroupSelectedValue(radioGroupDisplay)
        val deliveryStatus = getRadioGroupSelectedValue(radioGroupDelivery)
        val powerStatus = getRadioGroupSelectedValue(radioGroupPower)
        val hygieneStatus = getRadioGroupSelectedValue(radioGroupHygiene)

        // 보고서 데이터를 쉼표로 구분된 문자열로 변환
        val reportData = "$exteriorStatus,$displayStatus,$deliveryStatus,$powerStatus,$hygieneStatus"

        // 보고서 저장
        val sharedPref = requireActivity().getSharedPreferences(
            "MaintenanceReports",
            android.content.Context.MODE_PRIVATE
        )

        with(sharedPref.edit()) {
            putString("report_$month", reportData)
            apply()
        }

        Toast.makeText(context, "${month}월 점검 보고서가 저장되었습니다.", Toast.LENGTH_SHORT).show()

        // 불량 항목이 있으면 자판기 상태 업데이트
        val hasIssue = listOf(exteriorStatus, displayStatus, deliveryStatus, powerStatus, hygieneStatus)
            .any { it == "불량" }

        if (hasIssue) {
            tvMachineStatus.text = "고장"
            tvMachineStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
        } else {
            tvMachineStatus.text = "정상"
            tvMachineStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
        }
    }

    private fun getRadioGroupSelectedValue(radioGroup: RadioGroup): String {
        val selectedId = radioGroup.checkedRadioButtonId
        val radioButton = view?.findViewById<RadioButton>(selectedId)
        return radioButton?.text?.toString() ?: "정상"
    }
}