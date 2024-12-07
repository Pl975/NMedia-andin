package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dto.Post
import java.io.IOException
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


    override fun getAllAsync(callback: PostRepository.PostCallback<List<Post>>) {
        val request: Request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()
        client.newCall(request)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    try {
                        callback.onSuccess(gson.fromJson(body, typeToken.type))
                    } catch (e: Exception) {
                        callback.onError(e)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e)
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
        client.newCall(getThePostRequest).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: throw RuntimeException("body is null")
                try {
                    callback.onSuccess(gson.fromJson(body, postTypeToken.type))
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }
        })
    }

    private fun getResponseUnitCallback(callback: PostRepository.PostCallback<Unit>): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    callback.onSuccess(Unit)
                } catch (e: Exception) {
                    callback.onError(e)
                }
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

    override fun removeById(id: Long, callback: PostRepository.PostCallback<Unit>) {
        val request: Request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/${id}")
            .build()
        client.newCall(request).enqueue(getResponseUnitCallback(callback))
    }
}
