package dev.ailearning.helper;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Books, chapters, and review cards live in one JSON blob.
 * Review intervals follow a short forgetting curve: 1, 3, 7, 16, 35 days.
 * A miss sends the card back to the first interval.
 */
final class Store {
    static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long[] GAPS = {DAY, 3 * DAY, 7 * DAY, 16 * DAY, 35 * DAY};

    final List<Book> books = new ArrayList<>();
    private final SharedPreferences prefs;

    Store(Context context) {
        prefs = context.getSharedPreferences("yan", Context.MODE_PRIVATE);
        load();
        if (books.isEmpty() && !prefs.getBoolean("seeded", false)) seed();
    }

    void seed() {
        Book book = new Book("如何阅读一本书", "艾德勒");
        book.chapters.add(new Chapter(
            "阅读的四个层次",
            "同一本书，读法不该只有一种。基础阅读是看懂句子。检视阅读是在有限时间里摸清这本书值不值得细读。分析阅读才是逐章追问作者到底在主张什么。多数人卡住，是用检视的速度做分析的事，或者把分析阅读当成抄笔记。",
            "分析阅读和检视阅读，差在哪一件事上？",
            new String[]{
                "时间只有二十分钟，你最该做的是哪一层？",
                "分析阅读最核心的动作是？"
            },
            new String[][]{
                {"分析阅读，逐段画线", "检视阅读，先摸清结构和主张", "主题阅读，同时打开五本书"},
                {"把金句摘进笔记", "追问作者的主张、论据和结构", "读得越慢越好"}
            },
            new int[]{1, 1},
            new String[]{
                "时间有限时先检视。分析阅读假设你已经决定这本书值得细读。",
                "分析阅读是主动追问，不是收集句子，也不是把慢当成深刻。"
            }
        ));
        book.chapters.add(new Chapter(
            "检视阅读",
            "检视阅读有两步。先系统略读：书名、序言、目录、索引、出版说明，再抽读与主题相关的章节。然后才决定要不要进入分析阅读。略读不是敷衍，是为了不把时间浪费在不值得细读的书上。",
            "检视阅读做完之后，你应该能回答什么？",
            new String[]{"系统略读至少要看哪几处？", "检视阅读的目的是什么？"},
            new String[][]{
                {"只看结尾", "书名、序言、目录、索引，再抽读相关章节", "把每一页都画线"},
                {"把书读完", "判断这本书值不值得进入分析阅读", "收集可以引用的句子"}
            },
            new int[]{1, 1},
            new String[]{
                "系统略读看结构，不看每一句。结尾单独看，看不出作者怎么论证。",
                "检视的产出是一个决定：细读，还是放下。不是笔记。"
            }
        ));
        books.add(book);
        prefs.edit().putBoolean("seeded", true).apply();
        save();
    }

    Book findExact(String id) {
        for (Book book : books) if (book.id.equals(id)) return book;
        return null;
    }

    Book find(String id) {
        for (Book book : books) if (book.id.equals(id)) return book;
        return books.isEmpty() ? null : books.get(0);
    }

    Chapter findChapter(String bookId, String chapterId) {
        Book book = find(bookId);
        if (book == null) return null;
        for (Chapter chapter : book.chapters) if (chapter.id.equals(chapterId)) return chapter;
        return book.chapters.isEmpty() ? null : book.chapters.get(0);
    }

    void addBook(String title, String author, String pasted) {
        Book book = new Book(title.trim(), author.trim().isEmpty() ? "未署名" : author.trim());
        String body = pasted == null ? "" : pasted.trim();
        if (body.isEmpty()) {
            book.chapters.add(blankChapter("还没写正文"));
        } else {
            String[] parts = body.split("\\n\\s*\\n");
            int n = 0;
            for (String part : parts) {
                String text = part.trim();
                if (text.length() < 12) continue;
                n++;
                book.chapters.add(chapterFrom(text, n));
            }
            if (book.chapters.isEmpty()) book.chapters.add(blankChapter("正文"));
        }
        books.add(0, book);
        save();
    }

    private Chapter chapterFrom(String text, int index) {
        String flat = text.replace('\n', ' ').trim();
        String title = flat.length() > 16 ? flat.substring(0, 16) : flat;
        String[] sentences = flat.split("[。！？]");
        java.util.List<String> usable = new java.util.ArrayList<>();
        for (String sentence : sentences) {
            String s = sentence.trim();
            if (s.length() >= 8 && s.length() <= 42) usable.add(s);
        }
        if (usable.isEmpty()) {
            Chapter chapter = blankChapter("第 " + index + " 段");
            chapter.title = title;
            chapter.lesson = text;
            chapter.prompt = "用你自己的话，写下这段最想记住的一句。";
            return chapter;
        }
        int count = Math.min(2, usable.size());
        String[] questions = new String[count];
        String[][] options = new String[count][];
        int[] answers = new int[count];
        String[] why = new String[count];
        for (int i = 0; i < count; i++) {
            String sentence = usable.get(i);
            String missing = sentence.length() > 10 ? sentence.substring(sentence.length() / 3, Math.min(sentence.length(), sentence.length() / 3 + 8)) : sentence;
            int answer = 1;
            String[] row = new String[]{
                "这段没有下判断，只是在列书名",
                sentence,
                "把「" + missing + "」换成相反的意思"
            };
            if (index % 2 == 0) {
                String swap = row[0];
                row[0] = row[1];
                row[1] = swap;
                answer = 0;
            }
            questions[i] = "哪一句能回到这段原文？";
            options[i] = row;
            answers[i] = answer;
            why[i] = "原话是：「" + sentence + "」。另外两项是编的，回到原文对一下。";
        }
        Chapter chapter = new Chapter(title, text, "用你自己的话，写下这段最想记住的一句。不要照抄。", questions, options, answers, why);
        return chapter;
    }

    private Chapter blankChapter(String title) {
        Chapter chapter = new Chapter(
            title,
            "这段还没有讲义。先用自己的话写你为什么要读它。",
            "你为什么要把这段放进学习，而不是只收藏？",
            new String[]{"现在最合适的下一步是？"},
            new String[][]{{"关掉应用", "用自己的话写下这段要解决的问题", "再找十本类似的书"}},
            new int[]{1},
            new String[]{"先把问题写清楚。收藏和再找书，都还没开始学。"}
        );
        return chapter;
    }

    void removeBook(String id) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).id.equals(id)) {
                books.remove(i);
                save();
                return;
            }
        }
    }

    void markStudied(String bookId, String chapterId) {
        Chapter chapter = findChapter(bookId, chapterId);
        if (chapter == null) return;
        chapter.studied = true;
        if (chapter.dueAt == 0) chapter.dueAt = System.currentTimeMillis() + GAPS[0];
        save();
    }

    void replaceQuiz(String bookId, String chapterId, String[] questions, String[][] options, int[] answers, String[] why) {
        Chapter chapter = findChapter(bookId, chapterId);
        if (chapter == null || questions.length == 0) return;
        chapter.questions = questions;
        chapter.options = options;
        chapter.answers = answers;
        chapter.why = why;
        save();
    }

    void grade(String bookId, String chapterId, boolean passed, String recallText) {
        Chapter chapter = findChapter(bookId, chapterId);
        if (chapter == null) return;
        chapter.studied = true;
        if (passed) {
            chapter.box = Math.min(chapter.box + 1, GAPS.length - 1);
        } else {
            chapter.box = 0;
        }
        chapter.dueAt = System.currentTimeMillis() + GAPS[chapter.box];
        chapter.lastResult = passed ? "记住了" : "还要再看";
        if (recallText != null && recallText.trim().length() > 0) chapter.recall = recallText.trim();
        save();
    }

    List<ChapterRef> due() {
        long now = System.currentTimeMillis();
        List<ChapterRef> out = new ArrayList<>();
        for (Book book : books) {
            for (Chapter chapter : book.chapters) {
                if (chapter.studied && chapter.dueAt > 0 && chapter.dueAt <= now) {
                    out.add(new ChapterRef(book, chapter));
                }
            }
        }
        Collections.sort(out, new Comparator<ChapterRef>() {
            @Override public int compare(ChapterRef a, ChapterRef b) {
                return Long.compare(a.chapter.dueAt, b.chapter.dueAt);
            }
        });
        return out;
    }

    List<ChapterRef> upcoming() {
        long now = System.currentTimeMillis();
        List<ChapterRef> out = new ArrayList<>();
        for (Book book : books) {
            for (Chapter chapter : book.chapters) {
                if (chapter.studied && chapter.dueAt > now) out.add(new ChapterRef(book, chapter));
            }
        }
        Collections.sort(out, new Comparator<ChapterRef>() {
            @Override public int compare(ChapterRef a, ChapterRef b) {
                return Long.compare(a.chapter.dueAt, b.chapter.dueAt);
            }
        });
        return out;
    }

    Store.ChapterRef nextNew() {
        for (Book book : books) {
            for (Chapter chapter : book.chapters) {
                if (!chapter.studied) return new ChapterRef(book, chapter);
            }
        }
        return null;
    }

    int studiedCount() {
        int n = 0;
        for (Book book : books) for (Chapter chapter : book.chapters) if (chapter.studied) n++;
        return n;
    }

    static String dueLabel(long dueAt) {
        long now = System.currentTimeMillis();
        long delta = dueAt - now;
        if (delta <= 0) return "今天";
        long days = (delta + DAY - 1) / DAY;
        if (days <= 1) return "明天";
        return days + " 天后";
    }

    private void load() {
        String raw = prefs.getString("books", "");
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) books.add(Book.from(array.getJSONObject(i)));
        } catch (JSONException ignored) {
            books.clear();
        }
    }

    void save() {
        JSONArray array = new JSONArray();
        for (Book book : books) array.put(book.toJson());
        prefs.edit().putString("books", array.toString()).apply();
    }

    static final class Book {
        final String id;
        String title;
        String author;
        final List<Chapter> chapters = new ArrayList<>();

        Book(String title, String author) {
            this(UUID.randomUUID().toString(), title, author);
        }

        Book(String id, String title, String author) {
            this.id = id;
            this.title = title;
            this.author = author;
        }

        int studiedChapters() {
            int n = 0;
            for (Chapter chapter : chapters) if (chapter.studied) n++;
            return n;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("title", title);
                o.put("author", author);
                JSONArray array = new JSONArray();
                for (Chapter chapter : chapters) array.put(chapter.toJson());
                o.put("chapters", array);
            } catch (JSONException ignored) {}
            return o;
        }

        static Book from(JSONObject o) throws JSONException {
            Book book = new Book(o.getString("id"), o.optString("title"), o.optString("author"));
            JSONArray array = o.optJSONArray("chapters");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) book.chapters.add(Chapter.from(array.getJSONObject(i)));
            }
            return book;
        }
    }

    static final class Chapter {
        final String id;
        String title;
        String lesson;
        String prompt;
        String[] questions;
        String[][] options;
        int[] answers;
        String[] why;
        boolean studied;
        int box;
        long dueAt;
        String lastResult = "";
        String recall = "";

        Chapter(String title, String lesson, String prompt, String[] questions, String[][] options, int[] answers, String[] why) {
            this(UUID.randomUUID().toString(), title, lesson, prompt, questions, options, answers, why);
        }

        Chapter(String id, String title, String lesson, String prompt, String[] questions, String[][] options, int[] answers, String[] why) {
            this.id = id;
            this.title = title;
            this.lesson = lesson;
            this.prompt = prompt;
            this.questions = questions;
            this.options = options;
            this.answers = answers;
            this.why = why;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("title", title);
                o.put("lesson", lesson);
                o.put("prompt", prompt);
                o.put("questions", new JSONArray(questions));
                JSONArray opts = new JSONArray();
                for (String[] row : options) opts.put(new JSONArray(row));
                o.put("options", opts);
                JSONArray ans = new JSONArray();
                for (int a : answers) ans.put(a);
                o.put("answers", ans);
                o.put("why", new JSONArray(why));
                o.put("studied", studied);
                o.put("box", box);
                o.put("dueAt", dueAt);
                o.put("lastResult", lastResult);
                o.put("recall", recall);
            } catch (JSONException ignored) {}
            return o;
        }

        static Chapter from(JSONObject o) throws JSONException {
            JSONArray q = o.getJSONArray("questions");
            String[] questions = new String[q.length()];
            for (int i = 0; i < q.length(); i++) questions[i] = q.getString(i);
            JSONArray opts = o.getJSONArray("options");
            String[][] options = new String[opts.length()][];
            for (int i = 0; i < opts.length(); i++) {
                JSONArray row = opts.getJSONArray(i);
                options[i] = new String[row.length()];
                for (int j = 0; j < row.length(); j++) options[i][j] = row.getString(j);
            }
            JSONArray ans = o.getJSONArray("answers");
            int[] answers = new int[ans.length()];
            for (int i = 0; i < ans.length(); i++) answers[i] = ans.getInt(i);
            JSONArray whyArr = o.getJSONArray("why");
            String[] why = new String[whyArr.length()];
            for (int i = 0; i < whyArr.length(); i++) why[i] = whyArr.getString(i);
            Chapter chapter = new Chapter(
                o.getString("id"), o.optString("title"), o.optString("lesson"), o.optString("prompt"),
                questions, options, answers, why
            );
            chapter.studied = o.optBoolean("studied");
            chapter.box = o.optInt("box");
            chapter.dueAt = o.optLong("dueAt");
            chapter.lastResult = o.optString("lastResult");
            chapter.recall = o.optString("recall");
            return chapter;
        }
    }

    static final class ChapterRef {
        final Book book;
        final Chapter chapter;
        ChapterRef(Book book, Chapter chapter) {
            this.book = book;
            this.chapter = chapter;
        }
    }
}
