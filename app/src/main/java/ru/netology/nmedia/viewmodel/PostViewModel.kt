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
    private val _postCreated = SingleLiveEvent<FeedModel>()
    val postCreated: LiveData<FeedModel>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        _data.value = FeedModel(loading = true)

        repository.getAllAsync(object : PostRepository.Callback<List<Post>> {

            override fun onSuccess(result: List<Post>) {
                _data.postValue(FeedModel(posts = result, empty = result.isEmpty()))
            }


            override fun onError(e: Exception) {

                _data.postValue(FeedModel(error = true))
            }
        })
    }


    fun save() {
        edited.value?.let {
            repository.saveAsync(it, object : PostRepository.Callback<Unit> {
                override fun onSuccess(result: Unit) {
                    _postCreated.postValue(FeedModel())
                }

                override fun onError(e: Exception) {
                    _postCreated.postValue(FeedModel(error = true))

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

        repository.likeByIdAsync(id, object : PostRepository.Callback<Unit> {

            override fun onSuccess(result: Unit) {
                _data.postValue(_data.value?.copy(posts = _data.value?.posts.orEmpty().map {
                    val delta = if (it.likedByMe) -1 else 1
                    if (it.id != id) it else it.copy(
                        likedByMe = !it.likedByMe,
                        likes = it.likes + delta
                    )
                }))

            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }

        })
    }


    fun removeById(id: Long) {

        // Оптимистичная модель
        repository.removeByIdAsync(id, object : PostRepository.Callback<Unit> {

            override fun onSuccess(result: Unit) {
                _data.postValue(
                    _data.value?.copy(posts = _data.value?.posts.orEmpty()
                        .filter { it.id != id }
                    )
                )

            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }

        })
    }
}