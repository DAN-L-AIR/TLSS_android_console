package tlss.controller;

// Команда включения нагрузки с заданным номером
public class TlssLoadOnCmd extends  TlssCmd {
    public TlssLoadOnCmd(Loads Load) {
        super();
        cmd = (byte) (OPC_SET_LOAD_ON | Load.ordinal());
        answerLen = 2;
        priority = 5;
    }
    @Override
    public void Log() {
        LogtoFile(String.format("%d LOAD %d ON", id, cmd & (byte)0x0F));
    }

}