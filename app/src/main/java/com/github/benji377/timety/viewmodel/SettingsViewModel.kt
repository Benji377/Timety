package com.github.benji377.timety.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.benji377.timety.data.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class SettingsViewModel(private val repository: MainRepository) : ViewModel() {

    val user: StateFlow<User?> = repository.user.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateUser(user: User) {
        viewModelScope.launch {
            repository.insertOrUpdateUser(user)
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            user.value?.let {
                repository.insertOrUpdateUser(it.copy(name = name))
            }
        }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            user.value?.let {
                repository.insertOrUpdateUser(it.copy(isDarkMode = isDark))
            }
        }
    }

    fun addCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, colorHex = colorHex, iconName = iconName))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun exportData(cacheDir: File, callback: (File?) -> Unit) {
        viewModelScope.launch {
            val userData = repository.user.first()
            val sessions = repository.allSessions.first()
            val tasks = repository.allTasks.first()
            val categoriesData = repository.allCategories.first()
            
            val exportMap = mapOf(
                "user" to userData,
                "sessions" to sessions,
                "tasks" to tasks,
                "categories" to categoriesData
            )
            
            val json = Gson().toJson(exportMap)
            val file = File(cacheDir, "timety_export_${System.currentTimeMillis()}.json")
            try {
                FileWriter(file).use { it.write(json) }
                callback(file)
            } catch (e: Exception) {
                callback(null)
            }
        }
    }

    fun importData(file: File, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = BufferedReader(FileReader(file)).use { it.readText() }
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val importMap: Map<String, Any> = Gson().fromJson(json, type)

                // Need to properly deserialize each part
                val gson = Gson()
                val userData = gson.fromJson(gson.toJson(importMap["user"]), User::class.java)
                val sessions = gson.fromJson<List<FocusSession>>(gson.toJson(importMap["sessions"]), object : TypeToken<List<FocusSession>>() {}.type)
                val tasks = gson.fromJson<List<Task>>(gson.toJson(importMap["tasks"]), object : TypeToken<List<Task>>() {}.type)
                val categoriesData = gson.fromJson<List<Category>>(gson.toJson(importMap["categories"]), object : TypeToken<List<Category>>() {}.type)

                if (userData != null) repository.insertOrUpdateUser(userData)
                categoriesData?.forEach { repository.insertCategory(it) }
                tasks?.forEach { repository.insertTask(it) }
                sessions?.forEach { repository.insertSession(it) }

                callback(true)
            } catch (e: Exception) {
                callback(false)
            }
        }
    }
}

class SettingsViewModelFactory(private val repository: MainRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
