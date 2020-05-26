package au.gov.health.covidsafe.streetpass

import android.bluetooth.BluetoothGatt
import android.content.pm.ApplicationInfo
import android.os.Build
import au.gov.health.covidsafe.logging.CentralLog
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.lang.reflect.Field


object StreetPassPairingFix {
    private const val TAG = "StreetPassPairingFix"
    private var initFailed = false
    private var initComplete = false

    private var bluetoothGattClass = BluetoothGatt::class.java

    private var mAuthRetryStateField: Field? = null
    private var mAuthRetryField: Field? = null

    /**
     * Initialises all the reflection references used by bypassAuthenticationRetry
     *
     * This has been checked against the source of Android 10_r36
     *
     * Returns true if object is in valid state
     */
    @Synchronized
    private fun tryInit(): Boolean {
        // Check if function has already run and failed
        if (initFailed || initComplete) {
            return !initFailed
        }

        // This technique works only up to Android P/API 28. This is due to mAuthRetryState being
        // a greylisted non-SDK interface.
        // See
        // https://developer.android.com/distribute/best-practices/develop/restrictions-non-sdk-interfaces
        // https://android.googlesource.com/platform/frameworks/base/+/45d2c252b19c08bbd20acaaa2f52ae8518150169%5E%21/core/java/android/bluetooth/BluetoothGatt.java
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && ApplicationInfo().targetSdkVersion > Build.VERSION_CODES.P) {
            CentralLog.i(TAG,
                    "Failed to initialise: mAuthRetryState is in restricted grey-list post API 28")
            initFailed = true
            initComplete = true
            return !initFailed
        }

        CentralLog.i(TAG, "Initialising StreetPassParingFix fields")
        try {
            try {
                // Get a reference to the mAuthRetryState
                // This will throw NoSuchFieldException on older android, which is handled below
                mAuthRetryStateField = bluetoothGattClass.getDeclaredField("mAuthRetryState")
                CentralLog.i(TAG, "Found mAuthRetryState")

            } catch (e: NoSuchFieldException) {
                // Prior to https://android.googlesource.com/platform/frameworks/base/+/3854e2267487ecd129bdd0711c6d9dfbf8f7ed0d%5E%21/#F0,
                // And at least after Nougat (7), mAuthRetryField (a boolean) was used instead
                // of mAuthRetryState
                CentralLog.i(TAG,
                        "No mAuthRetryState on this device, trying for mAuthRetry")

                // This will throw NoSuchFieldException again on fail, which is handled below
                mAuthRetryField = bluetoothGattClass.getDeclaredField("mAuthRetry")
                CentralLog.i(TAG, "Found mAuthRetry")

            }

            // Should be good to go now
            CentralLog.i(TAG, "Initialisation complete")
            initComplete = true
            initFailed = false
            return !initFailed

        } catch (e: NoSuchFieldException) {
            // One of the fields was missing - likely an API version issue
            CentralLog.i(TAG, "Unable to find field while initialising: "+ e.message)
        } catch (e: SecurityException) {
            // Sandbox didn't like reflection
            CentralLog.i(TAG,
                    "Encountered sandbox exception while initialising: " + e.message)
        } catch (e: NullPointerException) {
            // Probably accessed an instance field as a static
            CentralLog.i(TAG, "Encountered NPE while initialising: " + e.message)
        } catch (e: RuntimeException) {
            // For any other undocumented exception we just want to fail silentely
            CentralLog.i(TAG, "Encountered Exception while initialising: " + e.message)
        }

        // If this point is reached the initialisation has failed
        CentralLog.i(TAG,
                "Failed to initialise, bypassAuthenticationRetry will quietly fail")
        initComplete = true
        initFailed = true

        return !initFailed
    }

    /**
     * This function will attempt to bypass the conditionals in BluetoothGatt.mBluetoothGattCallback
     * that cause bonding to occur.
     *
     * The function will fail silently if any errors occur during initialisation or patching.
     *
     * See
     * https://android.googlesource.com/platform/frameworks/base/+/76c1d9d5e15f48e54fc810c3efb683a0c5fd14b0/core/java/android/bluetooth/BluetoothGatt.java#367
     * for an example of the conditional that is bypassed
     */
    @Synchronized
    fun bypassAuthenticationRetry(gatt: BluetoothGatt) {
        if (!tryInit()) {
            // Class failed to initialised correctly, return quietly
            return
        }

        try {
            // Attempt the bypass for newer android
            if (mAuthRetryStateField != null) {
                CentralLog.i(TAG, "Attempting to bypass mAuthRetryState bonding conditional")
                // Set the field accessible (if required)
                val mAuthRetryStateAccessible = mAuthRetryStateField!!.isAccessible
                if (!mAuthRetryStateAccessible) {
                    mAuthRetryStateField!!.isAccessible = true
                }

                // The conditional branch that causes binding to occur in BluetoothGatt do not occur
                // if mAuthRetryState == AUTH_RETRY_STATE_MITM (int 2), as this signifies that both
                // steps of authenticated/encrypted reading have failed to establish. See
                // https://android.googlesource.com/platform/frameworks/base/+/76c1d9d5e15f48e54fc810c3efb683a0c5fd14b0/core/java/android/bluetooth/BluetoothGatt.java#70
                //
                // Previously this class reflectively read the value of AUTH_RETRY_STATE_MITM,
                // instead of using a constant, but reportedly this doesn't work API 27+.
                //
                // Write mAuthRetryState to this value so it appears that bonding has already failed
                mAuthRetryStateField!!.setInt(gatt, 2) // Unwrap is safe

                // Reset accessibility
                mAuthRetryStateField!!.isAccessible = mAuthRetryStateAccessible
            } else  {
                CentralLog.i(TAG, "Attempting to bypass mAuthRetry bonding conditional")
                // Set the field accessible (if required)
                val mAuthRetryAccessible = mAuthRetryField!!.isAccessible
                if (!mAuthRetryAccessible) {
                    mAuthRetryField!!.isAccessible = true
                }

                // The conditional branch that causes binding to occur in BluetoothGatt do not occur
                // if mAuthRetry == true, as this signifies an attempt was made to bind
                //
                // See https://android.googlesource.com/platform/frameworks/base/+/63b4f6f5db4d5ea0114d195a0f33970e7070f21b/core/java/android/bluetooth/BluetoothGatt.java#263
                //
                // Write mAuthRetry to true so it appears that bonding has already failed
                mAuthRetryField!!.setBoolean(gatt, true)

                // Reset accessibility
                mAuthRetryField!!.isAccessible = mAuthRetryAccessible
            }

        } catch (e: SecurityException) {
            // Sandbox didn't like reflection
            CentralLog.i(TAG,
                    "Encountered sandbox exception in bypassAuthenticationRetry: " + e.message)
        } catch (e: IllegalArgumentException) {
            // Either a bad field access or wrong type was read
            CentralLog.i(TAG,
                    "Encountered argument exception in bypassAuthenticationRetry: " + e.message)
        } catch (e: NullPointerException) {
            // Probably accessed an instance field as a static
            CentralLog.i(TAG,
                    "Encountered NPE in bypassAuthenticationRetry: " + e.message)
        } catch (e: ExceptionInInitializerError) {
            CentralLog.i(TAG,
                    "Encountered reflection in bypassAuthenticationRetry: " + e.message)
        }
    }
}
