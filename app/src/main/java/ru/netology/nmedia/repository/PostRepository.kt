package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post


interface PostRepository {

    fun getAllAsync(callback: PostCallback<List<Post>>)
    fun likeById(id: Long, callback: PostCallback<Unit>)
    fun save(post: Post, callback: PostCallback<Unit>)
    fun removeById(id: Long, callback: PostCallback<Unit>)

    interface PostCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: Throwable)
    }
}