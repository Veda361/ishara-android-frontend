package com.ishara.app.data.local.datasource

import com.ishara.app.domain.model.StudentDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Local privacy-preserving cache for recent student destinations.
 * Keeps max 5 items in memory, deduplicates by address/name, and never uploads to remote servers.
 */
interface RecentDestinationsLocalDataSource {
    fun getRecentDestinations(): Flow<List<StudentDestination>>
    suspend fun saveDestination(destination: StudentDestination)
    suspend fun clear()
}

class InMemoryRecentDestinationsLocalDataSource : RecentDestinationsLocalDataSource {

    private val mutex = Mutex()
    private val _recentFlow = MutableStateFlow<List<StudentDestination>>(emptyList())

    override fun getRecentDestinations(): Flow<List<StudentDestination>> = _recentFlow.asStateFlow()

    override suspend fun saveDestination(destination: StudentDestination) {
        mutex.withLock {
            val current = _recentFlow.value.toMutableList()
            // Remove existing duplicate matching same address or name
            current.removeAll {
                it.formattedAddress.equals(destination.formattedAddress, ignoreCase = true) ||
                    (it.name.equals(destination.name, ignoreCase = true) &&
                        it.formattedAddress.isBlank() && destination.formattedAddress.isBlank())
            }
            // Insert at the front (most recent)
            current.add(0, destination)
            // Limit to max 5 items
            val truncated = if (current.size > 5) current.subList(0, 5) else current
            _recentFlow.value = truncated.toList()
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            _recentFlow.value = emptyList()
        }
    }
}
