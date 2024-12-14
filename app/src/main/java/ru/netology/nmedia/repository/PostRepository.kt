package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post


interface PostRepository {

    fun getAll(): List<Post>
    fun likeById(id: Long)
    fun save(post: Post)
    fun removeById(id: Long)
    fun getAllAsync(callback: Callback<List<Post>>)
    fun saveAsync(post: Post, callback: Callback<Unit>)
    fun likeByIdAsync(id: Long, callback: Callback<Unit>)
    fun removeByIdAsync(id: Long, callback: Callback<Unit>)

    interface Callback<T> {
        fun onSuccess(result: T) {}
        fun onError(e: Exception) {}

    }
}