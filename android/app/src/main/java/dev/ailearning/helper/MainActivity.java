package dev.ailearning.helper;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int PAPER = Color.parseColor("#F6F1E7");
    private static final int CARD = Color.parseColor("#FFFDF8");
    private static final int INK = Color.parseColor("#1C1915");
    private static final int MUTED = Color.parseColor("#6F675E");
    private static final int ACCENT = Color.parseColor("#1F4D45");
    private static final int ACCENT_INK = Color.parseColor("#F4F7F4");
    private static final int WARN = Color.parseColor("#8A4B2F");
    private static final int OK = Color.parseColor("#2F6B45");

    private LinearLayout body;
    private LinearLayout tabs;
    private int tab;
    private int step;
    private String recall = "";
    private int quiz;
    private Integer picked;

    private static final String[][] OPTIONS = {
        {"分析阅读，逐段画线", "检视阅读，先摸清结构和主张", "主题阅读，同时打开五本书"},
        {"把金句摘进笔记", "追问作者的主张、论据和结构", "读得越慢越好"}
    };
    private static final String[] QUESTIONS = {
        "时间只有二十分钟，你最该做的是哪一层？",
        "分析阅读最核心的动作是？"
    };
    private static final String[] WHY = {
        "时间有限时先检视。分析阅读假设你已经决定这本书值得细读。",
        "分析阅读是主动追问，不是收集句子，也不是把慢当成深刻。"
    };
    private static final int[] ANSWERS = {1, 1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = column();
        root.setBackgroundColor(PAPER);
        root.setPadding(0, dp(28), 0, 0);
        root.addView(header());
        body = column();
        body.setPadding(dp(22), dp(8), dp(22), dp(16));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(scroll);
        tabs = tabBar();
        root.addView(tabs);
        setContentView(root);
        show();
    }

    private View header() {
        LinearLayout row = row();
        row.setPadding(dp(22), dp(8), dp(22), 0);
        row.setGravity(Gravity.BOTTOM);
        TextView mark = text("砚", 28, INK, true);
        mark.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(mark);
        row.addView(text("0.2.0", 12, MUTED, false));
        return row;
    }

    private void show() {
        body.removeAllViews();
        if (tab == 0) today();
        else if (tab == 1) shelf();
        else if (tab == 2) review();
        else me();
        paintTabs();
    }

    private void today() {
        if (step == 0) {
            kicker("第三章 · 阅读的四个层次");
            title("先分清你在哪一层");
            steps(0);
            card("同一本书，读法不该只有一种。艾德勒把阅读分成四层：基础、检视、分析、主题。\n\n"
                + "基础阅读是看懂句子。检视阅读是在有限时间里摸清这本书值不值得细读。分析阅读才是逐章追问作者到底在主张什么。\n\n"
                + "多数人卡住，是用检视的速度做分析的事，或者把分析阅读当成抄笔记。");
            primary("我读完了，用自己的话说", () -> { step = 1; show(); });
        } else if (step == 1) {
            kicker("复述");
            title("不要抄刚才的句子");
            steps(1);
            EditText input = new EditText(this);
            input.setText(recall);
            input.setHint("用你自己的话写两句");
            input.setMinLines(4);
            input.setTextColor(INK);
            input.setHintTextColor(MUTED);
            input.setBackground(rounded(Color.WHITE, dp(12)));
            input.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout box = column();
            box.setBackground(rounded(CARD, dp(16)));
            box.setPadding(dp(16), dp(16), dp(16), dp(16));
            box.addView(text("用你自己的话：分析阅读和检视阅读，差在哪一件事上？", 15, MUTED, false));
            box.addView(space(12));
            box.addView(input);
            body.addView(box);
            primary("去检验", () -> {
                recall = input.getText().toString().trim();
                if (recall.length() < 8) {
                    input.setError("再写具体一点");
                } else {
                    picked = null;
                    step = 2;
                    show();
                }
            });
        } else {
            quiz();
        }
    }

    private void quiz() {
        kicker("第 " + (quiz + 1) + " / " + QUESTIONS.length + " 题");
        title(QUESTIONS[quiz]);
        steps(2);
        for (int i = 0; i < OPTIONS[quiz].length; i++) {
            final int index = i;
            int color = INK;
            if (picked != null && index == ANSWERS[quiz]) color = OK;
            else if (picked != null && index == picked) color = WARN;
            Button button = new Button(this);
            button.setText(OPTIONS[quiz][i]);
            button.setAllCaps(false);
            button.setTextColor(color);
            button.setBackground(rounded(CARD, dp(14)));
            button.setPadding(dp(14), dp(12), dp(14), dp(12));
            button.setOnClickListener(v -> {
                if (picked == null) {
                    picked = index;
                    show();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            body.addView(button, lp);
        }
        if (picked == null) return;
        body.addView(space(14));
        String lead = picked == ANSWERS[quiz] ? "对了。" : "还差一点。";
        body.addView(text(lead + " " + WHY[quiz], 15, MUTED, false));
        boolean last = quiz == QUESTIONS.length - 1;
        primary(last ? "今天这段先到这里" : "下一题", () -> {
            if (!last) {
                quiz += 1;
                picked = null;
            } else {
                step = 0;
                quiz = 0;
                picked = null;
                tab = 3;
            }
            show();
        });
    }

    private void shelf() {
        kicker("书架");
        title("在学的书");
        body.addView(text("一次只推进一本。上传新书是下一版的事。", 15, MUTED, false));
        body.addView(space(16));
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rounded(CARD, dp(16)));
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView spine = text("阅", 18, ACCENT_INK, true);
        spine.setGravity(Gravity.CENTER);
        spine.setBackground(rounded(ACCENT, dp(8)));
        spine.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(72)));
        row.addView(spine);
        row.addView(spaceW(14));
        LinearLayout info = column();
        info.addView(text("如何阅读一本书", 20, INK, true));
        info.addView(text("艾德勒 · 已学 18%", 14, MUTED, false));
        row.addView(info);
        body.addView(row);
        primary("继续第三章", () -> { tab = 0; step = 0; show(); });
    }

    private void review() {
        kicker("复习");
        title("到期的，才出现在这里");
        body.addView(text("没到期的不列出来。", 15, MUTED, false));
        body.addView(space(16));
        reviewCard("阅读的四个层次", "今天");
        body.addView(space(12));
        reviewCard("检视阅读的步骤", "明天");
        primary("先复习今天到期的", () -> { tab = 0; step = 0; show(); });
    }

    private void reviewCard(String name, String due) {
        LinearLayout box = column();
        box.setBackground(rounded(CARD, dp(16)));
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout line = row();
        TextView left = text(name, 18, INK, true);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        line.addView(left);
        line.addView(text(due, 13, WARN, true));
        box.addView(line);
        box.addView(text("如何阅读一本书", 14, MUTED, false));
        body.addView(box);
    }

    private void me() {
        kicker("我的");
        title("学习记录");
        card("原生安卓界面，不是网页套壳。版本 0.2.0。样例课程还没接模型和上传。");
    }

    private LinearLayout tabBar() {
        LinearLayout bar = row();
        bar.setBackgroundColor(Color.parseColor("#F7FFFDF8"));
        bar.setPadding(dp(8), dp(6), dp(8), dp(10));
        String[] labels = {"今天", "书架", "复习", "我的"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = text(labels[i], 15, MUTED, false);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> { tab = index; show(); });
            item.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));
            bar.addView(item);
        }
        return bar;
    }

    private void paintTabs() {
        for (int i = 0; i < tabs.getChildCount(); i++) {
            TextView view = (TextView) tabs.getChildAt(i);
            boolean on = i == tab;
            view.setTextColor(on ? INK : MUTED);
            view.setTypeface(null, on ? Typeface.BOLD : Typeface.NORMAL);
            view.setBackground(on ? rounded(Color.parseColor("#141F4D45"), dp(12)) : null);
        }
    }

    private void kicker(String value) {
        body.addView(text(value, 13, ACCENT, true));
        body.addView(space(6));
    }

    private void title(String value) {
        body.addView(text(value, 30, INK, true));
        body.addView(space(14));
    }

    private void steps(int index) {
        String[] names = {"听懂", "复述", "做题"};
        LinearLayout line = row();
        for (int i = 0; i < names.length; i++) {
            TextView item = text(names[i], 13, i == index ? INK : MUTED, i == index);
            item.setGravity(Gravity.CENTER);
            item.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            line.addView(item);
        }
        body.addView(line);
        body.addView(space(16));
    }

    private void card(String value) {
        TextView view = text(value, 16, INK, false);
        view.setBackground(rounded(CARD, dp(16)));
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        view.setLineSpacing(dp(4), 1f);
        body.addView(view);
    }

    private void primary(String label, Runnable action) {
        body.addView(space(16));
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(ACCENT_INK);
        button.setTextSize(16);
        button.setBackground(rounded(ACCENT, dp(12)));
        button.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        button.setOnClickListener(v -> action.run());
        body.addView(button);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
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
        view.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return view;
    }

    private View spaceW(int w) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(w, 1));
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
