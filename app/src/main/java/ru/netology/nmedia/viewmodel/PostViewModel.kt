package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
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
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        _data.value = FeedModel(loading = true)
        repository.getAllAsync(object : PostRepository.PostCallback<List<Post>> {
            override fun onSuccess(result: List<Post>) {
                _data.postValue(FeedModel(posts = result, empty = result.isEmpty()))
            }

            override fun onError(error: Throwable) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }


    fun save() {
        edited.value?.let {
            repository.save(it, object : PostRepository.PostCallback<Unit> {
                override fun onSuccess(result: Unit) {
                    _postCreated.postValue(result)
                }

                override fun onError(error: Throwable) {
                    _postCreated.postValue(Unit)
                }
            })
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

    fun likeById(id: Long) {
        val old = _data.value?.posts.orEmpty()
        repository.likeById(id, object : PostRepository.PostCallback<Unit> {
            override fun onSuccess(result: Unit) {
                _data.postValue(_data.value?.copy(posts = _data.value?.posts.orEmpty().map {
                    val delta = if (it.likedByMe) -1 else 1
                    if (it.id != id) it else it.copy(
                        likedByMe = !it.likedByMe,
                        likes = it.likes + delta
                    )
                }))
            }

            override fun onError(error: Throwable) {
                _data.postValue(_data.value?.copy(posts = old))
            }
        })
    }


    fun removeById(id: Long) {
        val old = _data.value?.posts.orEmpty()
        repository.removeById(id, object : PostRepository.PostCallback<Unit> {
            override fun onSuccess(result: Unit) {
                _data.postValue(
                    _data.value?.copy(posts = _data.value?.posts.orEmpty()
                        .filter { it.id != id }
                    )
                )
            }

            override fun onError(error: Throwable) {
                _data.postValue(_data.value?.copy(posts = old))
            }
        })
    }
}