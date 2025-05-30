package com.example.vendingmanager

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient

object AwsClients {
    private const val REGION = "ap-northeast-2"
    // 1) IAM 사용자의 액세스 키
    private const val ACCESS_KEY = "xxxxxxxxxxxxxxxxxxxxx"
    private const val SECRET_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxx"  // 절대 깃에 올리지 마세요!

    private val provider = StaticCredentialsProvider {
        accessKeyId = ACCESS_KEY
        secretAccessKey = SECRET_KEY
    }

    fun dynamo() = DynamoDbClient {
        region = REGION
        credentialsProvider = provider
    }
}

// 새로 만든 코드