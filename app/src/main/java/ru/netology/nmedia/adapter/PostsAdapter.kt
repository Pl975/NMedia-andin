package ru.netology.nmedia.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private const val AVATARS = "/avatars"
        private const val IMAGES = "/images"
    }

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH)
            published.text = dateFormat.format(Date(post.published.toLong() * 1000))
            content.text = post.content
            // в адаптере
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"

            val options: RequestOptions = RequestOptions.circleCropTransform()
            Glide.with(avatar)
                .load(BASE_URL + AVATARS + "/${post.authorAvatar}")
                .placeholder(R.drawable.ic_loading_100dp)
                .error(R.drawable.ic_error_100dp)
                .timeout(10_000)
                .apply(options)
                .into(avatar)

            if (post.attachment != null) {
                image.loadingWithGlide(
                    BASE_URL + IMAGES + "/${post.attachment.url}",
                    10_000,
                    fullWidth = true
                )
                image.visibility = View.VISIBLE
            } else {
                image.visibility = View.GONE
            }

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }

            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
        }
    }
}

fun ImageView.loadingWithGlide(
    url: String,
    timeOut: Int = 30_000,
    placeholderIndex: Int = R.drawable.ic_loading_100dp,
    errorIndex: Int = R.drawable.ic_error_100dp,
    options: RequestOptions = RequestOptions(),
    fullWidth: Boolean = false,
) {
    Glide.with(this)
        .load(url)
        .timeout(timeOut)
        .placeholder(placeholderIndex)
        .error(errorIndex)
        .apply(options)
        .apply {
            if (fullWidth)
                this.into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        this@loadingWithGlide.setImageDrawable(resource)
                        val layoutParams = this@loadingWithGlide.layoutParams
                        val widthOriginal = resource.intrinsicWidth
                        val heightOriginal = resource.intrinsicHeight
                        val displayMetrics = context.resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        layoutParams.width = screenWidth
                        val calculatedHeight =
                            (screenWidth.toFloat() / widthOriginal.toFloat() * heightOriginal).toInt()
                        layoutParams.height = calculatedHeight
                        this@loadingWithGlide.layoutParams = layoutParams
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        this@loadingWithGlide.setImageDrawable(placeholder)
                    }
                }) else this.into(this@loadingWithGlide)
        }
}


class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}
