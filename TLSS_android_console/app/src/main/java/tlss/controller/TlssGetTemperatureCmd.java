package tlss.controller;

// Команда получения текушей температуры
public class TlssGetTemperatureCmd extends TlssCmd {
    public TlssGetTemperatureCmd() {
        super();
        cmd = OPC_GET_TEMPERATURE;
        answerLen = 4;
        priority = 1;
    }

    // получить температуру (в дискретах по 0.5 градусов
    public short GetTemperature() {
//TODO::  Выбросить исключение, если answer = null
        short a = (short) (answer[0] & 0xff);
        short b = (short) (answer[1] & 0xff);
        b = (short) (b << 8);
        short r = (short) (b + a);
//        short r = (short)((answer[0] & 0xff) + (answer[1] & 0xff) << 8);
        return r;
    }

    @Override
    public void Log() {
        LogtoFile(String.format("%d GET_TEMPERATURE", id));
    }
}
