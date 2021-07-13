package tlss.controller;

import java.io.Serializable;

// команда инициирует перезагрузку контроллнра
public class TlssResetCmd extends TlssCmd implements Serializable {
    public TlssResetCmd() {
        super();
        cmd = (OPC_RESET_CONTROLLER);
        answerLen = 2;
        priority = 7;
    }

    @Override
    public void Log() {
        LogtoFile(String.format("%d RESET", id));
    }
}
