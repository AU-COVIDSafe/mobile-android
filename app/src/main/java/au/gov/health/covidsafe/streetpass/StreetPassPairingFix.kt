package au.gov.health.covidsafe.streetpass

import android.bluetooth.BluetoothGatt
import android.content.pm.ApplicationInfo
import android.os.Build
import au.gov.health.covidsafe.logging.CentralLog
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.lang.reflect.Field
import java.lang.reflect.Proxy

/**
 * When BluetoothGatt.readCharacteristic is called on a characteristic that requires bonding of some
 * sort (i.e. Authenticated/Authorised/Encrypted), BluetoothGatt.mBluetoothGattCallback will
 * automatically attempt to bond with the remote device (see
 * https://android.googlesource.com/platform/frameworks/base/+/76c1d9d5e15f48e54fc810c3efb683a0c5fd14b0/core/java/android/bluetooth/BluetoothGatt.java).
 *
 * Specifically, BluetoothGatt.mBluetoothGattCallback.onCharacteristicRead and
 * BluetoothGatt.mBluetoothGattCallback.onCharacteristicWrite will be called by the operating system
 * (see https://android.googlesource.com/platform/system/bt/) and, should the response status  be
 * GATT_INSUFFICIENT_AUTHENTICATION or GATT_INSUFFICIENT_ENCRYPTION it will attempt to re-read/write
 * the characteristic requesting either AUTHENTICATION_MITM or AUTHENTICATION_NO_MITM and increment
 * the retry counter BluetoothGatt.mAuthRetryState. Should the counter already be equal to
 * AUTH_RETRY_STATE_MITM (int 2) the read will fail.
 *
 * Should the read succeed, mAuthRetryState will be reset to AUTH_RETRY_STATE_IDLE (int 0)
 *
 * In a previous version of this patch, the mAuthRetryState (or older mAuthState) fields were
 * rewritten, using the reflection API, prior to a GATT such that, should authentication be required
 * for the next GATT operation the device would not offer it. This relied on the application targeting
 * API28 as mAuthRetryState was marked as greylist-max-p and Google is instituting an API29 (post-P)
 * requirement on all published applications in November 2020. This would mean the fix could not be
 * included in releases post November 2020 and devices without android security patches would become
 * vulnerable to CVE-2020-12856 again.
 *
 * In order to protect devices, a second version of the patch has been written which does not target
 * API29 blacklisted APIs. Specifically, the mService field of BLuetoothGatt is marked as grey-listed,
 * meaning that it may be blacklisted in a future version but is available for use in API29 (and it
 * appears API30 too). The patch replaces the reference to mService with a Proxy object which rewrites
 * the arguments of readCharacteristic and writeCharacterstic to ensure, even if the Android platform
 * attempts to initiate pairing to exchange with a malicious COVIDSafe/fake device, the request sent
 * to the underlying bluetooth daemon will not contain this pairing directive.

 *
 * This leverages Java's reflection API.
 *
 * Caveats:
 *   * Nil presently as mService is currently greylisted with no enforced max API version yet.
 */
object StreetPassPairingFix {
    private const val TAG = "StreetPassPairingFix"
    private var initFailed = false
    private var initComplete = false

    private var bluetoothGattClass = BluetoothGatt::class.java
    private var iBluetoothGattClass: Class<*>? = null


    private var mServiceField: Field? = null

    /**
     * Initialises all the reflection references used by bypassAuthenticationRetry
     *
     * It has been verified that the accessed fields have existed since android's initial BLE
     * support was added
     *
     * Returns true if object is in valid state
     */
    @Synchronized
    private fun tryInit(): Boolean {
        // Check if function has already run and failed
        if (initFailed || initComplete) {
            return !initFailed
        }
        CentralLog.i(TAG, "Initialising StreetPassParingFix fields")
        try {

            // mService has been available since the first Android BLE commit
            // https://android.googlesource.com/platform/frameworks/base/+/9908112fd085d8b0d91e0562d32eebd1884f09a5
            //
            // As of 22/07/2020, Android 10/API29 marks BluetoothGatt mService as "greylist" and are
            // not restricted at any API level
            // Landroid/bluetooth/BluetoothGatt;->mService:Landroid/bluetooth/IBluetoothGatt;,greylist
            //
            // Per googles documentation, "Greylist: Non-SDK interfaces that you can use as long as
            // they are not restricted for your app's target API level."
            // https://developer.android.com/distribute/best-practices/develop/restrictions-non-sdk-interfaces
            mServiceField = bluetoothGattClass.getDeclaredField("mService")
            CentralLog.i(TAG, "Found mService")
            // IBLuetoothGatt is not included in the android SDK, but we can get a reference to it
            // via the field type
            iBluetoothGattClass = mServiceField?.type
            CentralLog.i(TAG, "Found IBluetoothGatt")
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
            CentralLog.i(TAG, "Attempting to proxy mService IBluetoothGatt instance")
            // Set the field accessible (if required)
            val mServiceAccessible = mServiceField!!.isAccessible
            if (!mServiceAccessible) {
                mServiceField!!.isAccessible = true
            }
            //Get a reference to the IBLuetoothGatt implementation being used by this BluetoothGatt
            // Instance - this is what is called to initiate a read/write to a preipheral
            val mService: Object = mServiceField!!.get(gatt) as Object

            // Wrap the IBLuetoothGatt instance in a Proxy object in order to intercept calls to
            // readCharacteristic and writeCharacteristic. IBluetoothGattInvocationHandler will catch
            // calls to these functions and rewrite their authReq field to ensure no pairing attempts
            // occur
            val mServiceProxy = Proxy.newProxyInstance(gatt.javaClass.classLoader,
                    Array(1) { iBluetoothGattClass!! },
                    IBluetoothGattInvocationHandler(mService))

            // Write the proxy back to BluetoothGatt.mService
            mServiceField!!.set(gatt, mServiceProxy)

            // Reset accessibility
            mServiceField!!.isAccessible = mServiceAccessible
            }
        catch (e: IllegalAccessException) {
            // Field was inaccessible when written
            CentralLog.i(TAG,
                    "Encountered access excepion in bypassAuthenticationRetry: " + e.message)

            }
        catch (e: SecurityException) {
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
