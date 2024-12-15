package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Post
import java.util.concurrent.TimeUnit


class PostRepositoryImpl : PostRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val typeToken = object : TypeToken<List<Post>>() {}
    private val postTypeToken = object : TypeToken<Post>() {}

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val jsonType = "application/json".toMediaType()
    }


    override fun getAll(): List<Post> {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        return client.newCall(request)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let {
                gson.fromJson(it, typeToken.type)
            }
    }

    override fun getAllAsync(callback: PostRepository.Callback<List<Post>>) {
        PostsApi.retrofitService.getAll().enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (!response.isSuccessful) {
                    callback.onError(java.lang.RuntimeException(response.message()))
                    return
                }
                callback.onSuccess(
                    response.body() ?: throw java.lang.RuntimeException("body is null")
                )
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                callback.onError(RuntimeException(t))
            }
        })

    }

    override fun likeById(id: Long, callback: PostRepository.PostCallback<Unit>) {
        getPostByIdAsync(id, object : PostRepository.PostCallback<Post> {

            override fun onSuccess(result: Post) {
                val request: Request = Request.Builder()
                    .postOrDelete(result.likedByMe, gson.toJson(id).toRequestBody(jsonType))
                    .url("${BASE_URL}/api/posts/${id}/likes")
                    .build()
                client.newCall(request).enqueue(getResponseUnitCallback(callback))
            }

            override fun onError(error: Throwable) {
                callback.onError(error)
            }

        })
    }

    private fun getPostByIdAsync(id: Long, callback: PostRepository.PostCallback<Post>) {
        val getThePostRequest = Request.Builder()
            .url("${BASE_URL}/api/posts/${id}")
            .build()

        val post = client.newCall(getThePostRequest)
            .execute()
            .let { it.body?.string() ?: throw RuntimeException("body is null") }
            .let { gson.fromJson(it, Post::class.java) }

        val request: Request = Request.Builder()
            .postOrDelete(post.likedByMe, gson.toJson(id).toRequestBody(jsonType))
            .url("${BASE_URL}/api/posts/${id}/likes")
            .build()

        client.newCall(request)
            .execute()
            .close()
    }

    override fun likeByIdAsync(id: Long, callback: PostRepository.Callback<Unit>) {
        getPostByIdAsync(id, object : PostRepository.Callback<Post> {
            override fun onSuccess(result: Post) {
                val call = if (result.likedByMe) {
                    PostsApi.retrofitService.dislikeById(id)
                } else {
                    PostsApi.retrofitService.likeById(id)
                }
                call.enqueue(getResponseUnitCallback(callback))
            }

            override fun onError(e: Exception) {
                callback.onError(e)
            }
        })


    }

    private fun getPostByIdAsync(id: Long, callback: PostRepository.Callback<Post>) {
        PostsApi.retrofitService.getById(id).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful) {
                    callback.onError(java.lang.RuntimeException(response.message()))
                    return
                }
                callback.onSuccess(
                    response.body() ?: throw java.lang.RuntimeException("body is null")
                )
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onError(RuntimeException(t))
            }
        })
    }

    private fun getResponseUnitCallback(callback: PostRepository.Callback<Unit>): Callback<Unit> {
        return object : Callback<Unit> {
            override fun onFailure(call: Call<Unit>, t: Throwable) {
                callback.onError(RuntimeException(t))
            }

            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (!response.isSuccessful) {
                    callback.onError(java.lang.RuntimeException(response.message()))
                    return
                }
                callback.onSuccess(Unit)
            }
        }
    }


    private fun Request.Builder.postOrDelete(likedByMe: Boolean, rb: RequestBody): Request.Builder {
        if (likedByMe) {
            delete(rb)
        } else {
            post(rb)
        }
        return this
    }

    override fun save(post: Post, callback: PostRepository.PostCallback<Unit>) {
        val request: Request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts")
            .build()
        client.newCall(request).enqueue(getResponseUnitCallback(callback))
    }


    override fun saveAsync(post: Post, callback: PostRepository.Callback<Unit>) {

        PostsApi.retrofitService.save(post).enqueue(getResponseUnitCallback(callback))
    }

    override fun removeById(id: Long) {

        val request: Request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/${id}")
            .build()
        client.newCall(request).enqueue(getResponseUnitCallback(callback))
    }

    override fun removeByIdAsync(id: Long, callback: PostRepository.Callback<Unit>) {
        PostsApi.retrofitService.removeById(id).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (!response.isSuccessful) {
                    callback.onError(java.lang.RuntimeException(response.message()))
                    return
                }
                callback.onSuccess(
                    response.body() ?: throw java.lang.RuntimeException("body is null")
                )
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                callback.onError(RuntimeException(t))
            }
        })
    }
}

