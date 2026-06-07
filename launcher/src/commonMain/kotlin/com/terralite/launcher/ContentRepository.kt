package com.terralite.launcher

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ContentRepository {
    fun getDownloadablePacks(): Flow<List<DownloadablePack>>
    suspend fun insertPack(pack: DownloadablePack)
    suspend fun clearPacks()
}

class SqlContentRepository(private val database: LauncherDatabase) : ContentRepository {
    private val queries = database.databaseQueries

    override fun getDownloadablePacks(): Flow<List<DownloadablePack>> {
        return queries.selectAllPacks { name, author, size, description ->
            DownloadablePack(name, author, size, description)
        }.asFlow().mapToList(Dispatchers.IO)
    }

    override suspend fun insertPack(pack: DownloadablePack) = withContext(Dispatchers.IO) {
        queries.insertPack(pack.name, pack.author, pack.size, pack.description)
        Unit
    }

    override suspend fun clearPacks() = withContext(Dispatchers.IO) {
        queries.clearPacks()
        Unit
    }
}
