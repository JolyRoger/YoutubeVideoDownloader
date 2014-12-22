package com.youtube.download;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
* Created by Daniil Monakhov on 12/12/14.
*
* Java or Scala code which downloads mp4 video from youtube url,
* i.e. given an url like this https://www.youtube.com/watch?v=JGku8J7Wb6Y
* it should discover mp4 video there and download it locally.
* The code should be in a runnable jar, so that it can be run like this:
* java -jar video_dowloader.jar https://www.youtube.com/watch?v=JGku8J7Wb6Y
*
*/
public class YT {


    enum YoutubeVideoInfo {
        Link, Title
    }

    private static boolean GET_FROM_MAIN_PAGE = true;
    private static HashMap<String, String> pars = null;
    private static final String VIDEO_LINK = "link";
    private static final String VALIDATION_ERROR_MESSAGE = "Please type or paste valid youtube video URL";
    private static final String COMMON_ERROR_MESSAGE = "Can't download youtube video: ";
//    private static final String GET_LINK_ERROR_MESSAGE = "Can't get link to the video";
    private static final String PARAMETERS_TO_REMOVE = "fallback_host,type,codecs,quality," + VIDEO_LINK;
//    private static final String PROTOCOL = "http";
    static final String VIDEO_INFO_URL = "http://www.youtube.com/get_video_info?el=detailpage&asv=3&video_id=";
    private static final String VIDEO_STREAM = "url_encoded_fmt_stream_map";
    //    private static final String VIDEO_STREAM = "adaptive_fmts";
    private static final String TITLE = "title";

    static final String FILENAME_REGEX = "^[^/:;*?'\"<>|%,#$!+{}&\\[\\]\\\\]+";
    private static final String YOUTUBE_REGEX = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=))((\\w|-){11})(?:\\S+)?$";

    private synchronized static void saveYoutubeFile(String url) {
        try {
            HashMap<YoutubeVideoInfo, String> info = new HashMap<>();
            obtainYoutubeVideo(url, info);
            String title = StringUtil.decode(info.get(YoutubeVideoInfo.Title));
            System.out.println("Title: " + title);
            System.out.println("Link to video: " + info.get(YoutubeVideoInfo.Link));
            NetUtil.saveToDisk(new URL(info.get(YoutubeVideoInfo.Link)), title + ".mp4");
//            NetUtil.saveToDisk(new URL(info.get(YoutubeVideoInfo.Link)), (title.matches(YT.FILENAME_REGEX) ? title : "video" +
//                    timestamp()) + ".mp4");
        } catch (IOException e) {
            System.err.println(COMMON_ERROR_MESSAGE + e.getMessage());
        }
    }

    private static void obtainYoutubeVideo(String url, HashMap<YoutubeVideoInfo, String> info) throws IOException {
        String videoId = StringUtil.getParameter(url.trim(), "\\?", "v");
        if (videoId == null) throw new IOException("Video ID can't be equals null");

//        String videoInfo2 = NetUtil.getVideoInfo2(url);
        String streamMap = null;
        if (GET_FROM_MAIN_PAGE) streamMap = NetUtil.getVideoInfo2(url);
        else streamMap = StringUtil.getParameter(NetUtil.getVideoInfo(videoId), "", VIDEO_STREAM);

        System.out.println("Decoded videoInfo: " + streamMap);
        if (streamMap == null) {
            throw new IOException("Video information does not contain link to mp4");
        }
//        String title = StringUtil.getParameter(/*videoInfo*/ "", "", TITLE);
        String title = StringUtil.getParameter(NetUtil.getVideoInfo(videoId), "", TITLE);
        info.put(YoutubeVideoInfo.Title, title);
        String decodedUrl = StringUtil.decode(streamMap);
//        String decodedUrl = streamMap;
        String[] urls = decodedUrl.split("url=");
        HashMap<String, HashMap<String, String>> videoData = new HashMap<>();
        for (int i = 0; i < urls.length; i++) {
            HashMap<String, String> hm = getVideoData(urls[i]);
            if (hm == null) continue;
            videoData.put(hm.get("itag"), hm);
            System.out.println(urls[i]);
        }

        String videoLink = getFinalUrl(videoData, url).replaceFirst(StringUtil.AMP + "s(ig)?=", StringUtil.AMP + "signature=");
        info.put(YoutubeVideoInfo.Link, videoLink/* + (videoLink.endsWith(StringUtil.AMP) ? "" : StringUtil.AMP) + TITLE + "=" + title*/);
    }


    private static HashMap<String, String> getVideoData(String url) {
        if (!url.startsWith("http")) return null;
        String[] equalities = url.split("[?&]");
        HashMap<String, String> out = new HashMap<>();
        out.put(VIDEO_LINK, url.substring(0, url.indexOf("?")));
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
        out.put(data[0].trim(), data[1].replaceAll(",", "%2C").replaceAll("/","%2F"));
    }

    private static String getFinalUrl(HashMap<String, HashMap<String, String>> meta, String url) throws IOException {
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
        if (data == null) throw new RuntimeException("Can't obtain mp4 itag parameter");

        StringBuilder sb = new StringBuilder();
        String[] remove = PARAMETERS_TO_REMOVE.split(",");

        sb.append(data.get(VIDEO_LINK) + "?");
        for (String s : data.keySet()) {
            if (contains(s, remove)) continue;
            String sig = data.get(s);
            if ("s".equals(s) || "sig".equals(s) || "signature".equals(s) && sig.length() > 81) {       // 81 is 40.40 - length of unscrambled youtube signature
                sb.append(s + "=" + Decipher.decipher(sig, url) + StringUtil.AMP);
            } else sb.append(s + "=" + data.get(s) + StringUtil.AMP);
        }
        return sb.substring(0, sb.length()-1);
    }

    private static boolean contains(String s, String[] sArr) {
        for (String o : sArr) if (o.trim().equals(s.trim())) return true;
        return false;
    }

   static long timestamp() {
       return new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()).getTime();
   }

   public static void main(String[] args) {
       if (args.length == 0) {
           System.err.println(VALIDATION_ERROR_MESSAGE);
           System.err.println();
       } else {
           if (args[0].matches(YOUTUBE_REGEX)) {
               System.out.println("Please wait a moment...");
               YT.saveYoutubeFile(args[0]);
           } else {
               System.out.println(VALIDATION_ERROR_MESSAGE);
           }
       }
   }
}


