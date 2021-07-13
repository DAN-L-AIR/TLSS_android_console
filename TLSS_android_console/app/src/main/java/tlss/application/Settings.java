package tlss.application;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

enum TlssMode {
    tlssModeDirect("Пульт"),
    tlssModeServer("Сервер"),
    tlssModeClient("Клиент");
    private String title;

    TlssMode(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }
}

public class Settings {
    public static final String APP_PREFERENCES_TLSS_MODE = "TLSS_MODE";
    //    public static final String APP_PREFERENCES_MODE_DIRECT = "TLSS_MODE_DIRECT";
//    public static final String APP_PREFERENCES_MODE_CLIENT = "TLSS_MODE_CLIENT";
//    public static final String APP_PREFERENCES_MODE_SERVER = "TLSS_MODE_SERVER";
    public static final int APP_PREFERENCES_MODE_DIRECT = 0;
    public static final int APP_PREFERENCES_MODE_CLIENT = 1;
    public static final int APP_PREFERENCES_MODE_SERVER = 2;
    public static final String APP_PREFERENCES_BT_DEVICE_NAME = "BT_DEVICE_NAME";
    public static final String APP_PREFERENCES_BT_DEVICE_MAC = "BT_DEVICE_MAC";
    public static final String APP_PREFERENCES_SERVER_IP = "SERVER_IP";
    public static final String APP_PREFERENCES_PORT = "PORT";
    private static final String APP_PREFERENCES = "Settings";
    private static final int DEFAULT_PORT = 30200;
    SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences = null;

    public Settings(Context context) {
        sharedPreferences = context.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public String GetBtName() {
        return sharedPreferences.getString(APP_PREFERENCES_BT_DEVICE_NAME, "Unknow");
    }

    public String GetBtMac() {
        return sharedPreferences.getString(APP_PREFERENCES_BT_DEVICE_MAC, "xx:xx:xx:xx:xx:xx");
    }

    public String GetServerIP() {
        return sharedPreferences.getString(APP_PREFERENCES_SERVER_IP, "Unknow");
    }

    public int GetPort() {
        return sharedPreferences.getInt(APP_PREFERENCES_PORT, DEFAULT_PORT);
    }

    public TlssMode GetTlssMode() {
        int tlssModeInt = sharedPreferences.getInt(APP_PREFERENCES_TLSS_MODE, APP_PREFERENCES_MODE_DIRECT);
        return TlssMode.values()[tlssModeInt];
    }

    public void Clear() {
        editor.clear();
        editor.commit();
    }


    public void SetBtName(String btName) {
        editor.putString(APP_PREFERENCES_BT_DEVICE_NAME, btName);
        editor.commit();
    }

    public void SetBtMac(String btMac) {
        editor.putString(APP_PREFERENCES_BT_DEVICE_MAC, btMac);
        editor.commit();
    }

    public void SetTlssMode(TlssMode tlssMode) {
        editor.putInt(APP_PREFERENCES_TLSS_MODE, tlssMode.ordinal());
        editor.commit();
    }

    public void SetServerIP(String ip) {
        editor.putString(APP_PREFERENCES_SERVER_IP, ip);
        editor.commit();
    }

    public void SetPort(int port) {
        editor.putInt(APP_PREFERENCES_PORT, port);
        editor.commit();
    }
}
