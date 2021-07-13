package tlss.controller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.UUID.fromString;

public class TlssCmd implements iTlssCmd, Comparable<TlssCmd>, Serializable {
    public static final String BT_NOT_SSUPPORTED = "Блютуз не поддерживается";
    public static final String BT_NOT_ENABLED = "Блютуз адаптер выключен";
    public static final String BT_BUSY = "Блютуз занят (discovering process)";
    public static final String BT_BAD_ADDRESS = "Недопустимый блютуз адрес";
    public static final String BT_SOCKET_ERROR = "Ошибка создания сокета блютуз";
    public static final String BT_CONNECTION_ERROR = "Ошибка блютуз-подключения к контроллеру";
    static final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
    static final byte GRANULARITY_TIME_MS = 5;      // Минимальная величина отсчёта времени, в милисекундах
    private static final String BT_NOT_INITIALISED = "Блютуз не инициализирован";
    private static final String BT_CLOSE_ERROR = "Ошибка закрытия блютуз сокета";
    private static final String BT_STREAMS_CREATION = "Ошибка создания потоков ввода/вывода";
    private static final String CONNECTION_SUCCESFULL = "TLSS контроллер подключён";
    static final private List<String> BtDeviceNames = new ArrayList<>();
    static final private List<String> BtDeviceMACs = new ArrayList<>();
    static int id_ = 0;
    static private InputStream connectedInputStream = null;
    static private OutputStream connectedOutputStream = null;
    // ошибка возникшая при инициализации соединения
    static private String connectionMessge = "";
    static private BluetoothAdapter btAdapter = null;
    static private BluetoothDevice btDevice = null;
    static private BluetoothSocket btSocket = null;
    private static String LogFilePath = "";

    byte cmd;
    int id;
    byte answerLen;
    byte answer[] = null;
    String error;
    int priority;

    public TlssCmd() {
        this.id = id_++;
    }

    private static void UpdateBtDevices() {
        BtDeviceNames.clear();
        BtDeviceMACs.clear();
        if (btAdapter != null) {
            for (BluetoothDevice dev : btAdapter.getBondedDevices()) {
                BtDeviceNames.add(dev.getName());
                BtDeviceMACs.add(dev.getAddress());
            }
        }
    }

    // получить ошибку соединения
    public static String GetConnectionMessage() {
        LogtoFile(connectionMessge);
        return connectionMessge;
    }

    // получить список имён сопряжённых устройств
    public static List<String> GetBtDeviceNames() {
        if (BtDeviceNames.isEmpty()) {
            UpdateBtDevices();
        }
        return BtDeviceNames;
    }

    // получить список MAC адресов сопряжённых устройств
    public static List<String> GetBtDeviceMACs() {
        if (BtDeviceMACs.isEmpty()) {
            UpdateBtDevices();
        }
        return BtDeviceMACs;
    }

    // инициализировать BT-адаптер
    public static boolean InitBtAdapter() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            connectionMessge = String.format("%s\n", BT_NOT_SSUPPORTED);
            return false;
        }
        if (!btAdapter.isEnabled()) {
            connectionMessge = String.format("%s\n", BT_NOT_ENABLED);
            return false;
        }
        if (btAdapter.isDiscovering()) {
            connectionMessge = String.format("%s\n", BT_BUSY);
            return false;
        }
        connectionMessge = String.format("Блютуз адаптер: <%s> (%s)\n", btAdapter.getName(), btAdapter.getAddress());
        return true;
    }

    // настроить BT - соединение
    public static boolean SetupBtDevice(String btDeviceName, String btDeviceMAC) {
        try {
            btDevice = btAdapter.getRemoteDevice(btDeviceMAC);
        } catch (IllegalArgumentException e) {
            connectionMessge = String.format("<%s> %s <%s>\n", btDeviceName, BT_BAD_ADDRESS, btDeviceMAC);
            return false;
        }

        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(fromString(UUID_STRING_WELL_KNOWN_SPP));
        } catch (IOException e) {

            connectionMessge = String.format("%s \n", BT_SOCKET_ERROR);
            return false;
        }
        connectionMessge = String.format("Устройство <%s> (MAC %s)\n", btDeviceName, btDeviceMAC);
        return true;
    }

    // соединиться с BT-устройством
    static public boolean ConnectBtDevice() {
        if (btSocket == null) {
            connectionMessge = String.format("%s  \n", BT_NOT_INITIALISED);
            return false;
        }
        if (btSocket.isConnected()) {
            return true;
        }
        // подключиться к сокету
        try {
            btSocket.connect();
        } catch (IOException e) {
            String temp = BT_CONNECTION_ERROR;
            try {
                btSocket.close();
            } catch (IOException e1) {
                temp += String.format(". %s", BT_CLOSE_ERROR);
            }
            connectionMessge = String.format("%s \n", temp);
            return false;
        }
        // создать потоки ввода/вывода для приёма/передачи
        try {
            connectedInputStream = btSocket.getInputStream();
            connectedOutputStream = btSocket.getOutputStream();
        } catch (IOException e) {
            connectionMessge = String.format("%s \n", BT_STREAMS_CREATION);
            return false;
        }
        connectionMessge = String.format("%s \n", CONNECTION_SUCCESFULL);
        return true;
    }

    // отсоединиться от BT-устройства
    static public boolean DisconnectBtDevice() {
        boolean ret = true;
        if (connectedInputStream != null) {
            try {
                connectedInputStream.close();
            } catch (IOException E) {
                connectionMessge = "Disconnect BT: Unable to close InputStream ";
                ret = false;
            }
        }
        if (connectedOutputStream != null) {
            try {
                connectedOutputStream.close();
            } catch (IOException E) {
                connectionMessge += "Disconnect BT: Unable to close OutputStream ";
                ret = false;
            }
        }
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException E) {
                ret = false;
            }
        }
        return ret;
    }

    static public void SetLogPath(String LogPath) {
        LogFilePath = String.format("%s/tlss.log", LogPath);
    }

    static public boolean LogtoFile(String mes) {
        if (!LogFilePath.isEmpty()) {
            try (FileWriter fileWriter = new FileWriter(LogFilePath, true)) {
                fileWriter.write(String.format("[%s] %s\r", new Date(), mes));
            } catch (IOException e) {
                return false;
            }
            return true;
        }else{
            return false;
        }
    }

    //@Override
    public boolean Send() {
        boolean result = true;
        try {
            connectedOutputStream.write(cmd);
        } catch (IOException e) {
            error = "Command write error";
            result = false;
        }
        return result;
    }

    //@Override
    public boolean WaitAnswer(int timeOutMS) {
        boolean result = false;
        long endTimeMS = System.currentTimeMillis() + timeOutMS;
        try {
            while (connectedInputStream.available() < answerLen) {
                if (System.currentTimeMillis() > endTimeMS) {
                    error = "Read command answer timeout";
                    return false;
                }
                try {
                    Thread.sleep(GRANULARITY_TIME_MS);
                } catch (InterruptedException e) {
                }
            }
            if (connectedInputStream.available() > answerLen) {
                error = "Command answer length is too long";
            } else if (connectedInputStream.read() != cmd) {
                error = "Echo byte not matched";
            } else {
                byte secondByte = (byte) connectedInputStream.read();
                byte status = (byte) (secondByte & CMD_STATUS_MASK);
                if (status == CMD_IS_INVALID) {
                    error = "Command is not recognized by controller";
                } else if (answerLen == 2) {
                    result = true;
                } else {
                    answer = new byte[answerLen - 2];
                    connectedInputStream.read(answer);
                    result = true;
                }
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    //@Override
    public void Log() {
    }

    //@Override
    public String Error() {
        return error;
    }

    @Override
    public int compareTo(TlssCmd o) {
        if (this.priority > o.priority) {
            return 1;
        } else if (this.priority < o.priority) {
            return -1;
        } else {
            return 0;
        }
    }
}
