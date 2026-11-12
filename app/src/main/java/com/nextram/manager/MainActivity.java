package com.nextram.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String BG = "#080B10";
    private static final String CARD = "#111722";
    private static final String STROKE = "#1C2433";
    private static final String ACCENT = "#4C8DFF";
    private static final String MINT = "#29D3A8";
    private static final String TEXT = "#E8ECF3";
    private static final String SUB = "#8C97A8";
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static final String[][] TOGGLES = {
            {"ZRAM_ENABLED", "ZRAM включён"},
            {"ZRAM_AUTO_TUNE", "Автотюнинг ZRAM"},
            {"SWAP_ENABLED", "Swap-файл"},
            {"DYNAMIC_SWAPPINESS", "Динамический swappiness"},
            {"PERFORMANCE_MODE", "Режим производительности"},
            {"EXTRA_TUNING", "Дополнительный тюнинг ядра"},
            {"PLAY_ENABLED", "Игровой режим PLAY"},
            {"IO_SCHEDULER_TUNE", "Тюнинг планировщика ввода-вывода"}
    };

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Map<String, String> cfg = new LinkedHashMap<String, String>();
    private final List<String> raw = new ArrayList<String>();

    private LinearLayout root;
    private LinearLayout algoRow;
    private Gauge gauge;
    private TextView tvStatus;
    private TextView tvUsed;
    private TextView tvFree;
    private TextView tvCached;
    private TextView tvZram;
    private TextView tvSwap;

    private String modDir;
    private boolean rootOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        new Thread(new Runnable() {
            @Override
            public void run() {
                init();
            }
        }).start();
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

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Mem m = readMem();
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            render(m);
                        }
                    });
                }
            }).start();
            ui.postDelayed(this, 2000);
        }
    };

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor(BG));
        scroll.setFillViewport(true);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(30));
        scroll.addView(root, new FrameLayout.LayoutParams(MATCH, WRAP));

        TextView title = new TextView(this);
        title.setText("NextRAM");
        title.setTextColor(Color.parseColor(TEXT));
        title.setTextSize(30);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView slogan = new TextView(this);
        slogan.setText("управление памятью модуля");
        slogan.setTextColor(Color.parseColor(SUB));
        slogan.setTextSize(13);
        root.addView(slogan);

        tvStatus = new TextView(this);
        tvStatus.setText("проверяю root");
        tvStatus.setTextSize(12);
        chip(tvStatus, "#16202E", SUB);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        statusLp.topMargin = dp(10);
        root.addView(tvStatus, statusLp);

        gauge = new Gauge(this);
        LinearLayout.LayoutParams gaugeLp = new LinearLayout.LayoutParams(MATCH, dp(190));
        gaugeLp.topMargin = dp(4);
        root.addView(gauge, gaugeLp);

        LinearLayout memCard = card("память");
        tvUsed = addRow(memCard, "Занято", "—");
        tvFree = addRow(memCard, "Свободно", "—");
        tvCached = addRow(memCard, "Кэш", "—");
        tvZram = addRow(memCard, "ZRAM", "—");
        tvSwap = addRow(memCard, "Swap", "—");

        LinearLayout params = card("параметры");
        addSwitches(params);
        addSlider(params, "ZRAM_RATIO", "Коэффициент ZRAM", 5, 40, 10f);
        addSlider(params, "SWAPPINESS", "swappiness", 0, 200, 1f);
        addSlider(params, "CACHE_PRESSURE", "vfs_cache_pressure", 0, 200, 1f);
        addSlider(params, "DIRTY_RATIO", "dirty_ratio", 1, 60, 1f);

        TextView algoLabel = new TextView(this);
        algoLabel.setText("Алгоритм сжатия ZRAM");
        algoLabel.setTextColor(Color.parseColor(SUB));
        algoLabel.setTextSize(13);
        algoLabel.setPadding(0, dp(14), 0, 0);
        params.addView(algoLabel);

        algoRow = new LinearLayout(this);
        algoRow.setOrientation(LinearLayout.HORIZONTAL);
        params.addView(algoRow);
        buildAlgoButtons();

        LinearLayout actions = card("действия");
        actions.addView(actionButton("Применить настройки", ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndApply();
            }
        }));
        actions.addView(actionButton("Сбросить кэш страниц", "#232C3B", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dropCaches();
            }
        }));
        actions.addView(actionButton("Показать логи", "#232C3B", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogs();
            }
        }));

        TextView footer = new TextView(this);
        footer.setText("Модуль NextRAM · нужен root");
        footer.setTextColor(Color.parseColor(SUB));
        footer.setTextSize(11);
        footer.setPadding(0, dp(18), 0, 0);
        root.addView(footer);

        return scroll;
    }

    private void init() {
        rootOk = Shell.hasRoot();
        if (rootOk) {
            Shell.Result r = Shell.run("for d in /data/adb/modules/NextRAM /data/adb/modules/nextram; do test -f $d/config.conf && echo $d; done");
            String[] parts = lines(r.out);
            for (int i = 0; i < parts.length; i++) {
                String candidate = parts[i].trim();
                if (candidate.length() > 0) {
                    modDir = candidate;
                    break;
                }
            }
        }
        loadConfig();
        ui.post(new Runnable() {
            @Override
            public void run() {
                setContentView(buildUi());
                updateStatus();
            }
        });
    }

    private void loadConfig() {
        raw.clear();
        cfg.clear();
        if (modDir == null) {
            return;
        }
        Shell.Result r = Shell.run("cat " + modDir + "/config.conf");
        String[] parts = lines(r.out);
        for (int i = 0; i < parts.length; i++) {
            String line = parts[i];
            raw.add(line);
            if (line.trim().startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                cfg.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
    }

    private void updateStatus() {
        if (tvStatus == null) {
            return;
        }
        if (!rootOk) {
            tvStatus.setText("нет root — доступен только просмотр");
            chip(tvStatus, "#2E1616", "#FF8A8A");
        } else if (modDir == null) {
            tvStatus.setText("root есть, модуль NextRAM не найден");
            chip(tvStatus, "#2E2416", "#FFC97A");
        } else {
            tvStatus.setText("root · " + modDir);
            chip(tvStatus, "#12291F", MINT);
        }
    }

    private void addSwitches(LinearLayout parent) {
        for (int i = 0; i < TOGGLES.length; i++) {
            final String key = TOGGLES[i][0];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(9), 0, dp(9));

            TextView label = new TextView(this);
            label.setText(TOGGLES[i][1]);
            label.setTextColor(Color.parseColor(TEXT));
            label.setTextSize(14);
            row.addView(label, new LinearLayout.LayoutParams(0, WRAP, 1f));

            Switch box = new Switch(this);
            box.setChecked("true".equalsIgnoreCase(cfg.get(key)));
            box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    cfg.put(key, checked ? "true" : "false");
                }
            });
            row.addView(box, new LinearLayout.LayoutParams(WRAP, WRAP));
            parent.addView(row);
        }
    }

    private void addSlider(LinearLayout parent, String key, String label, int min, int max, float divisor) {
        final String cfgKey = key;
        final int cfgMin = min;
        final float cfgDivisor = divisor;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, 0);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);

        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextColor(Color.parseColor(SUB));
        caption.setTextSize(13);
        head.addView(caption, new LinearLayout.LayoutParams(0, WRAP, 1f));

        final TextView value = new TextView(this);
        value.setTextColor(Color.parseColor(ACCENT));
        value.setTextSize(13);
        value.setTypeface(null, Typeface.BOLD);
        value.setGravity(Gravity.END);
        head.addView(value, new LinearLayout.LayoutParams(WRAP, WRAP));
        box.addView(head);

        int current = cfgMin;
        try {
            current = Math.round(Float.parseFloat(cfg.get(cfgKey)) * cfgDivisor);
        } catch (Exception ignored) {
        }
        if (current < cfgMin) {
            current = cfgMin;
        }
        if (current > max) {
            current = max;
        }

        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setProgress(current - min);
        value.setText(pretty(current, cfgDivisor));

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int real = progress + cfgMin;
                String formatted = pretty(real, cfgDivisor);
                value.setText(formatted);
                cfg.put(cfgKey, formatted);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        box.addView(bar, new LinearLayout.LayoutParams(MATCH, WRAP));
        parent.addView(box);
    }

    private String pretty(int value, float divisor) {
        if (divisor > 1f) {
            return String.format(Locale.US, "%.1f", value / divisor);
        }
        return String.valueOf(value);
    }

    private void buildAlgoButtons() {
        final String[] algos = {"lz4", "lzo-rle", "zstd", "deflate"};
        algoRow.removeAllViews();
        for (int i = 0; i < algos.length; i++) {
            final String algo = algos[i];
            TextView b = new TextView(this);
            b.setText(algo);
            b.setTextSize(12);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(6), dp(9), dp(6), dp(9));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            lp.setMargins(dp(3), dp(8), dp(3), 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cfg.put("ZRAM_ALGORITHM", algo);
                    paintAlgo();
                }
            });
            algoRow.addView(b);
        }
        paintAlgo();
    }

    private void paintAlgo() {
        String current = cfg.get("ZRAM_ALGORITHM");
        for (int i = 0; i < algoRow.getChildCount(); i++) {
            TextView view = (TextView) algoRow.getChildAt(i);
            boolean active = view.getText().toString().equals(current);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(Color.parseColor(active ? ACCENT : "#1A2130"));
            view.setBackground(bg);
            view.setTextColor(Color.parseColor(active ? "#FFFFFF" : SUB));
        }
    }

    private TextView actionButton(String text, String colorHex, View.OnClickListener listener) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, dp(13), 0, dp(13));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(Color.parseColor(colorHex));
        button.setBackground(bg);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(10);
        button.setLayoutParams(lp);
        return button;
    }

    private void saveAndApply() {
        if (modDir == null) {
            toast("Модуль NextRAM не найден");
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.size(); i++) {
            String line = raw.get(i);
            int eq = line.indexOf('=');
            if (eq > 0 && !line.trim().startsWith("#")) {
                String key = line.substring(0, eq).trim();
                if (cfg.containsKey(key)) {
                    sb.append(key).append('=').append(cfg.get(key)).append(System.lineSeparator());
                    continue;
                }
            }
            sb.append(line).append(System.lineSeparator());
        }
        final String content = sb.toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File tmp = new File(getCacheDir(), "config.conf");
                    FileOutputStream out = new FileOutputStream(tmp);
                    out.write(content.getBytes("UTF-8"));
                    out.close();
                    Shell.Result copy = Shell.run("cp " + tmp.getAbsolutePath() + " " + modDir + "/config.conf && chmod 644 " + modDir + "/config.conf");
                    if (copy.code != 0) {
                        toast("Не удалось записать конфиг: " + copy.out);
                        return;
                    }
                    Shell.Result applied = Shell.run("sh " + modDir + "/service.sh > /dev/null 2>&1; echo APPLIED");
                    if (applied.out.contains("APPLIED")) {
                        toast("Сохранено и применено");
                    } else {
                        toast("Сохранено, но скрипт вернул ошибку");
                    }
                } catch (Exception e) {
                    toast("Ошибка: " + e.getMessage());
                }
            }
        }).start();
    }

    private void dropCaches() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Shell.Result r = Shell.run("sync; echo 3 > /proc/sys/vm/drop_caches");
                toast(r.code == 0 ? "Кэш страниц сброшен" : "Не удалось: " + r.out);
            }
        }).start();
    }

    private void showLogs() {
        if (modDir == null) {
            toast("Модуль NextRAM не найден");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Shell.Result r = Shell.run("tail -n 200 $(ls -t " + modDir + "/logs/* 2>/dev/null | head -n 1) 2>/dev/null");
                final String text = r.out.length() == 0 ? "Логов пока нет" : r.out;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        TextView viewer = new TextView(MainActivity.this);
                        viewer.setText(text);
                        viewer.setTextSize(11);
                        viewer.setTextColor(Color.parseColor(TEXT));
                        viewer.setTypeface(Typeface.MONOSPACE);
                        viewer.setPadding(dp(14), dp(14), dp(14), dp(14));
                        ScrollView holder = new ScrollView(MainActivity.this);
                        holder.addView(viewer);
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Логи NextRAM")
                                .setView(holder)
                                .setPositiveButton("Закрыть", null)
                                .show();
                    }
                });
            }
        }).start();
    }

    private void toast(final String message) {
        ui.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render(Mem m) {
        if (m == null || gauge == null) {
            return;
        }
        float percent = m.total > 0 ? (m.total - m.avail) * 100f / m.total : 0f;
        gauge.setData(percent, String.format(Locale.US, "%.1f%%", percent), fmt(m.total - m.avail) + " из " + fmt(m.total));
        if (tvUsed != null) {
            tvUsed.setText(fmt(m.total - m.avail));
        }
        if (tvFree != null) {
            tvFree.setText(fmt(m.avail));
        }
        if (tvCached != null) {
            tvCached.setText(m.cached > 0 ? fmt(m.cached) : "—");
        }
        if (tvZram != null) {
            tvZram.setText(m.zramTotal > 0 ? fmt(m.zramUsed) + " из " + fmt(m.zramTotal) : "выключен");
        }
        if (tvSwap != null) {
            tvSwap.setText(m.swapTotal > 0 ? fmt(m.swapTotal - m.swapFree) + " из " + fmt(m.swapTotal) : "нет");
        }
    }

    private Mem readMem() {
        Mem m = new Mem();
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            m.total = info.totalMem;
            m.avail = info.availMem;
        } catch (Exception ignored) {
        }

        BufferedReader meminfo = null;
        try {
            meminfo = new BufferedReader(new FileReader("/proc/meminfo"));
            String line;
            while ((line = meminfo.readLine()) != null) {
                if (line.startsWith("Cached:")) {
                    m.cached = kb(line);
                } else if (line.startsWith("SwapTotal:")) {
                    m.swapTotal = kb(line);
                } else if (line.startsWith("SwapFree:")) {
                    m.swapFree = kb(line);
                }
            }
        } catch (Exception ignored) {
        } finally {
            closeQuietly(meminfo);
        }

        BufferedReader swaps = null;
        try {
            swaps = new BufferedReader(new FileReader("/proc/swaps"));
            String line;
            boolean header = true;
            while ((line = swaps.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.contains("zram")) {
                    String[] parts = line.trim().split(" +");
                    if (parts.length >= 4) {
                        m.zramTotal = Long.parseLong(parts[2]) * 1024L;
                        m.zramUsed = Long.parseLong(parts[3]) * 1024L;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            closeQuietly(swaps);
        }

        return m;
    }

    private void closeQuietly(BufferedReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception ignored) {
            }
        }
    }

    private long kb(String line) {
        try {
            String[] parts = line.trim().split(" +");
            return Long.parseLong(parts[1]) * 1024L;
        } catch (Exception e) {
            return 0;
        }
    }

    private String fmt(long bytes) {
        if (bytes <= 0) {
            return "0 МБ";
        }
        double gb = bytes / 1073741824.0;
        if (gb >= 1.0) {
            return String.format(Locale.US, "%.2f ГБ", gb);
        }
        return String.format(Locale.US, "%.0f МБ", bytes / 1048576.0);
    }

    private String[] lines(String source) {
        if (source == null || source.length() == 0) {
            return new String[0];
        }
        return source.split(String.valueOf((char) 10));
    }

    private LinearLayout card(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(CARD));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), Color.parseColor(STROKE));
        card.setBackground(bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));

        TextView header = new TextView(this);
        header.setText(title.toUpperCase(Locale.US));
        header.setTextColor(Color.parseColor(SUB));
        header.setTextSize(11);
        header.setLetterSpacing(0.14f);
        header.setTypeface(null, Typeface.BOLD);
        card.addView(header);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(14);
        root.addView(card, lp);
        return card;
    }

    private TextView addRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(7), 0, dp(7));

        TextView left = new TextView(this);
        left.setText(label);
        left.setTextColor(Color.parseColor(SUB));
        left.setTextSize(13);
        row.addView(left, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView right = new TextView(this);
        right.setText(value);
        right.setTextColor(Color.parseColor(TEXT));
        right.setTextSize(13);
        right.setTypeface(null, Typeface.BOLD);
        right.setGravity(Gravity.END);
        row.addView(right, new LinearLayout.LayoutParams(WRAP, WRAP));

        parent.addView(row);
        return right;
    }

    private void chip(TextView view, String background, String textColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(Color.parseColor(background));
        view.setBackground(bg);
        view.setTextColor(Color.parseColor(textColor));
        view.setPadding(dp(11), dp(6), dp(11), dp(6));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    static class Mem {
        long total;
        long avail;
        long cached;
        long swapTotal;
        long swapFree;
        long zramTotal;
        long zramUsed;
    }
}
