// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plugin for controlling a set of GoogleMap views to be shown as overlays on top of the Flutter
 * view. The overlay should be hidden during transformations or while Flutter is rendering on top of
 * the map. A Texture drawn using GoogleMap bitmap snapshots can then be shown instead of the
 * overlay.
 */
public class GoogleMapsPlugin implements Application.ActivityLifecycleCallbacks {
  static final int CREATED = 1;
  static final int STARTED = 2;
  static final int RESUMED = 3;
  static final int PAUSED = 4;
  static final int STOPPED = 5;
  static final int DESTROYED = 6;
  private final AtomicInteger state = new AtomicInteger(0);

  private static WeakReference<Activity> currentActivity;

  public static Activity getActivity() {
    return currentActivity.get();
  }


  public static void registerWith(Registrar registrar) {
    final GoogleMapsPlugin plugin = new GoogleMapsPlugin();

    getApplicationUsingReflection().registerActivityLifecycleCallbacks(plugin);

    registrar
        .platformViewRegistry()
        .registerViewFactory(
            "plugins.flutter.io/google_maps", new GoogleMapFactory(plugin.state, registrar));
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    if ((isFlutterActivity(activity))) {
      currentActivity = new WeakReference(activity);
      state.set(CREATED);

    }
  }

  @Override
  public void onActivityStarted(Activity activity) {
    if ((isFlutterActivity(activity))) {
      currentActivity = new WeakReference(activity);
      state.set(STARTED);
    }
  }

  @Override
  public void onActivityResumed(Activity activity) {
    if (isFlutterActivity(activity)) {
      currentActivity = new WeakReference(activity);
      state.set(RESUMED);
    }
  }

  @Override
  public void onActivityPaused(Activity activity) {
    if (isFlutterActivity(activity)) {
      state.set(PAUSED);
    }
  }

  @Override
  public void onActivityStopped(Activity activity) {
    if (isFlutterActivity(activity)) {
      state.set(STOPPED);
    }
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

  @Override
  public void onActivityDestroyed(Activity activity) {
    if ((isFlutterActivity(activity))) {
      activity.getApplication().unregisterActivityLifecycleCallbacks(this);
      state.set(DESTROYED);
    }
  }

  private GoogleMapsPlugin() {
  }

  private static Application getApplicationUsingReflection()  {
    try {
      return (Application) Class.forName("android.app.ActivityThread")
              .getMethod("currentApplication").invoke(null, (Object[]) null);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static boolean isFlutterActivity(Context context) {
    return (context instanceof io.flutter.embedding.android.FlutterActivity || context instanceof  io.flutter.app.FlutterActivity);
  }
}

