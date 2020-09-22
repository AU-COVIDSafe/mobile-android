package au.gov.health.covidsafe.ui.devicename

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import au.gov.health.covidsafe.R
import kotlinx.android.synthetic.main.activity_device_name_change_prompt.*
import kotlinx.android.synthetic.main.fragment_permission_device_name.*

private const val TAG = "DeviceNameChangePromptActivity"

class DeviceNameChangePromptActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_device_name_change_prompt)

        change_device_name_text_box.setText(Settings.Secure.getString(contentResolver, "bluetooth_name"))

        button_continue.setOnClickListener {
            BluetoothAdapter.getDefaultAdapter()?.name = change_device_name_text_box.text.toString()
            finish()
        }
    }

}