package com.youtube.download;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by torquemada on 12/20/14.
 */
public class StringUtil {

    static final String AMP = "&";
    static final String VIDEO_STREAM = "url_encoded_fmt_stream_map";

    static String getHtml5PlayerName(String playerLink, String pattern) throws RuntimeException {
        if (pattern == null) pattern = "s.ytimg.com/yts/jsbin/html5player-";
        int index = playerLink.indexOf(pattern) + pattern.length();
        if (index - pattern.length() == - 1) throw new RuntimeException("Body does not contain html5player");
        String player = playerLink.substring(pattern.length(), index + playerLink.substring(index).indexOf(".js"));
        return player;
    }

    static String getHtml5PlayerLink(StringBuilder body, String pattern) {
        if (body == null || body.length() == 0) return "";
        if (pattern == null) pattern = "s.ytimg.com\\/yts\\/jsbin\\/html5player-";
        int index = body.indexOf(pattern) + pattern.length();
        if (index == pattern.length() - 1) return getHtml5PlayerLink(body, pattern.replace("\\\\", ""));
        if (index - pattern.length() == - 1) throw new RuntimeException("Body does not contain html5player");

        String playerLink = body.substring(index - pattern.length(), index + body.substring(index).indexOf(".js") + 3).replaceAll("\\\\", "");
        Log.print("Link to player: " + playerLink, YT.Mode.Debug, true);
        return playerLink.replaceAll("\\\\", "");
    }

    static String decode(String encodedUrl) throws UnsupportedEncodingException {
        String result = java.net.URLDecoder.decode(encodedUrl, "UTF-8").replaceAll("\\\\u0026", AMP);
//        return result;
        if (result.equals(encodedUrl)) return result;
        else return decode(result);
    }

    public static String getDecodeFunction(StringBuilder body, StringBuilder name) {
        StringBuilder out = new StringBuilder();
        String sf = getScrambleFunction(body, name);
        out.append(sf);
        Set<String> of = getOperationFunctionSet(body, sf);
        for (String func : of) {
            out.append(getFunction(body, func));
        }
        return out.toString();
    }

    private static String getScrambleFunction(StringBuilder body, StringBuilder name) {
        String sigPat = "\"signature\"\\s*,\\s*";
        Matcher m = Pattern.compile(sigPat + "\\w*").matcher(body);
        if (m.find()) {
            String funcName = m.group().replaceAll(sigPat, "");
            name.append(funcName);
            return getFunction(body, funcName);
        }
        return null;
    }

    private static Set<String> getOperationFunctionSet(StringBuilder body, String scrambleFunction) {
        Matcher m = Pattern.compile("\\w+\\.\\w+\\([\\w,]*\\)").matcher(scrambleFunction);
        HashSet<String> functions = new HashSet<>();
        while (m.find()) {
            functions.add(m.group().substring(0, m.group().indexOf('.')));
        }
        return functions;
    }

    private static String getFunction(StringBuilder body, String name) {
        String funcPat1 = "function\\s+" + name + "\\s*\\([\\w*,*]\\)\\{.*?\\}\\s*;";
        String funcPat2 = "var\\s+" + name + "\\s*=\\s*\\{.*?\\}\\s*;";

        String out = getFunction0(body, funcPat1);
        if (out.isEmpty()) out = getFunction0(body, funcPat2);
        return out;
    }

    private static String getFunction0(StringBuilder body,  String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(body);
        if (m.find()) return m.group(); else return "";
    }

    private static String getParameter(String[] pList, String key) {
        for (int i = 0; i < pList.length; i++) {
            String[] kvPair = pList[i].split("=");
            if (kvPair[0].equals(key) ) {
                if (kvPair[1] != null) {
                    return kvPair[1];
                } else return null;
            }
        }
        return null;
    }

    static String[] getParameterList(String videoUrl) {
        String[] parameters = videoUrl.split("\\?");
        if (parameters.length > 1) {
            return parameters[1].split(AMP);
        } else return parameters[0].split(AMP);
    }



    static String getParameter(String videoUrl, String key) {
        return getParameter(getParameterList(videoUrl), key);
    }

    static String trimSignature(String s) {
        Matcher m = Pattern.compile("([0-9A-Z]{40,})\\.([0-9A-Z]{40,})").matcher(s);
        if (m.find()) {
            return m.group();
        }
        return s;
    }

    public static String getVideoInfo(StringBuilder body) throws IOException {
        String pattern = "\\\"\\s*" + StringUtil.VIDEO_STREAM + "\\s*\\\"\\s*:.*?\\\".*?\\\"";
        Matcher m = Pattern.compile(pattern).matcher(body);

        if (m.find()) {
            String[] res = m.group().split("\"");
            return res[res.length-1];
        } else return null;

//        int streamPos = body.indexOf(StringUtil.VIDEO_STREAM);
//        if (streamPos == -1) throw new IOException("Check your youtube link. Can't obtain video information");
//        int beg = body.indexOf("\"", body.indexOf(":", streamPos + StringUtil.VIDEO_STREAM.length()));
//        int end = body.indexOf("\"", beg + 1);
//        return body.substring(beg, end);
    }
}


