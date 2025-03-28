package ui.lazyColumn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.DataSource
import io.github.ahmad_hamwi.compose.pagination.PaginatedLazyColumn
import io.github.ahmad_hamwi.compose.pagination.rememberPaginationState
import kotlinx.coroutines.launch
import ui.lazyRow.PaginatedLazyRowSampleContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaginatedLazyColumnSampleContent(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val dataSource = remember { DataSource() }

    val paginationState = rememberPaginationState(
        initialPageKey = 1,
        onRequestPage = { pageKey: Int ->
            scope.launch {
                try {
                    val page = dataSource.getPage(pageNumber = pageKey)

                    appendPage(
                        items = page.items,
                        nextPageKey = page.nextPageKey,
                        isLastPage = page.isLastPage
                    )
                } catch (e: Exception) {
                    setError(e)
                }
            }
        }
    )

    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isAnimating) {
        LaunchedEffect(true) {
            paginationState.refresh()
            pullToRefreshState.animateToHidden()
        }
    }

    Box {
        PaginatedLazyColumn(
            modifier = modifier,
            paginationState = paginationState,
            firstPageProgressIndicator = { FirstPageProgressIndicator() },
            newPageProgressIndicator = { NewPageProgressIndicator() },
            firstPageErrorIndicator = { e ->
                FirstPageErrorIndicator(
                    exception = e,
                    onRetryClicked = {
                        paginationState.retryLastFailedRequest()
                    }
                )
            },
            newPageErrorIndicator = { e ->
                NewPageErrorIndicator(
                    exception = e,
                    onRetryClicked = {
                        paginationState.retryLastFailedRequest()
                    }
                )
            },
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                paginationState.allItems!!,
            ) { index, item ->
                if ((index + 1) % 4 == 0) {
                    PaginatedLazyRowSampleContent(
                        modifier = Modifier.height(128.dp),
                    )
                } else {
                    ColumnItem(value = item)
                }
            }
        }

        if (pullToRefreshState.distanceFraction > 0) {
            PullToRefreshBox(
                isRefreshing = true,
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                onRefresh = {}
            ) {}
        }
    }
}
