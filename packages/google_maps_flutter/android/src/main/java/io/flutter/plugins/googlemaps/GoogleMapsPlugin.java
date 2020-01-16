// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

import java.util.HashMap;
import java.util.Map;
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

  //private final AtomicInteger state = new AtomicInteger(0);
  //private final int registrarActivityHashCode;

  GoogleMapsDelegateFactory delegateFactory;

  public static void registerWith(Registrar registrar) {

    final GoogleMapsPlugin plugin = new GoogleMapsPlugin(registrar);
    registerLifecycleCallbacks(registrar, plugin);
    registrar
            .platformViewRegistry()
            .registerViewFactory(
                    "plugins.flutter.io/google_maps", plugin.delegateFactory);
  }

  private static void registerLifecycleCallbacks(Registrar registrar, GoogleMapsPlugin plugin) {
    if (registrar.activeContext() instanceof Activity) {
      ((Application)(registrar.activeContext()).getApplicationContext()).registerActivityLifecycleCallbacks(plugin);
    } else {
      ((Application)registrar.activeContext()).registerActivityLifecycleCallbacks(plugin);
    }
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    delegateFactory.onActivityCreated(activity, savedInstanceState);
    /*
    if (activity.hashCode() != registrarActivityHashCode) {
      return;
    }
    state.set(CREATED);
    */
  }

  @Override
  public void onActivityStarted(Activity activity) {
    delegateFactory.onActivityStarted(activity);
//    if (activity.hashCode() != registrarActivityHashCode) {
//      return;
//    }
//    state.set(STARTED);
  }

  @Override
  public void onActivityResumed(Activity activity) {
    delegateFactory.onActivityResumed(activity);

    //if (activity.hashCode() != registrarActivityHashCode) {
    //  return;
    //}
    //state.set(RESUMED);
  }

  @Override
  public void onActivityPaused(Activity activity) {
    delegateFactory.onActivityPaused(activity);

    //if (activity.hashCode() != registrarActivityHashCode) {
    //  return;
    //}
    //state.set(PAUSED);
  }

  @Override
  public void onActivityStopped(Activity activity) {
    delegateFactory.onActivityStopped(activity);
    //if (activity.hashCode() != registrarActivityHashCode) {
    //  return;
    //}
    //state.set(STOPPED);
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

  @Override
  public void onActivityDestroyed(Activity activity) {
    delegateFactory.onActivityDestroyed(activity);

    //if (activity.hashCode() != registrarActivityHashCode) {
    //  return;
    //}
    //activity.getApplication().unregisterActivityLifecycleCallbacks(this);
    //state.set(DESTROYED);
  }



  private GoogleMapsPlugin(Registrar registrar) {
    delegateFactory = new GoogleMapsDelegateFactory(registrar);
    //this.registrarActivityHashCode = registrar.activeContext().hashCode();
  }
}

class GoogleMapsDelegateFactory extends PlatformViewFactory implements Application.ActivityLifecycleCallbacks {
  int activeActivity = 0;

  Map<Integer, AtomicInteger> states = new HashMap();
  Map<Integer, GoogleMapFactory> factories = new HashMap();
  final Registrar registrar;

  public GoogleMapsDelegateFactory(Registrar registrar) {
    super(StandardMessageCodec.INSTANCE);
    this.registrar = registrar;
  }

  @Override
  public PlatformView create(Context context, int viewId, Object args) {
    return factories.get(activeActivity).create(context, viewId, args);
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    final int activityHashcode = activity.hashCode();
    AtomicInteger state = initForActivity(activityHashcode);

    state.set(GoogleMapsPlugin.CREATED);
  }

  private AtomicInteger initForActivity(int activityHashcode) {
    AtomicInteger state;
    if (!states.containsKey(activityHashcode)) {
      state = new AtomicInteger(0);
      states.put(activityHashcode, state);
      GoogleMapFactory factory = new GoogleMapFactory(state, registrar);
      factories.put(activityHashcode, factory);

    } else {
      state = states.get(activityHashcode);
    }
    return state;
  }

  @Override
  public void onActivityStarted(Activity activity) {

    setState(activity, GoogleMapsPlugin.STARTED);
  }

  private void setState(Activity activity, int newState) {
    initForActivity(activity.hashCode()).set(newState);
  }

  @Override
  public void onActivityResumed(Activity activity) {
    activeActivity = activity.hashCode();
    setState(activity, GoogleMapsPlugin.RESUMED);

  }

  @Override
  public void onActivityPaused(Activity activity) {
    activeActivity = 0;
    setState(activity, GoogleMapsPlugin.PAUSED);

  }

  @Override
  public void onActivityStopped(Activity activity) {
    setState(activity, GoogleMapsPlugin.STOPPED);

  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    setState(activity, GoogleMapsPlugin.DESTROYED);
  }
}