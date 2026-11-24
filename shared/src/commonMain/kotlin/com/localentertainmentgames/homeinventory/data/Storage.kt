package com.localentertainmentgames.homeinventory.data

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString

/** Persists the whole inventory as one JSON document. */
interface DataStorage {
    suspend fun read(): String?
    suspend fun write(text: String)
}

class InMemoryStorage(var content: String? = null) : DataStorage {
    var writes = 0
        private set

    override suspend fun read(): String? = content
    override suspend fun write(text: String) {
        content = text
        writes++
    }
}

/** File helpers on kotlinx-io, shared by every platform. */
object Files {
    fun ensureDir(dir: String) {
        val p = Path(dir)
        if (!SystemFileSystem.exists(p)) SystemFileSystem.createDirectories(p)
    }

    fun exists(path: String): Boolean = SystemFileSystem.exists(Path(path))

    fun readText(path: String): String? {
        val p = Path(path)
        if (!SystemFileSystem.exists(p)) return null
        return SystemFileSystem.source(p).buffered().use { it.readString() }
    }

    fun readBytes(path: String): ByteArray? {
        val p = Path(path)
        if (!SystemFileSystem.exists(p)) return null
        return SystemFileSystem.source(p).buffered().use { it.readByteArray() }
    }

    /** Writes through a temp file and an atomic move, so a crash never leaves half a file. */
    fun writeText(path: String, text: String) {
        val target = Path(path)
        target.parent?.let { if (!SystemFileSystem.exists(it)) SystemFileSystem.createDirectories(it) }
        val tmp = Path("$path.tmp")
        SystemFileSystem.sink(tmp).buffered().use { it.writeString(text) }
        SystemFileSystem.atomicMove(tmp, target)
    }

    fun writeBytes(path: String, bytes: ByteArray) {
        val target = Path(path)
        target.parent?.let { if (!SystemFileSystem.exists(it)) SystemFileSystem.createDirectories(it) }
        SystemFileSystem.sink(target).buffered().use { it.write(bytes) }
    }

    fun delete(path: String) {
        val p = Path(path)
        if (SystemFileSystem.exists(p)) SystemFileSystem.delete(p)
    }
}

class FileStorage(private val path: String) : DataStorage {
    override suspend fun read(): String? = Files.readText(path)
    override suspend fun write(text: String) = Files.writeText(path, text)
}
