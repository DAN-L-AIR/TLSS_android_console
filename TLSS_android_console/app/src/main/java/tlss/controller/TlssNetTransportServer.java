package tlss.controller;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TlssNetTransportServer extends Thread implements TlssNetTransport {

    public static final String CREATING_SOCKET_ERROR = "Ошибка создания сокета";
    public static final String WAITING_CONNECTION_ERROR = "Ошибка во время ожидания соединения";
    public static final String SERVER_INTERRUPTED = "Сервер прерван";
    public static final String SECUTITY_PERMISION_ABSENT = "Отсутствуют разрешения на доступ в Интернет";
    private String error;

    private int port;
    private ServerSocket serverSocket;
    private final int START_RECONNECT_DELAY_MS = 200;
    private final int RECONNECT_NUM = 5;

    private BlockingQueue<TlssCmd> inputQueue = null;
    private BlockingQueue<TlssCmd> outputQueue = new ArrayBlockingQueue<>(64);

    private Thread outputNetThread;
    private Thread inputNetThread;

    public TlssNetTransportServer(int port){
        this.port = port;
    }

    public void Setup(int port){
        this.port = port;
    }

    @Override
    public void SetInputQueue(@NonNull BlockingQueue<TlssCmd> queue) {
        inputQueue = queue;
    }

    @Override
    public BlockingQueue<TlssCmd> GetOutputQueue() {
        return outputQueue;
    }

    @Override
    public boolean StartService() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            error = String.format("%s \n", CREATING_SOCKET_ERROR);
            return false;
        }
        this.start();
        return true;
    }

    @Override
    public void StopService(){
        this.interrupt();
    }

    @Override
    public String GetError() {
        TlssCmd.LogtoFile(String.format("TlssNetTransport: %s",error));
        return error;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try(Socket socket = serverSocket.accept();
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());)
            {
                inputNetThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            inputQueue.put((TlssCmd) inputStream.readObject());
                        } catch (IOException e) {
                            outputNetThread.interrupt();
                            break;
//TODO ЛОГ!!
                        } catch (InterruptedException e) {
                            break;
                        } catch (ClassNotFoundException e) {
//TODO ЛОГ!!
                            e.printStackTrace();
                        }
                    }
                });
                outputNetThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            outputStream.writeObject(outputQueue.take());
                        } catch (IOException e) {
                            inputNetThread.interrupt();
                            break;
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });
                inputNetThread.start();
                outputNetThread.start();
                inputNetThread.join();
                outputNetThread.join();
            } catch (InterruptedException e) {
                error = String.format("%s \n", SERVER_INTERRUPTED);
                break;
            } catch (IOException e){
                error = String.format("%s \n", WAITING_CONNECTION_ERROR);
                break;
            }
            catch (SecurityException e){
                error = String.format("%s \n", SECUTITY_PERMISION_ABSENT);
                break;
            }
        }
        inputNetThread.interrupt();
        outputNetThread.interrupt();
        try {
            serverSocket.close();
        } catch (IOException e) {
//TODO В лог!            e.printStackTrace();
        }
    }
}
