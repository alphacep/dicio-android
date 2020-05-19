package com.dicio.dicio_android.components.processing;

import com.dicio.component.IntermediateProcessor;
import com.dicio.component.standard.StandardResult;
import com.dicio.dicio_android.components.output.LyricsOutput;
import com.dicio.dicio_android.util.ConnectionUtils;
import com.dicio.dicio_android.util.RegexUtils;
import com.dicio.dicio_android.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.unbescape.javascript.JavaScriptEscape;
import org.unbescape.json.JsonEscape;

public class GeniusProcessor implements IntermediateProcessor<StandardResult, LyricsOutput.Data> {

    private static final String geniusSearchUrl = "https://genius.com/api/search/multi";
    private static final String geniusLyricsUrl = "https://genius.com/songs/";

    @Override
    public LyricsOutput.Data process(StandardResult data) throws Exception {
        JSONObject search = ConnectionUtils.getPageJson(geniusSearchUrl + "?q="
                + ConnectionUtils.urlEncode(StringUtils.join(data.getCapturingGroups().get(0))));
        JSONArray searchHits = search.getJSONObject("response").getJSONArray("sections")
                .getJSONObject(0).getJSONArray("hits");

        LyricsOutput.Data result = new LyricsOutput.Data();
        if (searchHits.length() == 0) {
            result.failed = true;
            result.title = StringUtils.join(data.getCapturingGroups().get(0));
            return result;
        }

        JSONObject song = searchHits.getJSONObject(0).getJSONObject("result");
        result.title = song.getString("title");
        result.artist = song.getJSONObject("primary_artist").getString("name");


        String lyricsHtml = ConnectionUtils.getPage(geniusLyricsUrl + song.getInt("id") + "/embed.js");
        lyricsHtml = RegexUtils.matchGroup1("document\\.write\\(JSON\\.parse\\('(.+)'\\)\\)", lyricsHtml);
        lyricsHtml = JsonEscape.unescapeJson(JavaScriptEscape.unescapeJavaScript(lyricsHtml));

        Document lyricsDocument = Jsoup.parse(lyricsHtml);
        Elements elements = lyricsDocument.select("div[class=rg_embed_body]");
        elements.select("br").append("{#%)");
        result.lyrics = elements.text().replaceAll("\\s*(\\\\n)?\\s*\\{#%\\)\\s*", "\n");

        return result;
    }
}
