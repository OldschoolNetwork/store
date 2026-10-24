package com.oldschoolnetwork.store;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageVersionItem implements Serializable {
    private final java.util.List<String> supportedAbis=new java.util.ArrayList<>();
    public boolean isCompatible(){if(getMinSdkInt()>android.os.Build.VERSION.SDK_INT)return false;if(supportedAbis.isEmpty())return true;String[] device=android.os.Build.VERSION.SDK_INT>=21?android.os.Build.SUPPORTED_ABIS:new String[]{android.os.Build.CPU_ABI,android.os.Build.CPU_ABI2};for(String abi:device)if(supportedAbis.contains(abi))return true;return false;}
    private final java.util.List<String> permissions=new java.util.ArrayList<>();
    private boolean permissionMetadata;
    public java.util.List<String> getSupportedAbis(){return new java.util.ArrayList<>(supportedAbis);}
    public boolean hasPermissionMetadata(){return permissionMetadata;}
    public java.util.List<String> getPermissions(){return new java.util.ArrayList<>(permissions);}
    private boolean supportsOnAccount;
    public boolean supportsOnAccount() { return supportsOnAccount; }
    private int id;
    private String fileName;
    private String versionName;
    private int versionCode;
    private String sizeLabel;
    private String minAndroid;
    private String vtSummary;
    private String downloadUrl;
    private String sha256 = "";
    private String vtStatus = "unknown";

    public PackageVersionItem(int id, String fileName, String versionName, int versionCode,
                              String sizeLabel, String minAndroid, String vtSummary, String downloadUrl) {
        this.id = id;
        this.fileName = fileName != null && !fileName.isEmpty() ? fileName : "package_" + id + ".apk";
        this.versionName = versionName != null && !versionName.isEmpty() ? versionName : "1.0";
        this.versionCode = versionCode;
        this.sizeLabel = sizeLabel != null && !sizeLabel.isEmpty() ? sizeLabel : "Unknown size";
        this.minAndroid = minAndroid != null && !minAndroid.isEmpty() ? minAndroid : "Android 4.0+";
        this.vtSummary = vtSummary != null && !vtSummary.isEmpty() ? vtSummary : "No scan information provided";
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
    }

    public static PackageVersionItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        int id = obj.optInt("id", 0);
        String fileName = obj.optString("file_name", "app_" + id + ".apk");
        String versionName = obj.optString("version_name", "1.0");
        int versionCode = obj.optInt("version_code", 1);
        String sizeLabel = obj.optString("size_label", "");
        String minAndroid = obj.optString("min_android", "Android 4.0+");
        String vtSummary = obj.optString("vt_summary", "No scan information provided");
        String downloadUrl = obj.optString("download_url", "");
        if (downloadUrl.isEmpty() && id > 0) {
            downloadUrl = "https://api.oldschoolnetwork.online/v1/on-account/api/ecosystem.php?action=store_download_apk&package_id=" + id;
        }
        PackageVersionItem item = new PackageVersionItem(id, fileName, versionName, versionCode, sizeLabel, minAndroid, vtSummary, downloadUrl);
        item.supportsOnAccount = obj.optBoolean("supports_on_account", false) || obj.optInt("supports_on_account", 0) == 1;
        org.json.JSONArray abis=obj.optJSONArray("supported_abis");if(abis!=null)for(int i=0;i<abis.length();i++)item.supportedAbis.add(abis.optString(i));
        org.json.JSONArray permissions=obj.optJSONArray("permissions_list");item.permissionMetadata=permissions!=null;
        if(permissions!=null)for(int i=0;i<permissions.length();i++){String value=permissions.optString(i,"").trim();if(!value.isEmpty())item.permissions.add(value);}
        item.sha256 = obj.optString("sha256", "");
        item.vtStatus = obj.optString("vt_status", "unknown");
        return item;
    }

    public int getId() { return id; }
    public String getFileName() { return fileName; }
    public String getVersionName() { return versionName; }
    public int getVersionCode() { return versionCode; }
    public String getSizeLabel() { return sizeLabel; }
    public String getMinAndroid() { return minAndroid; }
    public String getVtSummary() { return vtSummary; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getSha256() { return sha256; }
    public String getVtStatus() { return vtStatus; }

    public int getMinSdkInt() {
        if (minAndroid == null) return 14;
        Matcher m = Pattern.compile("SDK\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(minAndroid);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return 14;
    }

    public float getSizeMbFloat() {
        if (sizeLabel == null) return 0f;
        try {
            String clean = sizeLabel.replace(",", ".").replaceAll("[^0-9.]", "");
            if (!clean.isEmpty()) {
                float size = Float.parseFloat(clean);
                String unit = sizeLabel.toUpperCase(java.util.Locale.US);
                return unit.contains("GB") ? size * 1024 : unit.contains("KB") ? size / 1024 : size;
            }
        } catch (Exception ignored) {}
        return 0f;
    }

    @Override
    public String toString() {
        return versionName + " (" + sizeLabel + ") — " + minAndroid;
    }
}
