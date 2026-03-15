package org.elsoft.bkdb.utils

import org.elsoft.bkdb.data.EBookRepository
import org.elsoft.bkdb.data.local.LocalCacheManager
import org.elsoft.bkdb.data.remote.DatabaseManager

object AppContainer {
    // DatabaseManager implements RemoteDataSource
    val remoteSource = DatabaseManager()

    // LocalCacheManager implements LocalDataSource
    val localSource = LocalCacheManager(Platform.getCacheDir())

    val repository = EBookRepository(remoteSource, localSource)
}
