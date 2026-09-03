package com.example.xaittslab;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String API_BASE = "https://api.x.ai";
    private static final int REQ_SAVE_AUDIO = 1001;
    private static final int REQ_SAVE_SRT = 1002;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private EditText apiKeyEt;
    private EditText customVoiceEt;
    private EditText textEt;
    private EditText replaceEt;
    private EditText advancedEt;
    private Spinner voiceSpinner;
    private Spinner languageSpinner;
    private Spinner codecSpinner;
    private Spinner sampleRateSpinner;
    private Spinner bitRateSpinner;
    private Spinner latencySpinner;
    private CheckBox normalizeCb;
    private CheckBox timestampsCb;
    private TextView speedLabel;
    private TextView srtLabel;
    private TextView statusTv;
    private TextView debugTv;
    private SeekBar speedSeek;
    private SeekBar srtSeek;
    private Button generateBtn;
    private Button playBtn;
    private Button saveAudioBtn;
    private Button saveSrtBtn;

    private final List<Voice> voices = new ArrayList<>();
    private byte[] lastAudio;
    private String lastSrt = "";
    private String lastCodec = "mp3";
    private MediaPlayer mediaPlayer;
    private File previewFile;

    private final String[] languages = {
            "auto — 自动识别", "zh — 简体中文", "en — English",
            "ar-EG — Arabic (Egypt)", "ar-SA — Arabic (Saudi Arabia)", "ar-AE — Arabic (UAE)",
            "bn — Bengali", "fr — Français", "de — Deutsch", "hi — Hindi", "id — Indonesian",
            "it — Italiano", "ja — 日本語", "ko — 한국어", "pt-BR — Português (Brasil)",
            "pt-PT — Português (Portugal)", "ru — Русский", "es-MX — Español (México)",
            "es-ES — Español (España)", "tr — Türkçe", "vi — Tiếng Việt"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        String saved = SecureKeyStore.load(this);
        if (!saved.isEmpty()) apiKeyEt.setText(saved);
        loadFallbackVoices();
        selectVoice("orion");
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("xAI TTS Lab");
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("全音色 · 参数调试 · Speech Tags · 时间戳 · SRT");
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(2), 0, dp(12));
        root.addView(subtitle);

        addSection(root, "1. API Key 与音色");
        apiKeyEt = edit("xAI API Key（不会写入 APK）", false);
        apiKeyEt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(apiKeyEt);

        LinearLayout keyButtons = row();
        Button saveKey = button("保存 Key");
        saveKey.setOnClickListener(v -> saveKey());
        Button clearKey = button("清除 Key");
        clearKey.setOnClickListener(v -> {
            SecureKeyStore.clear(this);
            apiKeyEt.setText("");
            toast("已清除本地 Key");
        });
        Button refresh = button("刷新全部音色");
        refresh.setOnClickListener(v -> refreshVoices());
        keyButtons.addView(saveKey);
        keyButtons.addView(clearKey);
        keyButtons.addView(refresh);
        root.addView(keyButtons);

        addLabel(root, "内置 / 已加载音色");
        voiceSpinner = spinner();
        root.addView(voiceSpinner);
        customVoiceEt = edit("可选：手动填写自定义 voice_id（填写后优先使用）", true);
        root.addView(customVoiceEt);

        addLabel(root, "语言");
        languageSpinner = spinner();
        languageSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages));
        languageSpinner.setSelection(1); // zh
        root.addView(languageSpinner);

        addSection(root, "2. 文本与 Speech Tags");
        textEt = edit("输入要朗读的正文（官方单次上限 15,000 字符）", true);
        textEt.setMinLines(9);
        textEt.setGravity(Gravity.TOP | Gravity.START);
        root.addView(textEt, matchWrap());

        addLabel(root, "插入式标签");
        root.addView(tagScroller(new String[]{
                "[pause]", "[long-pause]", "[hum-tune]", "[laugh]", "[chuckle]", "[giggle]",
                "[cry]", "[tsk]", "[tongue-click]", "[lip-smack]", "[breath]", "[inhale]", "[exhale]", "[sigh]"
        }, false));

        addLabel(root, "包裹式标签（选中文本后点按）");
        root.addView(tagScroller(new String[]{
                "soft", "whisper", "loud", "build-intensity", "decrease-intensity",
                "higher-pitch", "lower-pitch", "slow", "fast", "sing-song", "singing", "emphasis"
        }, true));

        addSection(root, "3. 合成参数");
        speedLabel = addLabel(root, "语速：1.00×");
        speedSeek = new SeekBar(this);
        speedSeek.setMax(80); // 0.70 - 1.50
        speedSeek.setProgress(30); // 1.00
        speedSeek.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speedLabel.setText(String.format(Locale.US, "语速：%.2f×", 0.70 + progress / 100.0));
            }
        });
        root.addView(speedSeek);

        addLabel(root, "输出编码");
        codecSpinner = spinnerWith(new String[]{"mp3", "wav", "pcm", "mulaw", "alaw"});
        root.addView(codecSpinner);

        addLabel(root, "采样率");
        sampleRateSpinner = spinnerWith(new String[]{"8000", "16000", "22050", "24000", "44100", "48000"});
        sampleRateSpinner.setSelection(3);
        root.addView(sampleRateSpinner);

        addLabel(root, "MP3 码率（非 MP3 时忽略）");
        bitRateSpinner = spinnerWith(new String[]{"32000", "64000", "96000", "128000", "192000"});
        bitRateSpinner.setSelection(3);
        root.addView(bitRateSpinner);

        addLabel(root, "延迟优化级别");
        latencySpinner = spinnerWith(new String[]{"0 — 质量优先", "1 — 更低首包延迟", "2 — 最低首包延迟"});
        root.addView(latencySpinner);

        normalizeCb = new CheckBox(this);
        normalizeCb.setText("text_normalization：数字、缩写、符号转为更自然的口语形式");
        root.addView(normalizeCb);

        timestampsCb = new CheckBox(this);
        timestampsCb.setText("with_timestamps：返回字符级时间戳，并可导出 SRT");
        timestampsCb.setChecked(true);
        root.addView(timestampsCb);

        srtLabel = addLabel(root, "SRT 每条目标字数：50（单行，不强制内部换行）");
        srtSeek = new SeekBar(this);
        srtSeek.setMax(100);
        srtSeek.setProgress(30); // 20 + 30 = 50
        srtSeek.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int target = 20 + progress;
                srtLabel.setText("SRT 每条目标字数：" + target + "（单行，不强制内部换行）");
            }
        });
        root.addView(srtSeek);

        addSection(root, "4. 发音替换");
        TextView replaceHelp = new TextView(this);
        replaceHelp.setText("支持 JSON 对象，或每行“原文=读音”。可把值写成 IPA。示例：\n须陀洹=须陀环\n{\"nginx\":\"/ˈɛndʒɪn ˈɛks/\"}");
        replaceHelp.setTextSize(13);
        root.addView(replaceHelp);
        replaceEt = edit("发音替换，可留空", true);
        replaceEt.setMinLines(4);
        root.addView(replaceEt);

        addSection(root, "5. 高级调试");
        TextView advancedHelp = new TextView(this);
        advancedHelp.setText("可选 JSON 对象，会覆盖上面生成的同名请求字段。用于测试 xAI 后续新增参数；API Key 不会放进这里。");
        advancedHelp.setTextSize(13);
        root.addView(advancedHelp);
        advancedEt = edit("例如：{\"speed\":0.9}", true);
        advancedEt.setMinLines(3);
        root.addView(advancedEt);

        Button previewJson = button("查看本次请求 JSON");
        previewJson.setOnClickListener(v -> {
            try {
                debugTv.setText(buildRequestJson().toString(2));
            } catch (Exception e) {
                debugTv.setText("请求 JSON 错误：" + e.getMessage());
            }
        });
        root.addView(previewJson);

        addSection(root, "6. 生成、试听与导出");
        LinearLayout action1 = row();
        generateBtn = button("生成音频");
        generateBtn.setOnClickListener(v -> generate());
        playBtn = button("试听");
        playBtn.setEnabled(false);
        playBtn.setOnClickListener(v -> playLast());
        Button stopBtn = button("停止");
        stopBtn.setOnClickListener(v -> stopPlayback());
        action1.addView(generateBtn);
        action1.addView(playBtn);
        action1.addView(stopBtn);
        root.addView(action1);

        LinearLayout action2 = row();
        saveAudioBtn = button("保存音频");
        saveAudioBtn.setEnabled(false);
        saveAudioBtn.setOnClickListener(v -> chooseSaveAudio());
        saveSrtBtn = button("导出 SRT");
        saveSrtBtn.setEnabled(false);
        saveSrtBtn.setOnClickListener(v -> chooseSaveSrt());
        action2.addView(saveAudioBtn);
        action2.addView(saveSrtBtn);
        root.addView(action2);

        statusTv = new TextView(this);
        statusTv.setText("状态：等待生成");
        statusTv.setTextSize(14);
        statusTv.setPadding(0, dp(8), 0, dp(8));
        root.addView(statusTv);

        debugTv = new TextView(this);
        debugTv.setText("调试日志会显示在这里。\n提示：MP3/WAV 可直接试听；PCM/μ-law/A-law 以保存导出为主。");
        debugTv.setTextSize(12);
        debugTv.setTypeface(Typeface.MONOSPACE);
        debugTv.setTextIsSelectable(true);
        debugTv.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(debugTv);

        TextView foot = new TextView(this);
        foot.setText("本应用直接请求 api.x.ai。费用、配额、地区与自定义音色权限由你的 xAI 账号决定。");
        foot.setTextSize(12);
        foot.setPadding(0, dp(16), 0, 0);
        root.addView(foot);

        setContentView(scroll);
    }

    private void saveKey() {
        String key = apiKeyEt.getText().toString().trim();
        if (key.isEmpty()) {
            toast("请先填写 API Key");
            return;
        }
        try {
            SecureKeyStore.save(this, key);
            toast("Key 已使用 Android Keystore 加密保存");
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage());
        }
    }

    private void refreshVoices() {
        final String key = apiKeyEt.getText().toString().trim();
        if (key.isEmpty()) {
            toast("请先填写 API Key");
            return;
        }
        statusTv.setText("状态：正在刷新音色…");
        executor.execute(() -> {
            try {
                List<Voice> loaded = getVoices(key, "/v1/tts/voices");
                int builtInCount = loaded.size();
                String customNote = "";
                try {
                    List<Voice> custom = getVoices(key, "/v1/custom-voices");
                    for (Voice v : custom) {
                        boolean exists = false;
                        for (Voice b : loaded) if (b.id.equals(v.id)) { exists = true; break; }
                        if (!exists) loaded.add(new Voice(v.id, v.name + " [自定义]", v.language));
                    }
                    customNote = "，自定义 " + custom.size();
                } catch (Exception e) {
                    customNote = "；自定义音色未加载（可能受账号/地区权限限制）";
                }
                final String note = "内置 " + builtInCount + customNote;
                main.post(() -> {
                    String keep = selectedVoiceId();
                    voices.clear();
                    voices.addAll(loaded);
                    updateVoiceSpinner();
                    selectVoice(keep.isEmpty() ? "orion" : keep);
                    statusTv.setText("状态：音色刷新完成，" + note);
                });
            } catch (Exception e) {
                main.post(() -> {
                    statusTv.setText("状态：刷新音色失败");
                    debugTv.setText("刷新音色失败：\n" + e.getMessage());
                });
            }
        });
    }

    private List<Voice> getVoices(String key, String path) throws Exception {
        HttpURLConnection conn = open(path, "GET", key);
        int code = conn.getResponseCode();
        byte[] data = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        String body = new String(data, StandardCharsets.UTF_8);
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + body);
        JSONObject json = new JSONObject(body);
        JSONArray arr = json.optJSONArray("voices");
        if (arr == null) arr = json.optJSONArray("custom_voices");
        if (arr == null && json.has("data")) arr = json.optJSONArray("data");
        List<Voice> out = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject v = arr.optJSONObject(i);
                if (v == null) continue;
                String id = v.optString("voice_id", v.optString("id", "")).trim();
                if (id.isEmpty()) continue;
                String name = v.optString("name", id);
                String language = v.optString("language", "");
                out.add(new Voice(id, name, language));
            }
        }
        return out;
    }

    private void generate() {
        final String key = apiKeyEt.getText().toString().trim();
        final String text = textEt.getText().toString();
        if (key.isEmpty()) { toast("请填写 API Key"); return; }
        if (text.trim().isEmpty()) { toast("请输入要朗读的文本"); return; }
        if (text.length() > 15000) { toast("文本超过 xAI 当前单次 15,000 字符上限"); return; }

        final int srtTargetChars = 20 + srtSeek.getProgress();
        final JSONObject body;
        try {
            body = buildRequestJson();
        } catch (Exception e) {
            debugTv.setText("请求参数错误：\n" + e.getMessage());
            return;
        }

        generateBtn.setEnabled(false);
        playBtn.setEnabled(false);
        saveAudioBtn.setEnabled(false);
        saveSrtBtn.setEnabled(false);
        statusTv.setText("状态：正在请求 xAI TTS…");
        debugTv.setText("POST /v1/tts\n\n" + body.toString());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = open("/v1/tts", "POST", key);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) { os.write(payload); }

                int code = conn.getResponseCode();
                String contentType = conn.getContentType();
                byte[] response = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
                if (code < 200 || code >= 300) {
                    throw new Exception("HTTP " + code + "\n" + new String(response, StandardCharsets.UTF_8));
                }

                boolean timestamps = body.optBoolean("with_timestamps", false);
                byte[] audio;
                String srt = "";
                double duration = -1;
                String debugExtra;

                if (timestamps || (contentType != null && contentType.toLowerCase(Locale.US).contains("application/json"))) {
                    JSONObject json = new JSONObject(new String(response, StandardCharsets.UTF_8));
                    String b64 = json.optString("audio", "");
                    if (b64.isEmpty()) throw new Exception("JSON 响应里没有 audio 字段");
                    audio = Base64.decode(b64, Base64.DEFAULT);
                    duration = json.optDouble("duration", -1);
                    JSONObject ts = json.optJSONObject("audio_timestamps");
                    if (ts != null) {
                        JSONArray chars = ts.optJSONArray("graph_chars");
                        JSONArray times = ts.optJSONArray("graph_times");
                        if (chars != null && times != null) {
                            srt = SrtBuilder.fromCharacterTimestamps(chars, times, srtTargetChars);
                        }
                    }
                    debugExtra = "响应：JSON + base64 audio + timestamps";
                } else {
                    audio = response;
                    debugExtra = "响应：原始音频字节";
                }

                final byte[] finalAudio = audio;
                final String finalSrt = srt;
                final double finalDuration = duration;
                final String finalDebugExtra = debugExtra;
                final int finalCode = code;
                final String finalContentType = contentType == null ? "" : contentType;
                final String codec = body.optJSONObject("output_format") != null
                        ? body.optJSONObject("output_format").optString("codec", "mp3") : "mp3";

                main.post(() -> {
                    lastAudio = finalAudio;
                    lastSrt = finalSrt;
                    lastCodec = codec;
                    generateBtn.setEnabled(true);
                    saveAudioBtn.setEnabled(finalAudio.length > 0);
                    saveSrtBtn.setEnabled(!finalSrt.isEmpty());
                    playBtn.setEnabled(("mp3".equals(codec) || "wav".equals(codec)) && finalAudio.length > 0);
                    String d = finalDuration >= 0 ? String.format(Locale.US, "，时长 %.2f 秒", finalDuration) : "";
                    statusTv.setText("状态：生成成功，" + finalAudio.length + " bytes" + d +
                            (!finalSrt.isEmpty() ? "，SRT 已生成" : ""));
                    debugTv.setText("HTTP " + finalCode + "\nContent-Type: " + finalContentType +
                            "\n" + finalDebugExtra + "\n音频字节：" + finalAudio.length +
                            "\nSRT 字符：" + finalSrt.length());
                });
            } catch (Exception e) {
                final String message = e.getMessage() == null ? e.toString() : e.getMessage();
                main.post(() -> {
                    generateBtn.setEnabled(true);
                    statusTv.setText("状态：生成失败");
                    debugTv.setText("生成失败：\n" + message);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private JSONObject buildRequestJson() throws Exception {
        JSONObject body = new JSONObject();
        body.put("text", textEt.getText().toString());

        String custom = customVoiceEt.getText().toString().trim();
        String voice = custom.isEmpty() ? selectedVoiceId() : custom;
        if (voice.isEmpty()) voice = "orion";
        body.put("voice_id", voice);
        body.put("language", selectedLanguageCode());

        JSONObject format = new JSONObject();
        String codec = codecSpinner.getSelectedItem().toString();
        format.put("codec", codec);
        format.put("sample_rate", Integer.parseInt(sampleRateSpinner.getSelectedItem().toString()));
        if ("mp3".equals(codec)) {
            format.put("bit_rate", Integer.parseInt(bitRateSpinner.getSelectedItem().toString()));
        }
        body.put("output_format", format);
        body.put("speed", 0.70 + speedSeek.getProgress() / 100.0);
        body.put("optimize_streaming_latency", latencySpinner.getSelectedItemPosition());
        body.put("text_normalization", normalizeCb.isChecked());
        body.put("with_timestamps", timestampsCb.isChecked());

        JSONObject replace = parseReplacementMap(replaceEt.getText().toString().trim());
        if (replace.length() > 0) body.put("replace", replace);

        String advanced = advancedEt.getText().toString().trim();
        if (!advanced.isEmpty()) {
            JSONObject overrides = new JSONObject(advanced);
            Iterator<String> keys = overrides.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                body.put(k, overrides.get(k));
            }
        }
        return body;
    }

    private JSONObject parseReplacementMap(String raw) throws Exception {
        JSONObject out = new JSONObject();
        if (raw == null || raw.trim().isEmpty()) return out;
        String t = raw.trim();
        if (t.startsWith("{")) return new JSONObject(t);
        String[] lines = t.split("\\r?\\n");
        for (String line : lines) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            int idx = s.indexOf('=');
            if (idx <= 0) throw new Exception("发音替换格式错误：" + s);
            String key = s.substring(0, idx).trim();
            String value = s.substring(idx + 1).trim();
            if (key.isEmpty() || value.isEmpty()) throw new Exception("发音替换不能为空：" + s);
            out.put(key, value);
        }
        return out;
    }

    private void playLast() {
        if (lastAudio == null || lastAudio.length == 0) return;
        if (!("mp3".equals(lastCodec) || "wav".equals(lastCodec))) {
            toast("当前编码不适合 Android MediaPlayer 直接试听，请保存后用专业工具打开");
            return;
        }
        stopPlayback();
        try {
            previewFile = new File(getCacheDir(), "preview." + lastCodec);
            try (FileOutputStream fos = new FileOutputStream(previewFile)) { fos.write(lastAudio); }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(previewFile.getAbsolutePath());
            mediaPlayer.setOnCompletionListener(mp -> statusTv.setText("状态：试听完成"));
            mediaPlayer.prepare();
            mediaPlayer.start();
            statusTv.setText("状态：正在试听…");
        } catch (Exception e) {
            debugTv.setText("试听失败：\n" + e.getMessage());
        }
    }

    private void stopPlayback() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (Exception ignored) {}
        mediaPlayer = null;
    }

    private void chooseSaveAudio() {
        if (lastAudio == null || lastAudio.length == 0) return;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mimeForCodec(lastCodec));
        i.putExtra(Intent.EXTRA_TITLE, "xai_tts_" + stamp() + "." + extensionForCodec(lastCodec));
        startActivityForResult(i, REQ_SAVE_AUDIO);
    }

    private void chooseSaveSrt() {
        if (lastSrt == null || lastSrt.isEmpty()) return;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/x-subrip");
        i.putExtra(Intent.EXTRA_TITLE, "xai_tts_" + stamp() + ".srt");
        startActivityForResult(i, REQ_SAVE_SRT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try (OutputStream os = getContentResolver().openOutputStream(uri, "w")) {
            if (os == null) throw new Exception("无法打开输出文件");
            if (requestCode == REQ_SAVE_AUDIO) {
                os.write(lastAudio);
                toast("音频已保存");
            } else if (requestCode == REQ_SAVE_SRT) {
                os.write(lastSrt.getBytes(StandardCharsets.UTF_8));
                toast("SRT 已保存");
            }
        } catch (Exception e) {
            debugTv.setText("保存失败：\n" + e.getMessage());
        }
    }

    private HttpURLConnection open(String path, String method, String key) throws Exception {
        URL url = new URL(API_BASE + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(180_000);
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setRequestProperty("Accept", "*/*");
        return conn;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        if (in == null) return new byte[0];
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = input.read(buf)) >= 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private void loadFallbackVoices() {
        voices.clear();
        voices.add(new Voice("carina", "Carina — Soft / empathetic / soothing", "multilingual"));
        voices.add(new Voice("zagan", "Zagan — Powerful / dramatic", "multilingual"));
        voices.add(new Voice("helix", "Helix — Bold / dynamic", "multilingual"));
        voices.add(new Voice("orion", "Orion — Rich / cinematic / resonant", "multilingual"));
        voices.add(new Voice("luna", "Luna — Gentle / patient / nurturing", "multilingual"));
        voices.add(new Voice("iris", "Iris — Friendly / upbeat", "multilingual"));
        voices.add(new Voice("altair", "Altair — Elegant / refined", "multilingual"));
        voices.add(new Voice("zenith", "Zenith — Sharp / focused", "multilingual"));
        voices.add(new Voice("perseus", "Perseus — Strong / confident", "multilingual"));
        voices.add(new Voice("helios", "Helios — Upbeat / energetic", "multilingual"));
        voices.add(new Voice("lux", "Lux — Grounded / calm / wise", "multilingual"));
        voices.add(new Voice("kepler", "Kepler — Inventive / charismatic", "multilingual"));
        voices.add(new Voice("rigel", "Rigel — Precise / professional", "multilingual"));
        voices.add(new Voice("cosmo", "Cosmo — Bright / curious", "multilingual"));
        voices.add(new Voice("celeste", "Celeste — Compassionate / reassuring", "multilingual"));
        voices.add(new Voice("ursa", "Ursa — Friendly / warm", "multilingual"));
        voices.add(new Voice("sirius", "Sirius — Clever / playful", "multilingual"));
        voices.add(new Voice("lumen", "Lumen — Warm / articulate", "multilingual"));
        voices.add(new Voice("castor", "Castor — Down-to-earth / easygoing", "multilingual"));
        voices.add(new Voice("naksh", "Naksh — Warm / thoughtful / wise", "multilingual"));
        voices.add(new Voice("atlas", "Atlas — Commanding / reassuring", "multilingual"));
        voices.add(new Voice("aurora", "Aurora — Serene / steady", "multilingual"));
        voices.add(new Voice("liora", "Liora — Calm / grounded", "multilingual"));
        voices.add(new Voice("ara", "Ara — Warm / friendly", "multilingual"));
        voices.add(new Voice("eve", "Eve — Energetic / upbeat", "multilingual"));
        voices.add(new Voice("leo", "Leo — Authoritative / strong", "multilingual"));
        voices.add(new Voice("rex", "Rex — Confident / clear", "multilingual"));
        voices.add(new Voice("sal", "Sal — Smooth / balanced", "multilingual"));
        updateVoiceSpinner();
    }

    private void updateVoiceSpinner() {
        List<String> labels = new ArrayList<>();
        for (Voice v : voices) {
            String suffix = v.language == null || v.language.isEmpty() ? "" : " · " + v.language;
            labels.add(v.name + "  [" + v.id + "]" + suffix);
        }
        voiceSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private String selectedVoiceId() {
        int pos = voiceSpinner.getSelectedItemPosition();
        return pos >= 0 && pos < voices.size() ? voices.get(pos).id : "";
    }

    private void selectVoice(String id) {
        if (id == null) return;
        for (int i = 0; i < voices.size(); i++) {
            if (id.equalsIgnoreCase(voices.get(i).id)) {
                voiceSpinner.setSelection(i);
                return;
            }
        }
    }

    private String selectedLanguageCode() {
        String s = languageSpinner.getSelectedItem().toString();
        int idx = s.indexOf(' ');
        return idx > 0 ? s.substring(0, idx) : s;
    }

    private HorizontalScrollView tagScroller(String[] tags, boolean wrapping) {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (String tag : tags) {
            Button b = button(wrapping ? "<" + tag + ">" : tag);
            b.setTextSize(12);
            b.setOnClickListener(v -> {
                if (wrapping) wrapSelection(tag);
                else insertAtCursor(tag);
            });
            row.addView(b);
        }
        hsv.addView(row);
        return hsv;
    }

    private void insertAtCursor(String text) {
        int start = Math.max(0, textEt.getSelectionStart());
        textEt.getText().insert(start, text);
        textEt.requestFocus();
    }

    private void wrapSelection(String tag) {
        int a = textEt.getSelectionStart();
        int b = textEt.getSelectionEnd();
        if (a < 0) a = 0;
        if (b < 0) b = a;
        int start = Math.min(a, b);
        int end = Math.max(a, b);
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        if (start == end) {
            textEt.getText().insert(start, open + close);
            textEt.setSelection(start + open.length());
        } else {
            textEt.getText().insert(end, close);
            textEt.getText().insert(start, open);
            textEt.setSelection(start + open.length(), end + open.length());
        }
        textEt.requestFocus();
    }

    private TextView addSection(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(19);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(18), 0, dp(7));
        root.addView(tv);
        return tv;
    }

    private TextView addLabel(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setPadding(0, dp(8), 0, dp(3));
        root.addView(tv);
        return tv;
    }

    private EditText edit(String hint, boolean multiline) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextSize(15);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        if (multiline) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            et.setGravity(Gravity.TOP | Gravity.START);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            et.setSingleLine(true);
        }
        return et;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setMinHeight(dp(44));
        return b;
    }

    private Spinner spinner() { return new Spinner(this); }

    private Spinner spinnerWith(String[] items) {
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        return s;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private static String stamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    private static String extensionForCodec(String codec) {
        if ("mulaw".equals(codec)) return "ulaw";
        return codec;
    }

    private static String mimeForCodec(String codec) {
        switch (codec) {
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "mulaw": return "audio/basic";
            case "alaw": return "audio/alaw";
            default: return "application/octet-stream";
        }
    }

    @Override
    protected void onDestroy() {
        stopPlayback();
        executor.shutdownNow();
        super.onDestroy();
    }

    private static final class Voice {
        final String id;
        final String name;
        final String language;
        Voice(String id, String name, String language) {
            this.id = id;
            this.name = name;
            this.language = language;
        }
    }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
