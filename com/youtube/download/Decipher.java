package com.youtube.download;

import java.io.IOException;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Decipher {

    static String decipher(String sig, String urlToPage) {
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
            Log.print("Old sig: " + sig + "\t" + sig.split("\\.")[0].length() + "." + sig.split("\\.")[1].length(),
                    YT.Mode.Debug, true);
            engine.eval(decodeFunction);
            Log.print("Decode function: " + decodeFunction, YT.Mode.Debug, true);
            String res = engine.eval(name.toString() + "('" + sig + "')").toString();
            Log.print("New sig: " + res + "\t" + res.split("\\.")[0].length() + "." + res.split("\\.")[1].length(),
                    YT.Mode.Debug, true);
            return res;
        } catch (ScriptException e) {
            Log.print("Can't translate javascript decrypt code." + e.getMessage(), YT.Mode.Debug, false);
            Log.print("Decode function: " + decodeFunction, YT.Mode.Debug, false);
            return sig;
        } catch (IOException e) {
            Log.print("Can't get body. " + e.getMessage(), YT.Mode.Debug, false);
            return sig;
        } catch (RuntimeException e) {
            Log.print("Can't get body. " + e.getMessage(), YT.Mode.Debug, false);
            return sig;
        }
    }


    public static void main(String[] args) {
        String str = "/\\/s.ytimg.com\\/yts\\/cssbin\\/www-player-vflPfi1TF.css\", \"js\": \"\\/\\/s.ytimg.com\\/yts\\/jsbin\\/html5player-en_US-vflsXGZP2\\/html5player.js\", \"html\": \"\\/html5_player_template\"}};ytplayer.load = function() {yt.player";
        String player = StringUtil.getHtml5PlayerLink(new StringBuilder(str), null);
        Log.print("Player: " + player, YT.Mode.Info, true);
         try {
            ScriptEngineManager engineManager = new ScriptEngineManager();
            ScriptEngine engine = engineManager.getEngineByName("nashorn");
//               en_US-vflsXGZP2/html5player
            engine.eval("var Vq={Zm:function(a,b){var c=a[0];a[0]=a[b%a.length];a[b]=c},qz:function(a,b){a.splice(0,b)},e8:function(a){a.reverse()}};");
            engine.eval("function Wq(a){a=a.split(\"\");Vq.qz(a,3);Vq.Zm(a,38);Vq.qz(a,1);Vq.Zm(a,16);Vq.e8(a,10);Vq.Zm(a,20);Vq.Zm(a,69);Vq.qz(a,2);Vq.Zm(a,15);return a.join(\"\")};");
         } catch (ScriptException e) {
            e.printStackTrace();
         }
    }
}


