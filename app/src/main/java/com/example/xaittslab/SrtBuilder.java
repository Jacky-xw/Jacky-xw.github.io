package com.example.xaittslab;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SrtBuilder {
    private static final Pattern SPEECH_TAG = Pattern.compile("\\[[^\\]]+\\]|</?[A-Za-z-]+>");
    private static final String STRONG = "。！？!?；;";
    private static final String WEAK = "，,、：:";

    private SrtBuilder() {}

    static String fromCharacterTimestamps(JSONArray chars, JSONArray times, int targetChars) throws Exception {
        int n = Math.min(chars.length(), times.length());
        if (n == 0) return "";

        StringBuilder raw = new StringBuilder();
        for (int i = 0; i < n; i++) raw.append(chars.optString(i, ""));
        boolean[] hidden = new boolean[raw.length()];
        Matcher matcher = SPEECH_TAG.matcher(raw);
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end() && i < hidden.length; i++) hidden[i] = true;
        }

        List<TimedChar> visible = new ArrayList<>();
        int rawIndex = 0;
        for (int i = 0; i < n; i++) {
            String s = chars.optString(i, "");
            JSONArray pair = times.optJSONArray(i);
            double start = pair != null ? pair.optDouble(0, 0) : 0;
            double end = pair != null ? pair.optDouble(1, start) : start;
            for (int j = 0; j < s.length(); j++) {
                boolean skip = rawIndex < hidden.length && hidden[rawIndex];
                char c = s.charAt(j);
                if (!skip) visible.add(new TimedChar(c, start, end));
                rawIndex++;
            }
        }

        int target = Math.max(20, Math.min(120, targetChars));
        int min = Math.max(10, (int) Math.round(target * 0.55));
        int max = Math.max(target + 8, (int) Math.round(target * 1.45));

        List<Cue> cues = new ArrayList<>();
        List<TimedChar> current = new ArrayList<>();
        int printable = 0;

        for (TimedChar tc : visible) {
            char c = tc.c;
            if (c == '\r') continue;
            if (c == '\n') c = ' ';
            current.add(new TimedChar(c, tc.start, tc.end));
            if (!Character.isWhitespace(c)) printable++;

            boolean strong = STRONG.indexOf(c) >= 0;
            boolean weak = WEAK.indexOf(c) >= 0;
            boolean naturalEnough = printable >= min && (strong || (weak && printable >= target));
            boolean tooLong = printable >= max;

            if (naturalEnough || tooLong) {
                addCue(cues, current);
                current = new ArrayList<>();
                printable = 0;
            }
        }
        addCue(cues, current);

        StringBuilder out = new StringBuilder();
        int index = 1;
        for (Cue cue : cues) {
            if (cue.text.isEmpty()) continue;
            out.append(index++).append('\n')
                    .append(format(cue.start)).append(" --> ").append(format(cue.end)).append('\n')
                    .append(cue.text).append("\n\n");
        }
        return out.toString();
    }

    private static void addCue(List<Cue> cues, List<TimedChar> chars) {
        if (chars == null || chars.isEmpty()) return;
        int first = 0;
        int last = chars.size() - 1;
        while (first <= last && Character.isWhitespace(chars.get(first).c)) first++;
        while (last >= first && Character.isWhitespace(chars.get(last).c)) last--;
        if (first > last) return;

        StringBuilder text = new StringBuilder();
        boolean lastWasSpace = false;
        for (int i = first; i <= last; i++) {
            char c = chars.get(i).c;
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace && text.length() > 0) text.append(' ');
                lastWasSpace = true;
            } else {
                text.append(c);
                lastWasSpace = false;
            }
        }
        String clean = text.toString().trim();
        if (clean.isEmpty()) return;
        double start = Math.max(0, chars.get(first).start);
        double end = Math.max(start + 0.08, chars.get(last).end);
        cues.add(new Cue(start, end, clean));
    }

    private static String format(double seconds) {
        long ms = Math.max(0, Math.round(seconds * 1000.0));
        long h = ms / 3_600_000;
        ms %= 3_600_000;
        long m = ms / 60_000;
        ms %= 60_000;
        long s = ms / 1000;
        long milli = ms % 1000;
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", h, m, s, milli);
    }

    private static final class TimedChar {
        final char c;
        final double start;
        final double end;
        TimedChar(char c, double start, double end) {
            this.c = c;
            this.start = start;
            this.end = end;
        }
    }

    private static final class Cue {
        final double start;
        final double end;
        final String text;
        Cue(double start, double end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }
}
