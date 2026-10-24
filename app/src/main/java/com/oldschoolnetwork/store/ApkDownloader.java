package com.oldschoolnetwork.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** Streaming download; call from a worker thread. No APK is kept in memory. */
public final class ApkDownloader {
    public interface Progress { void onProgress(long bytes, long total); }
    private volatile boolean cancelled;
    private final java.util.Map<String,String> headers = new java.util.HashMap<>();
    public void setHeader(String name, String value) { if (value != null && !value.isEmpty()) headers.put(name, value); }
    public void cancel() { cancelled = true; }
    public boolean isCancelled() { return cancelled; }

    public File download(String address, File directory, String sha256, Progress progress) throws Exception {
        File file = null;
        HttpURLConnection connection = null;
        boolean complete = false;
        try {
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create download folder");
            URL url = new URL(address);
            for (int redirects = 0; ; redirects++) {
                checkCancelled();
                if (!"https".equalsIgnoreCase(url.getProtocol()) && !"http".equalsIgnoreCase(url.getProtocol())) {
                    throw new IOException("Unsupported download URL");
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept-Encoding", "identity");
                if (url.getHost().equalsIgnoreCase(new URL(address).getHost())) for (java.util.Map.Entry<String,String> header : headers.entrySet()) connection.setRequestProperty(header.getKey(), header.getValue());
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (redirects >= 5 || location == null) throw new IOException("Invalid download redirect");
                    URL next = new URL(url, location);
                    if ("https".equalsIgnoreCase(url.getProtocol()) && !"https".equalsIgnoreCase(next.getProtocol())) {
                        throw new IOException("Insecure download redirect");
                    }
                    connection.disconnect();
                    connection = null;
                    url = next;
                    continue;
                }
                if (status != 200) throw new IOException("Download failed (HTTP " + status + ")");
                break;
            }
            String type = connection.getContentType();
            if (type != null && (type.contains("text/html") || type.contains("application/json"))) {
                throw new IOException("Server returned a page instead of an APK");
            }
            long total = -1;
            try { total = Long.parseLong(connection.getHeaderField("Content-Length")); } catch (Exception ignored) {}
            file = File.createTempFile("onstore-", ".apk", directory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long received = 0;
            long lastUpdate = 0;
            try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[32 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    checkCancelled();
                    output.write(buffer, 0, count);
                    digest.update(buffer, 0, count);
                    received += count;
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate >= 100) {
                        progress.onProgress(received, total);
                        lastUpdate = now;
                    }
                }
            }
            checkCancelled();
            if (received == 0 || (total >= 0 && total != received)) throw new IOException("Incomplete APK download");
            if (sha256 != null && !sha256.trim().isEmpty()) {
                StringBuilder actual = new StringBuilder();
                for (byte value : digest.digest()) actual.append(String.format(java.util.Locale.US, "%02x", value & 255));
                if (!actual.toString().equalsIgnoreCase(sha256.trim())) throw new IOException("APK checksum does not match");
            }
            progress.onProgress(received, received);
            complete = true;
            return file;
        } finally {
            if (connection != null) connection.disconnect();
            if (!complete && file != null) file.delete();
        }
    }

    private void checkCancelled() throws IOException {
        if (cancelled) throw new IOException("Download cancelled");
    }
}
