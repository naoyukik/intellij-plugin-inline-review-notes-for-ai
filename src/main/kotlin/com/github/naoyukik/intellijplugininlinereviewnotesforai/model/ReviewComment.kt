package com.github.naoyukik.intellijplugininlinereviewnotesforai.model

import kotlinx.serialization.Serializable

@Serializable
data class ReviewComment(
    val id: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val comment: String,
    val createdAt: String,
    val resolvedAt: String? = null,
    val isOutdated: Boolean = false,
)
