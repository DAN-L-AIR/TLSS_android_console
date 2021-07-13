package tlss.controller;
// Команда вЫключения нагрузки с заданным номером
public class TlssLoadOffCmd extends TlssCmd{

    public TlssLoadOffCmd(Loads load){
        super();
        cmd = (byte) (OPC_SET_LOAD_OFF | load.ordinal());
        answerLen = 2;
        priority = 5;
    }

    @Override
    public void Log() {
        LogtoFile(String.format("%d LOAD %d Off", id, cmd & (byte)0x0F));
    }
}
