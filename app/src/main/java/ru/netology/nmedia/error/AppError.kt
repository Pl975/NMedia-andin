package ru.netology.nmedia.error

sealed class AppError(val text: String): Exception()

class ApiError(val code: Int, val msg: String): AppError(msg)
object NetworkError: AppError("Network Error")
object UnknownError: AppError("Unknown Error")