package tlss.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

public class TlssTransceiverNet extends TlssTransceiverBase {
    public static final String UNKNOW_SERVER = "Неизвестный сервер";
    public static final String CREATING_SOCKET = "Ошибка соединения через сокет";
    public static final String IO_SOCKET_ERROR = "Ошибка обмена данными через сокет";
    public static final String QUEUE_IS_FULL = "Очередь результатов заполнена";
    public static final String SECUTITY_PERMISION_ABSENT = "Отсутствуют разрешения на использование сети";
    private String error;

    private String ip;
    private int port;
    private Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    Thread inputThread;
    Thread outputThread;

//    private  BlockingQueue<TlssCmd> cmds = new ArrayBlockingQueue<>(256);
//    private  Set<BlockingQueue<TlssCmd>> outputList = new LinkedHashSet<>();

    public TlssTransceiverNet(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void Setup(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public String GetError(){
        TlssCmd.LogtoFile(String.format("TlssTransceiverNet: %s",error));
        return error;
    }

    @Override
    public void run() {

        while (!Thread.interrupted()){
            try {
                inputThread = new Thread(() -> {
                    while (!Thread.interrupted()) {
                        try {
                            TlssCmd command = (TlssCmd) inputStream.readObject();
                            for (BlockingQueue<TlssCmd> q : outputList) {
                                q.offer(command);
                            }
                        } catch (IOException e) {
                            outputThread.interrupt();
                            break;
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                outputThread = new Thread(() -> {
                    while (!Thread.interrupted()) {
                        try {
                            TlssCmd command = cmds.take();
                            outputStream.writeObject(command);
                            command.Log();
                        } catch (InterruptedException e) {
                            break;
                        } catch (IOException e) {
                            inputThread.interrupt();
//TODO логгировать
                        }
                    }
                });
                inputThread.start();
                outputThread.start();
                inputThread.join();
                outputThread.join();
                Disconnect();
                Connect();
            }catch(InterruptedException e){
                inputThread.interrupt();
                outputThread.interrupt();
//TODO надо?
//                inputThread.join();
//                outputThread.join();
                Disconnect();
                break;
            }
        }
    }

    @Override
    public boolean Start() {
        if(Connect()){
            this.start();
            return true;
        }
        return false;
    }

    @Override
    public void Stop() {
        super.Stop();
        Disconnect();
    }

    private boolean Connect() {
        try {
            clientSocket = new Socket(ip, port);
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (UnknownHostException e) {
            error = String.format("%s:%s\n", UNKNOW_SERVER, ip);
            return false;
        } catch (IOException e) {
            error = String.format("%s\n",CREATING_SOCKET);
            return false;
        } catch (SecurityException e){
            error = String.format("%s\n",SECUTITY_PERMISION_ABSENT);
            return false;
        }
        return true;
    }

    private boolean Disconnect() {
        if (clientSocket.isConnected()) {
            try {
                inputStream.close();
                outputStream.close();
                clientSocket.close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
}
