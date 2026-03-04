package com.example.simplenotepad

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.simplenotepad.data.*
import com.example.simplenotepad.ui.theme.SimpleNotepadTheme
import java.io.File
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.Paragraph
import java.io.FileOutputStream
import android.widget.Toast
import androidx.core.content.FileProvider




@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // ensures status/nav bars blend nicely
        setContent {
            SimpleNotepadTheme {
                NotepadApp()
            }
        }
    }
}

/* ---------------- Root NavHost ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadApp() {
    val context = LocalContext.current
    var db by remember { mutableStateOf(FileRepo.load(context)) }
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {

        // --- Home screen ---
        composable("home") {
            NotepadHome(
                db = db,
                onAddTopic = { name ->
                    val nextId = (db.topics.maxOfOrNull { it.id } ?: 0) + 1
                    db.topics.add(Topic(nextId, name))
                    FileRepo.sortTopics(db)
                    FileRepo.save(context, db)
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onOpenTopic = { topicId ->
                    navController.navigate("notes/$topicId")
                },
                onRenameTopic = { topic, newName ->
                    topic.name = newName
                    FileRepo.save(context, db)
                    db = FileRepo.load(context)
                },
                onDeleteTopic = { topic ->
                    db.topics.remove(topic)
                    FileRepo.save(context, db)
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onExportTopic = { topic ->
                    exportTopicToPdf(context, topic)
                },
                onShareTopic = { topic ->
                    shareText(context, buildTopicPlainText(topic), "Share topic")
                }
            )
        }

        // --- Notes screen ---
        composable(
            route = "notes/{topicId}",
            arguments = listOf(navArgument("topicId") { type = NavType.IntType })
        ) { backStack ->
            val topicId = backStack.arguments?.getInt("topicId") ?: 0
            val topic = db.topics.find { it.id == topicId }
            if (topic != null) {
                NotesScreen(
                    topic = topic,
                    onAddNote = { text ->
                        val nextId = (topic.notes.maxOfOrNull { it.id } ?: 0) + 1
                        topic.notes.add(Note(nextId, text, FileRepo.today()))
                        FileRepo.sortNotes(topic)
                        FileRepo.save(context, db)
                        navController.navigate("notes/${topic.id}") {
                            popUpTo("notes/${topic.id}") { inclusive = true }
                        }
                    },
                    onEditNote = { note, newText ->
                        note.text = newText
                        FileRepo.save(context, db)
                        db = FileRepo.load(context)
                    },
                    onDeleteNote = { note ->
                        topic.notes.remove(note)
                        FileRepo.save(context, db)
                        navController.navigate("notes/${topic.id}") {
                            popUpTo("notes/${topic.id}") { inclusive = true }
                        }
                    },
                    onOpenDescription = { noteId ->
                        navController.navigate("desc/${topic.id}/$noteId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // --- Description screen ---
        composable(
            route = "desc/{topicId}/{noteId}",
            arguments = listOf(
                navArgument("topicId") { type = NavType.IntType },
                navArgument("noteId") { type = NavType.IntType }
            )
        ) { backStack ->
            val topicId = backStack.arguments?.getInt("topicId") ?: 0
            val noteId = backStack.arguments?.getInt("noteId") ?: 0
            val topic = db.topics.find { it.id == topicId }
            val note = topic?.notes?.find { it.id == noteId }
            if (topic != null && note != null) {
                DescriptionScreen(
                    topic = topic,
                    note = note,
                    onSave = { newDesc ->
                        note.desc = newDesc
                        FileRepo.save(context, db)
                        db = FileRepo.load(context)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/* ---------------- Home Screen ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadHome(
    db: Database,
    onAddTopic: (String) -> Unit,
    onOpenTopic: (Int) -> Unit,
    onRenameTopic: (Topic, String) -> Unit,
    onDeleteTopic: (Topic) -> Unit,
    onExportTopic: (Topic) -> Unit,
    onShareTopic: (Topic) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var newTopic by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Simple Notepad", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            // --- Search bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search topics") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))

            // --- New topic field ---
            OutlinedTextField(
                value = newTopic,
                onValueChange = { newTopic = it },
                label = { Text("Add new topic") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        val name = newTopic.trim()
                        if (name.isNotEmpty()) {
                            onAddTopic(name)
                            newTopic = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Topic")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val name = newTopic.trim()
                        if (name.isNotEmpty()) {
                            onAddTopic(name)
                            newTopic = ""
                        }
                    }
                )
            )

            Spacer(Modifier.height(12.dp))

            val filtered = if (searchQuery.isBlank()) db.topics
            else db.topics.filter { it.name.contains(searchQuery, ignoreCase = true) }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered) { topic ->
                    TopicCard(topic, onOpenTopic, onRenameTopic, onDeleteTopic, onExportTopic, onShareTopic)
                }
            }
        }
    }
}

@Composable
private fun TopicCard(
    topic: Topic,
    onOpen: (Int) -> Unit,
    onRename: (Topic, String) -> Unit,
    onDelete: (Topic) -> Unit,
    onExport: (Topic) -> Unit,
    onShare: (Topic) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(topic.name) }
    val context = LocalContext.current  // you need context here

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onOpen(topic.id) },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                topic.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }

            // --- Dropdown menu ---
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit name") },
                    onClick = { expanded = false; showEdit = true }
                )
                DropdownMenuItem(
                    text = { Text("Delete topic") },
                    onClick = { expanded = false; onDelete(topic) }
                )
                DropdownMenuItem(
                    text = { Text("Export topic") },
                    onClick = {
                        expanded = false
                        val file = exportTopicToPdf(context, topic)
                        if (file != null) {
                            Toast
                                .makeText(
                                    context,
                                    "Exported to: ${file.absolutePath}",
                                    Toast.LENGTH_LONG
                                )
                                .show()
                        } else {
                            Toast
                                .makeText(
                                    context,
                                    "Export failed",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share topic") },
                    onClick = { expanded = false; onShare(topic) }
                )
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Rename topic") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }) },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) onRename(topic, newName.trim())
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("Cancel") }
            }
        )
    }
}


/* ---------------- Notes Screen ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    topic: Topic,
    onAddNote: (String) -> Unit,
    onEditNote: (Note, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onOpenDescription: (Int) -> Unit,
    onBack: () -> Unit
) {
    var newNote by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(topic.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newNote,
                onValueChange = { newNote = it },
                label = { Text("Add new note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        if (newNote.isNotBlank()) {
                            onAddNote(newNote.trim())
                            newNote = ""
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "Add Note") }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newNote.isNotBlank()) {
                        onAddNote(newNote.trim())
                        newNote = ""
                    }
                })
            )
            Spacer(Modifier.height(12.dp))

            val filteredNotes = if (searchQuery.isBlank()) topic.notes
            else topic.notes.filter {
                it.text.contains(searchQuery, ignoreCase = true) || it.desc.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredNotes) { note ->
                    NoteCard(note, topic, onEditNote, onDeleteNote, onOpenDescription)
                }
            }
        }
    }
}

/* ---------------- Note card ---------------- */

@Composable
private fun NoteCard(
    note: Note,
    topic: Topic,
    onEdit: (Note, String) -> Unit,
    onDelete: (Note) -> Unit,
    onOpen: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var newText by remember { mutableStateOf(note.text) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onOpen(note.id) },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(note.text, Modifier.weight(1f))
            if (note.desc.isNotBlank()) Text("📝")
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit note") },
                    onClick = { expanded = false; showEdit = true }
                )
                DropdownMenuItem(
                    text = { Text("Delete note") },
                    onClick = { expanded = false; onDelete(note) }
                )
                DropdownMenuItem(
                    text = { Text("Share note") },
                    onClick = {
                        expanded = false
                        shareText(
                            context,
                            buildNotePlainText(topic, note),
                            "Share note"
                        )
                    }
                )
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit note") },
            text = { OutlinedTextField(value = newText, onValueChange = { newText = it }) },
            confirmButton = {
                TextButton(onClick = {
                    if (newText.isNotBlank()) onEdit(note, newText.trim())
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }
}



/* ---------------- Description Screen ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptionScreen(
    topic: Topic,
    note: Note,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    var editMode by remember { mutableStateOf(note.desc.isBlank()) }
    var field by remember { mutableStateOf(note.desc) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "${note.text} · ${topic.name}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .fillMaxSize()
                .background(bg)
                .padding(16.dp)
        ) {
            if (!editMode) {
                // read-only view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { editMode = true }
                        .padding(4.dp)
                ) {
                    Text(
                        text = if (note.desc.isBlank()) "(Tap to add description…)" else note.desc,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // editable view
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("Description") },
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            onSave(field.trim())
                            editMode = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }

                    OutlinedButton(
                        onClick = { editMode = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }
            }
        }
    }
}


/* ---------------- Utility ---------------- */

private fun sanitizeFilename(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|]+"), "_").trim().ifEmpty { "export" }

private fun buildTopicPlainText(topic: Topic): String {
    val sb = StringBuilder().appendLine("# ${topic.name}")
    if (topic.notes.isEmpty()) sb.appendLine("(no notes)")
    for (n in topic.notes) {
        sb.appendLine("- ${n.text}  [${n.date}]")
        if (n.desc.isNotBlank()) sb.appendLine("  ${n.desc.trim().lines().joinToString("\n  ")}")
    }
    return sb.toString().trimEnd()
}

private fun exportTopicToPdf(context: Context, topic: Topic): File? {
    return try {
        // Public "Documents" directory, visible in file manager
        val dir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOCUMENTS
        )
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, sanitizeFilename("${topic.name}.pdf"))

        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()
        document.add(Paragraph("Topic: ${topic.name}\n"))
        document.add(Paragraph("----------------------------\n\n"))

        if (topic.notes.isEmpty()) {
            document.add(Paragraph("(no notes)\n"))
        } else {
            for (note in topic.notes) {
                document.add(Paragraph("- ${note.text}  [${note.date}]\n"))
                if (note.desc.isNotBlank()) {
                    document.add(Paragraph("  ${note.desc.trim()}\n"))
                }
                document.add(Paragraph("\n"))
            }
        }

        document.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


private fun shareText(context: Context, text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private fun buildNotePlainText(topic: Topic, note: Note): String {
    val sb = StringBuilder()
    sb.appendLine(note.text)
    sb.appendLine("Topic: ${topic.name}   Date: ${note.date}")
    if (note.desc.isNotBlank()) {
        sb.appendLine()
        sb.appendLine(note.desc.trim())
    }
    return sb.toString().trimEnd()
}

