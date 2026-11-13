package com.nextram.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    public static final String AUTHOR = "LineXin";
    public static final String CREDITS = "Rexamm1t";
    public static final String APP_VERSION = "2.2";
    public static final String REPO = "https://github.com/LineXin/NextRAM";

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final View[] navInd = new View[4];
    private final TextView[] navTxt = new TextView[4];

    private FrameLayout content;
    private LinearLayout footerHost;
    private int tab = 0;

    private boolean rooted;
    private Config cfg;

    private RingGauge gauge;
    private TextView tileZramVal;
    private TextView tileSwapVal;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateMem();
            ui.postDelayed(this, 2000L);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.SURFACE);
        rooted = Shell.hasRoot();
        cfg = Config.load();
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.removeCallbacks(tick);
        ui.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(tick);
    }

    private int dp(float v) {
        return Ui.dp(this, v);
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        footerHost = new LinearLayout(this);
        footerHost.setOrientation(LinearLayout.VERTICAL);
        footerHost.setBackgroundColor(Ui.BG);
        root.addView(footerHost, new LinearLayout.LayoutParams(MATCH, WRAP));

        View line = new View(this);
        line.setBackgroundColor(Ui.STROKE);
        root.addView(line, new LinearLayout.LayoutParams(MATCH, Math.max(1, dp(1))));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Ui.SURFACE);
        String[] labels = {"Обзор", "Настройки", "Профили", "О нас"};
        for (int i = 0; i < labels.length; i++) {
            nav.addView(navItem(i, labels[i]));
        }
        root.addView(nav, new LinearLayout.LayoutParams(MATCH, WRAP));

        setContentView(root);
        showTab(tab);
    }

    private View navItem(final int index, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, dp(9), 0, dp(11));
        box.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1f));

        View ind = new View(this);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(16), dp(3));
        ilp.bottomMargin = dp(8);
        ind.setLayoutParams(ilp);

        TextView tv = Ui.text(this, label, 11.5f, Ui.TEXT_MUTE, Ui.medium());
        tv.setLetterSpacing(0.02f);

        navInd[index] = ind;
        navTxt[index] = tv;

        box.addView(ind);
        box.addView(tv);
        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(index);
            }
        });
        return box;
    }

    private void paintNav() {
        int accent = Prefs.accent(this);
        for (int i = 0; i < navInd.length; i++) {
            boolean on = i == tab;
            if (navInd[i] != null) {
                navInd[i].setBackground(Ui.round(on ? accent : Color.TRANSPARENT, dp(2), 0, 0));
            }
            if (navTxt[i] != null) {
                navTxt[i].setTextColor(on ? Ui.TEXT : Ui.TEXT_MUTE);
            }
        }
    }

    private void showTab(int index) {
        if (content == null) {
            return;
        }
        tab = index;
        gauge = null;
        tileZramVal = null;
        tileSwapVal = null;
        content.removeAllViews();
        if (footerHost != null) {
            footerHost.removeAllViews();
        }

        View page;
        if (index == 0) {
            page = scrollOf(screenOverview());
        } else if (index == 1) {
            page = scrollOf(screenSettings());
            if (footerHost != null) {
                footerHost.addView(applyBar());
            }
        } else if (index == 2) {
            page = scrollOf(screenProfiles());
        } else {
            page = scrollOf(screenAbout());
        }
        content.addView(page);
        paintNav();
        if (index == 0) {
            updateMem();
        }
    }

    private View scrollOf(View inner) {
        ScrollView s = new ScrollView(this);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.addView(inner, new FrameLayout.LayoutParams(MATCH, WRAP));
        return s;
    }

    private LinearLayout column() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(14), dp(16), dp(24));
        return col;
    }

    private View pageHeader(String title, String sub) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(0, dp(4), 0, dp(16));
        h.addView(Ui.text(this, title, 27, Ui.TEXT, Ui.medium()));
        TextView s = Ui.text(this, sub, 12.5f, Ui.TEXT_MUTE, Ui.mono());
        s.setPadding(0, dp(7), 0, 0);
        h.addView(s);
        return h;
    }

    private TextView chip(String label) {
        TextView t = Ui.text(this, label, 11, Prefs.accent(this), Ui.medium());
        t.setPadding(dp(9), dp(4), dp(9), dp(5));
        t.setLetterSpacing(0.06f);
        t.setBackground(Ui.round(Prefs.accentSoft(this), dp(20), 0, 0));
        return t;
    }

    private View legendDot(int color, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.rightMargin = dp(16);
        row.setLayoutParams(lp);

        View dot = new View(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(7), dp(7));
        dlp.rightMargin = dp(6);
        dot.setLayoutParams(dlp);
        dot.setBackground(Ui.round(color, dp(4), 0, 0));
        row.addView(dot);
        row.addView(Ui.text(this, label, 11, Ui.TEXT_MUTE, Ui.sans()));
        return row;
    }

    private View useCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(Ui.round(Ui.SURFACE, dp(18), Ui.STROKE, Math.max(1, dp(1))));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);

        View dot = new View(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(8), dp(8));
        dlp.rightMargin = dp(9);
        dot.setLayoutParams(dlp);
        dot.setBackground(Ui.round(rooted ? Ui.GREEN : Ui.DANGER, dp(4), 0, 0));
        card.addView(dot);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, rooted ? "Root предоставлен" : "Root недоступен", 13.5f, Ui.TEXT, Ui.medium()));
        TextView s = Ui.text(this, cfg.found ? cfg.dir : "config.conf не найден", 10.5f, Ui.TEXT_MUTE, Ui.mono());
        s.setPadding(0, dp(4), 0, 0);
        texts.addView(s);
        card.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView state = Ui.text(this, cfg.found ? "АКТИВЕН" : "ОФФЛАЙН", 10, cfg.found ? Prefs.accent(this) : Ui.DANGER, Ui.medium());
        state.setLetterSpacing(0.14f);
        card.addView(state);
        return card;
    }

    private View screenOverview() {
        LinearLayout col = column();

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, dp(4), 0, dp(16));
        head.addView(Ui.text(this, "NextRAM", 27, Ui.TEXT, Ui.medium()));
        TextView v = chip("v" + APP_VERSION);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(WRAP, WRAP);
        clp.leftMargin = dp(10);
        v.setLayoutParams(clp);
        head.addView(v);
        head.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        col.addView(head);

        col.addView(useCard());

        LinearLayout gcard = Ui.card(this);
        gcard.addView(Ui.caption(this, "ОПЕРАТИВНАЯ ПАМЯТЬ"));
        gauge = new RingGauge(this);
        gcard.addView(gauge, new LinearLayout.LayoutParams(MATCH, dp(214)));

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER);
        legend.addView(legendDot(Prefs.accent(this), "занято"));
        legend.addView(legendDot(0xFF3A3A42, "кэш"));
        legend.addView(legendDot(0xFF55555E, "свободно"));
        gcard.addView(legend);
        col.addView(gcard);

        LinearLayout tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.HORIZONTAL);
        tiles.setPadding(0, 0, 0, dp(12));

        LinearLayout t1 = tileView("Zram", "сжатие в ОЗУ");
        tileZramVal = (TextView) t1.getChildAt(1);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, WRAP, 1f);
        lp1.rightMargin = dp(6);
        tiles.addView(t1, lp1);

        LinearLayout t2 = tileView("Swap", "подкачка на диск");
        tileSwapVal = (TextView) t2.getChildAt(1);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, WRAP, 1f);
        lp2.leftMargin = dp(6);
        tiles.addView(t2, lp2);

        col.addView(tiles);

        LinearLayout actions = Ui.card(this);
        actions.addView(Ui.caption(this, "БЫСТРЫЕ ДЕЙСТВИЯ"));
        actions.addView(Ui.divider(this));
        actions.addView(actionRow("Сбросить кэш страниц", "drop_caches 3", new Runnable() {
            @Override
            public void run() {
                Shell.Result r = Shell.run("sync; echo 3 > /proc/sys/vm/drop_caches");
                toast(r.ok() ? "Кэш страниц сброшен" : "Не удалось: " + r.out.trim());
            }
        }));
        actions.addView(actionRow("Перезапустить модуль", "service.sh", new Runnable() {
            @Override
            public void run() {
                if (cfg.save()) {
                    toast("Модуль перезапущен");
                } else {
                    toast("Конфиг не найден");
                }
            }
        }));
        actions.addView(actionRow("Лог модуля", "последние 40 строк", new Runnable() {
            @Override
            public void run() {
                openLog();
            }
        }));
        actions.addView(actionRow("Обновить данные", "перечитать /proc", new Runnable() {
            @Override
            public void run() {
                updateMem();
                toast("Обновлено");
            }
        }));
        col.addView(actions);

        return col;
    }

    private LinearLayout tileView(String title, String sub) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setBackground(Ui.round(Ui.SURFACE, dp(16), Ui.STROKE, Math.max(1, dp(1))));
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        t.addView(Ui.caption(this, title.toUpperCase()));

        TextView value = Ui.text(this, "—", 20, Ui.TEXT, Ui.mono());
        value.setPadding(0, dp(9), 0, 0);
        t.addView(value);

        TextView s = Ui.text(this, sub, 10.5f, Ui.TEXT_MUTE, Ui.sans());
        s.setPadding(0, dp(5), 0, 0);
        t.addView(s);
        return t;
    }

    private View actionRow(String title, String sub, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(50));
        row.setPadding(0, dp(9), 0, dp(9));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, title, 14.5f, Ui.TEXT, Ui.sans()));
        TextView s = Ui.text(this, sub, 10.5f, Ui.TEXT_MUTE, Ui.mono());
        s.setPadding(0, dp(4), 0, 0);
        texts.addView(s);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));
        row.addView(Ui.text(this, "›", 20, Ui.TEXT_MUTE, Ui.sans()));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        return row;
    }

    private View screenSettings() {
        LinearLayout col = column();
        col.addView(pageHeader("Настройки", cfg.found ? cfg.dir : "модуль не найден"));

        if (!cfg.found) {
            col.addView(noteCard("Конфиг не прочитан",
                    "Нет root или модуль не установлен. Показаны ключи по умолчанию, правки не сохранятся."));
        }

        List<String> keys = cfg.keys();
        LinearLayout card = Ui.card(this);
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            View row;
            if (cfg.isBool(key)) {
                row = yesNoRow(key);
            } else if (cfg.isNumber(key)) {
                row = numberRow(key);
            } else {
                row = textRow(key);
            }
            card.addView(row);
            if (i < keys.size() - 1) {
                card.addView(Ui.divider(this));
            }
        }
        col.addView(card);

        TextView hint = Ui.text(this, "Ключи читаются из config.conf модуля. Незнакомые строки сохраняются как есть.",
                11, Ui.TEXT_MUTE, Ui.sans());
        hint.setPadding(dp(4), dp(2), dp(4), 0);
        col.addView(hint);
        return col;
    }

    private View yesNoRow(final String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(46));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, Config.pretty(key), 14, Ui.TEXT, Ui.sans()));
        TextView code = Ui.text(this, key, 10, Ui.TEXT_MUTE, Ui.mono());
        code.setPadding(0, dp(3), 0, 0);
        texts.addView(code);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));

        PillSwitch sw = new PillSwitch(this);
        sw.setChecked(cfg.boolValue(key, false));
        sw.setListener(new PillSwitch.Listener() {
            @Override
            public void onChanged(boolean on) {
                cfg.set(key, on ? "true" : "false");
            }
        });
        row.addView(sw);
        return row;
    }

    private View numberRow(final String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(52));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, Config.pretty(key), 14, Ui.TEXT, Ui.sans()));
        TextView code = Ui.text(this, key, 10, Ui.TEXT_MUTE, Ui.mono());
        code.setPadding(0, dp(3), 0, 0);
        texts.addView(code);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));

        final TextView value = Ui.text(this, cfg.value(key, "0"), 14, Prefs.accent(this), Ui.mono());
        value.setGravity(Gravity.CENTER);
        value.setMinWidth(dp(38));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setBackground(Ui.round(Ui.SURFACE_HI, dp(12), Ui.STROKE, Math.max(1, dp(1))));
        box.setPadding(dp(2), dp(2), dp(2), dp(2));

        TextView minus = stepButton("−");
        TextView plus = stepButton("+");

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int next = Math.max(0, cfg.intValue(key, 0) - 5);
                cfg.set(key, String.valueOf(next));
                value.setText(String.valueOf(next));
            }
        });
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int next = cfg.intValue(key, 0) + 5;
                cfg.set(key, String.valueOf(next));
                value.setText(String.valueOf(next));
            }
        });
        value.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptNumber(key, value);
            }
        });

        box.addView(minus);
        box.addView(value);
        box.addView(plus);
        row.addView(box);
        return row;
    }

    private TextView stepButton(String label) {
        TextView t = Ui.text(this, label, 16, Ui.TEXT_DIM, Ui.sans());
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(32)));
        return t;
    }

    private void promptNumber(final String key, final TextView value) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(cfg.value(key, "0"));
        new AlertDialog.Builder(this)
                .setTitle(Config.pretty(key))
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String s = input.getText().toString().trim();
                        if (s.length() > 0) {
                            cfg.set(key, s);
                            value.setText(s);
                        }
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private View textRow(final String key) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(52));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, Config.pretty(key), 14, Ui.TEXT, Ui.sans()));
        TextView code = Ui.text(this, key, 10, Ui.TEXT_MUTE, Ui.mono());
        code.setPadding(0, dp(3), 0, 0);
        texts.addView(code);
        row.addView(texts, new LinearLayout.LayoutParams(0, WRAP, 1f));

        final TextView value = Ui.text(this, cfg.value(key, ""), 13, Prefs.accent(this), Ui.mono());
        value.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText input = new EditText(MainActivity.this);
                input.setText(cfg.value(key, ""));
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(Config.pretty(key))
                        .setView(input)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                String s = input.getText().toString().trim();
                                cfg.set(key, s);
                                value.setText(s);
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        });
        row.addView(value);
        return row;
    }

    private View applyBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(16), dp(6), dp(16), dp(10));

        TextView btn = new TextView(this);
        btn.setText("ПРИМЕНИТЬ");
        btn.setGravity(Gravity.CENTER);
        btn.setTextSize(13);
        btn.setTypeface(Ui.medium());
        btn.setLetterSpacing(0.14f);
        btn.setTextColor(Ui.BG);
        btn.setPadding(0, dp(15), 0, dp(15));
        btn.setBackground(Ui.gradient(Prefs.accent(this), Prefs.accent2(this), dp(14)));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyNow();
            }
        });
        bar.addView(btn, new LinearLayout.LayoutParams(MATCH, WRAP));
        return bar;
    }

    private View screenProfiles() {
        LinearLayout col = column();
        col.addView(pageHeader("Профили", "готовые наборы значений"));

        col.addView(profileCard("Экономный", "4 ГБ ОЗУ, приоритет свободной памяти",
                new String[][]{
                        {"ZRAM_ENABLED", "true"}, {"ZRAM_RATIO", "25"},
                        {"SWAP_ENABLED", "true"}, {"SWAPPINESS", "90"},
                        {"DYNAMIC_SWAPPINESS", "true"}, {"CACHE_PRESSURE", "60"}}));

        col.addView(profileCard("Баланс", "значения модуля по умолчанию",
                new String[][]{
                        {"ZRAM_ENABLED", "true"}, {"ZRAM_RATIO", "50"},
                        {"SWAP_ENABLED", "false"}, {"SWAPPINESS", "60"},
                        {"DYNAMIC_SWAPPINESS", "true"}, {"CACHE_PRESSURE", "100"}}));

        col.addView(profileCard("Максимум", "больше ZRAM и агрессивное сжатие",
                new String[][]{
                        {"ZRAM_ENABLED", "true"}, {"ZRAM_RATIO", "75"},
                        {"SWAP_ENABLED", "true"}, {"SWAPPINESS", "100"},
                        {"DYNAMIC_SWAPPINESS", "false"}, {"CACHE_PRESSURE", "50"}}));

        col.addView(profileCard("Игра", "меньше свопа, меньше фризов",
                new String[][]{
                        {"ZRAM_ENABLED", "true"}, {"ZRAM_RATIO", "50"},
                        {"SWAP_ENABLED", "false"}, {"SWAPPINESS", "40"},
                        {"PLAY_ENABLED", "true"}, {"CACHE_PRESSURE", "100"}}));

        return col;
    }

    private View profileCard(final String name, String sub, final String[][] pairs) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, name, 17, Ui.TEXT, Ui.medium()));
        TextView s = Ui.text(this, sub, 11.5f, Ui.TEXT_MUTE, Ui.sans());
        s.setPadding(0, dp(5), 0, 0);
        card.addView(s);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.length; i++) {
            sb.append(pairs[i][0]).append('=').append(pairs[i][1]);
            if (i < pairs.length - 1) {
                sb.append('\n');
            }
        }
        TextView kv = Ui.text(this, sb.toString(), 11, Ui.TEXT_DIM, Ui.mono());
        kv.setLineSpacing(dp(3), 1f);
        kv.setPadding(0, dp(11), 0, 0);
        card.addView(kv);

        card.addView(smallButton("ПРИМЕНИТЬ ПРОФИЛЬ", new Runnable() {
            @Override
            public void run() {
                applyProfile(name, pairs);
            }
        }));
        return card;
    }

    private View smallButton(String label, final Runnable action) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(11.5f);
        b.setLetterSpacing(0.12f);
        b.setTypeface(Ui.medium());
        b.setTextColor(Prefs.accent(this));
        b.setPadding(0, dp(11), 0, dp(11));
        b.setBackground(Ui.round(Prefs.accentSoft(this), dp(12), Prefs.accentLine(this), Math.max(1, dp(1))));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(13);
        b.setLayoutParams(lp);
        return b;
    }

    private void applyProfile(String name, String[][] pairs) {
        for (int i = 0; i < pairs.length; i++) {
            if (cfg.value(pairs[i][0], null) != null) {
                cfg.set(pairs[i][0], pairs[i][1]);
            }
        }
        if (applyNow()) {
            toast("Профиль «" + name + "» применён");
        }
    }

    private View screenAbout() {
        LinearLayout col = column();
        col.addView(pageHeader("О приложении", "v" + APP_VERSION + " · native"));

        LinearLayout credits = Ui.card(this);
        credits.addView(Ui.caption(this, "CREDITS"));
        credits.addView(Ui.vspace(this, 12));
        credits.addView(creditRow("Автор", AUTHOR));
        credits.addView(Ui.divider(this));
        credits.addView(creditRow("Дизайн и идея", CREDITS));
        credits.addView(Ui.divider(this));
        credits.addView(creditRow("Модуль", "NextRAM · Magisk"));
        col.addView(credits);

        LinearLayout colors = Ui.card(this);
        colors.addView(Ui.caption(this, "АКЦЕНТНЫЙ ЦВЕТ"));
        TextView hint = Ui.text(this, "Меняется сразу и запоминается", 11, Ui.TEXT_MUTE, Ui.sans());
        hint.setPadding(0, dp(8), 0, dp(13));
        colors.addView(hint);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < Prefs.PRESETS.length; i++) {
            swatches.addView(swatch(Prefs.PRESETS[i]));
        }
        hs.addView(swatches);
        colors.addView(hs);
        col.addView(colors);

        LinearLayout dev = Ui.card(this);
        dev.addView(Ui.caption(this, "УСТРОЙСТВО"));
        dev.addView(Ui.vspace(this, 12));
        dev.addView(creditRow("Модель", Build.MANUFACTURER + " " + Build.MODEL));
        dev.addView(Ui.divider(this));
        dev.addView(creditRow("Android", Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT));
        dev.addView(Ui.divider(this));
        dev.addView(creditRow("Root", rooted ? "есть" : "нет"));
        dev.addView(Ui.divider(this));
        dev.addView(creditRow("Конфиг", cfg.found ? "найден" : "нет"));
        col.addView(dev);

        LinearLayout links = Ui.card(this);
        links.addView(actionRow("Репозиторий модуля", "github.com/LineXin/NextRAM", new Runnable() {
            @Override
            public void run() {
                openRepo();
            }
        }));
        col.addView(links);

        return col;
    }

    private View creditRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.text(this, label, 12, Ui.TEXT_MUTE, Ui.sans()),
                new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView v = Ui.text(this, value, 12.5f, Ui.TEXT, Ui.mono());
        v.setGravity(Gravity.END);
        v.setMaxLines(2);
        row.addView(v, new LinearLayout.LayoutParams(0, WRAP, 1f));
        return row;
    }

    private View swatch(final int color) {
        View v = new View(this);
        int size = dp(40);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.rightMargin = dp(10);
        v.setLayoutParams(lp);
        boolean selected = Prefs.accent(this) == color;
        v.setBackground(Ui.round(color, dp(20), selected ? Ui.TEXT : Ui.STROKE, Math.max(1, dp(selected ? 2 : 1))));
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Prefs.setAccent(MainActivity.this, color);
                build();
            }
        });
        return v;
    }

    private View noteCard(String title, String text) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, title, 14, Ui.DANGER, Ui.medium()));
        TextView t = Ui.text(this, text, 12, Ui.TEXT_DIM, Ui.sans());
        t.setPadding(0, dp(7), 0, 0);
        card.addView(t);
        return card;
    }

    private void updateMem() {
        Mem.read();
        if (gauge != null) {
            float used = Mem.usedFraction();
            gauge.setColors(Prefs.accent(this), Prefs.accent2(this));
            gauge.setData(used, Math.round(used * 100f) + "%", "ЗАНЯТО",
                    Mem.gb(Mem.totalKb - Mem.availKb) + " из " + Mem.gb(Mem.totalKb));
        }
        if (tileZramVal != null) {
            boolean zram = cfg.boolValue("ZRAM_ENABLED", false);
            tileZramVal.setText(zram ? "ВКЛ" : "ВЫКЛ");
            tileZramVal.setTextColor(zram ? Prefs.accent(this) : Ui.TEXT_MUTE);
        }
        if (tileSwapVal != null) {
            boolean swap = cfg.boolValue("SWAP_ENABLED", false);
            tileSwapVal.setText(swap ? "ВКЛ" : "ВЫКЛ");
            tileSwapVal.setTextColor(swap ? Prefs.accent(this) : Ui.TEXT_MUTE);
        }
    }

    private boolean applyNow() {
        if (!cfg.found) {
            toast("config.conf не найден");
            return false;
        }
        boolean ok = cfg.save();
        toast(ok ? "Записано, модуль перезапущен" : "Не удалось записать конфиг");
        return ok;
    }

    private void openLog() {
        if (!cfg.found) {
            toast("Модуль не найден");
            return;
        }
        Shell.Result r = Shell.run("ls -1 '" + cfg.dir + "' 2>/dev/null; echo '--- log ---'; "
                + "tail -n 40 '" + cfg.dir + "/logs/nextram.log' 2>/dev/null || "
                + "tail -n 40 '" + cfg.dir + "/nextram.log' 2>/dev/null || "
                + "tail -n 40 '" + cfg.dir + "/service.log' 2>/dev/null");
        new AlertDialog.Builder(this)
                .setTitle("Лог модуля")
                .setMessage(r.out == null || r.out.trim().length() == 0 ? "Пусто" : r.out)
                .setPositiveButton("Закрыть", null)
                .show();
    }

    private void openRepo() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPO)));
        } catch (Exception e) {
            toast("Браузер не найден");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
