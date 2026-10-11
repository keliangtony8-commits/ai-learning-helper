package dev.ailearning.helper;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * One job per screen. Tabs only switch places: today, shelf, review, me.
 * Learning is a stack: chapter list, listen, recall, quiz.
 */
public class MainActivity extends Activity {
    private static final int PAPER = Color.parseColor("#F6F1E7");
    private static final int CARD = Color.parseColor("#FFFDF8");
    private static final int INK = Color.parseColor("#1C1915");
    private static final int MUTED = Color.parseColor("#6F675E");
    private static final int ACCENT = Color.parseColor("#1F4D45");
    private static final int ACCENT_SOFT = Color.parseColor("#141F4D45");
    private static final int ACCENT_INK = Color.parseColor("#F4F7F4");
    private static final int WARN = Color.parseColor("#8A4B2F");
    private static final int OK = Color.parseColor("#2F6B45");

    private Store store;
    private LinearLayout body;
    private LinearLayout tabs;
    private TextView mark;

    private int tab;
    private String bookId;
    private String chapterId;
    private int step;
    private int quiz;
    private boolean quizWrong;
    private Integer picked;
    private String recall = "";
    private boolean adding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        LinearLayout root = column();
        root.setBackgroundColor(PAPER);
        root.setPadding(0, dp(28), 0, 0);
        root.addView(header());
        body = column();
        body.setPadding(dp(22), dp(8), dp(22), dp(18));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(body);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(scroll);
        tabs = tabBar();
        root.addView(tabs);
        setContentView(root);
        show(false);
    }

    private View header() {
        LinearLayout row = row();
        row.setPadding(dp(22), dp(8), dp(22), 0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        mark = text("砚", 26, INK, true);
        mark.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(mark);
        row.addView(text("0.3.2", 12, MUTED, false));
        return row;
    }

    private void show(boolean animate) {
        body.removeAllViews();
        boolean learning = bookId != null && !adding;
        tabs.setVisibility(learning ? View.GONE : View.VISIBLE);
        if (adding) addBook();
        else if (bookId != null) learn();
        else if (tab == 0) today();
        else if (tab == 1) shelf();
        else if (tab == 2) review();
        else me();
        paintTabs();
        if (animate) {
            body.setAlpha(0f);
            body.setTranslationY(dp(8));
            body.animate().alpha(1f).translationY(0f).setDuration(280)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void today() {
        mark.setText("砚");
        int due = store.due().size();
        kicker(due == 0 ? "今天没有到期的复习" : "有 " + due + " 段到期");
        title(due == 0 ? "从书架选一段来学" : "先把到期的复习完");
        if (due > 0) {
            final Store.ChapterRef ref = store.due().get(0);
            card(ref.chapter.title, ref.book.title);
            primary("去复习", new Runnable() {
                @Override public void run() { open(ref.book.id, ref.chapter.id, 0); }
            });
            quiet("或者去书架开一段新的", new Runnable() {
                @Override public void run() { tab = 1; show(true); }
            });
        } else if (!store.books.isEmpty()) {
            final Store.Book book = store.books.get(0);
            card(book.title, book.author + " · " + book.studiedChapters() + " / " + book.chapters.size() + " 段已学");
            primary("打开这本书", new Runnable() {
                @Override public void run() { open(book.id, null, 0); }
            });
        } else {
            card("书架是空的", "先放进一本书，再选一段来学。");
            primary("放进一本书", new Runnable() {
                @Override public void run() { adding = true; show(true); }
            });
        }
    }

    private void shelf() {
        mark.setText("砚");
        kicker("书架");
        title("一次推进一本");
        for (int i = 0; i < store.books.size(); i++) {
            body.addView(bookRow(store.books.get(i)));
            body.addView(space(10));
        }
        primary("放进一本书", new Runnable() {
            @Override public void run() { adding = true; show(true); }
        });
    }

    private View bookRow(final Store.Book book) {
        LinearLayout line = row();
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setBackground(rounded(CARD, dp(16)));
        line.setPadding(dp(14), dp(14), dp(14), dp(14));
        String spineText = book.title.isEmpty() ? "书" : book.title.substring(0, 1);
        TextView spine = text(spineText, 18, ACCENT_INK, true);
        spine.setGravity(Gravity.CENTER);
        spine.setBackground(rounded(ACCENT, dp(8)));
        spine.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(64)));
        line.addView(spine);
        line.addView(spaceW(14));
        LinearLayout info = column();
        info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        info.addView(text(book.title, 18, INK, true));
        info.addView(space(4));
        info.addView(text(book.author + " · " + book.studiedChapters() + "/" + book.chapters.size(), 13, MUTED, false));
        line.addView(info);
        line.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { open(book.id, null, 0); }
        });
        return line;
    }

    private void addBook() {
        mark.setText("放进书架");
        kicker("新书");
        title("先给它一个名字");
        final EditText titleInput = field("书名");
        final EditText authorInput = field("作者，可以空着");
        final EditText bodyInput = field("把想学的文字贴在这里。空一行，就分成一段。");
        bodyInput.setMinLines(6);
        cardWrap(titleInput);
        body.addView(space(10));
        cardWrap(authorInput);
        body.addView(space(10));
        cardWrap(bodyInput);
        primary("放进书架", new Runnable() {
            @Override public void run() {
                String name = titleInput.getText().toString().trim();
                if (name.length() < 1) {
                    titleInput.setError("写个书名");
                    return;
                }
                store.addBook(name, authorInput.getText().toString(), bodyInput.getText().toString());
                adding = false;
                tab = 1;
                show(true);
            }
        });
        quiet("取消", new Runnable() {
            @Override public void run() { adding = false; show(true); }
        });
    }

    private void review() {
        mark.setText("砚");
        kicker("复习");
        title("到期的，才出现在前面");
        java.util.List<Store.ChapterRef> due = store.due();
        if (due.isEmpty()) {
            body.addView(text("现在没有到期的。", 16, MUTED, false));
            body.addView(space(16));
        }
        for (int i = 0; i < due.size(); i++) body.addView(reviewRow(due.get(i), true));
        java.util.List<Store.ChapterRef> later = store.upcoming();
        if (!later.isEmpty()) {
            body.addView(space(8));
            body.addView(text("还没到期", 13, MUTED, true));
            body.addView(space(8));
            int limit = Math.min(later.size(), 4);
            for (int i = 0; i < limit; i++) body.addView(reviewRow(later.get(i), false));
        }
    }

    private View reviewRow(final Store.ChapterRef ref, final boolean openable) {
        LinearLayout box = column();
        box.setBackground(rounded(CARD, dp(16)));
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout line = row();
        TextView left = text(ref.chapter.title, 17, INK, true);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(left);
        line.addView(text(Store.dueLabel(ref.chapter.dueAt), 13, openable ? WARN : MUTED, true));
        box.addView(line);
        box.addView(space(4));
        String extra = ref.book.title;
        if (ref.chapter.lastResult.length() > 0) extra += " · " + ref.chapter.lastResult;
        box.addView(text(extra, 13, MUTED, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        box.setLayoutParams(lp);
        if (openable) {
            box.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { open(ref.book.id, ref.chapter.id, 0); }
            });
        }
        return box;
    }

    private void me() {
        mark.setText("砚");
        kicker("我的");
        title("学习记录");
        card("已学 " + store.studiedCount() + " 段", "复习间隔是 1、3、7、16、35 天。答错会回到第一天。书只存在这台手机上。");
        java.util.List<Store.ChapterRef> done = new java.util.ArrayList<>();
        for (Store.Book book : store.books) {
            for (Store.Chapter chapter : book.chapters) {
                if (chapter.studied) done.add(new Store.ChapterRef(book, chapter));
            }
        }
        if (!done.isEmpty()) {
            body.addView(space(18));
            body.addView(text("最近写下的", 13, MUTED, true));
            body.addView(space(8));
            int shown = 0;
            for (int i = done.size() - 1; i >= 0 && shown < 3; i--) {
                Store.ChapterRef ref = done.get(i);
                if (ref.chapter.recall == null || ref.chapter.recall.length() == 0) continue;
                card(ref.chapter.title, ref.chapter.recall);
                body.addView(space(10));
                shown++;
            }
        }
        body.addView(space(8));
        card("还没接模型", "粘贴进来的文字会按空行分成段。讲义和题目还是规则生成的，不是模型写的。");
    }

    private void learn() {
        final Store.Book book = store.find(bookId);
        if (book == null) {
            bookId = null;
            show(false);
            return;
        }
        if (chapterId == null) {
            chapterList(book);
            return;
        }
        final Store.Chapter chapter = store.findChapter(bookId, chapterId);
        if (chapter == null) {
            chapterId = null;
            show(false);
            return;
        }
        mark.setText(book.title);
        if (step == 0) listen(chapter);
        else if (step == 1) recall(chapter);
        else quiz(book, chapter);
    }

    private void chapterList(final Store.Book book) {
        mark.setText(book.title);
        kicker(book.author);
        title("选一段");
        for (int i = 0; i < book.chapters.size(); i++) {
            final Store.Chapter chapter = book.chapters.get(i);
            LinearLayout box = column();
            box.setBackground(rounded(CARD, dp(16)));
            box.setPadding(dp(16), dp(14), dp(16), dp(14));
            box.addView(text(chapter.title, 17, INK, true));
            box.addView(space(4));
            String state = chapter.studied ? "已学 · " + Store.dueLabel(chapter.dueAt) : "还没学";
            box.addView(text(state, 13, chapter.studied ? ACCENT : MUTED, false));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            box.setLayoutParams(lp);
            box.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    chapterId = chapter.id;
                    step = 0;
                    quiz = 0;
                    picked = null;
                    recall = "";
                    show(true);
                }
            });
            body.addView(box);
        }
        quiet("回到书架", new Runnable() {
            @Override public void run() {
                bookId = null;
                tab = 1;
                show(true);
            }
        });
    }

    private void listen(final Store.Chapter chapter) {
        kicker(chapter.title);
        title("先读懂这一段");
        steps(0);
        TextView lesson = text(chapter.lesson, 17, INK, false);
        lesson.setLineSpacing(dp(6), 1f);
        lesson.setBackground(rounded(CARD, dp(16)));
        lesson.setPadding(dp(16), dp(16), dp(16), dp(16));
        body.addView(lesson);
        primary("我读完了，用自己的话说", new Runnable() {
            @Override public void run() { step = 1; show(true); }
        });
        quiet("换一段", new Runnable() {
            @Override public void run() { chapterId = null; show(true); }
        });
    }

    private void recall(final Store.Chapter chapter) {
        kicker("复述");
        title("不要抄刚才的句子");
        steps(1);
        body.addView(text(chapter.prompt, 15, MUTED, false));
        body.addView(space(12));
        final EditText input = field("用你自己的话写两句");
        if (recall.length() == 0 && chapter.recall != null) recall = chapter.recall;
        input.setText(recall);
        input.setMinLines(4);
        cardWrap(input);
        primary("去检验", new Runnable() {
            @Override public void run() {
                recall = input.getText().toString().trim();
                if (recall.length() < 8) {
                    input.setError("再写具体一点");
                    return;
                }
                picked = null;
                quiz = 0;
                step = 2;
                show(true);
            }
        });
        quiet("再看一遍原文", new Runnable() {
            @Override public void run() { step = 0; show(true); }
        });
    }

    private void quiz(final Store.Book book, final Store.Chapter chapter) {
        kicker("第 " + (quiz + 1) + " / " + chapter.questions.length + " 题");
        title(chapter.questions[quiz]);
        steps(2);
        for (int i = 0; i < chapter.options[quiz].length; i++) {
            final int index = i;
            int color = INK;
            if (picked != null && index == chapter.answers[quiz]) color = OK;
            else if (picked != null && index == picked) color = WARN;
            Button button = new Button(this);
            button.setText(chapter.options[quiz][i]);
            button.setAllCaps(false);
            button.setTextColor(color);
            button.setTextSize(15);
            button.setBackground(rounded(CARD, dp(14)));
            button.setPadding(dp(14), dp(12), dp(14), dp(12));
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (picked == null) {
                        picked = index;
                        show(false);
                    }
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            body.addView(button, lp);
        }
        if (picked == null) {
            quiet("回到复述", new Runnable() {
                @Override public void run() { step = 1; show(true); }
            });
            return;
        }
        body.addView(space(14));
        boolean right = picked == chapter.answers[quiz];
        body.addView(text((right ? "对了。" : "还差一点。") + " " + chapter.why[quiz], 15, MUTED, false));
        final boolean last = quiz == chapter.questions.length - 1;
        primary(last ? "记下这次复习" : "下一题", new Runnable() {
            @Override public void run() {
                if (!last) {
                    if (picked == null || picked != chapter.answers[quiz]) quizWrong = true;
                    quiz += 1;
                    picked = null;
                    show(true);
                    return;
                }
                if (picked == null || picked != chapter.answers[quiz]) quizWrong = true;
                store.grade(book.id, chapter.id, !quizWrong, recall);
                bookId = null;
                chapterId = null;
                tab = 2;
                show(true);
            }
        });
    }

    private void open(String nextBook, String nextChapter, int nextStep) {
        bookId = nextBook;
        chapterId = nextChapter;
        step = nextStep;
        quiz = 0;
        quizWrong = false;
        picked = null;
        recall = "";
        show(true);
    }

    private LinearLayout tabBar() {
        LinearLayout bar = row();
        bar.setBackgroundColor(Color.parseColor("#FBF8F2"));
        bar.setPadding(dp(8), dp(6), dp(8), dp(10));
        String[] labels = {"今天", "书架", "复习", "我的"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = text(labels[i], 14, MUTED, false);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    tab = index;
                    bookId = null;
                    chapterId = null;
                    adding = false;
                    show(true);
                }
            });
            item.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));
            bar.addView(item);
        }
        return bar;
    }

    private void paintTabs() {
        for (int i = 0; i < tabs.getChildCount(); i++) {
            TextView view = (TextView) tabs.getChildAt(i);
            boolean on = i == tab && bookId == null && !adding;
            view.setTextColor(on ? INK : MUTED);
            view.setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            view.setBackground(on ? rounded(ACCENT_SOFT, dp(12)) : null);
        }
    }

    private void kicker(String value) {
        body.addView(text(value, 13, ACCENT, true));
        body.addView(space(6));
    }

    private void title(String value) {
        body.addView(text(value, 28, INK, true));
        body.addView(space(14));
    }

    private void steps(int index) {
        String[] names = {"听懂", "复述", "做题"};
        LinearLayout line = row();
        line.setBackground(rounded(CARD, dp(12)));
        line.setPadding(dp(4), dp(8), dp(4), dp(8));
        for (int i = 0; i < names.length; i++) {
            TextView item = text(names[i], 13, i == index ? INK : MUTED, i == index);
            item.setGravity(Gravity.CENTER);
            if (i == index) item.setBackground(rounded(ACCENT_SOFT, dp(8)));
            item.setPadding(0, dp(4), 0, dp(4));
            item.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(item);
        }
        body.addView(line);
        body.addView(space(16));
    }

    private void card(String heading, String value) {
        LinearLayout box = column();
        box.setBackground(rounded(CARD, dp(16)));
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.addView(text(heading, 18, INK, true));
        box.addView(space(6));
        TextView bodyText = text(value, 15, MUTED, false);
        bodyText.setLineSpacing(dp(3), 1f);
        box.addView(bodyText);
        body.addView(box);
    }

    private void cardWrap(View child) {
        LinearLayout box = column();
        box.setBackground(rounded(CARD, dp(16)));
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        box.addView(child);
        body.addView(box);
    }

    private EditText field(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(dp(4), dp(8), dp(4), dp(8));
        input.setTextSize(16);
        return input;
    }

    private void primary(String label, final Runnable action) {
        body.addView(space(16));
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(ACCENT_INK);
        button.setTextSize(16);
        button.setBackground(rounded(ACCENT, dp(14)));
        button.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        body.addView(button);
    }

    private void quiet(String label, final Runnable action) {
        body.addView(space(8));
        TextView view = text(label, 14, MUTED, false);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(10), 0, dp(6));
        view.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        body.addView(view);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private View space(int h) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return view;
    }

    private View spaceW(int w) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(w), 1));
        return view;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
