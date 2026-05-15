package com.robohon.nhknews;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * NHK RSSフィードからニュースタイトルを取得するクラス.
 */
public class NewsFetcher {
    private static final String TAG = NewsFetcher.class.getSimpleName();

    private static final String RSS_URL = "https://www.nhk.or.jp/rss/news/cat6.xml";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;

    /**
     * ニュースアイテム.
     */
    public static class NewsItem {
        public String title;
        public String link;
        public long pubDateMillis;

        public NewsItem(String title, String link, long pubDateMillis) {
            this.title = title;
            this.link = link;
            this.pubDateMillis = pubDateMillis;
        }
    }

    /**
     * RSSフィードを取得し、タイトル一覧を返す.
     * ネットワーク通信を行うため、バックグラウンドスレッドで呼ぶこと.
     */
    public static List<NewsItem> fetchNews() {
        List<NewsItem> items = new ArrayList<>();
        HttpURLConnection connection = null;

        try {
            URL url = new URL(RSS_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "RoBoHoN-NHKNews/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Log.e(TAG, "HTTP error: " + responseCode);
                return items;
            }

            InputStream inputStream = connection.getInputStream();
            items = parseRss(inputStream);
            inputStream.close();

        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch RSS: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        Log.d(TAG, "Fetched " + items.size() + " news items");
        return items;
    }

    /**
     * RSS XMLをパースしてNewsItemリストを返す.
     */
    private static List<NewsItem> parseRss(InputStream inputStream) throws Exception {
        List<NewsItem> items = new ArrayList<>();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(inputStream, "UTF-8");

        boolean insideItem = false;
        String currentTag = "";
        String title = "";
        String link = "";
        String pubDate = "";

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    currentTag = parser.getName();
                    if ("item".equals(currentTag)) {
                        insideItem = true;
                        title = "";
                        link = "";
                        pubDate = "";
                    }
                    break;

                case XmlPullParser.TEXT:
                    if (insideItem) {
                        String text = parser.getText();
                        if (text != null) {
                            if ("title".equals(currentTag)) {
                                title += text.trim();
                            } else if ("link".equals(currentTag)) {
                                link += text.trim();
                            } else if ("pubDate".equals(currentTag)) {
                                pubDate += text.trim();
                            }
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    if ("item".equals(parser.getName()) && insideItem) {
                        insideItem = false;
                        long pubDateMillis = parsePubDate(pubDate);
                        if (!title.isEmpty()) {
                            items.add(new NewsItem(title, link, pubDateMillis));
                        }
                    }
                    if (parser.getName().equals(currentTag)) {
                        currentTag = "";
                    }
                    break;
            }
            eventType = parser.next();
        }

        return items;
    }

    /**
     * RSS pubDate文字列をミリ秒に変換する.
     * 形式例: "Wed, 11 Mar 2026 02:42:38 GMT"
     */
    private static long parsePubDate(String pubDate) {
        if (pubDate == null || pubDate.isEmpty()) {
            return 0;
        }

        // よくあるRSS日付フォーマットを複数試す
        String[] formats = {
                "EEE, dd MMM yyyy HH:mm:ss z",
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date date = sdf.parse(pubDate);
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception e) {
                // 次のフォーマットを試す
            }
        }

        Log.w(TAG, "Could not parse pubDate: " + pubDate);
        return 0;
    }

    /**
     * 1時間以内に公開されたニュースだけをフィルタリングする.
     */
    public static List<NewsItem> filterRecentNews(List<NewsItem> items) {
        List<NewsItem> recent = new ArrayList<>();
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);

        for (NewsItem item : items) {
            if (item.pubDateMillis >= oneHourAgo) {
                recent.add(item);
            }
            // 日付がパースできなかった場合(pubDateMillis==0)はスキップ
        }

        Log.d(TAG, "Filtered to " + recent.size() + " recent items (from " + items.size() + ")");
        return recent;
    }
}
