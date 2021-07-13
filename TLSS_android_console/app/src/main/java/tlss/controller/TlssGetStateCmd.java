package tlss.controller;

import android.util.Log;
import java.io.Serializable;

// команда получения состояния нагрузок
public class TlssGetStateCmd extends TlssCmd implements Serializable {
    public TlssGetStateCmd() {
        super();
        cmd = OPC_GET_LOAD_STATE;
        answerLen = 3;
        priority = 3;
    }
    // получить байт, каждый бит которого отражает состояние одной нагрузки (одного выхода)
    public byte GetLoadState(){
//TODO::  Выбросить исключение, если answer = null
        return answer[0];
    }

    @Override
    public void Log() {
        LogtoFile(String.format("%d GET_STATE", id));
    }
}
