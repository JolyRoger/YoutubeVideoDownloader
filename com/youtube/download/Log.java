package com.youtube.download;

/**
 * Created by torquemada on 12/22/14.
 */
public class Log {

    public static void print(String message, YT.Mode mode, boolean toOut) {
        if (mode == null) mode = YT.Mode.Info;
        if (mode.ordinal() <= YT.mode.ordinal())
            if (toOut) System.out.println(message);
            else System.err.println(message);
    }
}
