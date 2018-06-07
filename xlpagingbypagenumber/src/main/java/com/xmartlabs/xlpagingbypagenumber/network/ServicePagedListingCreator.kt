package com.xmartlabs.template.repository.common

import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.xmartlabs.xlpagingbypagenumber.Listing
import com.xmartlabs.xlpagingbypagenumber.common.IoExecutors
import com.xmartlabs.xlpagingbypagenumber.fetcher.ListResponsePagingHandler
import com.xmartlabs.xlpagingbypagenumber.dbsupport.ServiceAndDatabasePagedListingCreator
import com.xmartlabs.xlpagingbypagenumber.network.ServicePagedDataSourceFactory
import java.util.concurrent.Executor

object ServicePagedListingCreator {
  fun <T> createListing(
      firstPage: Int = 1,
      ioServiceExecutor: Executor = IoExecutors.NETWORK_EXECUTOR,
      pagedListConfig: PagedList.Config = ServiceAndDatabasePagedListingCreator.DEFAULT_PAGED_LIST_CONFIG,
      pagingHandler: ListResponsePagingHandler<T>
  ): Listing<T> {
    val sourceFactory = ServicePagedDataSourceFactory(
        firstPage = firstPage,
        ioServiceExecutor = ioServiceExecutor,
        pagedListConfig = pagedListConfig,
        pagingHandler = pagingHandler
    )
    val livePagedList = LivePagedListBuilder(sourceFactory, pagedListConfig)
        .build()

    val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
      it.initialLoad
    }
    return Listing(
        pagedList = livePagedList,
        networkState = Transformations.switchMap(sourceFactory.sourceLiveData, {
          it.networkState
        }),
        retry = {
          sourceFactory.sourceLiveData.value?.retryAllFailed()
        },
        refresh = {
          sourceFactory.sourceLiveData.value?.invalidate()
        },
        refreshState = refreshState
    )
  }
}
