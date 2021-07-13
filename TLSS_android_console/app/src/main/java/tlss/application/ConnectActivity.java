package tlss.application;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tlss_android_console.R;
import tlss.controller.*;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectActivity extends AppCompatActivity {
    public static final int SETUP_TYPE_MODE = 0;
    public static final int SETUP_TYPE_DEVICES = 1;
    public static final int SETUP_TYPE_NET = 2;
    public static final int SETUP_TYPE_ALL = 127;
    // настройки
    private static final int REQUEST_BT_ENABLE = 1;
    private static final int REQUEST_BT_DEVICE = 2;
    private static final int REQUEST_TLSS_SETUP_MODE = 3;
    private static final int REQUEST_NET_SETUP = 4;
    private static final int ENTER_SELECT_MODE_DELAY_MS = 2500;   // в течении этого времени можно войти в экран выбора режима
    private static final int DISCOVERING_TIMEOUT_SEC = 10;
    private static final int ANIMATION_TICK_MS = 100;
    private static final int BT_TRY_CONNECT = 3;

    ReentrantLock changeModeLock = new ReentrantLock();

    private boolean startInProgress = true;

    private Settings settings;

    private TlssMode tlssMode;

    private Timer timer;
    private Timer timer1;
    private ActivityRequest activityRequest;

    private ImageView connectProgressImage;
    private Button changeModeButton;
    private TextView initLogScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        connectProgressImage = findViewById(R.id.connectImageView);
        changeModeButton = findViewById(R.id.changeModeButton);
        initLogScreen = findViewById(R.id.initLogScreen);

        timer = new Timer("TLSS connection animation timer");
        timer1 = new Timer("Init delay Timer");
        activityRequest = new ActivityRequest();

        ((TlssApplication) getApplication()).setSettings(new Settings(this));
        settings = ((TlssApplication) getApplication()).getSettings();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (startInProgress) {
            startInProgress = false;
            tlssMode = settings.GetTlssMode();
            initLogScreen.append(String.format("Режим: %s \n", tlssMode.toString()));
            changeModeButton.setEnabled(true);

            SetupLog();

            //запуск анимации
            timer.schedule(new TimerTask() {
                final int[] progressImage = {
                        R.drawable.turtle_progressbar_0,
                        R.drawable.turtle_progressbar_1,
                        R.drawable.turtle_progressbar_2,
                        R.drawable.turtle_progressbar_3,
                        R.drawable.turtle_progressbar_4,
                        R.drawable.turtle_progressbar_5,
                        R.drawable.turtle_progressbar_6};
                int progressImageIndex = 0;
                int counter = 1;

                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressImageIndex == progressImage.length - 1) progressImageIndex = 1;
                            connectProgressImage.setImageResource(progressImage[progressImageIndex++]);
                            //initLogScreen.setText(initLogScreen.getText() + "Этап настройки" + counter++ + "... Oк\n");
                        }
                    });
                }
            }, 0, ANIMATION_TICK_MS);

            // планируем выбор режима инициализации
            timer1.schedule(new SelectInitMode(), ENTER_SELECT_MODE_DELAY_MS);
        }
    }

    private void SetupLog() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath() + "/tlss");
            if(!sdPath.exists()){
                sdPath.mkdir();
            }
            TlssCmd.SetLogPath(sdPath.getAbsolutePath());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected  void onDestroy(){
        timer.cancel();
        timer1.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_BT_ENABLE: {
                activityRequest.SetResult(resultCode == Activity.RESULT_OK);
                break;
            }
            case REQUEST_BT_DEVICE: {
                activityRequest.SetResult(resultCode == Activity.RESULT_OK);
                break;
            }
            case REQUEST_TLSS_SETUP_MODE: {
                TlssMode newMode = settings.GetTlssMode();
                if (newMode.ordinal() != tlssMode.ordinal()) {
                    tlssMode = newMode;
                    initLogScreen.append(String.format("Режим изменён на %s \n", tlssMode.toString()));
                }
                changeModeLock.unlock();
                break;
            }
            case REQUEST_NET_SETUP: {
                activityRequest.SetResult(resultCode == Activity.RESULT_OK);
                break;
            }
        }
    }

    public void onSetupModeClick(View v) {
        if (changeModeLock.tryLock()) {
            Intent intent = new Intent(this, SetupActivity.class);
            //intent.putExtra("InitMode", settings.GetTlssMode().ordinal());
            intent.putExtra("SetupMode", SETUP_TYPE_MODE);
            startActivityForResult(intent, REQUEST_TLSS_SETUP_MODE);
        }
        changeModeButton.setVisibility(View.INVISIBLE);
    }

    public void  onResetClick(View v){
        settings.Clear();
    }


    private boolean InitBt() {
        int count = 0;
        while (!TlssCmd.InitBtAdapter()) {
            runOnUiThread(() -> initLogScreen.append(TlssCmd.GetConnectionMessage()));
            if (TlssCmd.GetConnectionMessage().contains(TlssCmd.BT_NOT_SSUPPORTED)) {
                return false;
            }
            if (TlssCmd.GetConnectionMessage().contains(TlssCmd.BT_NOT_ENABLED)) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_BT_ENABLE);
                if (activityRequest.GetResult()) {
                    continue;
                } else {
                    return false;
                }
            }
            if (TlssCmd.GetConnectionMessage().contains(TlssCmd.BT_BUSY)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (count++ > DISCOVERING_TIMEOUT_SEC) {
                    return false;
                }
            }
            //runOnUiThread(() -> initLogScreen.append("Ошибка не обрабатывается\n"));
            return false;
        }
        runOnUiThread(() -> initLogScreen.append(TlssCmd.GetConnectionMessage()));

        count = 0;
        while (!TlssCmd.SetupBtDevice(settings.GetBtName(), settings.GetBtMac())) {
            {
                runOnUiThread(() -> initLogScreen.append(TlssCmd.GetConnectionMessage()));
                if (TlssCmd.GetConnectionMessage().contains(TlssCmd.BT_BAD_ADDRESS)) {
                    Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
                    intent.putExtra("SetupMode", SETUP_TYPE_DEVICES);
                    startActivityForResult(intent, REQUEST_BT_DEVICE);
                    if (activityRequest.GetResult()) {
                        continue;
                    } else {
                        return false;
                    }
                }
//                Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
//                intent.putStringArrayListExtra("btNames", (ArrayList<String>) TlssCmd.GetBtDeviceNames())
//                        .putStringArrayListExtra("btMACs", (ArrayList<String>) TlssCmd.GetBtDeviceMACs());
//                startActivityForResult(intent, REQUEST_BT_DEVICE);
//                if (activityRequest.GetResult()) {
//                    continue;
//                } else {
//                    return false;
//                }
            }
        }
        runOnUiThread(() -> initLogScreen.append(TlssCmd.GetConnectionMessage()));

        count = 0;
        while (!TlssCmd.ConnectBtDevice()) {
            runOnUiThread(() -> initLogScreen.append(TlssCmd.GetConnectionMessage()));
            if (TlssCmd.GetConnectionMessage().contains(TlssCmd.BT_CONNECTION_ERROR)) {
                if(count++ <= BT_TRY_CONNECT){
                    int finalCount = count;
                    runOnUiThread(() -> initLogScreen.append(String.format("Подключение попытка %d\n", finalCount)));
                    continue;
                }
                else {
                    Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
                    intent.putExtra("SetupMode", SETUP_TYPE_DEVICES);
                    startActivityForResult(intent, REQUEST_BT_DEVICE);
                    if (activityRequest.GetResult()) {
                        TlssCmd.SetupBtDevice(settings.GetBtName(), settings.GetBtMac());
                        continue;
                    } else {
                        return false;
                    }
                }
            }
        }
        runOnUiThread(() -> initLogScreen.append(TlssCmd.GetConnectionMessage()));
        return true;
    }

    private boolean StartServer(TlssNetTransportServer netTransportServer) {
        while (!netTransportServer.StartService()) {
            runOnUiThread(() -> initLogScreen.append(netTransportServer.GetError()));
            if (netTransportServer.GetError().contains(TlssNetTransportServer.WAITING_CONNECTION_ERROR)) {
                Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
                intent.putExtra("SetupMode", SETUP_TYPE_NET);
                startActivityForResult(intent, REQUEST_NET_SETUP);
                if (activityRequest.GetResult()) {
                    netTransportServer.Setup(settings.GetPort());
                    continue;
                } else {
                    return false;
                }
            }
            if (netTransportServer.GetError().contains(TlssNetTransportServer.SECUTITY_PERMISION_ABSENT)) {
//TODO Добавить переход в активити включения разрешений работы с сетью
                return false;
            }
            runOnUiThread(() -> initLogScreen.append("Обработка ошибки не предусмотрена\n"));
            return false;
        }
        return true;
    }

    private boolean StartClient(TlssTransceiver tlssTransceiver) {
        while (!tlssTransceiver.Start()) {
            runOnUiThread(() -> initLogScreen.append(tlssTransceiver.GetError()));
            if (tlssTransceiver.GetError().contains(TlssTransceiverNet.UNKNOW_SERVER)) {
                Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
                intent.putExtra("SetupMode", SETUP_TYPE_NET);
                startActivityForResult(intent, REQUEST_NET_SETUP);
                if (activityRequest.GetResult()) {
                    ((TlssTransceiverNet) tlssTransceiver).Setup(settings.GetServerIP(), settings.GetPort());
                    continue;
                } else {
                    return false;
                }
            }
            if (tlssTransceiver.GetError().contains(TlssTransceiverNet.SECUTITY_PERMISION_ABSENT)) {
//TODO Добавить переход в активити включения разрешений работы с сетью
                return false;
            }
            runOnUiThread(() -> initLogScreen.append("Обработка ошибки не предусмотрена\n"));
            return false;
        }
        return true;
    }

    private class ActivityRequest {
        private boolean activityRequest;
        private boolean resultInValid = true;

        synchronized void SetResult(boolean res) {
            activityRequest = res;
            resultInValid = false;
            notify();
        }

        synchronized boolean GetResult() {
            try {
                while (resultInValid) {
                    wait();
                }
                resultInValid = true;
                return activityRequest;
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    private class SelectInitMode extends TimerTask {
        @Override
        public void run() {
            changeModeLock.lock();
            runOnUiThread(() -> changeModeButton.setEnabled(false));
            changeModeLock.unlock();

            boolean result = false;
            TlssTransceiver tlssTransceiver = null;
            TlssNetTransportServer netTransportServer = null;

            switch (tlssMode) {
                case tlssModeDirect:
                    ((TlssApplication) getApplication()).setTlssTransceiver(new TlssTransceiverBase());
                    tlssTransceiver = ((TlssApplication) getApplication()).getTlssTransceiver();
                    tlssTransceiver.Start();
                    result = InitBt();
                    break;
                case tlssModeServer:
                    ((TlssApplication) getApplication()).setTlssTransceiver(new TlssTransceiverBase());
                    tlssTransceiver = ((TlssApplication) getApplication()).getTlssTransceiver();

                    ((TlssApplication) getApplication()).setNetTransportServer(new TlssNetTransportServer(settings.GetPort()));
                    netTransportServer = ((TlssApplication) getApplication()).getNetTransportServer();
                    netTransportServer.SetInputQueue(tlssTransceiver.GetInput());
                    tlssTransceiver.AddOutput(netTransportServer.GetOutputQueue());
                    tlssTransceiver.Start();
                    result = InitBt() && StartServer(netTransportServer);
                    break;
                case tlssModeClient:
                    ((TlssApplication) getApplication()).setTlssTransceiver(new TlssTransceiverNet(settings.GetServerIP(), settings.GetPort()));
                    tlssTransceiver = ((TlssApplication) getApplication()).getTlssTransceiver();
                    result = StartClient(tlssTransceiver);
                    break;
            }
            timer.cancel();
            timer1.cancel();
            if (result) {
                runOnUiThread(() -> initLogScreen.append("Инициализация успешно завершена\n"));
                startActivity(new Intent(getApplicationContext(), ConsoleActivity.class));
            } else {
                runOnUiThread(() -> initLogScreen.append("Ошибка инициализации\nПриложение будет закрыто\n"));
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finishAffinity();
                System.exit(0);
            }
        }
    }
}