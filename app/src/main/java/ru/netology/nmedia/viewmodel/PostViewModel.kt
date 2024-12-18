package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import ru.netology.nmedia.model.FeedError
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent


private val empty = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    likes = 0,
    published = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao())

    val data = repository.data.map { FeedModel(posts = it, empty = it.isEmpty()) }

    private val _dataState = MutableLiveData(FeedModelState())
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    //Загрузка постов
    fun loadPosts() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModelState(loading = true)
                repository.getAll()
                _dataState.value = FeedModelState()
            } catch (e: AppError) {
                when (e) {
                    is ApiError -> _dataState.value = FeedModelState(error = FeedError.API)
                    is NetworkError -> _dataState.value = FeedModelState(error = FeedError.NETWORK)
                    is UnknownError -> _dataState.value = FeedModelState(error = FeedError.UNKNOWN)
                }
            }
        }
    }

    fun save() {
        edited.value?.let {
            viewModelScope.launch {
                repository.save(it)
                _postCreated.value = Unit
            }
        }
        edited.value = empty
    }


    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) = viewModelScope.launch {
        try {
            repository.likeById(id)
            _dataState.value = FeedModelState()
        } catch (e: AppError) {
            when (e) {
                is ApiError -> _dataState.value = FeedModelState(error = FeedError.API)
                is NetworkError -> _dataState.value = FeedModelState(error = FeedError.NETWORK)
                is UnknownError -> _dataState.value = FeedModelState(error = FeedError.UNKNOWN)
            }
        }
    }


    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeById(id)
            _dataState.value = FeedModelState()
        } catch (e: AppError) {
            when (e) {
                is ApiError -> _dataState.value = FeedModelState(error = FeedError.API)
                is NetworkError -> _dataState.value = FeedModelState(error = FeedError.NETWORK)
                is UnknownError -> _dataState.value = FeedModelState(error = FeedError.UNKNOWN)
            }
        }
    }

}

