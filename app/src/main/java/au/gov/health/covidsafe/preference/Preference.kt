package au.gov.health.covidsafe.preference

import android.content.Context
import android.os.Build
import au.gov.health.covidsafe.R
import au.gov.health.covidsafe.security.crypto.EncryptedSharedPreferences
import au.gov.health.covidsafe.security.crypto.MasterKeys
import au.gov.health.covidsafe.security.crypto.AESEncryptionForPreAndroidM

object Preference {
    private const val PREF_ID = "Tracer_pref"
    private const val IS_ONBOARDED = "IS_ONBOARDED"
    private const val HAS_DEVICE_NAME_NOTIFICATION_DISPLAYED = "HAS_DEVICE_NAME_NOTIFICATION_DISPLAYED"
    private const val CALLING_CODE = "CALLING_CODE"
    private const val AUSTRALIA_CALLING_CODE = 61
    private const val COUNTRY_NAME_RES_ID = "COUNTRY_NAME"
    private const val AUSTRALIA_COUNTRY_NAME_RES_ID = R.string.country_region_name_au
    private const val NATIONAL_FLAG_RES_ID = "NATIONAL_FLAG_RES_ID"
    private const val AUSTRALIA_NATIONAL_FLAG_RES_ID = R.drawable.ic_list_country_au
    private const val PHONE_NUMBER = "PHONE_NUMBER"
    private const val HANDSHAKE_PIN = "HANDSHAKE_PIN"
    private const val DEVICE_ID = "DEVICE_ID"
    private const val ENCRYPTED_AES_KEY = "ENCRYPTED_AES_KEY"
    private const val AES_IV = "AES_IV"
    private const val JWT_TOKEN = "JWT_TOKEN"
    private const val IS_DATA_UPLOADED = "IS_DATA_UPLOADED"
    private const val DATA_UPLOADED_DATE_MS = "DATA_UPLOADED_DATE_MS"
    private const val UPLOADED_MORE_THAN_24_HRS = "UPLOADED_MORE_THAN_24_HRS"
    private const val FIREBASE_INSTANCE_ID = "FIREBASE_INSTANCE_ID"
    private const val NEXT_FETCH_TIME = "NEXT_FETCH_TIME"
    private const val EXPIRY_TIME = "EXPIRY_TIME"
    private const val NAME = "NAME"
    private const val IS_MINOR = "IS_MINOR"
    private const val POST_CODE = "POST_CODE"
    private const val AGE = "AGE"
    private const val CASE_STATISTIC = "CASESTATISTIC"
    private const val IS_DEVICE_NAME_CHANGE_PROMPT_DISPLAYED = "IS_DEVICE_NAME_CHANGE_DISPLAYED"
    private const val BUILD_NUMBER_FOR_POP_UP_NOTIFICATION = "BUILD_NUMBER_FOR_POP_UP_NOTIFICATION"
    private const val TURN_CASE_NUMBER = "TURN_CASE_NUMBER"
    private const val IS_REREGISTER = "IS_REREGISTER"

    fun putDeviceID(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(DEVICE_ID, value)?.apply()
    }

    fun getDeviceID(context: Context?): String {
        return context?.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.getString(DEVICE_ID, "") ?: ""
    }

    fun putEncodedAESInitialisationVector(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(AES_IV, value)?.apply()
    }

    fun getEncodedAESInitialisationVector(context: Context): String? {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getString(AES_IV, null)
    }

    fun putEncodedRSAEncryptedAESKey(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(ENCRYPTED_AES_KEY, value)?.apply()
    }

    fun getEncodedRSAEncryptedAESKey(context: Context): String? {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.getString(ENCRYPTED_AES_KEY, null)
    }

    fun putEncrypterJWTToken(context: Context?, jwtToken: String?) {
        context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                        PREF_ID,
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).edit()?.putString(JWT_TOKEN, jwtToken)?.apply()
            } else {
                val aesEncryptedJwtToken = jwtToken?.let {
                    AESEncryptionForPreAndroidM.encrypt(it)
                }

                context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                        .edit().putString(JWT_TOKEN, aesEncryptedJwtToken)?.apply()
            }
        }
    }

    fun getEncrypterJWTToken(context: Context?): String? {
        return context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                        PREF_ID,
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).getString(JWT_TOKEN, null)
            } else {
                context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                        ?.getString(JWT_TOKEN, null)?.let {
                            AESEncryptionForPreAndroidM.decrypt(it)
                        }
            }
        }
    }

    fun putHandShakePin(context: Context?, value: String?) {
        context?.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.edit()?.putString(HANDSHAKE_PIN, value)?.apply()
    }

    fun putIsOnBoarded(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putBoolean(IS_ONBOARDED, value).apply()
    }

    fun isOnBoarded(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getBoolean(IS_ONBOARDED, false)
    }

    fun putIsReRegister(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putBoolean(IS_REREGISTER, value).apply()
    }

    fun isReRegister(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getBoolean(IS_REREGISTER, false)
    }

    fun putHasDeviceNameNotificationDisplayed(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putBoolean(HAS_DEVICE_NAME_NOTIFICATION_DISPLAYED, value).apply()
    }

    fun getHasDeviceNameNotificationDisplayed(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getBoolean(HAS_DEVICE_NAME_NOTIFICATION_DISPLAYED, false)
    }

    fun putPhoneNumber(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(PHONE_NUMBER, value).apply()
    }

    fun getPhoneNumber(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.getString(PHONE_NUMBER, "") ?: ""
    }

    fun putFirebaseInstanceID(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(FIREBASE_INSTANCE_ID, value).apply()
    }

    fun getFirebaseInstanceID(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.getString(FIREBASE_INSTANCE_ID, "") ?: ""
    }

    fun putCallingCode(context: Context, value: Int) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putInt(CALLING_CODE, value).apply()
    }

    fun getCallingCode(context: Context): Int {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.getInt(CALLING_CODE, AUSTRALIA_CALLING_CODE) ?: AUSTRALIA_CALLING_CODE
    }

    fun putCountryNameResID(context: Context, value: Int) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putInt(COUNTRY_NAME_RES_ID, value).apply()
    }

    fun getCountryNameResID(context: Context): Int {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.getInt(COUNTRY_NAME_RES_ID, AUSTRALIA_COUNTRY_NAME_RES_ID)
                ?: AUSTRALIA_COUNTRY_NAME_RES_ID
    }

    fun putNationalFlagResID(context: Context, value: Int) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putInt(NATIONAL_FLAG_RES_ID, value).apply()
    }

    fun getNationalFlagResID(context: Context): Int {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.getInt(NATIONAL_FLAG_RES_ID, AUSTRALIA_NATIONAL_FLAG_RES_ID)
                ?: AUSTRALIA_NATIONAL_FLAG_RES_ID
    }

    fun putNextFetchTimeInMillis(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putLong(NEXT_FETCH_TIME, time).apply()
    }

    fun getNextFetchTimeInMillis(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getLong(
                        NEXT_FETCH_TIME, 0
                )
    }

    fun putExpiryTimeInMillis(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putLong(EXPIRY_TIME, time).apply()
    }

    fun getExpiryTimeInMillis(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getLong(
                        EXPIRY_TIME, 0
                )
    }

    fun isDataUploaded(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getBoolean(IS_DATA_UPLOADED, false)
    }

    fun setDataIsUploaded(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).edit().also { editor ->
            editor.putBoolean(IS_DATA_UPLOADED, value)
            if (value) {
                editor.putLong(DATA_UPLOADED_DATE_MS, System.currentTimeMillis())
            } else {
                editor.remove(DATA_UPLOADED_DATE_MS)
            }
        }.apply()
    }

    fun getDataUploadedDateMs(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getLong(DATA_UPLOADED_DATE_MS, System.currentTimeMillis())
    }

    fun putName(context: Context, name: String): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(NAME, name).commit()
    }

    fun getName(context: Context): String? {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getString(NAME, null)
    }

    fun putIsMinor(context: Context, minor: Boolean): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putBoolean(IS_MINOR, minor).commit()
    }

    fun isMinor(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).getBoolean(IS_MINOR, false)
    }

    fun putPostCode(context: Context, state: String): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(POST_CODE, state).commit()
    }

    fun getPostCode(context: Context): String? {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getString(POST_CODE, null)
    }

    fun putAge(context: Context, age: String): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(AGE, age).commit()
    }

    fun getAge(context: Context): String? {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getString(AGE, null)
    }

    fun setDeviceNameChangePromptDisplayed(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putBoolean(IS_DEVICE_NAME_CHANGE_PROMPT_DISPLAYED, true).commit()
    }

    fun isDeviceNameChangePromptDisplayed(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getBoolean(IS_DEVICE_NAME_CHANGE_PROMPT_DISPLAYED, false)
    }

    fun putCaseStatisticData(context: Context, caseStaticData: String): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(CASE_STATISTIC, caseStaticData).commit()
    }

    fun getCaseStatisticData(context: Context): String? {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .getString(CASE_STATISTIC, null)
    }

    fun putBuildNumber(context: Context, buildNumber: Int) {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putInt(BUILD_NUMBER_FOR_POP_UP_NOTIFICATION, buildNumber).apply()
    }

    fun getBuildNumber(context: Context): Int {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                ?.getInt(BUILD_NUMBER_FOR_POP_UP_NOTIFICATION, 0)
                ?: 0
    }

    fun setTurnCaseNumber(context: Context, turnOff: Boolean): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putBoolean(TURN_CASE_NUMBER, turnOff).commit()
    }

    fun getTurnCaseNumber(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(TURN_CASE_NUMBER, true)
    }
}
