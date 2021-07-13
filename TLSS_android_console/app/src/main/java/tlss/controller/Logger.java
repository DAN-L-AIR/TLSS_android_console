package tlss.controller;

import java.io.*;
import java.util.Date;

public class Logger {
    static final String logPath = "tlss.log";

    static public boolean LogtoFile(String mes){
        try(FileWriter fileWriter = new FileWriter(logPath, true)) {
            fileWriter.write(String.format("[%s] %s\r", new Date(), mes));
        } catch (IOException e) {
            return false;
        }
        return  true;
    }
}
