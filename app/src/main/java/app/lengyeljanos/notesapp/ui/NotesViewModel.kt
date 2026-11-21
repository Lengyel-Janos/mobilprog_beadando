package app.lengyeljanos.notesapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lengyeljanos.notesapp.data.Note
import app.lengyeljanos.notesapp.data.NotesDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isAddEditDialogOpen: Boolean = false,
    val noteToEdit: Note? = null,
    val noteToDelete: Note? = null
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = NotesDatabaseHelper(application)

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentQuery = _uiState.value.searchQuery
            val list = dbHelper.searchNotes(currentQuery)
            _uiState.update { it.copy(notes = list) }
        }
    }

    fun openAddNoteDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = true, noteToEdit = null) }
    }

    fun openEditNoteDialog(note: Note) {
        _uiState.update { it.copy(isAddEditDialogOpen = true, noteToEdit = note) }
    }

    fun closeAddEditDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = false, noteToEdit = null) }
    }

    fun saveNote(title: String, description: String, tagsInput: String) {
        if (title.isBlank()) return
        val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val currentEdit = _uiState.value.noteToEdit

        viewModelScope.launch(Dispatchers.IO) {
            if (currentEdit == null) {
                dbHelper.insertNote(
                    Note(
                        title = title.trim(),
                        description = description.trim(),
                        tags = tags
                    )
                )
            } else {
                dbHelper.updateNote(
                    currentEdit.copy(
                        title = title.trim(),
                        description = description.trim(),
                        tags = tags
                    )
                )
            }
            _uiState.update { it.copy(isAddEditDialogOpen = false, noteToEdit = null) }
            loadNotes()
        }
    }

    fun showDeleteConfirmation(note: Note) {
        _uiState.update { it.copy(noteToDelete = note) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(noteToDelete = null) }
    }

    fun deleteConfirmedNote() {
        val note = _uiState.value.noteToDelete ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteNote(note.id)
            _uiState.update { it.copy(noteToDelete = null) }
            loadNotes()
        }
    }
}
