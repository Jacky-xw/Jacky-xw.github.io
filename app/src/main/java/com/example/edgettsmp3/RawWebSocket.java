package com.example.edgettsmp3;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Minimal RFC 6455 WebSocket client for Android.
 *
 * It intentionally uses a raw TLS socket so the app can send the same custom
 * WebSocket headers used by edge-tts. Browser/WebView WebSocket APIs don't
 * allow setting all of those headers.
 */
public final class RawWebSocket implements Closeable {
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MAX_HTTP_HEADER = 64 * 1024;
    private static final long MAX_MESSAGE_SIZE = 32L * 1024L * 1024L;

    private final SSLSocket socket;
    private final InputStream input;
    private final OutputStream output;
    private final SecureRandom random = new SecureRandom();
    private volatile boolean closed;

    private RawWebSocket(SSLSocket socket) throws IOException {
        this.socket = socket;
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
    }

    public static RawWebSocket connect(
            String host,
            int port,
            String pathAndQuery,
            Map<String, String> extraHeaders,
            int connectTimeoutMs,
            int readTimeoutMs) throws IOException {

        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
        try {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            SSLParameters sslParameters = socket.getSSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(sslParameters);
            socket.startHandshake();

            RawWebSocket ws = new RawWebSocket(socket);
            ws.performHandshake(host, port, pathAndQuery, extraHeaders);
            return ws;
        } catch (IOException | RuntimeException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    private void performHandshake(
            String host,
            int port,
            String pathAndQuery,
            Map<String, String> extraHeaders) throws IOException {

        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        String secKey = Base64.getEncoder().encodeToString(keyBytes);

        StringBuilder request = new StringBuilder();
        request.append("GET ").append(pathAndQuery).append(" HTTP/1.1\r\n");
        request.append("Host: ").append(host);
        if (port != 443) {
            request.append(':').append(port);
        }
        request.append("\r\n");
        request.append("Upgrade: websocket\r\n");
        request.append("Connection: Upgrade\r\n");
        request.append("Sec-WebSocket-Key: ").append(secKey).append("\r\n");
        request.append("Sec-WebSocket-Version: 13\r\n");
        for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
            if (isCoreHandshakeHeader(entry.getKey())) {
                continue;
            }
            request.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        request.append("\r\n");

        output.write(request.toString().getBytes(StandardCharsets.ISO_8859_1));
        output.flush();

        byte[] responseBytes = readHttpHeader(input);
        String response = new String(responseBytes, StandardCharsets.ISO_8859_1);
        String[] lines = response.split("\r\n");
        if (lines.length == 0) {
            throw new IOException("Empty WebSocket handshake response");
        }

        int status = parseStatus(lines[0]);
        Map<String, String> headers = parseHeaders(lines);
        if (status != 101) {
            throw new HandshakeException(status, lines[0], headers);
        }

        String accept = headers.get("sec-websocket-accept");
        String expected = websocketAccept(secKey);
        if (accept == null || !expected.equals(accept.trim())) {
            throw new IOException("Invalid Sec-WebSocket-Accept from server");
        }
    }

    private static boolean isCoreHandshakeHeader(String name) {
        String key = name.toLowerCase(Locale.US);
        return key.equals("host")
                || key.equals("upgrade")
                || key.equals("connection")
                || key.equals("sec-websocket-key")
                || key.equals("sec-websocket-version");
    }

    private static int parseStatus(String statusLine) throws IOException {
        String[] parts = statusLine.split(" ", 3);
        if (parts.length < 2) {
            throw new IOException("Malformed HTTP status line: " + statusLine);
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("Malformed HTTP status code: " + statusLine, e);
        }
    }

    private static Map<String, String> parseHeaders(String[] lines) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = lines[i].substring(0, colon).trim().toLowerCase(Locale.US);
            String value = lines[i].substring(colon + 1).trim();
            headers.put(key, value);
        }
        return headers;
    }

    private static byte[] readHttpHeader(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int state = 0;
        while (buffer.size() < MAX_HTTP_HEADER) {
            int b = input.read();
            if (b < 0) {
                throw new EOFException("Connection closed during WebSocket handshake");
            }
            buffer.write(b);
            if (state == 0 && b == '\r') state = 1;
            else if (state == 1 && b == '\n') state = 2;
            else if (state == 2 && b == '\r') state = 3;
            else if (state == 3 && b == '\n') return buffer.toByteArray();
            else state = (b == '\r') ? 1 : 0;
        }
        throw new IOException("HTTP handshake header is too large");
    }

    private static String websocketAccept(String key) throws IOException {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest((key + WS_GUID).getBytes(StandardCharsets.ISO_8859_1));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 unavailable", e);
        }
    }

    public void sendText(String text) throws IOException {
        sendFrame(0x1, text.getBytes(StandardCharsets.UTF_8));
    }

    private void sendPong(byte[] payload) throws IOException {
        sendFrame(0xA, payload);
    }

    private synchronized void sendFrame(int opcode, byte[] payload) throws IOException {
        if (closed) {
            throw new IOException("WebSocket is closed");
        }
        output.write(0x80 | (opcode & 0x0F));
        long length = payload.length;
        if (length <= 125) {
            output.write(0x80 | (int) length);
        } else if (length <= 0xFFFF) {
            output.write(0x80 | 126);
            output.write((int) ((length >>> 8) & 0xFF));
            output.write((int) (length & 0xFF));
        } else {
            output.write(0x80 | 127);
            for (int i = 7; i >= 0; i--) {
                output.write((int) ((length >>> (8 * i)) & 0xFF));
            }
        }

        byte[] mask = new byte[4];
        random.nextBytes(mask);
        output.write(mask);
        for (int i = 0; i < payload.length; i++) {
            output.write(payload[i] ^ mask[i & 3]);
        }
        output.flush();
    }

    public Message readMessage() throws IOException {
        ByteArrayOutputStream fragments = null;
        int messageOpcode = -1;

        while (true) {
            Frame frame = readFrame();
            if (frame.opcode == 0x8) {
                closed = true;
                return Message.close();
            }
            if (frame.opcode == 0x9) {
                sendPong(frame.payload);
                continue;
            }
            if (frame.opcode == 0xA) {
                continue;
            }

            if (frame.opcode == 0x1 || frame.opcode == 0x2) {
                if (messageOpcode != -1) {
                    throw new IOException("Received a new data frame before fragmented message completed");
                }
                messageOpcode = frame.opcode;
                if (frame.fin) {
                    return new Message(messageOpcode, frame.payload);
                }
                fragments = new ByteArrayOutputStream();
                fragments.write(frame.payload);
                continue;
            }

            if (frame.opcode == 0x0) {
                if (messageOpcode == -1 || fragments == null) {
                    throw new IOException("Unexpected WebSocket continuation frame");
                }
                fragments.write(frame.payload);
                if (fragments.size() > MAX_MESSAGE_SIZE) {
                    throw new IOException("WebSocket message is too large");
                }
                if (frame.fin) {
                    return new Message(messageOpcode, fragments.toByteArray());
                }
                continue;
            }

            throw new IOException("Unsupported WebSocket opcode: " + frame.opcode);
        }
    }

    private Frame readFrame() throws IOException {
        int b0 = input.read();
        int b1 = input.read();
        if (b0 < 0 || b1 < 0) {
            throw new EOFException("WebSocket connection closed");
        }

        boolean fin = (b0 & 0x80) != 0;
        int rsv = b0 & 0x70;
        if (rsv != 0) {
            throw new IOException("Compressed/extended WebSocket frame received unexpectedly");
        }
        int opcode = b0 & 0x0F;
        boolean masked = (b1 & 0x80) != 0;
        long length = b1 & 0x7F;
        if (length == 126) {
            length = ((long) readRequiredByte() << 8) | readRequiredByte();
        } else if (length == 127) {
            length = 0;
            for (int i = 0; i < 8; i++) {
                length = (length << 8) | readRequiredByte();
            }
            if (length < 0) {
                throw new IOException("Invalid WebSocket frame length");
            }
        }
        if (length > MAX_MESSAGE_SIZE) {
            throw new IOException("WebSocket frame is too large: " + length);
        }

        byte[] mask = null;
        if (masked) {
            mask = readExactly(4);
        }
        byte[] payload = readExactly((int) length);
        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i & 3];
            }
        }
        return new Frame(fin, opcode, payload);
    }

    private int readRequiredByte() throws IOException {
        int value = input.read();
        if (value < 0) {
            throw new EOFException("Unexpected end of WebSocket frame");
        }
        return value;
    }

    private byte[] readExactly(int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(data, offset, length - offset);
            if (read < 0) {
                throw new EOFException("Unexpected end of WebSocket frame payload");
            }
            offset += read;
        }
        return data;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                sendFrame(0x8, new byte[0]);
            } catch (IOException ignored) {
            }
        }
        closed = true;
        socket.close();
    }

    private static final class Frame {
        final boolean fin;
        final int opcode;
        final byte[] payload;

        Frame(boolean fin, int opcode, byte[] payload) {
            this.fin = fin;
            this.opcode = opcode;
            this.payload = payload;
        }
    }

    public static final class Message {
        public static final int TYPE_CLOSE = 0x8;
        public static final int TYPE_TEXT = 0x1;
        public static final int TYPE_BINARY = 0x2;

        public final int type;
        public final byte[] data;

        private Message(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        static Message close() {
            return new Message(TYPE_CLOSE, new byte[0]);
        }
    }

    public static final class HandshakeException extends IOException {
        private final int statusCode;
        private final Map<String, String> headers;

        HandshakeException(int statusCode, String statusLine, Map<String, String> headers) {
            super("WebSocket handshake failed: " + statusLine);
            this.statusCode = statusCode;
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getHeader(String name) {
            return headers.get(name.toLowerCase(Locale.US));
        }
    }
}
