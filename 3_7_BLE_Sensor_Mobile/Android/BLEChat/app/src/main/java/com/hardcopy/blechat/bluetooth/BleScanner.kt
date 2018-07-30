package com.hardcopy.blechat.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.os.Build
import android.os.ParcelUuid
import android.support.annotation.RequiresApi
import com.hardcopy.blechat.utils.BeaconUtils
import com.hardcopy.btchat.utils.Logs
import java.util.*

object BleScanner {
    private val TAG = "BleScanner"

    val SCAN_FILTER_ALL = 0
    val SCAN_FILTER_NAME = 1
    val SCAN_FILTER_ADDRESS = 2
    val SCAN_FILTER_UUID = 3

    private var mBtAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var mLeScanner: BluetoothLeScanner
    private var mScanListener: BleScanListener? = null      // Scan 결과를 받기위해 외부에서 등록하는 Listener
    private var mConnectTimer: Timer? = null

    private val mDefaultUuidFilterList = ArrayList<ScanFilter>()
    private val mUuidFilterList = ArrayList<ParcelUuid>()
    private val mNameFilterList = ArrayList<String>()
    private val mAddressFilterList = ArrayList<String>()

    var isBleScanning: Boolean = false      // Scan 진행 여부를 체크
    val isBluetoothAvailable: Boolean       // 블루투스 사용 가능여부 체크
        get() = mBtAdapter.isEnabled
    var scanIBeaconOnly = false


    init {
        // 블루투스 어댑터, 스캐너 초기화
        isBleScanning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLeScanner = mBtAdapter.bluetoothLeScanner
        }
        makeDefaultScanFilters()
    }



    fun getAdapter(): BluetoothAdapter {
        return mBtAdapter
    }

    /**
     * Scan 결과를 전달받을 listener 등록 (Scan 전에 반드시 등록)
     * @param listener
     */
    fun setScanListener(listener: BleScanListener) {
        this.mScanListener = listener
    }

    /**
     * 롤리팝 이상 스캔 필터 설정
     * @param type              SCAN_FILTER_NAME = 1, SCAN_FILTER_ADDRESS = 2, SCAN_FILTER_UUID = 3
     * @param filteringText     이름으로 필터링 할 경우 설정 (SCAN_FILTER_NAME)
     * @param address           address 필터링 할 경우 설정 (SCAN_FILTER_ADDRESS)
     * @param uuid              uuid 필터링 할 경우 설정 (SCAN_FILTER_UUID)
     */
    fun addFilter(type: Int, filteringText: String?, address: String?, uuid: ParcelUuid?) {
        if (type == SCAN_FILTER_NAME) {
            if (filteringText != null) {
                mNameFilterList.add(filteringText)
            }
        } else if (type == SCAN_FILTER_ADDRESS) {
            if (address != null) {
                mAddressFilterList.add(address)
            }
        } else if (type == SCAN_FILTER_UUID) {
            if (uuid != null) {
                mUuidFilterList.add(uuid)
            }
        }
    }

    /**
     * 스캔 필터 삭제
     * @param type
     */
    fun clearScanFilter(type: Int) {
        if (type == SCAN_FILTER_NAME) {
            mNameFilterList.clear()
        } else if (type == SCAN_FILTER_ADDRESS) {
            mAddressFilterList.clear()
        } else if (type == SCAN_FILTER_UUID) {
            mUuidFilterList.clear()
        } else {
            mNameFilterList.clear()
            mAddressFilterList.clear()
            mUuidFilterList.clear()
        }
    }


    /**
     * Scan 시작/종료
     * @param enable    Scan 시작/종료
     * @param scanTime  Scan 지속시간 (단위 ms, 0일 경우 무한 스캔)
     */
    fun scheduleLeScan(enable: Boolean, scanTime: Long) {
        if (enable) {
            Logs.d(TAG, "##### start BLE scanning...")
            if (isBleScanning) {
                // Do not interfere current scanning
            } else {
                if (mBtAdapter.isEnabled) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        // 롤리팝 미만인 경우 사용되는 scan
                        mBtAdapter.startLeScan(mScanCallback)
                        isBleScanning = true

                        // Reserve scan stop
                        if (scanTime > 0) {
                            mConnectTimer?.cancel()
                            mConnectTimer = Timer()
                            mConnectTimer?.schedule(ScanTimerTask(), scanTime)
                        }
                    } else {
                        // 롤리팝 이상인 경우 사용되는 Scan
                        if (mLeScanner == null) {
                            mLeScanner = mBtAdapter.bluetoothLeScanner
                        }

                        isBleScanning = true
                        val settings = ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .build()
                        // Filtering
                        val filters = ArrayList<ScanFilter>()
                        for (strName in mNameFilterList) {
                            val filter = ScanFilter.Builder().setDeviceName(strName).build()
                            filters.add(filter)
                        }
                        for (strName in mAddressFilterList) {
                            val filter = ScanFilter.Builder().setDeviceName(strName).build()
                            filters.add(filter)
                        }
                        for (uuid in mUuidFilterList) {
                            val filter = ScanFilter.Builder().setServiceUuid(uuid).build()
                            filters.add(filter)
                        }
                        // Add default filters
                        for (filter in mDefaultUuidFilterList) {
                            filters.add(filter)
                        }
                        // Start scan
                        mLeScanner.startScan(filters, settings, mScanCallbackLp)

                        // Reserve scan stop
                        if (scanTime > 0) {
                            mConnectTimer?.cancel()
                            mConnectTimer = Timer()
                            mConnectTimer?.schedule(ScanTimerTask(), scanTime)
                        }
                    }
                } else {
                    Logs.d(TAG, "##### Cannot start scanning... BluetoothAdapter is null or not enabled")
                }
            }
        } else {
            Logs.d(TAG, "##### stop BLE scanning...")
            isBleScanning = false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mBtAdapter.stopLeScan(mScanCallback)
            } else {
                mLeScanner.stopScan(mScanCallbackLp)
            }
            mConnectTimer?.cancel()
            mConnectTimer = null

            mScanListener!!.onScanDone()
        }
    }


    /*****************************************************************
     * Private methods
     */
    /**
     * 롤리팝 이상인 경우 기본 Scan filter 생성
     */
    private fun makeDefaultScanFilters() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mDefaultUuidFilterList.clear()
            // add filter
            /**
             * Service UUID 필터를 설정할 경우 비컨이 검색되지 않는 문제 발생
             * scan result 처리 루틴에서 별도로 filtering 하도록 변경
             */
            /*ParcelUuid sample = ParcelUuid.fromString(BLE_SCAN_SAMPLE_UUID_FOR_FILTERING);
            ParcelUuid mask = ParcelUuid.fromString(BLE_SCAN_FILTERING_MASK);
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(sample, mask).build();
            mDefaultUuidFilterList.add(filter);*/
        }
    }

    /**
     * scanRecord 버퍼에서 Beacon 정보 추출
     * @param device
     * @param rssi
     * @param scanRecord
     * @return
     */
    private fun extractBeaconInfo(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray): Beacon? {
        if (device == null)
            return null

        val beacon = BeaconUtils.beaconFromLeScan(device, rssi, scanRecord)

        return if (beacon?.proximityUUID == null || beacon?.proximityUUID.length < 16) {
            null
        } else beacon
    }

    /**
     * 롤리팝 미만인 경우 사용되는 scan callback
     */
    private val mScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        val beacon = extractBeaconInfo(device, rssi, scanRecord)

        if(scanIBeaconOnly) {
            if (mScanListener != null && beacon != null
                    && beacon.proximityUUID.isNotEmpty()
                    && beacon.proximityUUID.length > 15) {
                // TODO: UUID filtering
                //if (beacon.proximityUUID.contains(Const.BLE_SCAN_FILTERING_MASK)) {
                //    mScanListener!!.onDeviceFound(device, beacon)
                //}
                mScanListener?.onDeviceFound(device, beacon)
            }
        } else {
            mScanListener?.onDeviceFound(device, beacon)
        }
    }

    /**
     * 롤리팝 이상인 경우 사용되는 Scan callback
     * @return
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val mScanCallbackLp = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val device = result.device
                val rssi = result.rssi
                val scanRecord = result.scanRecord!!.bytes

                val beacon = extractBeaconInfo(device, rssi, scanRecord)
                if(scanIBeaconOnly) {
                    if (mScanListener != null && beacon != null && beacon.proximityUUID.isNotEmpty()
                            && beacon.proximityUUID.length > 15) {
                        // TODO: UUID filtering
                        //if (beacon.proximityUUID.contains(Const.BLE_SCAN_FILTERING_MASK)) {
                        //    mScanListener!!.onDeviceFound(device, beacon)
                        //}
                        mScanListener?.onDeviceFound(device, beacon)
                    }
                } else {
                    mScanListener?.onDeviceFound(device, beacon)
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>?) {
            Logs.d(TAG, "##### onBatchScanResults")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (results != null && results.isNotEmpty()) {
                    for (result in results) {
                        val device = result.device
                        val rssi = result.rssi
                        val scanRecord = result.scanRecord!!.bytes
                        val beacon = extractBeaconInfo(device, rssi, scanRecord)

                        if(scanIBeaconOnly) {
                            if (mScanListener != null && beacon != null && beacon.proximityUUID.isNotEmpty()
                                    && beacon.proximityUUID.length > 15) {
                                // TODO: UUID filtering
                                //if (beacon.proximityUUID.contains(Const.BLE_SCAN_FILTERING_MASK)) {
                                //    mScanListener!!.onDeviceFound(device, beacon)
                                //}
                                mScanListener?.onDeviceFound(device, beacon)
                            }
                        } else {
                            mScanListener?.onDeviceFound(device, beacon)
                        }
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Logs.d(TAG, "##### Scan Failed... Error Code: $errorCode")
            mScanListener?.onScanFailed(errorCode)
        }
    }

    private class ScanTimerTask: TimerTask() {
        override fun run() {
            scheduleLeScan(false, 0)
        }
    }

}
