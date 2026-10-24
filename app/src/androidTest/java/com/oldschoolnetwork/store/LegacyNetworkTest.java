package com.oldschoolnetwork.store;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class LegacyNetworkTest {
    @Test public void catalogAndBannerLoadOverHttps() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(StoreRepository.BASE_API_URL + "?action=store_catalog").openConnection();
        connection.setConnectTimeout(20000); connection.setReadTimeout(20000);
        try {
            assertEquals(200, connection.getResponseCode());
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) { byte[] bytes = new byte[8192]; int read; while ((read = input.read(bytes)) >= 0) response.write(bytes, 0, read); }
            JSONObject json = new JSONObject(response.toString("UTF-8"));
            assertEquals("ok", json.optString("status"));
            assertTrue("Live catalog should include apps", json.getJSONArray("apps").length() > 0);
            JSONArray banners = json.getJSONArray("banners"); assertTrue(banners.length() > 0);
            HttpURLConnection image = (HttpURLConnection) new URL(banners.getJSONObject(0).getString("image_url")).openConnection();
            image.setConnectTimeout(20000); image.setReadTimeout(20000);
            try { assertEquals(200, image.getResponseCode()); assertTrue(image.getInputStream().read() >= 0); } finally { image.disconnect(); }
        } finally { connection.disconnect(); }
    }
}
