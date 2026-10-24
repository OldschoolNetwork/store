package com.oldschoolnetwork.store;
import org.junit.Test;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ApkDownloaderTest {
    private String serve(byte[] bytes, int declaredLength) throws Exception {
        ServerSocket server = new ServerSocket(0);
        new Thread(() -> {
            try (Socket socket = server.accept()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                while (!"".equals(reader.readLine())) {}
                OutputStream output = socket.getOutputStream();
                output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/vnd.android.package-archive\r\nContent-Length: " + declaredLength + "\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
                output.write(bytes); output.flush();
            } catch (Exception ignored) {} finally { try { server.close(); } catch (Exception ignored) {} }
        }).start();
        return "http://127.0.0.1:" + server.getLocalPort() + "/app.apk";
    }
    @Test public void downloadsAndChecksSha256() throws Exception {
        byte[] bytes = "abc".getBytes("UTF-8"); File directory = Files.createTempDirectory("onstore-test").toFile();
        File result = new ApkDownloader().download(serve(bytes, 3), directory, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", (received,total) -> {});
        assertArrayEquals(bytes, Files.readAllBytes(result.toPath())); result.delete(); directory.delete();
    }
    @Test public void wrongHashDeletesThePartialFile() throws Exception {
        File directory = Files.createTempDirectory("onstore-test").toFile();
        try { new ApkDownloader().download(serve(new byte[]{1,2,3}, 3), directory, "wrong", (received,total) -> {}); fail("Bad hash was accepted"); }
        catch (Exception expected) { assertTrue(expected.getMessage().contains("checksum")); }
        assertEquals(0, directory.list().length); directory.delete();
    }
    @Test public void truncatedFileIsNotInstallable() throws Exception {
        File directory = Files.createTempDirectory("onstore-test").toFile();
        try { new ApkDownloader().download(serve(new byte[]{1,2,3}, 100), directory, "", (received,total) -> {}); fail("Truncated APK was accepted"); }
        catch (Exception expected) { assertEquals(0, directory.list().length); }
        directory.delete();
    }
    @Test public void cancelledTaskCreatesNoFile() throws Exception {
        File directory = Files.createTempDirectory("onstore-test").toFile(); ApkDownloader task = new ApkDownloader(); task.cancel();
        try { task.download("http://127.0.0.1:1/app.apk", directory, "", (received,total) -> {}); fail("Cancelled task ran"); }
        catch (Exception expected) { assertTrue(expected.getMessage().contains("cancelled")); }
        assertEquals(0, directory.list().length); directory.delete();
    }
}
