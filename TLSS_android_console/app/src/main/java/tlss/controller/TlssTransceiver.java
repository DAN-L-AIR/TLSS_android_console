package tlss.controller;

import java.util.concurrent.BlockingQueue;

public interface TlssTransceiver {
    public BlockingQueue<TlssCmd> GetInput();
    public void AddOutput(BlockingQueue<TlssCmd> output);
    public boolean AddCmd(TlssCmd cmd);
    public boolean Start();
    public void Stop();
    public String GetError();
}
