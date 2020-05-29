package com.suhotrub.easywebrtc.helpers

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.webrtc.SessionDescription
import java.io.Serializable

@Parcelize
class SDPParcelable(
    private val type: String,
    private val desc: String
) : Parcelable {

    fun toSDP() = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), desc)

    companion object {
        fun fromSDP(sessionDescription: SessionDescription): SDPParcelable {
            return SDPParcelable(sessionDescription.type.canonicalForm(), sessionDescription.description)
        }
    }
}