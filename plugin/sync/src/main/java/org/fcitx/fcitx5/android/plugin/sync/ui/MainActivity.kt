package org.fcitx.fcitx5.android.plugin.sync.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.fcitx.fcitx5.android.plugin.sync.data.LocalRepository
import org.fcitx.fcitx5.android.plugin.sync.data.LocalRepository
import org.fcitx.fcitx5.android.plugin.sync.databinding.ActivityMainBinding
import org.fcitx.fcitx5.android.plugin.sync.engine.GistSyncProvider
import org.fcitx.fcitx5.android.plugin.sync.engine.SyncProvider
import org.fcitx.fcitx5.android.plugin.sync.engine.WebDavSyncProvider
import okhttp3.OkHttpClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var localRepository: LocalRepository
    private val httpClient = OkHttpClient()

    private val openTreeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                localRepository.takePersistableUriPermission(uri)
                updatePathDisplay()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        localRepository = LocalRepository(this)

        setupUI()
        setupUI()
        loadConfig()
        updatePathDisplay()
    }

    private fun loadConfig() {
        if (localRepository.serviceType == "webdav") {
            binding.rbWebDav.isChecked = true
            binding.webDavConfig.visibility = View.VISIBLE
            binding.gistConfig.visibility = View.GONE
        } else {
            binding.rbGist.isChecked = true
            binding.webDavConfig.visibility = View.GONE
            binding.gistConfig.visibility = View.VISIBLE
        }
        
        binding.etGistToken.setText(localRepository.gistToken)
        binding.etGistId.setText(localRepository.gistId)
        binding.etWebDavUrl.setText(localRepository.webDavUrl)
        binding.etWebDavUser.setText(localRepository.webDavUser)
        binding.etWebDavPass.setText(localRepository.webDavPass)
    }

    private fun saveConfig() {
        localRepository.serviceType = if (binding.rbWebDav.isChecked) "webdav" else "gist"
        localRepository.gistToken = binding.etGistToken.text.toString()
        localRepository.gistId = binding.etGistId.text.toString()
        localRepository.webDavUrl = binding.etWebDavUrl.text.toString()
        localRepository.webDavUser = binding.etWebDavUser.text.toString()
        localRepository.webDavPass = binding.etWebDavPass.text.toString()
    }

    private fun getProvider(): SyncProvider {
        return if (binding.rbWebDav.isChecked) {
            WebDavSyncProvider(
                httpClient,
                binding.etWebDavUrl.text.toString(),
                binding.etWebDavUser.text.toString(),
                binding.etWebDavPass.text.toString()
            )
        } else {
            GistSyncProvider(
                httpClient,
                binding.etGistToken.text.toString(),
                binding.etGistId.text.toString()
            )
        }
    }

    private fun setupUI() {
        binding.serviceTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbGist.id) {
                binding.gistConfig.visibility = View.VISIBLE
                binding.webDavConfig.visibility = View.GONE
            } else {
                binding.gistConfig.visibility = View.GONE
                binding.webDavConfig.visibility = View.VISIBLE
            }
        }

        binding.btnSelectFolder.setOnClickListener {
            openTreeLauncher.launch(localRepository.getAccessIntent())
        }

        binding.btnSelectiveSync.setOnClickListener {
            startActivity(Intent(this, SelectiveSyncActivity::class.java))
        }

        binding.btnSyncUpload.setOnClickListener {
        binding.btnSyncUpload.setOnClickListener {
            saveConfig()
            binding.tvStatus.text = "Status: Connecting..."
            binding.btnSyncUpload.isEnabled = false
            
            lifecycleScope.launch(Dispatchers.IO) {
                val provider = getProvider()
                val connectResult = provider.connect()
                
                withContext(Dispatchers.Main) {
                    if (connectResult.isSuccess) {
                        binding.tvStatus.text = "Status: Uploading..."
                        // TODO: Implement actual file iteration and upload
                        binding.tvStatus.text = "Status: Upload Success (Mock)"
                    } else {
                        binding.tvStatus.text = "Error: ${connectResult.exceptionOrNull()?.message}"
                    }
                    binding.btnSyncUpload.isEnabled = true
                }
            }
        }

        binding.btnSyncDownload.setOnClickListener {
        binding.btnSyncDownload.setOnClickListener {
             saveConfig()
             binding.tvStatus.text = "Status: Connecting..."
             binding.btnSyncDownload.isEnabled = false
             
             lifecycleScope.launch(Dispatchers.IO) {
                val provider = getProvider()
                val connectResult = provider.connect()
                
                withContext(Dispatchers.Main) {
                     if (connectResult.isSuccess) {
                        binding.tvStatus.text = "Status: Downloading..."
                        // TODO: Implement actual download logic
                        binding.tvStatus.text = "Status: Download Success (Mock)"
                     } else {
                        binding.tvStatus.text = "Error: ${connectResult.exceptionOrNull()?.message}"
                     }
                     binding.btnSyncDownload.isEnabled = true
                }
             }
        }
    }

    private fun updatePathDisplay() {
        val path = localRepository.getFriendlyPath()
        binding.tvSelectedPath.text = path
        binding.btnSelectiveSync.isEnabled = localRepository.getRootDocument() != null
        binding.btnSyncUpload.isEnabled = localRepository.getRootDocument() != null
        binding.btnSyncDownload.isEnabled = localRepository.getRootDocument() != null
    }
}
