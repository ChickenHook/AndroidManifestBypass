package org.chickenhook.manifestbypass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chickenhook.binderhooks.BinderListener;
import org.chickenhook.binderhooks.ServiceHooks;
import org.chickenhook.binderhooks.proxyListeners.ProxyListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.content.pm.ActivityInfo.FLAG_ALLOW_TASK_REPARENTING;
import static android.content.pm.ActivityInfo.FLAG_CLEAR_TASK_ON_LAUNCH;
import static android.content.pm.ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK;
import static org.chickenhook.binderhooks.Logger.log;
import static org.chickenhook.restrictionbypass.helpers.Reflection.setReflective;

public class ActivtiyProvider extends ContentProvider {

    public static final String EXTRA_ORIGINAL_INTENT = "original_intent";


    public ActivtiyProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return -1;
    }

    @Override
    public String getType(Uri uri) {
        return "";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        new Handler().post(new Runnable() { // wait till initialization process has finished
            @Override
            public void run() {
                try {
                    installHooks();
                } catch (Exception e) {
                    log("ActivityProvider - error while install hooks", e);
                }
            }
        });

        return true;
    }

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);

    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return -1;
    }

    void dump(Method method, Object[] objects) {
        String str = "\n===================\n" + method.getName() + "\n" + "------------------\n" + "\n";
        if (objects != null) {
            for (Object o : objects) {
                str += o + "\n";
            }
        }

        str += "\n===================\n";
        log("ActivityProvider" + str);
    }

    /**
     * Will be triggered when startActivity(...) or startActivityForResult(...) is called.
     * The arguments will be forwarded to Activity[Task]ManagerService
     *
     * @param args arguments of IActivity[Task]Manager.startActivity(...)
     * @return args to be forwarded
     */
    Object[] handleStartActivity(Object[] args) {
        Intent oringalIntent = (Intent) args[2];
        // todo check if activity supports trampoline
        if (oringalIntent.getComponent() != null && oringalIntent.getComponent().getPackageName().equals(getContext().getPackageName())) {
            log("ActivityProvider - Initiate trampoline for activity <" + oringalIntent.getComponent() + ">");
            Intent i = new Intent();
            i.setComponent(new ComponentName(getContext().getPackageName(), "org.chickenhook.manifestbypass.Trampoline"));
            i.putExtra(EXTRA_ORIGINAL_INTENT, oringalIntent);
            args[2] = i;
        }

        return args;
    }

    /**
     * Will be triggered when Activity[Task]ManagerService tells our Process to start the given Activity.
     * We catch this via Instrumentation is going to instantiate the Activity.
     * <p>
     * This handler will extract the original intent and the original component classname in order to
     * start the real Activity.
     * <p>
     * If original intent was not found in the given intent's extra we call newActivity(...) with the
     * given args.
     *
     * @param original  original instrumentation instance
     * @param cl        classloader to be used for load the Activity class
     * @param className name of the Activity to be loaded
     * @param intent    intent intended to be launched
     * @return the loaded activity
     */
    Activity handleNewActivity(@NonNull Instrumentation original, ClassLoader cl, String className, Intent intent) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Intent originalIntent = intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT);
        if (originalIntent != null) {
            // todo fix Bundle classloader
            log("ActivityProvider - Launch original activity <" + originalIntent.getComponent() + ">");
            return original.newActivity(cl, originalIntent.getComponent().getClassName(), originalIntent);

        } else {
            return original.newActivity(cl, className, intent);
        }
    }

    /**
     * Will be triggered when PackageManager tries to get ActivityInfo of a non manifest Activity
     *
     * @param componentName of the Activity
     * @return the ActivityInfo
     */
    ActivityInfo handleGetActivityInfo(@NonNull ComponentName componentName) {
        ActivityInfo info = new ActivityInfo();
        info.packageName = componentName.getClassName();
        info.applicationInfo = getContext().getApplicationInfo();
        // todo forward to the user
        log("ActivityProvider - forward ActivityInfo for <" + info + ">");
        return info;
    }

    /**
     * Install necessary hooks
     */
    void installHooks() throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        log("ActivityProvider - install hooks");
        // Below Android Q
        ServiceHooks.hookActivityManager(new ProxyListener() {
            @Override
            public Object invoke(Object original, Object proxy, Method method, Object[] args) throws Throwable {
                dump(method, args);
                if (method.getName().equals("startActivity")) {
                    args = handleStartActivity(args);
                }
                return method.invoke(original, args);
            }
        });

        // Starting with Android Q we have to hook the task manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceHooks.hookActivityTaskManager(new ProxyListener() {
                @Override
                public Object invoke(Object original, Object proxy, Method method, Object[] args) throws Throwable {
                    dump(method, args);
                    if (method.getName().equals("startActivity")) {
                        args = handleStartActivity(args);
                    }

                    return method.invoke(original, args);
                }
            });
        }

        // Hook instrumentation in order to manipulate Activity's class loading
        InstrumentationHook.hookInstrumentation(new InstrumentationHook.InstrumentationCallback() {
            @Override
            public Activity newActivity(@NonNull Instrumentation original, ClassLoader cl, String className, Intent intent) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
                return handleNewActivity(original, cl, className, intent);
            }

            @Override
            public void callActivityOnCreate(@NonNull Instrumentation original, Activity activity, Bundle icicle) {
                ActivityInfo activityInfo = handleGetActivityInfo(activity.getComponentName());
                try {
                    setReflective(activity, Activity.class, "mActivityInfo", activityInfo);
                } catch (Exception e) {
                    log("ActivityProvider - unable to set activity info", e);
                }

                super.callActivityOnCreate(original, activity, icicle);
            }
        });

        // Hook PackageManager in order to manage getActivityInfo(...) calls
//        BinderHook.VERBOSE = true;
        ((ContextWrapper) getContext().getApplicationContext()).getBaseContext().getPackageManager().getInstalledPackages(0);
        ServiceHooks.hookPackageManager(((ContextWrapper) getContext().getApplicationContext()).getBaseContext().getPackageManager(), new BinderListener() {
            public static final java.lang.String DESCRIPTOR = "android.content.pm.IPackageManager";

            @Override
            @SuppressLint("SoonBlockedPrivateApi")
            protected boolean transact(@NonNull IBinder originalBinder, int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
                if (code == 14) {
                    data.setDataPosition(0);
                    data.writeInterfaceToken(DESCRIPTOR);
                    android.content.ComponentName _arg0 = null;
                    if ((0 != data.readInt())) {
                        _arg0 = android.content.ComponentName.CREATOR.createFromParcel(data);
                    }
                    data.setDataPosition(0);
                    boolean res = originalBinder.transact(code, data, reply, flags);
                    if (res) {
                        int hasActivityInfo = reply.readInt();
                        if (hasActivityInfo == 0) {
                            // gotcha!
                            ActivityInfo activityInfo = handleGetActivityInfo(_arg0);
                            reply.recycle();
                            try {
                                Method initMethod = Parcel.class.getDeclaredMethod("init", long.class);
                                initMethod.setAccessible(true);
                                initMethod.invoke(reply, 0);
                                reply.setDataPosition(0);
                                reply.writeNoException();
                                reply.writeInt(1);
                                activityInfo.writeToParcel(reply, 0);
                                reply.setDataPosition(0);

                            } catch (Exception e) {
                                log("ActivityProvider - Error while manipulate getActivityInfo() reply", e);
                            }
                        }
                    }

                    return res;
                }

                return originalBinder.transact(code, data, reply, flags);
            }
        });


        log("ActivityProvider - successfully installed hooks");

    }


}
