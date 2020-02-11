// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;
import io.flutter.plugin.platform.PlatformViewRegistry;
import io.flutter.view.FlutterView;
import io.flutter.view.TextureRegistry;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plugin for controlling a set of GoogleMap views to be shown as overlays on top of the Flutter
 * view. The overlay should be hidden during transformations or while Flutter is rendering on top of
 * the map. A Texture drawn using GoogleMap bitmap snapshots can then be shown instead of the
 * overlay.
 */
public class GoogleMapsPlugin {
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
            ((Application) (registrar.activeContext()).getApplicationContext()).registerActivityLifecycleCallbacks(plugin.delegateFactory);
        } else {
            ((Application) registrar.activeContext()).registerActivityLifecycleCallbacks(plugin.delegateFactory);
        }
    }


    private GoogleMapsPlugin(Registrar registrar) {
        delegateFactory = new GoogleMapsDelegateFactory(registrar);
    }
}

// Creates Factories for different Activities
//
// Tracks the Active Activity
// Assumes that only one Activity is Resumed
// It'll always be returned by registrar.getActivity()
// because this activity assigns registrars with the activity
// injected
class GoogleMapsDelegateFactory extends PlatformViewFactory implements Application.ActivityLifecycleCallbacks, ActivityGetter {
    static WeakReference<Activity> activeActivity = null;

    Map<Integer, AtomicInteger> states = new HashMap();
    Map<Integer, GoogleMapFactory> factories = new HashMap();
    final Registrar registrar;

    public GoogleMapsDelegateFactory(Registrar registrar) {
        super(StandardMessageCodec.INSTANCE);
        this.registrar = registrar;
    }

    @Override
    public PlatformView create(Context context, int viewId, Object args) {
        return factories.get(activeActivity.get().hashCode()).create(context, viewId, args);
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
            Log.i("GOOGLE MAPS PLUGIN", "Registering against activeActivity " + activeActivity);
            GoogleMapFactory factory = new GoogleMapFactory(state, new ActivityInjectedRegistrar(registrar, this));
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


    @Override
    public void onActivityResumed(Activity activity) {
        activeActivity = new WeakReference(activity);
        setState(activity, GoogleMapsPlugin.RESUMED);
    }

    @Override
    public void onActivityPaused(Activity activity) {
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

    private void setState(Activity activity, int newState) {
        initForActivity(activity.hashCode()).set(newState);
    }

    @Override
    public Activity get() {
        return activeActivity.get();
    }
}

/// Interface to get the Activity
interface ActivityGetter {
    Activity get();
}

/// Wrap the Registrar interface
/// But inject an Activity from the creator
class ActivityInjectedRegistrar implements Registrar {
    final Registrar delegate;
    final ActivityGetter activity;

    ActivityInjectedRegistrar(Registrar delegate, ActivityGetter activity) {
        this.delegate = delegate;
        this.activity = activity;
    }

    @Override
    public Activity activity() {
        return activity.get();
    }

    @Override
    public Context context() {
        return delegate.context();
    }

    @Override
    public Context activeContext() {
        return delegate.activeContext();
    }

    @Override
    public BinaryMessenger messenger() {
        return delegate.messenger();
    }

    @Override
    public TextureRegistry textures() {
        return delegate.textures();
    }

    @Override
    public PlatformViewRegistry platformViewRegistry() {
        return delegate.platformViewRegistry();
    }

    @Override
    public FlutterView view() {
        return delegate.view();
    }

    @Override
    public String lookupKeyForAsset(String asset) {
        return delegate.lookupKeyForAsset(asset);
    }

    @Override
    public String lookupKeyForAsset(String asset, String packageName) {
        return delegate.lookupKeyForAsset(asset, packageName);
    }

    @Override
    public Registrar publish(Object value) {
        return delegate.publish(value);
    }

    @Override
    public Registrar addRequestPermissionsResultListener(PluginRegistry.RequestPermissionsResultListener listener) {
        return delegate.addRequestPermissionsResultListener(listener);
    }

    @Override
    public Registrar addActivityResultListener(PluginRegistry.ActivityResultListener listener) {
        return delegate.addActivityResultListener(listener);
    }

    @Override
    public Registrar addNewIntentListener(PluginRegistry.NewIntentListener listener) {
        return delegate.addNewIntentListener(listener);
    }

    @Override
    public Registrar addUserLeaveHintListener(PluginRegistry.UserLeaveHintListener listener) {
        return delegate.addUserLeaveHintListener(listener);
    }

    @Override
    public Registrar addViewDestroyListener(PluginRegistry.ViewDestroyListener listener) {
        return delegate.addViewDestroyListener(listener);
    }
}