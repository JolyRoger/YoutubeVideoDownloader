package com.youtube.download;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

/**
 * Created by torquemada on 12/20/14.
 */
public class NetUtil {

    private static HashMap<String, StringBuilder> response = new HashMap<>(2);
    static StringBuilder videoBody = null;

    static StringBuilder getBody(String u) throws IOException {
        StringBuilder body = response.get(u);
        if (body == null) {
            System.out.println("try to get body of " + u);
            URL url = new URL(u);
            body = new StringBuilder(getBody(url));
            response.put(u, body);
        }
        return body;

//        URLConnection con = url.openConnection();
//        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
//        InputStream in = con.getInputStream();
//        String encoding = con.getContentEncoding();
//        encoding=encoding==null?"UTF-8":encoding;
//        return IOUtils.toString(in, encoding);
    }

    static String getVideoInfo(String videoId) throws IOException {
        URL videoInfoUrl = new URL(YT.VIDEO_INFO_URL + videoId);
        return getBody(videoInfoUrl);
    }

    static String getVideoInfo2(String url) throws IOException {
        String videoInfo = StringUtil.getVideoInfo(getBody(url));
        if (videoInfo == null)
            videoInfo = StringUtil.getParameter(getVideoInfo(StringUtil.getParameter(url.trim(), "\\?", "v")), "", StringUtil.VIDEO_STREAM);
        if (videoInfo == null) throw new RuntimeException("Can't obtain video info");
        System.out.println("Undecoded videoInfo: " + videoInfo);
        return StringUtil.decode(videoInfo);
    }

    static String getBody(URL url) throws IOException {
        StringBuilder info = new StringBuilder();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null)
            info.append(inputLine);
        in.close();
        return info.toString();
    }

    static void saveToDisk(URL videoUrl, String title) throws IOException {
        FileOutputStream fos = null;
        ReadableByteChannel rbc = Channels.newChannel(videoUrl.openStream());
        try {
            fos = new FileOutputStream(title);
        } catch (FileNotFoundException e) {
            fos = new FileOutputStream("video" + YT.timestamp() + ".mp4");
        }
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        System.out.println("Check " + System.getProperty("user.dir") + " for file");
    }
}


