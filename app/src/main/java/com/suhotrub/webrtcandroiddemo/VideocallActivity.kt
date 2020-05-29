package com.suhotrub.webrtcandroiddemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.suhotrub.easywebrtc.VideocallFragment
import com.suhotrub.easywebrtc.config.DefaultEasyWebRTCConfig
import com.suhotrub.easywebrtc.helpers.Consts
import com.suhotrub.easywebrtc.helpers.SDPParcelable
import com.suhotrub.webrtcandroiddemo.network.SocketsInteractor
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

class VideocallActivity : AppCompatActivity() {

    private var videocallFragment: VideocallFragment? = null

    private val defaultEasyWebRTCConfig = object : DefaultEasyWebRTCConfig() {
        override fun onHangup() {
            runOnUiThread {
                videocallFragment?.onHangup()
                finish()
            }
        }

        override fun onLocalVideoTrack(localVideoTrack: VideoTrack) {
            runOnUiThread {
                videocallFragment?.onLocalVideoTrack(localVideoTrack)
            }
        }

        override fun onRemoteVideoTrack(remoteVideoTrack: VideoTrack) {
            runOnUiThread {
                videocallFragment?.onRemoteVideoTrack(remoteVideoTrack)
            }
        }

        override fun getRTCConfig(): PeerConnection.RTCConfiguration {
            return PeerConnection.RTCConfiguration(
                listOf(
                    PeerConnection.IceServer.builder(Consts.GOOGLE_STUN)
                        .createIceServer(),
                    PeerConnection.IceServer.builder("turn:82.146.49.33:443?transport=tcp")
                        .setUsername("test")
                        .setPassword("test")
                        .createIceServer()
                )
            ).apply { iceTransportsType = PeerConnection.IceTransportsType.RELAY }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videocall)

        val fm = supportFragmentManager
        val tag = "VideocallFragment"
        val sdp = intent.extras?.getParcelable<SDPParcelable>(SDP_EXTRA)?.toSDP()

        videocallFragment = fm.findFragmentByTag(tag) as? VideocallFragment

        if (videocallFragment == null) {
            videocallFragment = VideocallFragment.createInstance(
                SocketsInteractor.instance,
                defaultEasyWebRTCConfig,
                sdp
            )
            fm.beginTransaction().add(R.id.container, videocallFragment!!, tag).commit()
        }
    }

    companion object {
        private const val SDP_EXTRA = "SDP_EXTRA"

        fun buildIntent(context: Context, sessionDescription: SessionDescription? = null): Intent {
            return Intent(context, VideocallActivity::class.java).apply {
                sessionDescription?.let {
                    putExtra(SDP_EXTRA, SDPParcelable.fromSDP(it))
                }
            }
        }
    }

}

