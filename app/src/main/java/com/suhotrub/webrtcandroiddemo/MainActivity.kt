package com.suhotrub.webrtcandroiddemo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import kotlinx.android.synthetic.main.activity_main.bottomInfoPanel
import kotlinx.android.synthetic.main.activity_main.hangup
import kotlinx.android.synthetic.main.activity_main.localSurfaceView
import kotlinx.android.synthetic.main.activity_main.remoteSurfaceView
import kotlinx.android.synthetic.main.activity_main.switchCamera
import kotlinx.android.synthetic.main.activity_main.topInfoPanel
import org.webrtc.VideoTrack

class MainActivity : AppCompatActivity() {

    companion object {
        const val PANELS_TIMEOUT = 3000L
        const val PANELS_DURATION = 300L
    }

    private var panelsAnimator: AnimatorSet? = null
    private var audioManager: AudioManager? = null
    private var oldAudioMode: Int? = null
    private var isSpeakerPhoneOn: Boolean? = null

    private val socketsInteractor by lazy {
        SocketsInteractor {}
    }

    private val webRTCContainer by lazy {
        WebRTCContainer(webRTCHooks, socketsInteractor)
    }

    private val webRTCHooks by lazy {
        object : WebRTCHooks({ this@MainActivity }) {
            override fun onLocalVideoTrack(localVideoTrack: VideoTrack) {
                runOnUiThread {
                    localSurfaceView.setMirror(true)
                    localSurfaceView.setEnableHardwareScaler(true)
                    localSurfaceView.init(ectx, null)

                    localVideoTrack.addSink(localSurfaceView)
                }
            }

            override fun onRemoteVideoTrack(remoteVideoTrack: VideoTrack) {
                runOnUiThread {
                    remoteVideoTrack.setEnabled(true)
                    remoteSurfaceView.setEnableHardwareScaler(true)
                    remoteSurfaceView.init(ectx, null)
                    remoteVideoTrack.addSink(remoteSurfaceView)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        socketsInteractor
        webRTCContainer

        remoteSurfaceView.setOnClickListener { showInfoPanels() }
        hangup.setOnClickListener {
            webRTCContainer.hangup()
        }
        switchCamera.setOnClickListener { webRTCContainer.switchCamera(localSurfaceView) }

        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            oldAudioMode = audioManager?.mode;
            isSpeakerPhoneOn = audioManager?.isSpeakerphoneOn;

            audioManager?.mode = AudioManager.MODE_IN_CALL
            audioManager?.isSpeakerphoneOn = true
        } catch (t: Throwable) {
        }
    }

    private fun showInfoPanels() {
        panelsAnimator?.cancel()

        topInfoPanel.visibility = View.VISIBLE
        bottomInfoPanel.visibility = View.VISIBLE
        topInfoPanel.alpha = 1.0f
        bottomInfoPanel.alpha = 1.0f

        panelsAnimator = AnimatorSet().apply {
            playTogether(
                listOf(topInfoPanel, bottomInfoPanel)
                    .map { ObjectAnimator.ofFloat(it, "alpha", .0f) }
            )
        }.apply {
            startDelay = PANELS_TIMEOUT
            duration = PANELS_DURATION
            doOnEnd {
                topInfoPanel.visibility = View.GONE
                bottomInfoPanel.visibility = View.GONE
            }
            start()
        }
    }

    override fun onDestroy() {
        try {
            audioManager?.isSpeakerphoneOn = isSpeakerPhoneOn!!
            audioManager?.mode = oldAudioMode!!
        } catch (t: Throwable) {
        }

        super.onDestroy()
    }
}
