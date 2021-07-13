package tlss.application;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tlss_android_console.R;
import tlss.controller.*;

import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConsoleActivity extends AppCompatActivity {
    // цвета 'задника' кнопок
    final int BTN_ON_COLOR = 0xff00ff00;              // нагрузка включена
    final int BTN_OFF_COLOR = 0xff000000;             // нагрузка выключена
    final int btnTransitStateColor = 0xffd0d0d0;    // нагрузка в переходном состоянии

    // цвет текста сообщений
    final int TXT_ERR_MESSAGE_COLOR = 0xffff0000;   // цвет сообщения об ошибке подключения
    final int TXT_INFO_MESSAGE_COLOR = 0xffefefef;  // цвет информационного сообщения
    final int UPDATE_TEMP_TIME_MS = 5000;           // время обновления температуры
    final int UPDATE_STATE_TIME_MS = 1000;          // время обновления состояния нагрузок

    private TlssTransceiver tlssTransceiver;
    TlssNetTransportServer tlssNetTransportServer;
    private Settings settings;
    private byte loads = 0;
    private Timer timer;

    private TextView temperatureDisplay;
    private TextView btMessageText;
    private TextView modeTextView;
    private ImageButton btButton = null;
    private ImageButton loadButton[] = new ImageButton[8];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        temperatureDisplay = findViewById(R.id.tempValueText);
        modeTextView = findViewById(R.id.modeTextView_id);
        btMessageText = findViewById(R.id.btConnectStatus);
        btButton = findViewById(R.id.btConnectBtn);
        loadButton[0] = findViewById(R.id.Load1OnOffButton);
        loadButton[1] = findViewById(R.id.Load2OnOffButton);
        loadButton[2] = findViewById(R.id.Load3OnOffButton);
        loadButton[3] = findViewById(R.id.Load4OnOffButton);
        loadButton[4] = findViewById(R.id.Load5OnOffButton);
        loadButton[5] = findViewById(R.id.Load6OnOffButton);
        loadButton[6] = findViewById(R.id.Load7OnOffButton);
        loadButton[7] = findViewById(R.id.Load8OnOffButton);

        settings = ((TlssApplication)getApplication()).getSettings();
        modeTextView.setText(settings.GetTlssMode().toString());

        tlssTransceiver = ((TlssApplication)getApplication()).getTlssTransceiver();
        tlssNetTransportServer = ((TlssApplication)getApplication()).getNetTransportServer();

        InitDisplay(tlssTransceiver);

//        timer = new Timer("TLSS commands generation timer");
//        if(settings.GetTlssMode() != tlssModeServer) {
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    tlssTransceiver.AddCmd(new TlssGetTemperatureCmd());
//                }
//            }, 0, UPDATE_TEMP_TIME_MS);
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    tlssTransceiver.AddCmd(new TlssGetStateCmd());
//                }
//            }, 0, UPDATE_STATE_TIME_MS);
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                engine.AddCmd(new TlssGetTemperatureCmd());
//            }
//        }, 0, UPDATE_TIME_MS);
    }

    @Override
    protected void onDestroy() {
        //super.onDestroy();
        timer.cancel();
        if(tlssNetTransportServer != null){
            tlssNetTransportServer.StopService();
        }

        if (tlssTransceiver != null) {
            tlssTransceiver.Stop();
            TlssCmd.DisconnectBtDevice();
        }
    }

    public void LoadOnOff(View v) {
        int btN = 0;
        while (loadButton[btN++] != v) ;
        if (btN > 8) return;
        btN--;
        Loads CurLoad = Loads.values()[btN];
        if ((loads & (1 << btN)) != 0) {
            tlssTransceiver.AddCmd(new TlssLoadOffCmd(CurLoad));
        } else {
            tlssTransceiver.AddCmd(new TlssLoadOnCmd(CurLoad));
        }
        loadButton[btN].setColorFilter(btnTransitStateColor);
        v.setEnabled(false);
    }

    public void InitDisplay(TlssTransceiver tlssTransceiver) {
        new Thread(() -> {
            BlockingQueue<TlssCmd> toDisplayQueue = new ArrayBlockingQueue<>(256);
            tlssTransceiver.AddOutput(toDisplayQueue);
            TlssCmd completedCmd = null;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    completedCmd = toDisplayQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                if (completedCmd.Error().isEmpty()) {
                    if (completedCmd.getClass() == TlssGetStateCmd.class) {
                        loads = ((TlssGetStateCmd) completedCmd).GetLoadState();
                        runOnUiThread(() -> {
                            for (int btN = 0; btN < 8; btN++) {
                                boolean state = ((loads & (1 << btN)) == 0);
                                loadButton[btN].setColorFilter(state ? BTN_ON_COLOR : BTN_OFF_COLOR);
                                loadButton[btN].setEnabled(true);
                            }
                        });
                    }
                    if (completedCmd.getClass() == TlssGetTemperatureCmd.class) {
                        final short temperature = (short) (((TlssGetTemperatureCmd) completedCmd).GetTemperature() / 10);
                        runOnUiThread(() -> {
                            temperatureDisplay.setText(String.format("%d °С", temperature));
                        });
                    }
                } else {
                    final String erstr = completedCmd.Error();
                    runOnUiThread(() -> btMessageText.setText(erstr));
                }
            }
        }).start();
    }
}