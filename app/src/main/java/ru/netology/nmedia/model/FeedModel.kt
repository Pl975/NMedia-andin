package ru.netology.nmedia.model

import ru.netology.nmedia.dto.Post

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val empty: Boolean = false,
)

enum class FeedError {
    NONE, API, NETWORK, UNKNOWN
}

data class FeedModelState(
    val loading: Boolean = false,
    val error: FeedError = FeedError.NONE,
    val refreshing: Boolean = false,
)
