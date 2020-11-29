package au.gov.health.covidsafe.streetpass;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import au.gov.health.covidsafe.logging.CentralLog;

/* This class is used by StreetPassPairingFix to proxy calls between BluetoothGatt and the underlying
 * IBluetoothGatt implementation (the interface between Android SDK's bluetooth API and the system
 * bluetooth daemon)

 * It rewrites the authReq field of calls to readCharacteristic and writeCharacteristic to
 * AUTHENTICATION_NONE in order to prevent pairing attempts being initiated in proximity to a malicious
 * COVIDSafe/fake device requiring MITM or Encryption. This is still secure as COVIDSafe uses its own
 * asymmetric encryption of sent payloads.

 * The ultimate purpose of the class is to mitigate CVE-2020-12856 when targeting API29, or on older
 * devices that do not receive android security patches, as the previous fix utilised a field which
 * is to be blacklisted by Google in November 2020.
 *
 * https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html
 * https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Proxy.html
 * */

 public class IBluetoothGattInvocationHandler implements InvocationHandler {
    private Object mService; // The IBluetoothGatt instance being proxied for
    private static String TAG = "IBluetoothGattInvocationHandler";

    IBluetoothGattInvocationHandler(Object mService) {
        this.mService = mService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String memberName = method.getName();
        Integer oldAuthReq = null; // Use Integer so null can be used to detect if a rewrite occurred
        
        // This switch statement is used to rewrite the authReq field of IBLuetoothGatt to always
        // be the value BluetoothGatt.AUTHENTICATION_NONE in order to prevent pairing
        //
        // https://cs.android.com/android/platform/superproject//master:system/bt/binder/android/bluetooth/IBluetoothGatt.aidl;drc=c704fc6d805b4772a2dd11c1f40b1bc72a19f832
        // https://android.googlesource.com/platform/frameworks/base//master/core/java/android/bluetooth/BluetoothGatt.java#371
        // https://android.googlesource.com/platform/frameworks/base//master/core/java/android/bluetooth/BluetoothGatt.java#431
        switch (memberName) {
            case "readCharacteristic":
                // 4th argument is int authReq
                oldAuthReq = (int) args[3];
                args[3] = 0; // AUTHENTICATION_NONE
                break;
                case "writeCharacteristic":
                    // 5th argument is authReq
                    oldAuthReq = (int) args[4];
                    args[4] = 0; // AUTHENTICATION_NONE
                    break;
        }

        // If oldAuthReq got updated it indicates either of the target methods were called
        if (oldAuthReq != null) {
            CentralLog.Companion.i(TAG, "Rewrote " + memberName + " authReq=" + oldAuthReq + " to authReq=0");
        }

        // Invoke whichever method was called on the object, potentially with modified arguments
        CentralLog.Companion.i(TAG, "Invoking method:" + memberName);
        return method.invoke(mService, args);
    }
}