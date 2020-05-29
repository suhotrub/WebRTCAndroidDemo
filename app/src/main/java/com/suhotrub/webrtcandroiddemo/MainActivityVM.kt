package com.suhotrub.webrtcandroiddemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suhotrub.webrtcandroiddemo.network.SocketsInteractor
import org.webrtc.SessionDescription

class MainActivityVM : ViewModel() {

    private val socketsInteractor: SocketsInteractor = SocketsInteractor.instance
    private val membersData = MutableLiveData<List<String>>()
    private val videocallData = MutableLiveData<VideocallData>()

    fun getMembers(): LiveData<List<String>> = membersData
    fun getVideocallData(): LiveData<VideocallData> = videocallData

    fun onCreate() {
        socketsInteractor.setOnMembersListener { membersData.postValue(it) }
        socketsInteractor.setOnSDPReceived { sessionDescription ->
            startVideocall(null, sessionDescription)
        }
    }

    fun onMemberSelected(sessionId: String) {
        startVideocall(sessionId)
    }

    private fun startVideocall(sessionId: String?, sessionDescription: SessionDescription? = null) {
        sessionId?.let(socketsInteractor::setActiveSessionId)
        videocallData.postValue(VideocallData(sessionDescription))
    }

    fun clearScreenData() {
        videocallData.postValue(null)
    }

    data class VideocallData(val sessionDescription: SessionDescription?)
}