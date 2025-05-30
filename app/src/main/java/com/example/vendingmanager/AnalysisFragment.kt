package com.example.vendingmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.vendingmanager.data.InventoryRepository


class AnalysisFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAnalysisAdapter
    private val productList = mutableListOf<Product>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analysis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewProducts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 예시 데이터 추가
        fetchInventory()

        productAdapter = ProductAnalysisAdapter(productList) { product ->
            showSalesDetailDialog(product)
        }
        recyclerView.adapter = productAdapter
    }

    /** DynamoDB에서 현재 재고를 읽어와 UI 새로고침 */
    private fun fetchInventory() = viewLifecycleOwner.lifecycleScope.launch {
        // 1. 네트워크 I/O
        val drinks = InventoryRepository.fetchAll()

        // 2. 기존 리스트 갱신
        productList.clear()
        drinks.forEach { di ->
            productList.add(
                Product(
                    name        = di.name,
                    salesCount  = di.salesCount,   // 따로 없으면 0
                    stockCount  = di.quantity,
                    ageGroups   = emptyMap()       // 통계 필요 없으면 빈 맵
                )
            )
        }

        // 3. 어댑터에 알리기
        productAdapter.notifyDataSetChanged()
    }


    private fun showSalesDetailDialog(product: Product) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sales_details, null)

        // 제품명 설정
        val tvProductName = dialogView.findViewById<TextView>(R.id.tvProductDetailName)
        tvProductName.text = product.name

        // 총 판매 수량 설정
        val progressBarTotal = dialogView.findViewById<ProgressBar>(R.id.progressBarTotalSales)
        val tvTotalSales = dialogView.findViewById<TextView>(R.id.tvTotalSales)
        progressBarTotal.max = 150  // 적절한 최대값 설정
        progressBarTotal.progress = product.salesCount
        tvTotalSales.text = product.salesCount.toString()

        // 연령별 데이터 설정
        setupAgeGroupData(dialogView, "20F", product.ageGroups["20F"] ?: 0)
        setupAgeGroupData(dialogView, "20M", product.ageGroups["20M"] ?: 0)
        setupAgeGroupData(dialogView, "30F", product.ageGroups["30F"] ?: 0)
        setupAgeGroupData(dialogView, "30M", product.ageGroups["30M"] ?: 0)
        setupAgeGroupData(dialogView, "40F", product.ageGroups["40F"] ?: 0)
        setupAgeGroupData(dialogView, "40M", product.ageGroups["40M"] ?: 0)
        setupAgeGroupData(dialogView, "50F", product.ageGroups["50F"] ?: 0)
        setupAgeGroupData(dialogView, "50M", product.ageGroups["50M"] ?: 0)

        // 닫기 버튼 설정
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDetails)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnClose.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun setupAgeGroupData(view: View, ageGroupCode: String, value: Int) {
        val progressBarId = resources.getIdentifier("progressBar$ageGroupCode", "id", requireContext().packageName)
        val textViewId = resources.getIdentifier("tv$ageGroupCode", "id", requireContext().packageName)

        val progressBar = view.findViewById<ProgressBar>(progressBarId)
        val textView = view.findViewById<TextView>(textViewId)

        progressBar.max = 50  // 적절한 최대값 설정
        progressBar.progress = value
        textView.text = value.toString()
    }
}

// 제품 데이터 클래스
data class Product(
    val name: String,
    val salesCount: Int,
    val stockCount: Int,
    val ageGroups: Map<String, Int> = mapOf()
)

// RecyclerView용 어댑터
class ProductAnalysisAdapter(
    private val products: List<Product>,
    private val onSalesDetailClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAnalysisAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_analysis, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val progressBarSales: ProgressBar = itemView.findViewById(R.id.progressBarSales)
        private val tvSalesCount: TextView = itemView.findViewById(R.id.tvSalesCount)
        private val progressBarStock: ProgressBar = itemView.findViewById(R.id.progressBarStock)
        private val tvStockCount: TextView = itemView.findViewById(R.id.tvStockCount)
        private val btnSalesDetails: Button = itemView.findViewById(R.id.btnSalesDetails)

        fun bind(product: Product) {
            tvProductName.text = product.name

            // 판매 수량 설정
            progressBarSales.max = 100  // 적절한 최대값 설정
            progressBarSales.progress = product.salesCount
            tvSalesCount.text = product.salesCount.toString()

            // 재고 수량 설정
            progressBarStock.max = 5  // 적절한 최대값 설정
            progressBarStock.progress = product.stockCount
            tvStockCount.text = product.stockCount.toString()

            // 상세 정보 버튼 클릭 리스너
            btnSalesDetails.setOnClickListener {
                onSalesDetailClick(product)
            }
        }
    }
}