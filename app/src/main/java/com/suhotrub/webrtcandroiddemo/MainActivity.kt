package com.suhotrub.webrtcandroiddemo

import android.Manifest
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_main.listView

class MainActivity : AppCompatActivity() {

    private val adapter by lazy {
        ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[MainActivityVM::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            ),
            123
        )

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val sessionId = adapter.getItem(position)
            sessionId?.let(viewModel::onMemberSelected)
        }

        viewModel.onCreate()
        viewModel.getMembers().observe(this, Observer<List<String>> {
            adapter.clear()
            adapter.addAll(it)
        })

        viewModel.getVideocallData().observe(this, Observer<MainActivityVM.VideocallData> {
            viewModel.clearScreenData()
            it?.let { startActivity(VideocallActivity.buildIntent(this, it.sessionDescription)) }
        })
    }
}
