package com.example.edgettsmp3;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final EdgeTtsClient ttsClient = new EdgeTtsClient();

    private EditText textInput;
    private Spinner voiceSpinner;
    private SeekBar rateSeek;
    private SeekBar pitchSeek;
    private SeekBar volumeSeek;
    private TextView rateLabel;
    private TextView pitchLabel;
    private TextView volumeLabel;
    private EditText fileNameInput;
    private Button generateButton;
    private Button playButton;
    private Button shareButton;
    private TextView statusView;
    private Uri lastAudioUri;
    private MediaPlayer mediaPlayer;

    private static final VoiceOption[] VOICES = new VoiceOption[] {
            new VoiceOption("晓晓 · 女声 / 自然", "zh-CN-XiaoxiaoNeural"),
            new VoiceOption("晓艺 · 女声 / 明快", "zh-CN-XiaoyiNeural"),
            new VoiceOption("云希 · 男声 / 年轻", "zh-CN-YunxiNeural"),
            new VoiceOption("云扬 · 男声 / 稳重", "zh-CN-YunyangNeural"),
            new VoiceOption("云健 · 男声 / 播报", "zh-CN-YunjianNeural"),
            new VoiceOption("小北 · 东北女声", "zh-CN-liaoning-XiaobeiNeural"),
            new VoiceOption("小妮 · 陕西女声", "zh-CN-shaanxi-XiaoniNeural")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Edge TTS MP3");
        setContentView(buildUi());
        bindControls();
        updateProsodyLabels();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Edge TTS → MP3");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title, fullWidth());

        TextView subtitle = new TextView(this);
        subtitle.setText("输入文字，选择声音和语速，直接生成 MP3 到手机 Downloads/EdgeTTS。需要联网，无需 API Key。");
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(6), 0, dp(16));
        root.addView(subtitle, fullWidth());

        addSectionLabel(root, "文本");
        textInput = new EditText(this);
        textInput.setHint("粘贴要朗读的文本……");
        textInput.setGravity(Gravity.TOP | Gravity.START);
        textInput.setMinLines(8);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(textInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(190)));

        addSectionLabel(root, "声音");
        voiceSpinner = new Spinner(this);
        ArrayAdapter<VoiceOption> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, VOICES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceSpinner.setAdapter(adapter);
        root.addView(voiceSpinner, fullWidth());

        rateLabel = addSectionLabel(root, "语速");
        rateSeek = new SeekBar(this);
        rateSeek.setMax(60);       // -30% ... +30%
        rateSeek.setProgress(26);  // default -4%
        root.addView(rateSeek, fullWidth());

        pitchLabel = addSectionLabel(root, "音调");
        pitchSeek = new SeekBar(this);
        pitchSeek.setMax(40);      // -20Hz ... +20Hz
        pitchSeek.setProgress(20);
        root.addView(pitchSeek, fullWidth());

        volumeLabel = addSectionLabel(root, "音量");
        volumeSeek = new SeekBar(this);
        volumeSeek.setMax(100);    // -50% ... +50%
        volumeSeek.setProgress(50);
        root.addView(volumeSeek, fullWidth());

        TextView tip = new TextView(this);
        tip.setText("自然口播建议：晓晓 / 云希，语速约 -4% ～ -8%，并保留自然标点和段落。");
        tip.setTextSize(13);
        tip.setPadding(0, dp(4), 0, dp(12));
        root.addView(tip, fullWidth());

        addSectionLabel(root, "文件名（可选）");
        fileNameInput = new EditText(this);
        fileNameInput.setHint("例如：旁白.mp3（留空自动命名）");
        fileNameInput.setSingleLine(true);
        root.addView(fileNameInput, fullWidth());

        generateButton = new Button(this);
        generateButton.setText("生成 MP3");
        LinearLayout.LayoutParams generateParams = fullWidth();
        generateParams.topMargin = dp(16);
        root.addView(generateButton, generateParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);
        playButton = new Button(this);
        playButton.setText("播放");
        playButton.setEnabled(false);
        shareButton = new Button(this);
        shareButton.setText("分享");
        shareButton.setEnabled(false);
        actions.addView(playButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(shareButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(actions, fullWidth());

        statusView = new TextView(this);
        statusView.setText("就绪");
        statusView.setTextSize(14);
        statusView.setPadding(0, dp(14), 0, 0);
        root.addView(statusView, fullWidth());

        TextView notice = new TextView(this);
        notice.setText("说明：这是基于 edge-tts 所使用的 Microsoft Edge Read Aloud 在线语音协议实现，不是微软官方 Azure Speech API。服务协议若发生变化，可能需要更新应用。生成音频请遵守适用的服务条款与当地法律。");
        notice.setTextSize(12);
        notice.setPadding(0, dp(18), 0, 0);
        root.addView(notice, fullWidth());

        return scroll;
    }

    private void bindControls() {
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateProsodyLabels();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        rateSeek.setOnSeekBarChangeListener(listener);
        pitchSeek.setOnSeekBarChangeListener(listener);
        volumeSeek.setOnSeekBarChangeListener(listener);

        generateButton.setOnClickListener(v -> startSynthesis());
        playButton.setOnClickListener(v -> playLastAudio());
        shareButton.setOnClickListener(v -> shareLastAudio());
    }

    private void startSynthesis() {
        String text = textInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "请先输入文本", Toast.LENGTH_SHORT).show();
            return;
        }

        VoiceOption selected = (VoiceOption) voiceSpinner.getSelectedItem();
        int rate = rateSeek.getProgress() - 30;
        int pitch = pitchSeek.getProgress() - 20;
        int volume = volumeSeek.getProgress() - 50;
        String rateValue = signed(rate) + "%";
        String pitchValue = signed(pitch) + "Hz";
        String volumeValue = signed(volume) + "%";
        String fileName = normalizeFileName(fileNameInput.getText().toString());

        generateButton.setEnabled(false);
        playButton.setEnabled(false);
        shareButton.setEnabled(false);
        statusView.setText("正在连接 Edge TTS……");

        executor.submit(() -> {
            File temp = null;
            try {
                temp = File.createTempFile("edge_tts_", ".mp3", getCacheDir());
                try (FileOutputStream output = new FileOutputStream(temp)) {
                    ttsClient.synthesize(
                            text,
                            selected.voice,
                            rateValue,
                            volumeValue,
                            pitchValue,
                            output,
                            (done, total) -> runOnUiThread(() ->
                                    statusView.setText("正在生成：" + done + "/" + total + " 段")));
                }
                if (temp.length() == 0) {
                    throw new IOException("生成的 MP3 文件为空");
                }
                Uri saved = saveToDownloads(temp, fileName);
                long bytes = temp.length();
                runOnUiThread(() -> {
                    lastAudioUri = saved;
                    generateButton.setEnabled(true);
                    playButton.setEnabled(true);
                    shareButton.setEnabled(true);
                    statusView.setText("已保存：Downloads/EdgeTTS/" + fileName
                            + "（" + humanBytes(bytes) + "）");
                    Toast.makeText(this, "MP3 已生成", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                String message = friendlyError(e);
                runOnUiThread(() -> {
                    generateButton.setEnabled(true);
                    playButton.setEnabled(lastAudioUri != null);
                    shareButton.setEnabled(lastAudioUri != null);
                    statusView.setText("生成失败：" + message);
                    Toast.makeText(this, "生成失败", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (temp != null) {
                    //noinspection ResultOfMethodCallIgnored
                    temp.delete();
                }
            }
        });
    }

    private Uri saveToDownloads(File source, String fileName) throws IOException {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EdgeTTS");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("无法在 Downloads 中创建文件");
        }

        try {
            try (InputStream input = new FileInputStream(source);
                 OutputStream output = resolver.openOutputStream(uri, "w")) {
                if (output == null) {
                    throw new IOException("无法打开输出文件");
                }
                byte[] buffer = new byte[32 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
            return uri;
        } catch (IOException e) {
            resolver.delete(uri, null, null);
            throw e;
        }
    }

    private void playLastAudio() {
        if (lastAudioUri == null) return;
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, lastAudioUri);
            if (mediaPlayer == null) {
                throw new IOException("播放器无法打开 MP3");
            }
            mediaPlayer.setOnCompletionListener(mp -> statusView.setText("播放完成"));
            mediaPlayer.start();
            statusView.setText("正在播放……");
        } catch (Exception e) {
            Toast.makeText(this, "播放失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareLastAudio() {
        if (lastAudioUri == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("audio/mpeg");
        intent.putExtra(Intent.EXTRA_STREAM, lastAudioUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "分享 MP3"));
    }

    private void updateProsodyLabels() {
        if (rateLabel != null && rateSeek != null) {
            rateLabel.setText("语速：" + signed(rateSeek.getProgress() - 30) + "%");
        }
        if (pitchLabel != null && pitchSeek != null) {
            pitchLabel.setText("音调：" + signed(pitchSeek.getProgress() - 20) + "Hz");
        }
        if (volumeLabel != null && volumeSeek != null) {
            volumeLabel.setText("音量：" + signed(volumeSeek.getProgress() - 50) + "%");
        }
    }

    private TextView addSectionLabel(LinearLayout root, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(15);
        label.setPadding(0, dp(12), 0, dp(4));
        root.addView(label, fullWidth());
        return label;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private static String normalizeFileName(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            value = "edge_tts_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        }
        value = value.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!value.toLowerCase(Locale.US).endsWith(".mp3")) {
            value += ".mp3";
        }
        return value;
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String friendlyError(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = current.getClass().getSimpleName();
        }
        return message;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private static final class VoiceOption {
        final String label;
        final String voice;

        VoiceOption(String label, String voice) {
            this.label = label;
            this.voice = voice;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
