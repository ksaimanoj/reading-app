package com.littlewords.app.domain

enum class Category {
    TWO_REAL,
    THREE_REAL,
    THREE_SILLY,
    FOUR_REAL,
    FIVE_REAL,
}

data class Word(
    val text: String,
    val category: Category,
    val patterns: Set<String>,
)

data class PracticeConfig(
    val enabledCategories: Set<Category> = setOf(
        Category.TWO_REAL,
        Category.THREE_REAL,
        Category.THREE_SILLY,
    ),
    val letters: String = "abcdefghijklmnopqrstuvwxyz",
    val patterns: Set<String> = setOf(
        "short_a",
        "short_e",
        "short_i",
        "short_o",
        "short_u",
    ),
)
