package com.example.simplenotepad.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


@Serializable
data class Note(
    var id: Int,
    var text: String,
    var date: String,      // dd/MM/yyyy
    var desc: String = ""
)

@Serializable
data class Topic(
    var id: Int,
    var name: String,
    var notes: MutableList<Note> = mutableListOf()
)

@Serializable
data class Database(
    var topics: MutableList<Topic> = mutableListOf()
)

object FileRepo {
    private const val DB_FILE = "notes_db.json"
    private val json = Json { prettyPrint = true }

    fun load(context: Context): Database {
        val file = File(context.filesDir, DB_FILE)
        if (!file.exists()) return Database()
        return runCatching { json.decodeFromString(Database.serializer(), file.readText()) }
            .getOrElse { Database() }
    }

    fun save(context: Context, db: Database) {
        val file = File(context.filesDir, DB_FILE)
        file.writeText(json.encodeToString(Database.serializer(), db))
    }

    fun today(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    /* ----- Sorting helpers ----- */
    fun sortTopics(db: Database) {
        db.topics.sortBy { it.name.lowercase(Locale.getDefault()) }
    }

    fun sortNotes(topic: Topic) {
        topic.notes.sortBy { it.text.lowercase(Locale.getDefault()) }
    }

    /* ----- Paging (tabs via swipe) ----- */
    fun <T> chunk(items: List<T>, pageSize: Int): List<List<T>> {
        if (items.isEmpty()) return listOf(emptyList())
        val chunks = mutableListOf<List<T>>()
        var i = 0
        while (i < items.size) {
            val end = (i + pageSize).coerceAtMost(items.size)
            chunks.add(items.subList(i, end))
            i = end
        }
        return chunks
    }

    /* ----- Plain-text export builders ----- */
    fun topicToPlainText(topic: Topic): String {
        val sb = StringBuilder()
        sb.appendLine("# ${topic.name}")
        if (topic.notes.isEmpty()) {
            sb.appendLine("(no notes)")
        } else {
            for (n in topic.notes) {
                sb.appendLine("- ${n.text}  [${n.date}]")
                if (n.desc.isNotBlank()) {
                    sb.appendLine("  ${n.desc.trim().lines().joinToString("\n  ")}")
                }
            }
        }
        return sb.toString().trimEnd()
    }

    fun noteToPlainText(topic: Topic, note: Note): String {
        val sb = StringBuilder()
        sb.appendLine(note.text)
        sb.appendLine("Topic: ${topic.name}   Date: ${note.date}")
        if (note.desc.isNotBlank()) {
            sb.appendLine()
            sb.appendLine(note.desc.trim())
        }
        return sb.toString().trimEnd()
    }
}
