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
        System.out.println("Link to player: " + playerLink);
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

    static String[] getParameterList(String videoUrl, String divider) {
        String[] pList;
        if (divider != null && !divider.isEmpty()) {
            String[] parameters = videoUrl.split(divider);
            pList = parameters[parameters.length - 1].split(AMP);
        } else pList = videoUrl.split(AMP);
        return pList;
    }

    static String getParameter(String videoUrl, String divider, String key) {
        return getParameter(getParameterList(videoUrl, divider), key);
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

    public static void main(String[] args) {
//            System.out.printf("I found the text \"%s\" starting at index %d and ending at index %d.%n", m.group(), m.start(), m.end());


        String b = "a.drm_families&&(r={},E(a.drm_families.split(\",\"),function(a){r[a]=b[a]}));e=new Zq(g,e,k,h,l,r,p);h=qr(a.init);k=qr(a.index);l=Fs(a.url,e,a.s);p=parseInt(a.clen,10);a=parseInt(a.lmt,10);l&&(c.g[g]=new ys(l,\n" +
                "e,h,k,p,a))});return c}function Gs(a){if(!a)return 0;var b=Ds.exec(a);return b?3600*parseFloat(b[2]||0)+60*parseFloat(b[4]||0)+parseFloat(b[6]||0):parseFloat(a)}function Fs(a,b,c){a=new ir(a);a.set(\"alr\",\"yes\");a.set(\"keepalive\",\"yes\");a.set(\"ratebypass\",\"yes\");a.set(\"mime\",encodeURIComponent(b.mimeType.split(\";\")[0]));c&&a.set(\"signature\",Wq(c));return a}\n" +
                "function Hs(a){var b=sr(a,\"id\"),b=b.replace(\":\",\";\");\"captions\"==b&&(b=sr(a,\"lang\"));var c=sr(a,\"mimeType\"),d=sr(a,\"codecs\"),c=d?c+'; codecs=\"'+d+'\"':c,d=parseInt(sr(a,\"bandwidth\"),10)/8,e=null;cr(c)&&(e=new fr(parseInt(sr(a,\"width\"),10),parseInt(sr(a,\"height\"),10),parseInt(sr(a,\"frameRate\"),10)));var g=null,h=null;if(br(c)){g=new Xq;var h=sr(a,\"lang\")||\"\",k=tr(a,\"Role\");if(k){var k=sr(k,\"value\")||\"\",l=\"invalid\";\"main\"==k?l=\"original\":\"dub\"==k?l=\"dubbed\":\"descriptive\"==k?l=\"descriptive\":\"commentary\"==\n" +
                "k&&(l=\"commentary\");h=\"invalid\"!=l&&h?new dr(sr(a,\"yt:langName\")||h+\" - \"+l,h,0,\"original\"==l):null}else h=null}k=null;if(a=tr(a,\"ContentProtection\"))if((k=a.attributes.schemeIdUri)&&\"http://youtube.com/drm/2012/10/10\"==k.textContent)for(k={},a=a.firstChild;null!=a;a=a.nextSibling)\"yt:SystemURL\"==a.nodeName&&(k[a.attributes.type.textContent]=a.textContent.trim());else k=null;return new Zq(b,c,g,e,h,k,d)}()&&(Iq(\"Dropping current screen with id: \"+a),Kq());Gq()||Kk()}var pq=null,sq=null,xq=null,kq=null;function tq(a){var b=Tq();if(Vb(b)){var b=vk(),c=fj(\"yt-remote-session-name\")||\"\",d=fj(\"yt-remote-session-app\")||\"\",b={device:\"REMOTE_CONTROL\",id:b,name:c,app:d};a&&(b[\"mdx-version\"]=3);" +
                "q(\"yt.mdx.remote.channelParams_\",b,void 0)}}function Tq(){return t(\"yt.mdx.remote.channelParams_\")||{}}var wq=[];function Uq(a,b,c){S.call(this);this.g=a;this.o=b||0;this.j=c;this.k=y(this.mA,this)}B(Uq,S);f=Uq.prototype;f.xa=0;f.L=function(){Uq.H.L.call(this);this.stop();delete this.g;delete this.j};f.start=function(a){this.stop();this.xa=wn(this.k,n(a)?a:this.o)};f.stop=function(){this.isActive()&&xn(this.xa);this.xa=0};f.isActive=function(){return 0!=this.xa};f.mA=function(){this.xa=0;this.g&&this.g.call(this.j)};var Vq={Zm:function(a,b){var c=a[0];a[0]=a[b%a.length];a[b]=c},qz:function(a,b){a.splice(0,b)},e8:function(a){a.reverse()}};function Wq(a)" +
                "{a=a.split(\"\");Vq.qz(a,3);Vq.Zm(a,38);Vq.qz(a,1);Vq.Zm(a,16);Vq.e8(a,10);Vq.Zm(a,20);Vq.Zm(a,69);Vq.qz(a,2);Vq.Zm(a,15);return a.join(\"\")};function Xq(){};var Yq={160:\"h\",133:\"h\",134:\"h\",135:\"h\",136:\"h\",137:\"h\",264:\"h\",266:\"h\",138:\"h\",298:\"h\",299:\"h\",304:\"h\",305:\"h\",140:\"a\",161:\"H\",142:\"H\",143:\"H\",144:\"H\",222:\"H\",223:\"H\",145:\"H\",224:\"H\",225:\"H\",146:\"H\",226:\"H\",147:\"H\",149:\"A\",261:\"M\",278:\"9\",242:\"9\",243:\"9\",244:\"9\",247:\"9\",248:\"9\",271:\"9\",313:\"9\",272:\"9\",302:\"9\",303:\"9\",308:\"9\",315:\"9\",171:\"v\",250:\"o\",251:\"o\",194:\"*\",195:\"*\",220:\"*\",221:\"*\",196:\"*\",197:\"*\",198:\"V\",279:\"(\",280:\"(\",273:\"(\",274:\"(\",275:\"(\",276:\"(\",314:\"(\",277:\"(\"};function Zq(a,b,c,d,e,g,h){this.id=\"\"+a;this.g=0<=b.indexOf(\"/mp4\")?1:0<=b.indexOf(\"/webm\")?2:0<=b.indexOf(\"/x-flv\")?3:0<=b.indexOf(\"/vtt\")?4:0;this.mimeType=b;this.pa=h||0;this.j=c||null;this.video=d||null;this.Ae=e||null;this.o=g||null;this.k=Yq[this.id.split(\";\")[0]]||\"\"}function $q(a){return 2==a.g}function ar(a){return!(a.j&&a.video)}function br(a){return 0<=a.indexOf(\"opus\")||0<=a.indexOf(\"vorbis\")||0<=a.indexOf(\"mp4a\")}";

        String c = "assa\"url_encoded_fmt_stream_map\": \"Hello boy! You are crazy\"; \"No, I'm not\"";
//        String pattern = "drm\\.*\\\".*\\\"";
//        String pattern = "drm.*?\\\".*?\\\"";
        String pattern = "\\\"\\s*" + StringUtil.VIDEO_STREAM + "\\s*\\\"\\s*:.*?\\\".*?\\\"";

        System.out.println(c);
        Matcher m = Pattern.compile(pattern).matcher(c);
        if (m.find()) {
            String[] res = m.group().split("\"");
            System.out.println(res[res.length-1]);
        }

//        getDecodeFunction(new StringBuilder(b), new StringBuilder());
    }

}


