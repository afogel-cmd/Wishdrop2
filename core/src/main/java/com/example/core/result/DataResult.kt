package com.example.core.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface DataResult<out T> {
  data object Loading : DataResult<Nothing>

  data class Success<T>(val data: T) : DataResult<T>

  data class Error(val message: String, val cause: Throwable? = null) : DataResult<Nothing>
}

fun <T> Flow<T>.asDataResult(): Flow<DataResult<T>> =
  this.map<T, DataResult<T>> { DataResult.Success(it) }
    .onStart { emit(DataResult.Loading) }
    .catch { e ->
      emit(
        DataResult.Error(
          message = e.localizedMessage ?: "Unable to load WishDrop data.",
          cause = e,
        )
      )
    }
