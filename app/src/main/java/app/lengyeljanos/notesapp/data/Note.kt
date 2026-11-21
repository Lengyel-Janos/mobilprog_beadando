package app.lengyeljanos.notesapp.data

data class Note(
    val id: Long = 0,
    val title: String,
    val description: String,
    val tags: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)
