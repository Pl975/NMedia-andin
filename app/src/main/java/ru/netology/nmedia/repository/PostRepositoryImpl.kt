package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException


class PostRepositoryImpl(private val dao: PostDao) : PostRepository {

    override val data: LiveData<List<Post>> = dao.getAll().map { it.toDto() }
    private var retryFun: RetryInterface? = null
    private fun clearRetryFun() {
        retryFun = null
    }


    override suspend fun getAll() {
        try {
            val response = PostsApi.retrofitService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val posts = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(posts.toEntity())
        } catch (e: ApiError) {
            throw e
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.retrofitService.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: ApiError) {
            throw e
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        dao.likeById(id)
        likeByIdOnServer(id)
    }

    private suspend fun likeByIdOnServer(id: Long) {
        try {
            val getThePostResponse = PostsApi.retrofitService.getById(id)
            if (!getThePostResponse.isSuccessful) {
                throw ApiError(getThePostResponse.code(), getThePostResponse.message())
            }
            val body = getThePostResponse.body() ?: throw ApiError(
                getThePostResponse.code(),
                getThePostResponse.message()
            )
            val likeResponse =
                if (body.likedByMe) PostsApi.retrofitService.dislikeById(id)
                else PostsApi.retrofitService.likeById(id)
            if (!likeResponse.isSuccessful) {
                throw ApiError(likeResponse.code(), likeResponse.message())
            }

            clearRetryFun()
        } catch (e: ApiError) {
            throw e
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        dao.removeById(id)
        removeByIdOnServer(id)
    }

    private suspend fun removeByIdOnServer(id: Long) {
        try {
            val response = PostsApi.retrofitService.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            clearRetryFun()
        } catch (e: ApiError) {
            throw e
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError

        }
    }
}

fun interface RetryInterface {
    suspend fun retry()
}

