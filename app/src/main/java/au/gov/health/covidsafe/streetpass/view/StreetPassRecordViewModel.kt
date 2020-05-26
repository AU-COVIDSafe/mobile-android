package au.gov.health.covidsafe.streetpass.view

import au.gov.health.covidsafe.streetpass.persistence.StreetPassRecord

class StreetPassRecordViewModel(record: StreetPassRecord, val number: Int) {
    val version = record.v
    val modelC = "Encrypted"
    val modelP = "Encrypted"
    val msg = record.remoteBlob
    val timeStamp = record.timestamp
    val rssi = 0
    val transmissionPower = 0
    val org = record.org

    constructor(record: StreetPassRecord) : this(record, 1)
}