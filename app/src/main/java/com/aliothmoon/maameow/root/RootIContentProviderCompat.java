package com.aliothmoon.maameow.root;

import android.content.AttributionSource;
import android.content.IContentProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.system.Os;

public final class RootIContentProviderCompat {

    private RootIContentProviderCompat() {
    }

    public static Bundle call(
            IContentProvider provider,
            String attributeTag,
            String callingPkg,
            String authority,
            String method,
            String arg,
            Bundle extras
    ) throws RemoteException {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                AttributionSource attributionSource = new AttributionSource.Builder(Os.getuid())
                        .setAttributionTag(attributeTag)
                        .setPackageName(callingPkg)
                        .build();
                return provider.call(attributionSource, authority, method, arg, extras);
            } catch (LinkageError e) {
                return provider.call(callingPkg, attributeTag, authority, method, arg, extras);
            }
        } else if (Build.VERSION.SDK_INT == 30) {
            return provider.call(callingPkg, attributeTag, authority, method, arg, extras);
        } else if (Build.VERSION.SDK_INT == 29) {
            return provider.call(callingPkg, authority, method, arg, extras);
        } else {
            return provider.call(callingPkg, method, arg, extras);
        }
    }
}
