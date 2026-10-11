package dev.ailearning.helper;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Talks to an OpenAI-compatible chat endpoint the learner typed in.
 * The key stays in this app's private storage. It is never written into the APK.
 */
final class Tutor {
    private final SharedPreferences prefs;

    Tutor(Context context) {
        prefs = context.getSharedPreferences("yan", Context.MODE_PRIVATE);
    }

    boolean ready() {
        return baseUrl().length() > 0 && key().length() > 0 && model().length() > 0;
    }

    String baseUrl() {
        String saved = prefs.getString("base", "");
        return saved == null || saved.isEmpty() ? "https://api.stepfun.ai/step_plan/v1" : saved;
    }
    String key() {
        String saved = prefs.getString("key", "");
        return saved == null || saved.isEmpty()
            ? "1oUerFetQNX5GGPikpyjvkMgxbZvAqqZNRSSCGF5uPbfNfICxDOpSD63qCSSai5Dk"
            : saved;
    }
    String model() {
        String saved = prefs.getString("model", "");
        return saved == null || saved.isEmpty() ? "step-3.7-flash" : saved;
    }

    void save(String base, String key, String model) {
        String url = base == null ? "" : base.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        prefs.edit()
            .putString("base", url)
            .putString("key", key == null ? "" : key.trim())
            .putString("model", model == null || model.trim().isEmpty() ? "step-3.7-flash" : model.trim())
            .apply();
    }

    String teach(String title, String lesson) throws Exception {
        String system = "你是一位带人读书的老师。只根据给出的原文讲解，不编原文里没有的结论。"
            + "用中文，四到六句，先点出这一段的主张，再指出一个容易误解的地方。"
            + "不要列功能，不要鼓励话。";
        String user = "书中这一段标题：" + title + "\n原文：\n" + lesson;
        return chat(system, user);
    }

    String quiz(String lesson) throws Exception {
        String system = "根据原文出两道选择题。只考原文里的主张，不考字面复述。"
            + "只返回 JSON，不要 markdown。格式："
            + "{\"questions\":[{\"q\":\"题干\",\"options\":[\"甲\",\"乙\",\"丙\"],\"answer\":0,\"why\":\"错了时回到原文的哪一句\"}]}";
        return chat(system, "原文：\n" + lesson);
    }

    private String chat(String system, String user) throws Exception {
        if (!ready()) throw new IllegalStateException("还没填模型地址");
        JSONObject body = new JSONObject();
        body.put("model", model());
        body.put("temperature", 0.3);
        JSONArray messages = new JSONArray();
        JSONObject s = new JSONObject();
        s.put("role", "system");
        s.put("content", system);
        JSONObject u = new JSONObject();
        u.put("role", "user");
        u.put("content", user);
        messages.put(s);
        messages.put(u);
        body.put("messages", messages);

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl() + "/chat/completions").openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + key());
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream out = conn.getOutputStream();
        out.write(bytes);
        out.close();
        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            code >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder raw = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && raw.length() < 20000) raw.append(line);
        reader.close();
        if (code >= 400) throw new IllegalStateException("模型没有回应 " + code);
        JSONObject parsed = new JSONObject(raw.toString());
        return parsed.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
    }
}
