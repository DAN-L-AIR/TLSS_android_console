package tlss.controller;
// команды контроллера TLSS
public interface iTlssCmd {
    boolean Send();
    boolean WaitAnswer(int timeOut);
    void Log();
    String Error();
    // коды операций команд TLSS- контроллера
    byte OPC_GET_LOAD_STATE = 0x00;    // получить состо¤ние нагрузок
    byte OPC_GET_TEMPERATURE = 0x10;   // получить температуру
    byte OPC_SET_LOAD_OFF = 0x20;      // включить нагрузку
    byte OPC_SET_LOAD_ON = 0x30;       // выключить нагрузку
    byte OPC_RESET_CONTROLLER = 0x40;  // выполнить сброс контроллера

    byte CMD_STATUS_MASK = (byte) 0xF0; // Маска для выделения статуса распознавания команды контроллером
    byte CMD_SIZE_MASK = (byte) 0x0F;   // Маска для выделения количества байт в ответе
    byte CMD_IS_VALID = (byte) 0xA0;    // Статус выполнения команды контроллером:  команда рампознана
    byte CMD_IS_INVALID = (byte) 0x50;  //Статус выполнения команды контроллером:  команда НЕ не распознана

}
