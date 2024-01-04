package com.github.vatbub.common.core;

/*-
 * #%L
 * FOKProjects Common Core
 * %%
 * Copyright (C) 2016 - 2018 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;


import java.io.File;
import java.nio.charset.Charset;

public class CommonAndroidImpl extends CommonPlatformIndependentImplementations {
    private Context context;

    protected CommonAndroidImpl(Context androidContext) {
        setContext(androidContext);
    }

    @Override
    public String getAppDataPath() {
        return getContext().getFilesDir().getPath() + File.separator;
    }

    @Override
    public String getPathAndNameOfCurrentJar() {
        throw new UnsupportedOperationException("Operation not supported on Android");
    }

    @Override
    public String getPackaging() {
        return "apk";
    }

    @Override
    public Hasher get32bitHasher() {
        return Hashing.murmur3_32().newHasher();
    }

    @Override
    public String getUniqueDeviceIdentifier(Hasher hasher) {
        hasher.putString(Settings.Secure.ANDROID_ID, Charset.forName("UTF-8"));

        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        hasher.putString(telephonyManager.getDeviceId(), Charset.forName("UTF-8"));

        return hasher.hash().toString();
    }


    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
