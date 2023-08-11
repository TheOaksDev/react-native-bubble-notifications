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
import com.facebook.react.bridge.ReadableMap;
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
  private LinearLayout driverInfoView;
  private LinearLayout pickupMessageView;

  private ImageView wridzIcon;
  private ImageView pathIcon;

  // Config Variables (set once, left unchanged)
  private TextView driverNameView;
  private String driverName;
  private TextView driverRatingView;
  private String driverRating;
  private TextView statusText;

  // Standard Varibales
  private TextView title;
  private TextView pickupMessage;
  private TextView fareDuration;
  private TextView fareDistance;
  private TextView pickUpAddr;
  private TextView dropOffAddr;
  private TextView farePrice;

  private String tripStateReact;
  private String pickUpLocReact;
  private String dropOffLocReact;
  private String fareReact;
  private String fareDistanceReact;
  private String fareDurationReact;
  private String assignmentIdReact;
  private String pickupMessageReact;

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
  public void setDriverInfo(String name, String rating, final Promise promise) {
    try {
      driverName = name;
      driverRating = rating;
      promise.resolve(true);
    } catch (Exception e) {
      promise.reject(e);
    }
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

  private void initLayout() {
    try {
      notificationView = bubbleView.findViewById(R.id.notification_layout);
      title = bubbleView.findViewById(R.id.title);
      driverNameView = bubbleView.findViewById(R.id.driver_name);
      statusText = bubbleView.findViewById(R.id.status_text);
      driverRatingView = bubbleView.findViewById(R.id.driver_rating);
      reEnter = (Button) bubbleView.findViewById(R.id.re_open_app);

      notificationView.setVisibility(View.GONE);

      if (tripStateReact == "0") {
        title.setText("Not receiving trip assignments");
        statusText.setText("Offline");
      }

      if (tripStateReact == "1") {
        title.setText("Waiting for trip assignments");
        statusText.setText("Online");
      }
      driverNameView.setText(driverName);
      driverRatingView.setText(driverRating);

      reEnter.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent launchIntent = reactContext.getPackageManager()
              .getLaunchIntentForPackage(reactContext.getPackageName());
          if (launchIntent != null) {
            sendEvent("app-opened-from-notification");
            reactContext.startActivity(launchIntent);
            notificationView.setVisibility(View.GONE);
          }
        }
      });

    } catch (Exception e) {
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

            WritableMap map = new WritableNativeMap();
            map.putString("state", tripStateReact);
            map.putString("origin", pickUpLocReact);
            map.putString("dest", dropOffLocReact);
            map.putString("distance", fareDistanceReact);
            map.putString("duration", fareDurationReact);
            map.putString("fare", fareReact);
            map.putString("pickupMessage", pickupMessageReact);

            expandNotification(map);
            sendEvent("floating-bubble-press");
          }
        });
    bubbleView.setShouldStickToWall(true);
    this.initLayout();
    bubblesManager.addBubble(bubbleView, x, y);
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return Settings.canDrawOverlays(reactContext);
    }
    return true;
  }

  public void expandNotification(ReadableMap trip) {
    // Identify all resources

    if (bubbleView != null) {

      try {
        notificationView = bubbleView.findViewById(R.id.notification_layout);
        addressView = bubbleView.findViewById(R.id.address_container);
        chipView = bubbleView.findViewById(R.id.chip_container);
        title = bubbleView.findViewById(R.id.title);
        driverNameView = bubbleView.findViewById(R.id.driver_name);
        driverRatingView = bubbleView.findViewById(R.id.driver_rating);
        driverInfoView = bubbleView.findViewById(R.id.driver_info);

        pickupMessageView = bubbleView.findViewById(R.id.pickup_message_view);
        // wridzIcon = bubbleView.findViewById(R.id.imageView2);
        // pathIcon = bubbleView.findViewById(R.id.imageView);

        pickUpAddr = bubbleView.findViewById(R.id.pickUpAddress);
        dropOffAddr = bubbleView.findViewById(R.id.dropOffAddress);
        fareDuration = bubbleView.findViewById(R.id.duration);
        fareDistance = bubbleView.findViewById(R.id.distance);
        farePrice = bubbleView.findViewById(R.id.fare);
        pickupMessage = bubbleView.findViewById(R.id.pickup_message);

        reEnter = (Button) bubbleView.findViewById(R.id.re_open_app);

        reEnter.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent launchIntent = reactContext.getPackageManager()
                .getLaunchIntentForPackage(reactContext.getPackageName());
            if (launchIntent != null) {
              sendEvent("app-opened-from-notification");
              reactContext.startActivity(launchIntent);
              notificationView.setVisibility(View.GONE);
            }
          }
        });
        // detailedMessage.setText("Waiting for trip assignments");

        // TODO
        // - make the parameters an object; add a second parameter for trip state;
        // - use the trip state param and add logic for rendering different views
        // by showing/hiding elements or changing verbiage on text

        if (notificationView.getVisibility() == View.GONE) {
          notificationView.setVisibility(View.VISIBLE);
        } else {
          notificationView.setVisibility(View.GONE);
        }

        if (Integer.parseInt(trip.getString("state")) == Integer.parseInt("3")) {
          // driver has trip assignment
          title.setText("Assigned a trip");
          pickUpAddr.setText(trip.getString("origin"));
          dropOffAddr.setText(trip.getString("dest"));
          fareDuration.setText(trip.getString("distance"));
          fareDistance.setText(trip.getString("duration"));
          farePrice.setText(trip.getString("fare"));
          pickupMessage.setText(trip.getString("pickupMessage"));
          driverInfoView.setVisibility(View.GONE);
          addressView.setVisibility(View.VISIBLE);
          chipView.setVisibility(View.VISIBLE);
          pickupMessageView.setVisibility(View.VISIBLE);
        } else if (Integer.parseInt(trip.getString("state")) > Integer.parseInt("3")) {
          // driver is in an active trip
          title.setText("Trip In Progress");
          pickUpAddr.setText("");
          dropOffAddr.setText("");
          fareDuration.setText("");
          fareDistance.setText("");
          addressView.setVisibility(View.GONE);
          chipView.setVisibility(View.GONE);
          pickupMessageView.setVisibility(View.GONE);
          driverInfoView.setVisibility(View.GONE);
        } else if (Integer.parseInt(trip.getString("state")) == Integer.parseInt("10")) {
          title.setText("Trip Cancelled");
          pickUpAddr.setText("");
          dropOffAddr.setText("");
          fareDuration.setText("");
          fareDistance.setText("");
          addressView.setVisibility(View.GONE);
          chipView.setVisibility(View.GONE);
          pickupMessageView.setVisibility(View.GONE);
          driverInfoView.setVisibility(View.GONE);
        } else {
          if (Integer.parseInt(trip.getString("state")) == Integer.parseInt("1")) {
            title.setText("Waiting for a trip assignment");
            statusText.setText("Online");
          } else {
            title.setText("Not receiving trip assignments");
            statusText.setText("Offline");
          }
          driverNameView.setText(driverName);
          driverRatingView.setText(driverRating);
          pickUpAddr.setText("");
          dropOffAddr.setText("");
          fareDuration.setText("");
          fareDistance.setText("");
          addressView.setVisibility(View.GONE);
          chipView.setVisibility(View.GONE);
          pickupMessageView.setVisibility(View.GONE);
          driverInfoView.setVisibility(View.VISIBLE);
          // not sure what is wrong?? should not reach here
        }
      } catch (Exception e) {
      }

    }
  }

  @ReactMethod
  public void loadData(ReadableMap trip) {
    tripStateReact = trip.getString("state");
    pickUpLocReact = trip.getString("origin");
    dropOffLocReact = trip.getString("dest");
    fareDistanceReact = trip.getString("distance");
    fareDurationReact = trip.getString("duration");
    fareReact = trip.getString("fare");
    assignmentIdReact = trip.getString("assignmfentId");
    pickupMessageReact = trip.getString("pickupMessage");
    // detailedMessageReact = pickupMessage;
  }

  @ReactMethod
  public void loadDataAndExpand(ReadableMap trip) {
    tripStateReact = trip.getString("state");
    pickUpLocReact = trip.getString("origin");
    dropOffLocReact = trip.getString("dest");
    fareDistanceReact = trip.getString("distance");
    fareDurationReact = trip.getString("duration");
    fareReact = trip.getString("fare");
    assignmentIdReact = trip.getString("assignmentId");
    pickupMessageReact = trip.getString("pickupMessage");
    // detailedMessageReact = pickupMessage;

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        expandNotification(trip);
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
      tripStateReact = null;
      pickupMessageReact = null;
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