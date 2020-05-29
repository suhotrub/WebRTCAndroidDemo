package com.suhotrub.easywebrtc

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import com.suhotrub.easywebrtc.config.DefaultEasyWebRTCConfig
import com.suhotrub.easywebrtc.config.EasyWebRTCConfig
import kotlinx.android.synthetic.main.fragment_videocall.bottomInfoPanel
import kotlinx.android.synthetic.main.fragment_videocall.hangup
import kotlinx.android.synthetic.main.fragment_videocall.localSurfaceView
import kotlinx.android.synthetic.main.fragment_videocall.remoteSurfaceView
import kotlinx.android.synthetic.main.fragment_videocall.switchCamera
import kotlinx.android.synthetic.main.fragment_videocall.switchMic
import kotlinx.android.synthetic.main.fragment_videocall.topInfoPanel
import org.webrtc.RendererCommon
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit


class VideocallFragment : Fragment() {

    companion object {

        private const val PANELS_TIMEOUT = 3000L
        private const val PANELS_DURATION = 300L

        fun createInstance(
            signallingService: SignallingService,
            easyWebRTCConfig: EasyWebRTCConfig,
            sessionDescription: SessionDescription? = null
        ): VideocallFragment {
            return VideocallFragment().apply {
                offer = sessionDescription
                this.signallingService = signallingService
                this.easyWebRTCConfig = easyWebRTCConfig
            }
        }
    }

    private var offer: SessionDescription? = null
    private lateinit var signallingService: SignallingService
    private lateinit var easyWebRTCClient: EasyWebRTCClient

    private var panelsAnimator: AnimatorSet? = null
    private var audioManager: AudioManager? = null
    private var oldAudioMode: Int? = null
    private var isMicrophoneMuted: Boolean = false
    private var isSpeakerPhoneOn: Boolean? = null

    private var time: Long = 0
    private val timer by lazy {
        Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    time++
                    activity?.runOnUiThread {
                        updateTextView()
                    }
                }
            }, 0, 1000)
        }
    }

    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    lateinit var easyWebRTCConfig: EasyWebRTCConfig

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_videocall, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true

        if (!::signallingService.isInitialized) {
            easyWebRTCConfig.onHangup()
            return
        }

        try {
            audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            oldAudioMode = audioManager?.mode;
            isSpeakerPhoneOn = audioManager?.isSpeakerphoneOn;
            isMicrophoneMuted = audioManager?.isMicrophoneMute ?: false

            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
            audioManager?.isMicrophoneMute = false
        } catch (t: Throwable) {
        }

        easyWebRTCClient = DefaultEasyWebRTCClient(context!!, signallingService, easyWebRTCConfig)
        if (offer != null) easyWebRTCClient.answerCall(offer!!) else easyWebRTCClient.initCall()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()

        localVideoTrack?.let(this::onLocalVideoTrack)
        remoteVideoTrack?.let(this::onRemoteVideoTrack)
    }

    fun onLocalVideoTrack(videoTrack: VideoTrack) {
        localVideoTrack = videoTrack
        localSurfaceView.setMirror(true)
        videoTrack.addSink(localSurfaceView)
    }

    fun onRemoteVideoTrack(videoTrack: VideoTrack) {
        remoteVideoTrack = videoTrack
        videoTrack.setEnabled(true)
        videoTrack.addSink(remoteSurfaceView)
        timer
    }

    private fun initUi() {

        remoteSurfaceView.init(easyWebRTCConfig.getEglBaseContext(), null)
        remoteSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        remoteSurfaceView.setEnableHardwareScaler(true)

        localSurfaceView.init(easyWebRTCConfig.getEglBaseContext(), null)
        localSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        localSurfaceView.setEnableHardwareScaler(true)


        remoteSurfaceView.setOnClickListener { showInfoPanels() }
        hangup.setOnClickListener {
            easyWebRTCClient.hangup()
        }

        switchMic.setOnClickListener {
            isMicrophoneMuted = !isMicrophoneMuted
            switchMic.setImageResource(
                if (isMicrophoneMuted) R.drawable.ic_mic_off_white_24dp else R.drawable.ic_mic_white_24dp
            )
            audioManager?.isMicrophoneMute = isMicrophoneMuted
        }
        switchCamera.setOnClickListener { easyWebRTCClient.switchCamera(localSurfaceView) }
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

    private fun updateTextView() {

        val seconds = time - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(time))
        val minutes =
            TimeUnit.SECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(time))
        val hours = TimeUnit.SECONDS.toHours(time)

        val b = StringBuilder()
        b.append(
            when {
                hours == 0L -> ""
                hours < 10 -> "0$hours:"
                else -> "$hours:"
            }
        )
        b.append(
            when {
                minutes == 0L -> "00"
                minutes < 10 -> "0$minutes"
                else -> minutes.toString()
            }
        )
        b.append(":")
        b.append(
            when {
                seconds == 0L -> "00"
                seconds < 10 -> "0$seconds"
                else -> seconds.toString()
            }
        )
        topInfoPanel.text = b
    }

    private fun restoreAudio() {
        try {
            audioManager?.isSpeakerphoneOn = isSpeakerPhoneOn!!
            audioManager?.mode = oldAudioMode!!
        } catch (t: Throwable) {
        }
    }

    override fun onDestroy() {
        easyWebRTCClient.hangup()
        onHangup()
        super.onDestroy()
    }

    fun onHangup() {
        restoreAudio()
        timer.cancel()
    }
}

