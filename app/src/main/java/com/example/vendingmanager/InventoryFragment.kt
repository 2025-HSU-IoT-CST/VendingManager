package com.example.vendingmanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope      // ★ 추가
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.vendingmanager.data.InventoryRepository // ★ 추가
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch                           // ★ 추가
import java.util.concurrent.TimeUnit

class InventoryFragment : Fragment() {

    companion object {
        private const val REQUEST_POST_NOTIF = 1001
        private const val WORK_NAME = "inventory_check"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var inventoryAdapter: InventoryAdapter

    private val inventoryList = mutableListOf<InventoryItem>()
    private val originalList = mutableListOf<InventoryItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_inventory, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Android 13 이상 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_POST_NOTIF
                )
            }
        }

        // RecyclerView 설정
        recyclerView = view.findViewById(R.id.recyclerViewInventory)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        inventoryAdapter = InventoryAdapter(inventoryList) { item, pos ->
            restockItem(item, pos)
        }
        recyclerView.adapter = inventoryAdapter

        // 알림 채널 생성
        createNotificationChannel()

        // 재고 로드 및 알림
        loadInventoryFromAws()

        // 버튼 리스너
        view.findViewById<Button>(R.id.btnSortAlphabetical)
            .setOnClickListener { sortByAlphabetical() }
        view.findViewById<Button>(R.id.btnSortPopularity)
            .setOnClickListener { sortByPopularity() }
        view.findViewById<Button>(R.id.btnSortOutOfStock)
            .setOnClickListener { filterOutOfStock() }
        view.findViewById<Button>(R.id.btnOrder)
            .setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.coupang.com/")))
            }
    }

    private fun restockItem(item: InventoryItem, position: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                InventoryRepository.setStock(item.id, 5)
                inventoryAdapter.updateQuantity(position, 5)
                Toast.makeText(requireContext(),
                    "${item.productName} 재고를 5개로 맞췄습니다", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "발주 실패: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun loadInventoryFromAws() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val drinks = InventoryRepository.fetchAll()

                inventoryList.clear()
                originalList.clear()
                drinks.forEach { d ->
                    inventoryList.add(
                        InventoryItem(
                            id             = d.drinkId,
                            productName    = d.name,
                            machineStock   = d.quantity,
                            warehouseStock = 0,
                            autoOrder      = false,
                            popularity     = d.salesCount
                        )
                    )
                }
                originalList.addAll(inventoryList)
                inventoryAdapter.notifyDataSetChanged()

                val prefs = requireContext()
                    .getSharedPreferences("VendingManagerPref", Context.MODE_PRIVATE)

                // 1) 재고가 3개 이상으로 복구된 녀석들에 대해 “알림 보냈다” 플래그 제거
                drinks
                    .filter { it.quantity > 2 }
                    .forEach { d ->
                        prefs.edit().remove("NOTIFIED_${d.drinkId}").apply()
                    }

                // 2) 재고 2개 이하면서, 아직 알림 안 보냈으면 알림 + 플래그 설정
                inventoryList
                    .filter { it.machineStock <= 2 }
                    .forEach { item ->
                        val key = "NOTIFIED_${item.id}"
                        if (!prefs.getBoolean(key, false)) {
                            showLowStockNotification(item)
                            prefs.edit().putBoolean(key, true).apply()
                        }
                    }

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "재고 불러오기 실패: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun sortByAlphabetical() {
        inventoryList.sortBy { it.productName }
        inventoryAdapter.notifyDataSetChanged()
        Toast.makeText(context, "가나다순으로 정렬했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun sortByPopularity() {
        inventoryList.clear()
        inventoryList.addAll(originalList)
        inventoryList.sortByDescending { it.popularity }
        inventoryAdapter.notifyDataSetChanged()
        Toast.makeText(context, "인기 순으로 정렬했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun filterOutOfStock() {
        inventoryList.clear()
        inventoryList.addAll(originalList.filter { it.machineStock == 0 })
        inventoryAdapter.notifyDataSetChanged()
        Toast.makeText(context, "품절 제품만 표시합니다.", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.channel_id)
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                this.description = description
            }
            (requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showLowStockNotification(item: InventoryItem) {
        val channelId = getString(R.string.channel_id)
        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("재고 알림")
            .setContentText("${item.productName} 재고가 ${item.machineStock}개 남았습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(requireContext())
            .notify(item.id, notification)
    }
}

// 워커 클래스: 백그라운드에서 주기적으로 재고 체크
class InventoryCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val drinks = InventoryRepository.fetchAll()
            val prefs = applicationContext
                .getSharedPreferences("VendingManagerPref", Context.MODE_PRIVATE)

            // 복구된 재고 플래그 삭제
            drinks.filter { it.quantity > 2 }.forEach { d ->
                prefs.edit().remove("NOTIFIED_${d.drinkId}").apply()
            }

            // 저재고 알림 (최초 1회만)
            drinks.filter { it.quantity <= 2 }.forEach { d ->
                val key = "NOTIFIED_${d.drinkId}"
                if (!prefs.getBoolean(key, false)) {
                    // 권한 체크 포함
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val channelId = applicationContext.getString(R.string.channel_id)
                        val notification = NotificationCompat.Builder(applicationContext, channelId)
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("재고 알림")
                            .setContentText("${d.name} 재고가 ${d.quantity}개 남았습니다.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .build()
                        NotificationManagerCompat
                            .from(applicationContext)
                            .notify(d.drinkId, notification)
                    }
                    prefs.edit().putBoolean(key, true).apply()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// 데이터 클래스 및 어댑터 (변경 없음)

data class InventoryItem(
    val id: Int,
    val productName: String,
    var machineStock: Int,
    val warehouseStock: Int,
    var autoOrder: Boolean,
    val popularity: Int
)

class InventoryAdapter(
    private val items: List<InventoryItem>,
    private val onRestockClick: (InventoryItem, Int) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {
    inner class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvInventoryProductName)
        private val tvMachine = itemView.findViewById<TextView>(R.id.tvMachineStock)
        private val tvWarehouse = itemView.findViewById<TextView>(R.id.tvWarehouseStock)
        private val btnRestock = itemView.findViewById<Button>(R.id.restockButton)
        fun bind(item: InventoryItem) {
            tvName.text = item.productName
            tvWarehouse.text = item.warehouseStock.toString()
            tvMachine.apply {
                if (item.machineStock == 0) {
                    setTextColor(context.getColor(android.R.color.holo_red_dark))
                    text = "품절"
                } else {
                    setTextColor(context.getColor(android.R.color.black))
                    text = item.machineStock.toString()
                }
            }
            btnRestock.setOnClickListener {
                onRestockClick(item, bindingAdapterPosition)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        InventoryViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_inventory, parent, false)
        )
    override fun onBindViewHolder(holder: InventoryViewHolder, pos: Int) = holder.bind(items[pos])
    override fun getItemCount() = items.size
    fun updateQuantity(position: Int, newQty: Int) {
        items[position].machineStock = newQty
        notifyItemChanged(position)
    }
}