package tlss.controller;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// класс для исполнения команд и получения данных на контроллере Tlss
public class TlssTransceiverBase extends Thread implements TlssTransceiver {
    final int timeOutMS = 5000;
    final Set<BlockingQueue<TlssCmd>> outputList = new LinkedHashSet<>();
    BlockingQueue<TlssCmd> cmds = new ArrayBlockingQueue<>(64);
    //    private BlockingQueue<TlssCmd> cmds  = new PriorityBlockingQueue<>(64);
    //private AtomicBoolean isUpateCmdPresent = new AtomicBoolean();

    public TlssTransceiverBase() {
        //isUpateCmdPresent.set(false);
    }

    @Override
    public synchronized boolean AddCmd(TlssCmd cmd) {
        try {
//            if (cmd.getClass() != TlssGetStateCmd.class ||
//                isUpateCmdPresent.compareAndSet(false, true)) {
//                cmds.put(cmd);
//            }
            cmds.put(cmd);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean Start() {
        this.start();
        return true;
    }

    @Override
    public BlockingQueue<TlssCmd> GetInput() {
        return cmds;
    }

    @Override
    public void AddOutput(BlockingQueue<TlssCmd> output) {
        outputList.add(output);
    }

    @Override
    public void Stop() {
        this.interrupt();
        cmds.clear();
        for (BlockingQueue<TlssCmd> q : outputList) {
            q.clear();
        }
    }

    @Override
    public String GetError() {
        return "";
    }

    @Override
    public void run() {
        TlssCmd Command;
        while (!Thread.interrupted()) {
            try {
                Command = GetNextCommand();
                if (Command.Send()) {
                    Command.WaitAnswer(timeOutMS);
                }
                Command.Log();
                TlssCmd finalCommand = Command;
                //outputList.forEach((q)->q.offer(finalCommand));
                for (BlockingQueue<TlssCmd> q : outputList) {
                    q.offer(finalCommand);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private TlssCmd GetNextCommand() throws InterruptedException {
        TlssCmd cmd = cmds.take();
//        if (cmd.getClass() != TlssGetStateCmd.class){
//            isUpateCmdPresent.set(false);
//        }
        return cmd;
    }
}
