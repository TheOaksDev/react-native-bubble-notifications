package com.reactnativebubblenotifications;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import androidx.annotation.NonNull;

import android.text.Layout;
import android.view.LayoutInflater;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.net.Uri;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.Activity;

import java.util.HashMap;
import java.util.Map;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.OnInitializedCallback;

@ReactModule(name = BubbleNotificationsModule.NAME)
public class BubbleNotificationsModule extends ReactContextBaseJavaModule {

  public static final String NAME = "BubbleNotifications";
  private static final int REQUEST_CODE = 66;
  private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
  private static final String E_PERMISSION_NOT_GRANTED = "E_PERMISSION_NOT_GRANTED";
  private static final String E_FAILED_TO_OPEN_SETTINGS = "E_FAILED_TO_OPEN_SETTINGS";

  private BubblesManager bubblesManager;
  private final ReactApplicationContext reactContext;
  private BubbleLayout bubbleView;
  // Layout and resources on view
  private Button reEnter;
  private LinearLayout notificationView;
  private LinearLayout addressView;
  private LinearLayout chipView;

  private ImageView wridzIcon;
  private ImageView pathIcon;

  private TextView title;
  private TextView detailedMessage;
  private TextView fareDuration;
  private TextView fareDistance;
  private TextView pickUpAddr;
  private TextView dropOffAddr;
  private TextView farePrice;

  private String pickUpLocReact;
  // private String pickUpAddrReact;
  private String dropOffLocReact;
  // private String dropOffAddrReact;
  private String fareReact;
  private String fareDistanceReact;
  private String fareDurationReact;

  private String assignmentIdReact;
  private HashMap<String, Boolean> bubbleStatus = new HashMap<String, Boolean>() {
    {
      put("ShowingBubble", new Boolean(false));
      put("hasPermission", new Boolean(false));
      put("bubbleInitialized", new Boolean(false));
    }
  };

  private Promise bubblePermPromise;

  private final ActivityEventListener bActivityEventListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      if (requestCode == REQUEST_CODE) {
        if (bubblePermPromise != null) {
          if (hasPermission()) {
            bubbleStatus.put("hasPermission", hasPermission());
            bubblePermPromise.resolve(hasPermission());
          } else {
            bubblePermPromise.reject(E_PERMISSION_NOT_GRANTED);
          }
          bubblePermPromise = null;
        }
      }
    }
  };

  public HashMap getBubbleStatus() {
    return bubbleStatus;
  }

  public BubbleNotificationsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    reactContext.addActivityEventListener(bActivityEventListener);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void showFloatingBubble(int x, int y, final Promise promise) {
    try {
      this.addNewBubble(x, y);
      bubbleStatus.put("ShowingBubble", new Boolean(true));
      promise.resolve("bubbleShown");
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  private void addNewBubble(int x, int y) {
    this.removeBubble();
    bubbleView = (BubbleLayout) LayoutInflater
        .from(reactContext)
        .inflate(R.layout.bubble_layout, null);
    bubbleView.setOnBubbleRemoveListener(
        new BubbleLayout.OnBubbleRemoveListener() {
          @Override
          public void onBubbleRemoved(BubbleLayout bubble) {
            bubbleView = null;
            sendEvent("floating-bubble-remove");
          }
        });
    bubbleView.setOnBubbleClickListener(
        new BubbleLayout.OnBubbleClickListener() {
          @Override
          public void onBubbleClick(BubbleLayout bubble) {

            expandNotification(
                pickUpLocReact,
                dropOffLocReact,
                fareDistanceReact,
                fareDurationReact,
                fareReact);

            sendEvent("floating-bubble-press");
          }
        });
    bubbleView.setShouldStickToWall(true);
    bubblesManager.addBubble(bubbleView, x, y);
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return Settings.canDrawOverlays(reactContext);
    }
    return true;
  }

  public void expandNotification(String origin, String dest, String duration, String distance, String fare) {
    // Identify all resources

    if (bubbleView != null) {

      try {
        notificationView = bubbleView.findViewById(R.id.notification_layout);
        addressView = bubbleView.findViewById(R.id.address_container);
        chipView = bubbleView.findViewById(R.id.chip_container);
        title = bubbleView.findViewById(R.id.title);
        detailedMessage = bubbleView.findViewById(R.id.detailed_message_content);
        // wridzIcon = bubbleView.findViewById(R.id.imageView2);
        // pathIcon = bubbleView.findViewById(R.id.imageView);

        fareDuration = bubbleView.findViewById(R.id.duration);
        fareDistance = bubbleView.findViewById(R.id.distance);
        farePrice = bubbleView.findViewById(R.id.fare);

        pickUpAddr = bubbleView.findViewById(R.id.pickUpAddress);
        dropOffAddr = bubbleView.findViewById(R.id.dropOffAddress);
        reEnter = (Button) bubbleView.findViewById(R.id.re_open_app);

        // TODO
        // - make the parameters an object; add a second parameter for trip state;
        // - use the trip state param and add logic for rendering different views
        // by showing/hiding elements or changing verbiage on text

        if (notificationView.getVisibility() == View.GONE) {
          // Set Resources according to what needs to be shown
          notificationView.setVisibility(View.VISIBLE);
          addressView.setVisibility(View.GONE);
          chipView.setVisibility(View.GONE);
          // wridzIcon.setImageResource(R.drawable.bubble_icon);
          title.setText("Currently Online");
          detailedMessage.setText("Waiting for trip assignments");

          // Set bottom Button to reopen the app on click
          reEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Intent launchIntent = reactContext.getPackageManager()
                  .getLaunchIntentForPackage(reactContext.getPackageName());
              if (launchIntent != null) {
                if (pickUpLocReact != null && dropOffLocReact != null && fareDistanceReact != null
                    && fareDurationReact != null && fareReact != null) {
                  sendEvent("app-opened-from-notification");
                }
                reactContext.startActivity(launchIntent);
                notificationView.setVisibility(View.GONE);
              }
            }
          });

          if (origin != null && dest != null && duration != null && distance != null && fare != null) {
            // pathIcon.setImageResource(R.drawable.path);
            title.setText("Assigned a trip");
            detailedMessage.setText("Some dist/dur from pickup");
            addressView.setVisibility(View.VISIBLE);
            chipView.setVisibility(View.VISIBLE);
            pickUpAddr.setText(origin);
            dropOffAddr.setText(dest);
            fareDuration.setText(distance);
            fareDistance.setText(duration);
            farePrice.setText(fare);
          }
        } else {
          // Hide notification and set text back to empty
          title.setText("Currently Online");
          detailedMessage.setText("Waiting for trip assignments");
          pickUpAddr.setText("");
          dropOffAddr.setText("");
          // pickUpLoc.setText("");
          fareDuration.setText("");
          fareDistance.setText("");
          notificationView.setVisibility(View.GONE);
          addressView.setVisibility(View.GONE);
          chipView.setVisibility(View.GONE);
        }
      } catch (Exception e) {
      }

    }
  }

  @ReactMethod
  public void loadData(String origin, String dest, String duration, String distance, String fare, String assignmentId) {
    pickUpLocReact = origin;
    dropOffLocReact = dest;
    fareDistanceReact = distance;
    fareDurationReact = duration;
    fareReact = fare;
    assignmentIdReact = assignmentId;
  }

  @ReactMethod
  public void loadDataAndExpand(String origin, String dest, String duration, String distance, String fare,
      String assignmentId) {
    pickUpLocReact = origin;
    dropOffLocReact = dest;
    fareDistanceReact = distance;
    fareDurationReact = duration;
    fareReact = fare;
    assignmentIdReact = assignmentId;

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        expandNotification(origin, dest, distance, duration, fare);
      }
    });
  }

  @ReactMethod
  public void reopenApp() {
    Intent launchIntent = reactContext
        .getPackageManager()
        .getLaunchIntentForPackage(reactContext.getPackageName());
    if (launchIntent != null) {
      reactContext.startActivity(launchIntent);
    }
  }

  @ReactMethod
  public void resetBubbleDataFromReact(final Promise promise) {
    try {
      pickUpLocReact = null;
      dropOffLocReact = null;
      fareDistanceReact = null;
      fareDurationReact = null;
      fareReact = null;
      assignmentIdReact = null;
      promise.resolve("Data Wiped");
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod // Notates a method that should be exposed to React
  public void hideFloatingBubble(final Promise promise) {
    try {
      this.removeBubble();
      promise.resolve("Bubble Hidden");
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod // Notates a method that should be exposed to React
  public void requestPermission(final Promise promise) {
    try {
      this.requestPermissionAction(promise);
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod // Notates a method that should be exposed to React
  public void checkPermission(final Promise promise) {
    try {
      bubbleStatus.put("hasPermission", hasPermission());
      promise.resolve(hasPermission());
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod // Notates a method that should be exposed to React
  public void initialize(final Promise promise) {
    try {
      this.initializeBubblesManager();
      bubbleStatus.put("bubbleInitialized", new Boolean(true));
      if (!bubbleStatus.containsKey("ShowingBubble")) {
        bubbleStatus.put("ShowingBubble", new Boolean(false));
      }
      promise.resolve("bubble Initialized");
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod // Notates a method that should be exposed to React
  public void destroy(final Promise promise) {
    try {
      bubblesManager.recycle();
      bubbleStatus.put("bubbleInitialized", new Boolean(false));
      bubbleStatus.put("ShowingBubble", new Boolean(false));
      promise.resolve("bubble destroyed");
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void getState(final Promise promise) {
    try {
      WritableMap map = new WritableNativeMap();

      for (Map.Entry<String, Boolean> entry : bubbleStatus.entrySet()) {
        map.putBoolean(entry.getKey(), entry.getValue());
      }

      promise.resolve(map);
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  private void removeBubble() {
    if (bubbleView != null) {
      try {
        bubblesManager.removeBubble(bubbleView);
        bubbleStatus.put("ShowingBubble", new Boolean(false));
      } catch (Exception e) {
      }
    }
  }

  public void requestPermissionAction(final Promise promise) {
    Activity currentActivity = getCurrentActivity();

    if (currentActivity == null) {
      promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "requestPermissionAction() - currentActivity == null");
      return;
    }

    // Store the promise to resolve/reject when settings returns data
    bubblePermPromise = promise;

    try {
      if (!hasPermission()) {
        Intent intent = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + reactContext.getPackageName()));
        Bundle bundle = new Bundle();
        reactContext.startActivityForResult(intent, REQUEST_CODE, bundle);
      }
    } catch (Exception e) {
      bubblePermPromise.reject(E_FAILED_TO_OPEN_SETTINGS, e);
      bubblePermPromise = null;
    }
  }

  private void initializeBubblesManager() {
    try {
      bubblesManager = new BubblesManager.Builder(reactContext)
          .setTrashLayout(R.layout.bubble_trash_layout)
          .build();
      bubblesManager.initialize();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  private void sendEvent(String eventName) {
    if (eventName == "floating-bubble-remove") {
      bubbleStatus.put("ShowingBubble", new Boolean(false));
    }

    WritableMap params = Arguments.createMap();
    params.putString("assignmentId", assignmentIdReact);
    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
}