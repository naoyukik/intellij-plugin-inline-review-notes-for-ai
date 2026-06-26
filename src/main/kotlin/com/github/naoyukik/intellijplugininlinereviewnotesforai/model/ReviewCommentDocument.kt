package com.github.naoyukik.intellijplugininlinereviewnotesforai.model

import kotlinx.serialization.Serializable

@Serializable
data class ReviewCommentDocument(
    val version: String = VERSION,
    val comments: List<ReviewComment> = emptyList(),
) {
    companion object {
        const val VERSION = "1.0"
    }
}
