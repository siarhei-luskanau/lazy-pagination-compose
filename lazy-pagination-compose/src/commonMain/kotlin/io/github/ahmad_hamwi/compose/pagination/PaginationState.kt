package io.github.ahmad_hamwi.compose.pagination

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Suppress("UNCHECKED_CAST")
@Stable
class PaginationState<KEY, T>(
    initialPageKey: KEY,
    internal val onRequestPage: PaginationState<KEY, T>.(KEY) -> Unit
) {
    internal var internalState =
        mutableStateOf<PaginationInternalState<KEY, T>>(
            PaginationInternalState.Initial(
                initialPageKey
            )
        )

    val requestedPageKey: KEY?
        get() = (internalState.value as? PaginationInternalState.IHasRequestedPageKey<KEY>)?.requestedPageKey

    /**
     * All the items currently loaded so far. null when no items has been loaded yet.
     */
    val allItems: List<T>?
        get() = internalState.value.items

    fun setError(exception: Exception) {
        val internalStateSnapshot = internalState.value
        val nextPageKeyOfLoadingState: KEY? =
            (internalStateSnapshot as? PaginationInternalState.Loaded)?.nextPageKey
        val requestedPageKeyOfLoadingOrErrorState: KEY? =
            (internalStateSnapshot as? PaginationInternalState.IHasRequestedPageKey<KEY>)?.requestedPageKey

        // requestedPageKey is going to either be
        // 1- the nextPageKey of the loaded state: because the page has already been loaded and showing an error means a new page error state
        // OR
        // 2- the requestedPageKey of either a loading or a previous error state
        internalState.value = PaginationInternalState.Error(
            initialPageKey = internalStateSnapshot.initialPageKey,
            requestedPageKey = nextPageKeyOfLoadingState
                ?: requestedPageKeyOfLoadingOrErrorState
                ?: internalStateSnapshot.initialPageKey,
            exception = exception,
            items = internalState.value.items
        )
    }

    @OptIn(ExperimentalPaginationApi::class)
    fun appendPage(items: List<T>, nextPageKey: KEY, isLastPage: Boolean = false) {
        appendPageWithUpdates(
            allItems = (internalState.value.items ?: listOf()) + items,
            nextPageKey = nextPageKey,
            isLastPage = isLastPage
        )
    }


    /**
     * Updates current allItems but should also include new changes from a new page.
     * This allows you to do a full list update while still do the same `appendPage()` behavior
     * in one shot, rather than having to set allItems first then call `appendPage()`.
     *
     * This API is experimental as it may be confusing to some users, naming can change without
     * notice and can be subject to removal.
     */
    @ExperimentalPaginationApi
    fun appendPageWithUpdates(allItems: List<T>, nextPageKey: KEY, isLastPage: Boolean = false) {
        val internalStateSnapshot = internalState.value
        val requestedPageKeyOfLoadingOrErrorState: KEY? =
            (internalStateSnapshot as? PaginationInternalState.IHasRequestedPageKey<KEY>)?.requestedPageKey

        internalState.value = PaginationInternalState.Loaded(
            initialPageKey = internalState.value.initialPageKey,
            requestedPageKey = requestedPageKeyOfLoadingOrErrorState
                ?: internalStateSnapshot.initialPageKey,
            nextPageKey = nextPageKey,
            items = allItems,
            isLastPage = isLastPage
        )
    }

    fun retryLastFailedRequest() {
        val internalStateSnapshot = internalState.value

        require(internalStateSnapshot is PaginationInternalState.Error || internalStateSnapshot is PaginationInternalState.Loading) {
            "retryLastFailedRequest cannot be invoked while on a state other than error or loading. Current state: $internalStateSnapshot"
        }

        requestPage(
            initialPageKey = internalStateSnapshot.initialPageKey,
            requestedPageKey = internalStateSnapshot.requestedPageKey,
            items = internalStateSnapshot.items
        )
    }

    fun refresh(initialPageKey: KEY? = null) {
        internalState.value = PaginationInternalState.Initial(
            initialPageKey ?: internalState.value.initialPageKey
        )
    }

    internal fun requestPage(
        initialPageKey: KEY,
        requestedPageKey: KEY,
        items: List<T>? = null,
    ) {
        if (internalState.value is PaginationInternalState.Loading) {
            return
        }

        internalState.value = PaginationInternalState.Loading(
            initialPageKey = initialPageKey,
            requestedPageKey = requestedPageKey ?: initialPageKey,
            items = items
        )

        onRequestPage(requestedPageKey ?: initialPageKey)
    }
}

@Composable
fun <KEY, T> rememberPaginationState(
    initialPageKey: KEY,
    onRequestPage: PaginationState<KEY, T>.(KEY) -> Unit
): PaginationState<KEY, T> {
    return remember {
        PaginationState(
            initialPageKey = initialPageKey,
            onRequestPage = onRequestPage
        )
    }
}