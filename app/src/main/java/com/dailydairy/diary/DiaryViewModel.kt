package com.dailydairy.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val lockStore = DiaryLockStore(application)
    private val dao = DiaryDatabase.get(application).diaryDao()

    val lock: StateFlow<DiaryLock?> = lockStore.lock.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val fingerprintEnabled: StateFlow<Boolean> = lockStore.fingerprint.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )

    val lockReady = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            lockStore.lock.collect { lockReady.value = true }
        }
    }

    val entries: StateFlow<List<DiaryEntry>> = dao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    fun matches(password: String): Boolean {
        val current = lock.value ?: return false
        return lockStore.matches(password, current.hash)
    }

    fun unlock() {
        _unlocked.value = true
    }

    fun createLock(password: String, hint: String) {
        viewModelScope.launch {
            lockStore.save(password, hint)
            _unlocked.value = true
        }
    }

    fun changePassword(current: String, next: String, hint: String, onResult: (Boolean) -> Unit) {
        val stored = lock.value
        if (stored == null || !lockStore.matches(current, stored.hash)) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            lockStore.save(next.ifBlank { current }, hint)
            onResult(true)
        }
    }

    fun setFingerprint(enabled: Boolean) {
        viewModelScope.launch { lockStore.setFingerprint(enabled) }
    }

    fun passwordMatches(password: String): Boolean {
        val stored = lock.value ?: return false
        return lockStore.matches(password, stored.hash)
    }

    fun save(id: Long?, title: String, body: String, bodyHtml: String, images: String, place: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (id == null) {
                dao.insert(
                    DiaryEntry(
                        title = title.trim(),
                        body = body.trim(),
                        bodyHtml = bodyHtml,
                        images = images,
                        place = place,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                val existing = dao.get(id) ?: return@launch
                val dropped = existing.imageNames() - images.split('|').filter { it.isNotBlank() }.toSet()
                DiaryImages.delete(getApplication(), dropped)
                dao.update(
                    existing.copy(
                        title = title.trim(),
                        body = body.trim(),
                        bodyHtml = bodyHtml,
                        images = images,
                        place = place,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val existing = dao.get(id)
            if (existing != null) DiaryImages.delete(getApplication(), existing.imageNames())
            dao.delete(id)
        }
    }
}
