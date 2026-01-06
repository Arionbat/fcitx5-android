package org.fcitx.fcitx5.android.plugin.sync.engine

import java.io.InputStream
import java.io.File

interface SyncProvider {
    suspend fun connect(): Result<Boolean>
    suspend fun list(path: String): Result<List<String>>
    suspend fun upload(path: String, content: InputStream): Result<Boolean>
    suspend fun download(path: String): Result<InputStream>
}
