package org.fcitx.fcitx5.android.plugin.sync.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile

class LocalRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "sync_prefs"
        private const val KEY_ROOT_URI = "root_uri"
        private const val KEY_SERVICE_TYPE = "service_type"
        private const val KEY_GIST_TOKEN = "gist_token"
        private const val KEY_GIST_ID = "gist_id"
        private const val KEY_WEBDAV_URL = "webdav_url"
        private const val KEY_WEBDAV_USER = "webdav_user"
        private const val KEY_WEBDAV_PASS = "webdav_pass"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serviceType: String
        get() = prefs.getString(KEY_SERVICE_TYPE, "gist") ?: "gist"
        set(value) = prefs.edit { putString(KEY_SERVICE_TYPE, value) }

    var gistToken: String
        get() = prefs.getString(KEY_GIST_TOKEN, "") ?: ""
        set(value) = prefs.edit { putString(KEY_GIST_TOKEN, value) }
    
    var gistId: String
        get() = prefs.getString(KEY_GIST_ID, "") ?: ""
        set(value) = prefs.edit { putString(KEY_GIST_ID, value) }

    var webDavUrl: String
        get() = prefs.getString(KEY_WEBDAV_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_WEBDAV_URL, value) }

    var webDavUser: String
        get() = prefs.getString(KEY_WEBDAV_USER, "") ?: ""
        set(value) = prefs.edit { putString(KEY_WEBDAV_USER, value) }

    var webDavPass: String
        get() = prefs.getString(KEY_WEBDAV_PASS, "") ?: ""
        set(value) = prefs.edit { putString(KEY_WEBDAV_PASS, value) }


    var rootUri: Uri?
        get() {
            val uriString = prefs.getString(KEY_ROOT_URI, null) ?: return null
            return Uri.parse(uriString)
        }
        set(value) {
            prefs.edit { putString(KEY_ROOT_URI, value?.toString()) }
        }

    fun getAccessIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            // Optional: try to open specific initial folder if known
        }
    }

    fun takePersistableUriPermission(uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        rootUri = uri
    }

    fun getRootDocument(): DocumentFile? {
        val uri = rootUri ?: return null
        return DocumentFile.fromTreeUri(context, uri)
    }

    fun getFriendlyPath(): String {
        return rootUri?.path ?: "Not Set"
    }
}
