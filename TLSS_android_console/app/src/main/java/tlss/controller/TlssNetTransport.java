package tlss.controller;

import androidx.annotation.NonNull;

import java.util.concurrent.BlockingQueue;

public interface TlssNetTransport extends Runnable {
    void SetInputQueue(@NonNull BlockingQueue<TlssCmd> queue);

    BlockingQueue<TlssCmd> GetOutputQueue();

    boolean StartService();

    void StopService();

    String GetError();

    @Override
    void run();
}
