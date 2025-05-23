package io.github.ahmad_hamwi.compose.pagination

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf

@OptIn(ExperimentalTestApi::class)
abstract class PaginatedLazyScrollableTest {

    companion object {
        const val LAZY_SCROLLABLE_TAG = "lazyScrollable"
        const val FIRST_PAGE_PROGRESS_INDICATOR_TAG = "firstPageProgressIndicator"
        const val FIRST_PAGE_ERROR_INDICATOR_TAG = "firstPageErrorIndicator"
        const val FIRST_PAGE_EMPTY_INDICATOR_TAG = "firstPageEmptyIndicator"
        const val ITEM_CONTENT_TAG = "itemContent"
        const val NEW_PAGE_PROGRESS_INDICATOR_TAG = "newPageProgressIndicator"
        const val NEW_PAGE_ERROR_INDICATOR_TAG = "newPageErrorIndicator"
        const val NEW_PAGE_EMPTY_INDICATOR_TAG = "newPageEmptyIndicator"
    }

    @Suppress("TestFunctionName")
    @Composable
    abstract fun SutComposable(
        paginationState: PaginationState<Int, String>
    )

    private fun defaultPaginationState(onRequestPage: (Int) -> Unit) = PaginationState<Int, String>(
        initialPageKey = 1,
        onRequestPage = {onRequestPage(it) }
    )

    open fun firstPageProgressIndicatorShownWhenNullPage() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()

        val state = defaultPaginationState { pageKey ->
            pageKeysCalled += pageKey
        }

        setContent { SutComposable(state) }

        onNodeWithTag(FIRST_PAGE_PROGRESS_INDICATOR_TAG).assertExists()
        assertThat(pageKeysCalled).isEqualTo(listOf(1))
    }

    open fun firstPageProgressIndicatorHiddenWhenAPageHasBeenAppended() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()

        val paginationState = defaultPaginationState { pageKeyCalled ->
            pageKeysCalled += pageKeyCalled
        }

        setContent { SutComposable(paginationState) }

        paginationState.appendPage(items = listOf(""), nextPageKey = 2, isLastPage = true)

        onNodeWithTag(FIRST_PAGE_PROGRESS_INDICATOR_TAG).assertDoesNotExist()
        assertThat(pageKeysCalled).isEqualTo(listOf(1))
    }

    open fun firstPageErrorIsShownWhenNoPageAndErrorHappened() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKey ->
            pageKeysCalled += pageKey
        }

        setContent {
            SutComposable(paginationState = state)
        }

        state.setError(Exception("First page error"))

        onNodeWithTag(FIRST_PAGE_ERROR_INDICATOR_TAG).assertExists()
        onNodeWithText("First page error").assertExists()
        assertThat(pageKeysCalled).isEqualTo(listOf(1))
    }

    open fun firstPageEmptyIsShownWhenLoadedAndPageIsEmpty() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKey ->
            pageKeysCalled += pageKey
        }

        setContent {
            SutComposable(paginationState = state)
        }

        state.appendPage(items = listOf(), nextPageKey = 2, isLastPage = true)

        onNodeWithTag(FIRST_PAGE_EMPTY_INDICATOR_TAG).assertExists()
        assertThat(pageKeysCalled).isEqualTo(listOf(1))
    }

    open fun firstPageIsShownWhenPutPageIsTriggeredForTheFirstTime() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKey ->
            pageKeysCalled += pageKey
        }

        setContent { SutComposable(state) }

        state.appendPage(items = listOf(""), nextPageKey = 2, isLastPage = true)

        onNodeWithTag(ITEM_CONTENT_TAG).assertExists()
        assertThat(pageKeysCalled).isEqualTo(listOf(1))
    }

    open fun scrollingDownTheListWillShowProgressAndTriggerPageRequest() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKey ->
            pageKeysCalled += pageKey
        }

        setContent { SutComposable(state) }

        state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = 2)

        onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToIndex(4)
        onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToKey(LazyScrollableKeys.NEW_PAGE_PROGRESS_INDICATOR_KEY)

        onNodeWithTag(NEW_PAGE_PROGRESS_INDICATOR_TAG).assertExists()
        assertThat(pageKeysCalled).isEqualTo(listOf(1, 2))
    }

    open fun scrollingDownTheListWillShowErrorAndTriggerPageRequest() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKey ->
            pageKeysCalled += pageKey
        }

        setContent { SutComposable(state) }

        state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = 2)
        state.setError(Exception("New page error"))

        onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToIndex(4)
        onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToKey(LazyScrollableKeys.NEW_PAGE_ERROR_INDICATOR_KEY)

        onNodeWithTag(NEW_PAGE_ERROR_INDICATOR_TAG).assertExists()
        onNodeWithText("New page error").assertExists()
        assertThat(pageKeysCalled).isEqualTo(listOf(1))
    }

    open fun scrollingDownTheListWillShowEmptyAndTriggerPageRequest() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKey ->
            pageKeysCalled += pageKey
        }

        setContent { SutComposable(state) }

        state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = 2)
        onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToIndex(4)
        waitForIdle()

        state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = -1, isLastPage = true)
        onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToIndex(10)
        waitForIdle()

        onNodeWithTag(NEW_PAGE_EMPTY_INDICATOR_TAG).assertExists()
        onNodeWithText("No more results").assertExists()
        assertThat(pageKeysCalled).isEqualTo(listOf(1, 2))
    }

    open fun appendingLastPagePreventsLoadingAndNewPageRequests() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKeysCalled += it }

        setContent { SutComposable(state) }

        state.appendPage(items = listOf(""), nextPageKey = 2, isLastPage = true)

        onNodeWithTag(NEW_PAGE_PROGRESS_INDICATOR_TAG).assertDoesNotExist()
        assertThat(pageKeysCalled).isEqualTo(listOf(1))
    }

    open fun retryFirstFailedRequestWouldRequestAgainTheSamePageAndShowProgress() =
        runComposeUiTest {
            val pageKeysCalled = mutableListOf<Int>()

            val state = defaultPaginationState { pageKeysCalled += it }

            setContent { SutComposable(state) }
            state.setError(Exception())
            state.retryLastFailedRequest()

            onNodeWithTag(FIRST_PAGE_PROGRESS_INDICATOR_TAG).assertExists()
            assertThat(pageKeysCalled).isEqualTo(listOf(1, 1))
        }

    open fun retryingFirstFailedRequestTwiceWouldRequestAgainOnlyOnce() =
        runComposeUiTest {
            val pageKeysCalled = mutableListOf<Int>()

            val state = defaultPaginationState { pageKeysCalled += it }

            setContent { SutComposable(state) }
            state.setError(Exception())
            state.retryLastFailedRequest()
            state.retryLastFailedRequest() // second time (for any reason)

            assertThat(pageKeysCalled).isEqualTo(listOf(1, 1))
        }

    open fun failRetrySuccessThenNextPageFailRetrySuccess() =
        runComposeUiTest {
            val pageKeysCalled = mutableListOf<Int>()

            val state = defaultPaginationState { pageKeysCalled += it }

            setContent { SutComposable(state) }

            state.setError(Exception()) // First page error
            state.retryLastFailedRequest() // First page retry
            state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = 2, isLastPage = false) // First page loaded
            onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToIndex(4) // scroll to next page causes request page 2
            waitForIdle()

            state.setError(Exception()) // Second page error
            state.retryLastFailedRequest() // Second page retry
            state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = 3, isLastPage = true) // Second page loaded
            waitForIdle()

            assertThat(pageKeysCalled).isEqualTo(listOf(1, 1, 2, 2))
        }

    open fun invokingRetryOnLoadedStateCausesAnIllegalArgumentException() =
        runComposeUiTest {
            val pageKeysCalled = mutableListOf<Int>()

            val state = defaultPaginationState { pageKeysCalled += it }

            setContent { SutComposable(state) }

            state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = 2, isLastPage = false)

            assertFailure { state.retryLastFailedRequest() }.isInstanceOf(IllegalArgumentException::class)
        }

    open fun retryNewPageFailedRequestWouldRequestAgainTheSamePageAndShowProgress() =
        runComposeUiTest {
            val pageKeysCalled = mutableListOf<Int>()

            val state = defaultPaginationState { pageKeysCalled += it }

            setContent { SutComposable(state) }
            state.appendPage(items = listOf(), nextPageKey = 2)
            state.setError(Exception())
            state.retryLastFailedRequest()

            onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToIndex(0)

            onNodeWithTag(NEW_PAGE_PROGRESS_INDICATOR_TAG).assertExists()
            assertThat(pageKeysCalled).isEqualTo(listOf(1, 2))
        }

    open fun refreshingResetsTheStateAndAnInitialLoadStarts() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKeysCalled += it }

        setContent { SutComposable(state) }

        state.refresh()

        onNodeWithTag(FIRST_PAGE_PROGRESS_INDICATOR_TAG).assertExists()
        assertThat(pageKeysCalled).isEqualTo(listOf(1, 1))
    }

    open fun firstPageErrorThenRefreshThenFirstPageRequestedAgainThenShowFirstPage() =
        runComposeUiTest {
            val pageKeysCalled = mutableListOf<Int>()
            val state = defaultPaginationState { pageKeysCalled += it }
            state.setError(Exception())
            setContent { SutComposable(state) }
            onNodeWithTag(FIRST_PAGE_ERROR_INDICATOR_TAG).assertExists()

            state.refresh()
            onNodeWithTag(FIRST_PAGE_PROGRESS_INDICATOR_TAG).assertExists()

            state.appendPage(listOf(""), nextPageKey = 2, isLastPage = true)
            onNodeWithTag(ITEM_CONTENT_TAG).assertExists()

            assertThat(pageKeysCalled).isEqualTo(listOf(1))
        }

    open fun firstPageLoadedThenRefreshThenFirstPageRequestedAgainThenShowFirstPage() =
        runComposeUiTest {
            val pageKeysCalled = mutableListOf<Int>()
            val state = defaultPaginationState { pageKeysCalled += it }
            state.appendPage(listOf(""), nextPageKey = 2, isLastPage = true)
            setContent { SutComposable(state) }

            state.refresh()
            onNodeWithTag(FIRST_PAGE_PROGRESS_INDICATOR_TAG).assertExists()

            state.appendPage(listOf(""), nextPageKey = 2, isLastPage = true)
            onNodeWithTag(ITEM_CONTENT_TAG).assertExists()

            assertThat(pageKeysCalled).isEqualTo(listOf(1))
        }

    open fun loadsOnlyOnePageAfterAScroll() = runComposeUiTest {
        val pageKeysCalled = mutableListOf<Int>()
        val state = defaultPaginationState { pageKeysCalled += it }

        setContent { SutComposable(state) }
        state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = 2, isLastPage = false)
        onNodeWithTag(LAZY_SCROLLABLE_TAG).performScrollToIndex(4)
        waitForIdle()
        state.appendPage(items = listOf("", "", "", "", ""), nextPageKey = 3, isLastPage = true)
        waitForIdle()

        assertThat(pageKeysCalled).isEqualTo(listOf(1, 2))
    }

    open fun initialPageIs2WouldLoadPage2() = runComposeUiTest {
        var pageKeyCalled: Int? = null

        val state = PaginationState<Int, String>(
            initialPageKey = 2,
            onRequestPage = { pageKey ->
                pageKeyCalled = pageKey
            }
        )

        setContent { SutComposable(state) }

        assertThat(pageKeyCalled).isEqualTo(2)
    }

    open fun refreshingWithInitialPageOf2WouldLoadPage2() = runComposeUiTest {
        var pageKeyCalled: Int? = null

        val state = PaginationState<Int, String>(
            initialPageKey = 1,
            onRequestPage = { pageKey ->
                pageKeyCalled = pageKey
            }
        )

        setContent { SutComposable(state) }

        state.refresh(initialPageKey = 3)
        waitForIdle()

        assertThat(pageKeyCalled).isEqualTo(3)
    }
}