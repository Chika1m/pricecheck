package com.belovedfx.pricecheck

data class Product(
    var id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val previousPrice: Double? = null,
    val priceUpdatedAt: Long = 0L,
    val imageUrl: String? = null,
    val addedAt: Long = 0L
)
