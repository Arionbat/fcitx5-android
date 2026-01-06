package org.fcitx.fcitx5.android.plugin.sync.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.plugin.sync.data.LocalRepository
import org.fcitx.fcitx5.android.plugin.sync.databinding.ActivitySelectiveSyncBinding

class SelectiveSyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectiveSyncBinding
    private lateinit var localRepository: LocalRepository
    private lateinit var adapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectiveSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        localRepository = LocalRepository(this)

        setupRecyclerView()
        loadFiles()
    }

    private fun setupRecyclerView() {
        adapter = FileAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadFiles() {
        val root = localRepository.getRootDocument() ?: return
        // Ideally this should be async
        val files = root.listFiles()
        adapter.submitList(files.toList())
    }

    inner class FileAdapter : RecyclerView.Adapter<FileAdapter.ViewHolder>() {
        private var items: List<DocumentFile> = emptyList()

        fun submitList(newItems: List<DocumentFile>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val checkbox = CheckBox(parent.context)
            checkbox.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            return ViewHolder(checkbox)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = items[position]
            holder.checkBox.text = file.name
            holder.checkBox.isChecked = true // Default to true for now
            // TODO: Bind with persistent state
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(val checkBox: CheckBox) : RecyclerView.ViewHolder(checkBox)
    }
}
