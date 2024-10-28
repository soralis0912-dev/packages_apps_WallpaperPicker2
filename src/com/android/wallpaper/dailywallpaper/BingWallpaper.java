package com.android.wallpaper.dailywallpaper;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BingWallpaper {

    private static final String IMAGE_API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";
    private static final String IMAGE_NAME = "bing_daily.jpg";
    private final Context context;

    public BingWallpaper(Context context) {
        this.context = context;
    }

    // Public method to initiate setting Bing's daily wallpaper
    public void setDailyBingWallpaper() {
        new DownloadWallpaperTask().execute(IMAGE_API_URL);
    }

    // AsyncTask for downloading and setting the wallpaper
    private class DownloadWallpaperTask extends AsyncTask<String, Void, Uri> {

        @Override
        protected Uri doInBackground(String... urls) {
            try {
                JSONObject json = fetchJsonFromUrl(urls[0]);
                String imageUrl = extractImageUrlFromJson(json);
                return downloadImageToMediaStore(imageUrl);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri imageUri) {
            if (imageUri != null) {
                setWallpaperFromUri(imageUri);
            }
        }

        // Fetches JSON from the given URL
        private JSONObject fetchJsonFromUrl(String urlString) throws Exception {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                return new JSONObject(content.toString());
            } finally {
                conn.disconnect();
            }
        }

        // Extracts the image URL from the JSON object
        private String extractImageUrlFromJson(JSONObject json) throws Exception {
            return "https://www.bing.com" + json.getJSONArray("images")
                    .getJSONObject(0).getString("url");
        }

        // Downloads the image to the MediaStore
        private Uri downloadImageToMediaStore(String imageUrl) throws Exception {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, IMAGE_NAME);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BingWallpapers");

            ContentResolver resolver = context.getContentResolver();
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                try (InputStream in = new URL(imageUrl).openStream();
                     OutputStream out = resolver.openOutputStream(imageUri)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                    resolver.delete(imageUri, null, null); // Remove partial file if error
                    throw e;
                }
            }
            return imageUri;
        }

        // Sets the downloaded image as the wallpaper
        private void setWallpaperFromUri(Uri imageUri) {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            try (InputStream imageStream = context.getContentResolver().openInputStream(imageUri)) {
                wallpaperManager.setStream(imageStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
