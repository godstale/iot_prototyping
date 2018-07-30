package com.hardcopy.blechat.bluetooth

import android.os.Parcel
import android.os.Parcelable

class Beacon : Parcelable {
    lateinit var name: String
        private set
    lateinit var macAddress: String
        private set
    lateinit var proximityUUID: String
        private set
    var major: Int = 0
        private set
    var minor: Int = 0
        private set
    var rssi: Int = 0
        private set
    var txPower: Int = 0
        private set
    var accuracy: Double = 0.toDouble()


    constructor() {
        this.name = ""
        this.macAddress = ""
        this.proximityUUID = ""
    }

    constructor(name: String, macAddress: String, rssi: Int, proximityUUID: String, major: Int,
                minor: Int, txPower: Int) {
        this.name = name
        this.macAddress = macAddress
        this.rssi = rssi
        this.proximityUUID = proximityUUID
        this.major = major
        this.minor = minor
        this.txPower = txPower
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(name)
        out.writeString(macAddress)
        out.writeInt(rssi)
        out.writeString(proximityUUID)
        out.writeInt(major)
        out.writeInt(minor)
        out.writeInt(txPower)
    }

    companion object {

        val CREATOR: Parcelable.Creator<Beacon> = object : Parcelable.Creator<Beacon> {
            override fun createFromParcel(`in`: Parcel): Beacon {
                val beacon = Beacon()
                beacon.name = `in`.readString()
                beacon.macAddress = `in`.readString()
                beacon.rssi = `in`.readInt()
                beacon.proximityUUID = `in`.readString()
                beacon.major = `in`.readInt()
                beacon.minor = `in`.readInt()
                beacon.txPower = `in`.readInt()

                return beacon
            }

            override fun newArray(size: Int): Array<Beacon?> {
                return arrayOfNulls(size)
            }
        }
    }
}
