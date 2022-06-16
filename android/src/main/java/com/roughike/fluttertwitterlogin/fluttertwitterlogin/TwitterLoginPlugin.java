package com.roughike.fluttertwitterlogin.fluttertwitterlogin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import androidx.annotation.NonNull;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;

import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

public class TwitterLoginPlugin extends Callback<TwitterSession> implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    private MethodChannel channel;
    private static final String CHANNEL_NAME = "com.roughike/flutter_twitter_login";
    private static final String METHOD_GET_CURRENT_SESSION = "getCurrentSession";
    private static final String METHOD_AUTHORIZE = "authorize";
    private static final String METHOD_LOG_OUT = "logOut";

    private TwitterAuthClient authClientInstance;
    private Result pendingResult;
    private Activity activity;
    private Context context;

    // Flutter Plugin
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        System.out.println("onAttachedToEngine");
        this.context = binding.getApplicationContext();
        channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL_NAME);
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        System.out.println("onMethodCall");
        switch (call.method) {
            case METHOD_GET_CURRENT_SESSION:
                getCurrentSession(result, call);
                break;
            case METHOD_AUTHORIZE:
                authorize(result, call);
                break;
            case METHOD_LOG_OUT:
                logOut(result, call);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        System.out.println("onDetachedFromEngine");
        this.context = null;
        this.activity = null;
        channel.setMethodCallHandler(null);
    }

    private void setPendingResult(String methodName, MethodChannel.Result result) {
        System.out.println("setPendingResult");
        if (pendingResult != null) {
            result.error(
                    "TWITTER_LOGIN_IN_PROGRESS",
                    methodName + " called while another Twitter " +
                            "login operation was in progress.",
                    null
            );
        }

        pendingResult = result;
    }

    private void getCurrentSession(Result result, MethodCall call) {
        System.out.println("getCurrentSession");
        initializeAuthClient(call);
        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
        HashMap<String, Object> sessionMap = sessionToMap(session);

        result.success(sessionMap);
    }

    private void authorize(Result result, MethodCall call) {
        System.out.println("authorize");

        setPendingResult("authorize", result);
        initializeAuthClient(call).authorize(this.activity, this);
    }

    private TwitterAuthClient initializeAuthClient(MethodCall call) {
        System.out.println("initializeAuthClient");
        if (authClientInstance == null) {
            String consumerKey = call.argument("consumerKey");
            String consumerSecret = call.argument("consumerSecret");

            authClientInstance = configureClient(consumerKey, consumerSecret);
        }

        return authClientInstance;
    }

    private TwitterAuthClient configureClient(String consumerKey, String consumerSecret) {
        System.out.println("configureClient");

        TwitterAuthConfig authConfig = new TwitterAuthConfig(consumerKey, consumerSecret);
        TwitterConfig config = new TwitterConfig.Builder(this.context)
                .twitterAuthConfig(authConfig)
                .build();
        Twitter.initialize(config);
        return new TwitterAuthClient();
    }

    private void logOut(Result result, MethodCall call) {
        System.out.println("logout");

        CookieSyncManager.createInstance(this.context);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();

        initializeAuthClient(call);
        TwitterCore.getInstance().getSessionManager().clearActiveSession();
        result.success(null);
    }

    private HashMap<String, Object> sessionToMap(final TwitterSession session) {
        System.out.println("sessionToMap");

        if (session == null) {
            return null;
        }

        return new HashMap<String, Object>() {{
            put("secret", session.getAuthToken().secret);
            put("token", session.getAuthToken().token);
            put("userId", String.valueOf(session.getUserId()));
            put("username", session.getUserName());
        }};
    }

    // Twitter Core
    @Override
    public void success(final com.twitter.sdk.android.core.Result<TwitterSession> result) {
        System.out.println("success");
        if (pendingResult != null) {
            final HashMap<String, Object> sessionMap = sessionToMap(result.data);
            final HashMap<String, Object> resultMap = new HashMap<String, Object>() {{
                put("status", "loggedIn");
                put("session", sessionMap);
            }};

            pendingResult.success(resultMap);
            pendingResult = null;
        }
    }

    @Override
    public void failure(final TwitterException exception) {
        System.out.println("failure");
        if (pendingResult != null) {
            final HashMap<String, Object> resultMap = new HashMap<String, Object>() {{
                put("status", "error");
                put("errorMessage", exception.getMessage());
            }};

            pendingResult.success(resultMap);
            pendingResult = null;
        }
    }

    // PluginRegistry.ActivityResultListener
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("onActivityResult");
        if (authClientInstance != null) {
            authClientInstance.onActivityResult(requestCode, resultCode, data);
        }

        return false;
    }

    // ActivityAware
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        System.out.println("onAttachedToActivity");
        this.activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        System.out.println("onDetachedFromActivityForConfigChanges");
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        System.out.println("onReattachedToActivityForConfigChanges");
    }

    @Override
    public void onDetachedFromActivity() {
        System.out.println("onDetachedFromActivity");
    }

}
