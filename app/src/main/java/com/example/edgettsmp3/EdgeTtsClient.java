package com.example.edgettsmp3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Native Android implementation of the Microsoft Edge Read Aloud protocol used by edge-tts.
 * No API key is required. An internet connection is required.
 */
public final class EdgeTtsClient {
    private static final String HOST = "speech.platform.bing.com";
    private static final String BASE_PATH = "/consumer/speech/synthesize/readaloud";
    private static final String TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4";
    private static final String ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold";
    private static final String UPSTREAM_CONSTANTS_URL =
            "https://raw.githubusercontent.com/rany2/edge-tts/refs/heads/master/src/edge_tts/constants.py";
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("CHROMIUM_FULL_VERSION\\s*=\\s*\\\"([0-9.]+)\\\"");
    private static final byte[] AUDIO_MARKER = "Path:audio\r\n".getBytes(StandardCharsets.ISO_8859_1);
    private static final int MAX_ESCAPED_TEXT_BYTES = 3900;
    private static final int CONNECT_TIMEOUT_MS = 12_000;
    private static final int READ_TIMEOUT_MS = 65_000;

    // Matches edge-tts 7.2.8 as of 2026-08. If Microsoft changes it, a 403 retry
    // attempts to refresh this value from the upstream edge-tts constants file.
    private volatile String chromiumFullVersion = "143.0.3650.75";
    private volatile long clockSkewMillis = 0L;
    private final SecureRandom random = new SecureRandom();

    public interface ProgressListener {
        void onChunkCompleted(int completed, int total);
    }

    public void synthesize(
            String text,
            String voice,
            String rate,
            String volume,
            String pitch,
            OutputStream destination,
            ProgressListener progressListener) throws IOException {

        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本不能为空");
        }
        validateProsody(rate, "%", "语速");
        validateProsody(volume, "%", "音量");
        validateProsody(pitch, "Hz", "音调");

        String cleaned = removeIncompatibleCharacters(text);
        List<String> chunks = splitText(cleaned, MAX_ESCAPED_TEXT_BYTES);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("文本为空或不包含可朗读字符");
        }

        for (int i = 0; i < chunks.size(); i++) {
            synthesizeChunkWithRetry(chunks.get(i), voice, rate, volume, pitch, destination);
            destination.flush();
            if (progressListener != null) {
                progressListener.onChunkCompleted(i + 1, chunks.size());
            }
        }
    }

    private void synthesizeChunkWithRetry(
            String text,
            String voice,
            String rate,
            String volume,
            String pitch,
            OutputStream destination) throws IOException {
        try {
            synthesizeChunk(text, voice, rate, volume, pitch, destination);
        } catch (RawWebSocket.HandshakeException e) {
            if (e.getStatusCode() != 403) {
                throw e;
            }

            String serverDate = e.getHeader("date");
            if (serverDate != null) {
                updateClockSkew(serverDate);
            }
            String latest = fetchLatestChromiumVersion();
            if (latest != null) {
                chromiumFullVersion = latest;
            }

            // edge-tts itself retries once after correcting 403 clock skew.
            synthesizeChunk(text, voice, rate, volume, pitch, destination);
        }
    }

    private void synthesizeChunk(
            String text,
            String voice,
            String rate,
            String volume,
            String pitch,
            OutputStream destination) throws IOException {

        String connectionId = uuidHex();
        String version = chromiumFullVersion;
        String path = BASE_PATH + "/edge/v1"
                + "?TrustedClientToken=" + TRUSTED_CLIENT_TOKEN
                + "&ConnectionId=" + connectionId
                + "&Sec-MS-GEC=" + generateSecMsGec()
                + "&Sec-MS-GEC-Version=1-" + version;

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Pragma", "no-cache");
        headers.put("Cache-Control", "no-cache");
        headers.put("Origin", ORIGIN);
        headers.put("User-Agent", buildUserAgent(version));
        headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Cookie", "muid=" + randomHex(16) + ";");

        boolean audioReceived = false;
        try (RawWebSocket ws = RawWebSocket.connect(
                HOST, 443, path, headers, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)) {

            String speechConfig =
                    "X-Timestamp:" + dateToString() + "\r\n"
                            + "Content-Type:application/json; charset=utf-8\r\n"
                            + "Path:speech.config\r\n\r\n"
                            + "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{"
                            + "\"sentenceBoundaryEnabled\":\"true\",\"wordBoundaryEnabled\":\"false\"},"
                            + "\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n";
            ws.sendText(speechConfig);

            String ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>"
                    + "<voice name='" + escapeXmlAttribute(voice) + "'>"
                    + "<prosody pitch='" + escapeXmlAttribute(pitch) + "' rate='" + escapeXmlAttribute(rate)
                    + "' volume='" + escapeXmlAttribute(volume) + "'>"
                    + escapeXml(text)
                    + "</prosody></voice></speak>";

            String ssmlMessage =
                    "X-RequestId:" + uuidHex() + "\r\n"
                            + "Content-Type:application/ssml+xml\r\n"
                            + "X-Timestamp:" + dateToString() + "Z\r\n"
                            + "Path:ssml\r\n\r\n"
                            + ssml;
            ws.sendText(ssmlMessage);

            while (true) {
                RawWebSocket.Message message = ws.readMessage();
                if (message.type == RawWebSocket.Message.TYPE_CLOSE) {
                    break;
                }
                if (message.type == RawWebSocket.Message.TYPE_TEXT) {
                    String response = new String(message.data, StandardCharsets.UTF_8);
                    if (response.contains("Path:turn.end")) {
                        break;
                    }
                } else if (message.type == RawWebSocket.Message.TYPE_BINARY) {
                    byte[] audio = extractAudioBytes(message.data);
                    if (audio.length > 0) {
                        destination.write(audio);
                        audioReceived = true;
                    }
                }
            }
        }

        if (!audioReceived) {
            throw new IOException("Edge TTS 未返回音频。请检查网络、Voice 名称或稍后重试。");
        }
    }

    static byte[] extractAudioBytes(byte[] payload) {
        // Current Edge binary frames start with a 2-byte big-endian header length.
        if (payload.length >= 4) {
            int headerLength = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
            int headerStart = 2;
            int audioStart = headerStart + headerLength;
            if (headerLength > 0 && audioStart <= payload.length) {
                String header = new String(
                        payload, headerStart, Math.min(headerLength, payload.length - headerStart),
                        StandardCharsets.ISO_8859_1);
                if (header.contains("Path:audio")) {
                    if (audioStart + 1 < payload.length
                            && payload[audioStart] == '\r' && payload[audioStart + 1] == '\n') {
                        audioStart += 2;
                    }
                    return Arrays.copyOfRange(payload, Math.min(audioStart, payload.length), payload.length);
                }
            }
        }

        // Fallback used by several edge-tts ports: find the audio marker directly.
        int marker = indexOf(payload, AUDIO_MARKER);
        if (marker >= 0) {
            int start = marker + AUDIO_MARKER.length;
            return Arrays.copyOfRange(payload, start, payload.length);
        }
        return new byte[0];
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    static List<String> splitText(String text, int maxEscapedBytes) {
        List<String> result = new ArrayList<>();
        int start = 0;
        final int length = text.length();

        while (start < length) {
            int cursor = start;
            int escapedBytes = 0;
            int lastNaturalBreak = -1;

            while (cursor < length) {
                int cp = text.codePointAt(cursor);
                int chars = Character.charCount(cp);
                String unit = new String(Character.toChars(cp));
                int unitBytes = escapeXml(unit).getBytes(StandardCharsets.UTF_8).length;
                if (escapedBytes + unitBytes > maxEscapedBytes) {
                    break;
                }
                escapedBytes += unitBytes;
                cursor += chars;
                if (isNaturalBreak(cp)) {
                    lastNaturalBreak = cursor;
                }
            }

            if (cursor >= length) {
                String chunk = text.substring(start).trim();
                if (!chunk.isEmpty()) result.add(chunk);
                break;
            }

            int end = cursor;
            if (lastNaturalBreak > start && lastNaturalBreak - start >= 64) {
                end = lastNaturalBreak;
            }
            if (end <= start) {
                int cp = text.codePointAt(start);
                end = start + Character.charCount(cp);
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) result.add(chunk);
            start = end;
        }
        return result;
    }

    private static boolean isNaturalBreak(int cp) {
        return Character.isWhitespace(cp)
                || cp == '。' || cp == '！' || cp == '？' || cp == '；'
                || cp == '，' || cp == '、' || cp == '.' || cp == '!'
                || cp == '?' || cp == ';' || cp == ',';
    }

    static String removeIncompatibleCharacters(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length();) {
            int cp = input.codePointAt(i);
            i += Character.charCount(cp);
            if ((cp >= 0 && cp <= 8) || (cp >= 11 && cp <= 12) || (cp >= 14 && cp <= 31)) {
                out.append(' ');
            } else {
                out.appendCodePoint(cp);
            }
        }
        return out.toString();
    }

    static String escapeXml(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length();) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            switch (cp) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&apos;"); break;
                default: out.appendCodePoint(cp);
            }
        }
        return out.toString();
    }

    private static String escapeXmlAttribute(String text) {
        return escapeXml(text == null ? "" : text);
    }

    private static void validateProsody(String value, String suffix, String name) {
        if (value == null || !value.matches("[+-]\\d+" + Pattern.quote(suffix))) {
            throw new IllegalArgumentException(name + "格式无效: " + value);
        }
    }

    private String generateSecMsGec() throws IOException {
        long nowMillis = System.currentTimeMillis() + clockSkewMillis;
        long unixSeconds = Math.floorDiv(nowMillis, 1000L);
        long rounded = unixSeconds - Math.floorMod(unixSeconds, 300L);
        long windowsSeconds = rounded + 11_644_473_600L;
        long ticks = windowsSeconds * 10_000_000L;
        String value = Long.toString(ticks) + TRUSTED_CLIENT_TOKEN;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(value.getBytes(StandardCharsets.US_ASCII));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    private void updateClockSkew(String rfc1123Date) {
        try {
            long serverMillis = ZonedDateTime.parse(
                    rfc1123Date, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
            clockSkewMillis = serverMillis - System.currentTimeMillis();
        } catch (RuntimeException ignored) {
            // Best-effort only; a second connection will surface the real error if needed.
        }
    }

    private String fetchLatestChromiumVersion() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(UPSTREAM_CONSTANTS_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.setRequestProperty("User-Agent", "EdgeTTS-MP3-Android/1.0");
            if (connection.getResponseCode() != 200) {
                return null;
            }
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] block = new byte[4096];
                int total = 0;
                int read;
                while ((read = input.read(block)) >= 0 && total < 64 * 1024) {
                    buffer.write(block, 0, read);
                    total += read;
                }
                String source = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
                Matcher matcher = VERSION_PATTERN.matcher(source);
                return matcher.find() ? matcher.group(1) : null;
            }
        } catch (IOException ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String buildUserAgent(String fullVersion) {
        String major = fullVersion;
        int dot = fullVersion.indexOf('.');
        if (dot > 0) major = fullVersion.substring(0, dot);
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                + " (KHTML, like Gecko) Chrome/" + major + ".0.0.0 Safari/537.36"
                + " Edg/" + major + ".0.0.0";
    }

    private String randomHex(int byteCount) {
        byte[] bytes = new byte[byteCount];
        random.nextBytes(bytes);
        return toHex(bytes);
    }

    private static String toHex(byte[] bytes) {
        char[] hex = "0123456789ABCDEF".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }

    private static String uuidHex() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String dateToString() {
        SimpleDateFormat format = new SimpleDateFormat(
                "EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new java.util.Date());
    }
}
