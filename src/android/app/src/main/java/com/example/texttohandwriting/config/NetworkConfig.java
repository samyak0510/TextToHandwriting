package com.example.texttohandwriting.config;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Network configuration manager for automatic server detection.
 * Intelligently detects development server IP addresses across different network configurations.
 */
public class NetworkConfig {
    
    private static final String TAG = "NetworkConfig";
    private static final int DEFAULT_PORT = 8080;
    private static final String API_BASE_PATH = "/api/v1/";
    
    private final Context context;
    private String baseUrl;

    public NetworkConfig(Context context) {
        this.context = context;
        this.baseUrl = detectServerUrl();
        Log.i(TAG, "Detected server URL: " + baseUrl);
    }

    /**
     * Automatically detects the server URL using intelligent network discovery.
     */
    private String detectServerUrl() {
        // Check if running on emulator
        if (isRunningOnEmulator()) {
            return "http://10.0.2.2:" + DEFAULT_PORT + API_BASE_PATH;
        }
        
        // Try to get device IP and guess server IP
        String deviceIp = getDeviceIpAddress();
        if (deviceIp != null) {
            String serverIp = guessServerIp(deviceIp);
            return "http://" + serverIp + ":" + DEFAULT_PORT + API_BASE_PATH;
        }
        
        // Fallback to localhost
        return "http://localhost:" + DEFAULT_PORT + API_BASE_PATH;
    }

    /**
     * Gets the device's WiFi IP address.
     */
    private String getDeviceIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                
                if (ipAddress != 0) {
                    return String.format("%d.%d.%d.%d",
                            (ipAddress & 0xff),
                            (ipAddress >> 8 & 0xff),
                            (ipAddress >> 16 & 0xff),
                            (ipAddress >> 24 & 0xff));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device IP", e);
        }
        return null;
    }

    /**
     * Guesses the server IP based on device IP (usually the gateway).
     */
    private String guessServerIp(String deviceIp) {
        // Extract network segment and assume server is at .1 (gateway)
        int lastDot = deviceIp.lastIndexOf('.');
        if (lastDot > 0) {
            String networkSegment = deviceIp.substring(0, lastDot + 1);
            return networkSegment + "1";  // Most development servers run on gateway
        }
        return deviceIp; // Fallback to device IP
    }

    /**
     * Checks if running on Android emulator.
     */
    private boolean isRunningOnEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.contains("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK");
    }

    /**
     * Creates configured Retrofit instance.
     */
    public Retrofit createRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }
} 