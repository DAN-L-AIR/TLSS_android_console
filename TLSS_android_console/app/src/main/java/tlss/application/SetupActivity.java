package tlss.application;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tlss_android_console.R;
import tlss.controller.TlssCmd;
import tlss.controller.TlssTransceiverBase;

import java.util.List;

public class SetupActivity extends AppCompatActivity {
    final int[] modeMenuItem = new int[]{
            R.id.setDirectModeRadioButton,
            R.id.setServerModeRadioButton,
            R.id.setClientModeRadioButton};
    List<String> names;
    List<String> macs;

    private RadioGroup modesRadioGroup;
    private Button modeSelectButton;

    private RadioGroup btDevicesRadioGroup;
    private Button btDeviceSelectButton;

    private TextView ipServerField;
    private TextView portField;
    private Button btApplyNetSettings;

    Settings settings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_device_setup);

        modesRadioGroup = findViewById(R.id.selectModeRadioGroup_id);
        modeSelectButton = findViewById(R.id.btModeSelect_id);

        btDevicesRadioGroup = findViewById(R.id.selectBTDeviceRadioGroup_id);
        btDeviceSelectButton = findViewById(R.id.btDeviceSelectButton_id);

        ipServerField = findViewById(R.id.serverIpEditText);
        portField = findViewById(R.id.portEditText);
        btApplyNetSettings = findViewById(R.id.applyNetSettingsButton);

        settings = ((TlssApplication) getApplication()).getSettings();
    }

    @Override
    protected void onStart() {
        super.onStart();

        modesRadioGroup.check(modeMenuItem[settings.GetTlssMode().ordinal()]);
        modesRadioGroup.setEnabled(true);

        // активити запущена для выбора BT-устройства - контроллера TLSS
        names = TlssCmd.GetBtDeviceNames();
        macs = TlssCmd.GetBtDeviceMACs();

        if (names != null && macs != null && names.size() == macs.size() && names.size() > 0) {
            btDevicesRadioGroup.clearCheck();
            if (btDevicesRadioGroup.getChildCount() > 2) {
                btDevicesRadioGroup.removeViews(1, btDevicesRadioGroup.getChildCount() - 2);
            }
            for (int i = 0; i < names.size(); i++) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setText(names.get(i) + "\n" + macs.get(i));
                radioButton.setId(i);
                btDevicesRadioGroup.addView(radioButton, i + 1);
            }
        }

        ipServerField.setText(settings.GetServerIP());
        portField.setText(String.valueOf(settings.GetPort()));
        btApplyNetSettings = findViewById(R.id.applyNetSettingsButton);

        modesRadioGroup.setEnabled(false);
        btDevicesRadioGroup.setEnabled(false);

        modeSelectButton.setEnabled(false);
        btDeviceSelectButton.setEnabled(false);

        ipServerField.setEnabled(false);
        portField.setEnabled(false);
        btApplyNetSettings.setEnabled(false);


        int setup_type = getIntent().getIntExtra("SetupMode", -1);
        if(setup_type == ConnectActivity.SETUP_TYPE_MODE){
            modesRadioGroup.setEnabled(true);
            modeSelectButton.setEnabled(true);
        }
        if(setup_type == ConnectActivity.SETUP_TYPE_DEVICES){
            btDevicesRadioGroup.setEnabled(true);
            btDeviceSelectButton.setEnabled(true);
        }
        if(setup_type == ConnectActivity.SETUP_TYPE_NET){
            ipServerField.setEnabled(true);
            portField.setEnabled(true);
            btApplyNetSettings.setEnabled(true);
        }

    }

    public void ClickApplyMode(View v) {
        int checkedRadioButtonId = modesRadioGroup.getCheckedRadioButtonId();
        for (int i = 0; i < modeMenuItem.length; i++) {
            if (modeMenuItem[i] == checkedRadioButtonId) {
                settings.SetTlssMode(TlssMode.values()[i]);
                setResult(RESULT_OK, getIntent());
                finish();
            }
        }
    }

    public void ClickApplyDevice(View v) {
        int btSelectIndex = btDevicesRadioGroup.getCheckedRadioButtonId();
        if (btSelectIndex >= 0) {
            settings.SetBtName(names.get(btSelectIndex));
            settings.SetBtMac(macs.get(btSelectIndex));
            setResult(RESULT_OK, getIntent());
        } else {
            setResult(RESULT_CANCELED, getIntent());
        }
        finish();
    }

    public void ClickApplyNetSettings(View v) {
        settings.SetServerIP(ipServerField.getText().toString());
        settings.SetPort(Integer.parseInt(portField.getText().toString()));
        setResult(RESULT_OK, getIntent());
        finish();
    }

}