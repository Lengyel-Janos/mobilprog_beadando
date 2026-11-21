package app.lengyeljanos.notesapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NotesDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notes.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_NOTES = "notes"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_TAGS = "tags"
        private const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_TAGS TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        onCreate(db)
    }

    fun insertNote(note: Note): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_DESCRIPTION, note.description)
            put(COLUMN_TAGS, note.tags.joinToString(","))
            put(COLUMN_CREATED_AT, note.createdAt)
        }
        return db.insert(TABLE_NOTES, null, values)
    }

    fun updateNote(note: Note): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, note.title)
            put(COLUMN_DESCRIPTION, note.description)
            put(COLUMN_TAGS, note.tags.joinToString(","))
        }
        return db.update(TABLE_NOTES, values, "$COLUMN_ID = ?", arrayOf(note.id.toString()))
    }

    fun deleteNote(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_NOTES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun searchNotes(searchQuery: String): List<Note> {
        val notes = mutableListOf<Note>()
        val db = readableDatabase

        val cursor = if (searchQuery.isBlank()) {
            db.query(
                TABLE_NOTES,
                null, null, null, null, null,
                "$COLUMN_CREATED_AT DESC"
            )
        } else {
            db.query(
                TABLE_NOTES,
                null,
                "$COLUMN_TITLE LIKE ?",
                arrayOf("%$searchQuery%"),
                null, null,
                "$COLUMN_CREATED_AT DESC"
            )
        }

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
            val titleIndex = c.getColumnIndexOrThrow(COLUMN_TITLE)
            val descIndex = c.getColumnIndexOrThrow(COLUMN_DESCRIPTION)
            val tagsIndex = c.getColumnIndexOrThrow(COLUMN_TAGS)
            val createdAtIndex = c.getColumnIndexOrThrow(COLUMN_CREATED_AT)

            while (c.moveToNext()) {
                val tagsString = c.getString(tagsIndex)
                val tagsList = if (tagsString.isBlank()) {
                    emptyList()
                } else {
                    tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                notes.add(
                    Note(
                        id = c.getLong(idIndex),
                        title = c.getString(titleIndex),
                        description = c.getString(descIndex),
                        tags = tagsList,
                        createdAt = c.getLong(createdAtIndex)
                    )
                )
            }
        }
        return notes
    }
}
