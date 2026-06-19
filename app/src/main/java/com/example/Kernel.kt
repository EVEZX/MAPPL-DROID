package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DomainEvent(
    val type: String,
    val domainId: String,
    val payload: Map<String, Any>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sequenceNumber: Int = 0
)

typealias ProjectionReducer<T> = (state: T?, event: DomainEvent) -> T

class ProjectionEntry<T>(
    val reducer: ProjectionReducer<T>,
    initialState: T? = null
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<T?> = _state.asStateFlow()

    fun handleEvent(event: DomainEvent) {
        _state.update { reducer(it, event) }
    }
}

object Kernel {
    private val _eventLog = MutableStateFlow<List<DomainEvent>>(emptyList())
    val eventLog: StateFlow<List<DomainEvent>> = _eventLog.asStateFlow()

    private val projections = mutableMapOf<String, ProjectionEntry<Any>>()

    @Synchronized
    fun appendEvent(event: DomainEvent): DomainEvent {
        val currentSize = _eventLog.value.size
        val storedEvent = event.copy(
            timestamp = System.currentTimeMillis(),
            sequenceNumber = currentSize
        )
        
        _eventLog.update { it + storedEvent }
        
        val projection = projections[event.domainId]
        projection?.handleEvent(storedEvent)
        
        return storedEvent
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T : Any> subscribeProjection(
        domainId: String,
        initialState: T?,
        reducer: ProjectionReducer<T>
    ): StateFlow<T?> {
        if (!projections.containsKey(domainId)) {
            val entry = ProjectionEntry(reducer as ProjectionReducer<Any>, initialState as Any?)
            projections[domainId] = entry
            
            // Replay events
            _eventLog.value.filter { it.domainId == domainId }.forEach {
                entry.handleEvent(it)
            }
        }
        return projections[domainId]!!.state as StateFlow<T?>
    }

    @Synchronized
    fun getRegisteredDomains(): List<String> {
        return projections.keys.toList()
    }
    
    @Synchronized
    fun getState(domainId: String): Any? {
        return projections[domainId]?.state?.value
    }
}
