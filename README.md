# AndroidManifestBypass
Project for bypass requirement for register Android components like Activities in the AndroidManifest.xml

<img src="./logo/manifestbypass.png" alt="AndroidManifestBypass logo" height="200" width="200" />


## Supported Components and their limitations

|Component type| Status | Limitations |
|----|----|----|
| Activity | OK | * Theme of Application will be used (you can change it programmatically) <br> * No Manifest configuration because there is no corresponding manifest entry. <br>  * Cannot be accessed externally|
| ContentProvider | WIP | * Cannot be accessed externally|
| Services | WIP |  * Cannot be accessed externally|
| ... | |

Feel free to submit feature requests!

## Integration
Just include the maven repository

1) In your root build.gradle:
```groovy
allprojects {
        repositories {
            [..]
            jcenter()
            maven { url "https://jitpack.io" }
        }
   }
```
2) In your library/build.gradle add:
```groovy
   dependencies {
        implementation 'com.github.ChickenHook:AndroidManifestBypass:1.0'
        implementation 'com.github.ChickenHook:BinderHook:3.0'
        implementation 'com.github.ChickenHook:RestrictionBypass:2.2'
   }
```

## Usage

Just include the library as explained in the Integration chapter.
The BypassProvider will automatically launch you're components when requested.


## Example

### ActivityBypass

The Manifest

```kt
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="org.chickenhook.androidmanifestbypass" >

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme.NoActionBar" >
        <!-- This is not needed anymore ;)
        <activity
            android:name=".SecondActivity"
            android:label="@string/title_activity_second"
            android:theme="@style/AppTheme.NoActionBar" >
        </activity>
                    -->

        <activity android:name=".MainActivity" >
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

Start the activity

```kt
startActivity(Intent(this@MainActivity, SecondActivity::class.java))
```

And voila you can start Secondary activity without the need of register it in Manifest!

## Troubleshooting

Please create a bug report if you find any issues. This chapter will be updated then.


## Donate

If you're happy with my library please order me a cup of coffee ;) Thanks.

[![Donate with PayPal](https://raw.githubusercontent.com/stefan-niedermann/paypal-donate-button/master/paypal-donate-button.png)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8UH5MBVYM3J36)

## Other Projects

| Project | Description |
|---------|-------------|
| [ChickenHook](https://github.com/ChickenHook/ChickenHook) | A linux / android / MacOS hooking framework  |
| [BinderHook](https://github.com/ChickenHook/BinderHook) | Library intended to hook Binder interface and manipulate events |
| [RestrictionBypass](https://github.com/ChickenHook/RestrictionBypass) |  Android API restriction bypass for all Android Versions |
| [AndroidManifestBypass](https://github.com/ChickenHook/AndroidManifestBypass) |  Android API restriction bypass for all Android Versions |
| .. | |
