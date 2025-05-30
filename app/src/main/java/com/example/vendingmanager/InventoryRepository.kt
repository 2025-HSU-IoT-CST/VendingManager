package com.example.vendingmanager.data

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import aws.sdk.kotlin.services.dynamodb.model.UpdateItemRequest
import com.example.vendingmanager.AwsClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/** FaceRegistrationApp3가 쓰는 테이블 이름과 필드 그대로 맞춰 주세요 */
private const val TABLE_NAME = "drink-inventory"

data class DrinkItem(
    val drinkId:    Int,           // ← 새로 추가
    val name:       String,
    val quantity:   Int,
    /** 판매량 통계를 따로 적재하고 있다면 여기에도 추가 */
    val salesCount: Int = 0
)

object InventoryRepository {

    private const val TABLE_NAME = "drink-inventory"

    /** 항상 IO dispatcher에서 실행되도록 보장 */
    suspend fun fetchAll(): List<DrinkItem> = withContext(Dispatchers.IO) {
        AwsClients.dynamo().use { db ->
            val resp = db.scan(ScanRequest {          // ← import 가 맞으면 tableName 빨간 줄 사라짐
                tableName = TABLE_NAME
            })

            resp.items?.mapNotNull { m ->
                try {
                    DrinkItem(
                        drinkId    = m["drinkId"]!!.asN().toInt(),   // ← 여기
                        name       = m["name"]!!.asS(),
                        quantity   = m["quantity"]!!.asN().toInt(),
                        salesCount = m["salesCount"]?.asN()?.toInt() ?: 0
                    )
                } catch (_: Exception) { null }
            } ?: emptyList()
        }
    }

    // 파일 맨 아래쪽 아무 곳에 붙여넣기
    suspend fun setStock(drinkId: Int, newQty: Int = 5) = withContext(Dispatchers.IO) {
        AwsClients.dynamo().use { db ->
            db.updateItem(
                UpdateItemRequest {
                    tableName = TABLE_NAME
                    key = mapOf(
                        "drinkId" to AttributeValue.N(drinkId.toString())
                    )
                    updateExpression = "SET quantity = :q"
                    expressionAttributeValues = mapOf(
                        ":q" to AttributeValue.N(newQty.toString())
                    )
                }
            )
        }
    }


}
