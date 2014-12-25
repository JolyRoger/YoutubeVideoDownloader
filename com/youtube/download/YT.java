package com.youtube.download;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Java or Scala code which downloads mp4 video from youtube url,
* i.e. given an url like this https://www.youtube.com/watch?v=JGku8J7Wb6Y
* it should discover mp4 video there and download it locally.
* The code should be in a runnable jar, so that it can be run like this:
* java -jar video_dowloader.jar https://www.youtube.com/watch?v=JGku8J7Wb6Y
*/
public class YT {

    enum Mode {
        Silent, Info, Debug, Chatty
    }
    enum YoutubeVideoInfo {
        Link, Title
    }

    static Mode mode = Mode.Silent;
    private static boolean GET_FROM_MAIN_PAGE = true;
    private static HashMap<String, String> pars = null;
    private static final String VIDEO_LINK = "link";
    private static final String COMMON_INFO = "You can also use -silent -info or -debug options to define output stream " +
            "severity. (" + mode + " by default)";
    private static final String VALIDATION_ERROR_MESSAGE = "Please type or paste valid youtube URL";
    private static final String COMMON_ERROR_MESSAGE = "Can't download youtube video. ";
    private static final String PARAMETERS_TO_REMOVE = "fallback_host,type,codecs,quality," + VIDEO_LINK;
    static final String VIDEO_INFO_URL = "http://www.youtube.com/get_video_info?el=detailpage&asv=3&video_id=";
    private static final String VIDEO_STREAM = "url_encoded_fmt_stream_map";
    private static final String TITLE = "title";
    private static final String YOUTUBE_REGEX = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=))((\\w|-){11})(?:\\S+)?$";
//    private static final String YOUTUBE_REGEX = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=))((\\w|-){11})(?:\\S+)?$";

    private synchronized static void saveYoutubeFile(String url) {
        try {
            HashMap<YoutubeVideoInfo, String> info = new HashMap<>();
            obtainYoutubeVideo(url, info);
            String title = StringUtil.decode(info.get(YoutubeVideoInfo.Title));
            Log.print("Title: " + title, Mode.Info, true);
            Log.print("Link to video: " + info.get(YoutubeVideoInfo.Link), Mode.Debug, true);
            NetUtil.saveToDisk(new URL(info.get(YoutubeVideoInfo.Link)), title + ".mp4");
        } catch (IOException e) {
            Log.print(COMMON_ERROR_MESSAGE, Mode.Silent, false);
            Log.print(e.getMessage(), Mode.Debug, false);
        } catch (RuntimeException e) {
            Log.print(COMMON_ERROR_MESSAGE, Mode.Silent, false);
            Log.print(e.getMessage(), Mode.Debug, false);
        }
    }

    private static void obtainYoutubeVideo(String url, HashMap<YoutubeVideoInfo, String> info) throws IOException {
        String videoId = StringUtil.getParameter(url.trim(), "v");
        if (videoId == null) throw new IOException("Video ID can't be equals null");

        if (mode == Mode.Chatty) {
            String stremaMap0 = StringUtil.getParameter(NetUtil.getVideoInfo(videoId), VIDEO_STREAM);
            String decodedUrl0 = StringUtil.decode(stremaMap0);
            Log.print("Stream map0: " + decodedUrl0, Mode.Chatty, true);
            String[] urls0 = decodedUrl0.split("url=");
            HashMap<String, String> sigMap = new HashMap<>();
            for (int i = 0; i < urls0.length; i++) {
                String par = StringUtil.getParameter(urls0[i], "s");
                if (par != null) {
                    String[] parr = par.split("\\.");
                    Log.print(urls0[i] + "\t" + (parr != null && parr.length > 1 ? (parr[0].length() + "." + parr[1].length()) : ("???")), Mode.Chatty, true);
                } else Log.print(urls0[i] + "\t" + "No sig", Mode.Chatty, true);
            }
            System.out.println("-----------------------------------------------------------------------------\n\n");
        }

        String streamMap;
        if (GET_FROM_MAIN_PAGE) streamMap = NetUtil.getVideoInfo2(url);
        else streamMap = StringUtil.getParameter(NetUtil.getVideoInfo(videoId), VIDEO_STREAM);

        Log.print("Decoded videoInfo: " + streamMap + '\n', Mode.Chatty, true);
        if (streamMap == null) {
            throw new IOException("Video information does not contain link to mp4");
        }
        String title = StringUtil.getParameter(NetUtil.getVideoInfo(videoId), TITLE);
        info.put(YoutubeVideoInfo.Title, title);
        String decodedUrl = StringUtil.decode(streamMap);
        String[] urls = decodedUrl.split("url=");
        HashMap<String, HashMap<String, String>> videoData = new HashMap<>();
        HashMap<String, HashMap<String, String>> noHttpVideoData = new HashMap<>();

        int noUrlIndex = 0;
        for (int i = 0; i < urls.length; i++) {
            HashMap<String, String> hm = getVideoData(urls[i]);
            Log.print(urls[i], Mode.Chatty, true);
            if (urls[i].startsWith("http")) videoData.put(hm.get("itag"), hm);
            else noHttpVideoData.put(hm.getOrDefault("itag", new Integer(--noUrlIndex).toString()), hm);
        }

        Log.print("Available itags: " + videoData.keySet(), Mode.Debug, true);

        HashMap<String, String> itagData = getPrefferedItag(videoData);
        appendValidSignature(noHttpVideoData, itagData);
        String videoLink = composeUrl(itagData, url);
        info.put(YoutubeVideoInfo.Link, videoLink);
    }

    private static void appendValidSignature(HashMap<String, HashMap<String, String>> noHttpVideoData, HashMap<String, String> itagData) {
        String itag = itagData.get("itag");
        if (noHttpVideoData.containsKey(itag)) {
            System.out.println("noHttpVideoData containsKey " + itag);
            HashMap<String, String> nhData = noHttpVideoData.get(itag);
            itagData.putAll(nhData);
            Log.print("Put all unhttp fields to data", Mode.Chatty, true);
            return;
        } else {
            noHttpVideoData.forEach((k, v) -> {
                if (!v.containsKey("itag")) {
                    System.out.println("v does not contains itag at all");
                    v.forEach((a,b) -> {
                        if (a.matches("s(ig)?(nature)?")) {
                            Log.print("Change old " + a + " with new: " + b, Mode.Chatty, true);
                            itagData.put(a, b);
                        }
                    });
                } else {
                    System.out.println("Alas v contains itag = " + v.get("itag"));
                }
            });
        }
    }

    public static HashMap<String, String> getVideoData(String url) {
//        if (!url.startsWith("http")) return null;
        String[] equalities = url.split("[?&]");
        HashMap<String, String> out = new HashMap<>();
        int directLink = url.indexOf("?");
        if (directLink != -1) out.put(VIDEO_LINK, url.substring(0, directLink));
        for (String equality : equalities) {
            addData(equality, out);
        }
        return out;
    }

    private static void addData(String equality, HashMap<String, String> out) {
        int firstEq = equality.indexOf('=');
        if (firstEq == -1) return;
        int lastEq = equality.lastIndexOf('=');
        if (firstEq != lastEq) {
            StringBuilder sb = new StringBuilder(equality.substring(firstEq+1, lastEq));
            sb = new StringBuilder(sb.reverse().toString().replaceFirst("[,;]", "&")).reverse();
            String[] newEq = sb.toString().split("&");
            addData(equality.substring(0, firstEq+1) + newEq[0], out);
            addData(newEq[1] + equality.substring(lastEq), out);
            return;
        }
        String[] data = equality.split("=");
        if (out.containsKey(data[0].trim())) return;
        out.put(data[0].trim(), data[1]/*.replaceAll(",", "%2C").replaceAll("/","%2F")*/);
    }

    private static String composeUrl(HashMap<String, String> data, String url) throws IOException, RuntimeException {
        if (data == null) throw new IOException("Can't get mp4 itag parameter");
        Log.print("Selected itag: " + data.get("itag"), Mode.Debug, true);
        StringBuilder sb = new StringBuilder();
        String[] remove = PARAMETERS_TO_REMOVE.split(",");
        boolean crypted = false;

        sb.append(data.get(VIDEO_LINK) + "?");
        for (String s : data.keySet()) {
            if (contains(s, remove)) continue;
            String sig = data.get(s);
            if ("s".equals(s) || "sig".equals(s) || "signature".equals(s) && sig.length() > 81) {       // 81 is 40.40 - length of unscrambled youtube signature
                crypted = true;
                Log.print("Video is " + "crypted", Mode.Info, true);
                sb.append("signature=" + Decipher.decipher(StringUtil.trimSignature(sig), url) + StringUtil.AMP);
            } else {
                sb.append(s + "=" + data.get(s) + StringUtil.AMP);
            }
        }
        if (!crypted) Log.print("Video is not crypted", Mode.Info, true);
        return sb.substring(0, sb.length() - 1);
    }

    private static HashMap<String, String> getPrefferedItag(HashMap<String, HashMap<String, String>> meta) {
        HashMap<String, String>  data = meta.get("22");
        if (data == null) data = meta.get("37");
        if (data == null) data = meta.get("38");
        if (data == null) data = meta.get("18");
        if (data == null) data = meta.get("138");
        if (data == null) data = meta.get("264");
        if (data == null) data = meta.get("137");
        if (data == null) data = meta.get("136");
        if (data == null) data = meta.get("135");
        if (data == null) data = meta.get("134");
        if (data == null) data = meta.get("133");
        if (data == null) data = meta.get("160");
        return data;
    }

    private static boolean contains(String s, String[] sArr) {
        for (String o : sArr) if (o.trim().equals(s.trim())) return true;
        return false;
    }

   static long timestamp() {
       return new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()).getTime();
   }

   public static void main(String... args) {
       if (args.length == 0) {
           Log.print(VALIDATION_ERROR_MESSAGE, Mode.Silent, false);
           Log.print(COMMON_INFO, Mode.Silent, true);
       } else {
           if (args.length > 1 && args[1].toLowerCase().trim().equals("-debug")) mode = Mode.Debug;
           if (args.length > 1 && args[1].toLowerCase().trim().equals("-info")) mode = Mode.Info;
           if (args.length > 1 && args[1].toLowerCase().trim().equals("-chatty")) mode = Mode.Chatty;

           if (args[0].matches(YOUTUBE_REGEX)) {
               if (!args[0].startsWith("http")) args[0] = "https://" + args[0];
               if (args[0].startsWith("http://")) args[0] = args[0].replace("http://", "https://");
               Log.print("Please wait a moment...", Mode.Silent, true);
               YT.saveYoutubeFile(args[0]);
           } else {
               Log.print(VALIDATION_ERROR_MESSAGE, Mode.Silent, true);
           }
       }
   }
}
