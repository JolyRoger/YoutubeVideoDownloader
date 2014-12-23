package com.youtube.download;

import java.io.IOException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Decipher {

    static String decipher(String sig, String urlToPage) {
        Log.print("try to decipher signature...", YT.Mode.Info, true);
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("nashorn");
        String decodeFunction = null;
        StringBuilder name = new StringBuilder();
//               en_US-vflsXGZP2/html5player
        try {
            StringBuilder body = NetUtil.getBody(urlToPage/* + "&gl=US&hl=en_US"*/);
            String html5PlayerLink = StringUtil.getHtml5PlayerLink(body, null);
            String html5PlayerName = StringUtil.getHtml5PlayerName(html5PlayerLink, null);
            Log.print("Player: " + html5PlayerName, YT.Mode.Info, true);
            StringBuilder scrambleBody = new StringBuilder(NetUtil.getBody("http://" + html5PlayerLink));
            decodeFunction = StringUtil.getDecodeFunction(scrambleBody, name);
            Log.print("Old sig: " + sig + "\t" + sig.split("\\.")[0].length() + "." + sig.split("\\.")[1].length() + '\n',
                    YT.Mode.Debug, true);
            engine.eval(decodeFunction);
            Log.print("Decode function: " + decodeFunction + '\n', YT.Mode.Debug, true);
            String res = engine.eval(name.toString() + "('" + sig + "')").toString();
            Log.print("New sig: " + res + "\t" + res.split("\\.")[0].length() + "." + res.split("\\.")[1].length() + '\n',
                    YT.Mode.Debug, true);
            return res;
        } catch (ScriptException e) {
            Log.print("Can't translate javascript decrypt code." + e.getMessage(), YT.Mode.Debug, false);
            Log.print("Decode function: " + decodeFunction, YT.Mode.Debug, false);
            return sig;
        } catch (IOException e) {
            Log.print("Can't decipher signature. " + e.getMessage(), YT.Mode.Debug, false);
            return sig;
        }
    }
}


