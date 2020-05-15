package org.chickenhook.manifestbypass;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;

import static org.chickenhook.restrictionbypass.helpers.Reflection.getReflective;

/**
 * Class intended for hooking Instrumentation
 * <p>
 * You can extend it by overwrite more functions and add a callback into InstrumentationCallback here.
 */
public class InstrumentationHook extends Instrumentation {

    private final InstrumentationCallback instrumentationCallback;
    private final Instrumentation original;

    public static void hookInstrumentation(@NonNull InstrumentationHook.InstrumentationCallback instrumentationCallback) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Object activityThread = getReflective(null, Class.forName("android.app.ActivityThread"), "sCurrentActivityThread");
        Field f = activityThread.getClass().getDeclaredField("mInstrumentation");
        f.setAccessible(true);
        Instrumentation instrumentation = (Instrumentation) f.get(activityThread);
        if (instrumentation == null) {
            // err handling

            return;
        }

        f.set(activityThread, new InstrumentationHook(instrumentation, instrumentationCallback));
    }

    public InstrumentationHook(@NonNull Instrumentation original, @NonNull InstrumentationCallback instrumentationCallback) {
        this.instrumentationCallback = instrumentationCallback;
        this.original = original;
    }


    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return instrumentationCallback.newActivity(original, cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        instrumentationCallback.callActivityOnCreate(original, activity, icicle);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        instrumentationCallback.callActivityOnCreate(original, activity, icicle, persistentState);
    }

    static class InstrumentationCallback {
        public Activity newActivity(@NonNull Instrumentation original, ClassLoader cl, String className, Intent intent) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
            return original.newActivity(cl, className, intent);
        }

        public void callActivityOnCreate(@NonNull Instrumentation original, Activity activity, Bundle icicle) {
            original.callActivityOnCreate(activity, icicle);
        }
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void callActivityOnCreate(@NonNull Instrumentation original, Activity activity, Bundle icicle, PersistableBundle persistentState) {
            original.callActivityOnCreate(activity, icicle, persistentState);
        }
    }
}