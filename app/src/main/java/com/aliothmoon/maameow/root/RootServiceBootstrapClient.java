package com.aliothmoon.maameow.root;

import android.content.IContentProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.aliothmoon.maameow.third.Ln;
import com.aliothmoon.maameow.third.wrappers.ServiceManager;

public final class RootServiceBootstrapClient {

    private RootServiceBootstrapClient() {
    }

    public static IBinder attachRemoteService(String packageName, int userId, String token, IBinder serviceBinder) {
        String authority = packageName + RootServiceBootstrapRegistry.AUTHORITY_SUFFIX;
        IBinder providerToken = new Binder();
        IContentProvider provider = null;

        try {
            provider = ServiceManager.getActivityManager()
                    .getContentProviderExternal(authority, userId, providerToken, authority);
            if (provider == null) {
                Ln.e("Root bootstrap provider is null: " + authority + " user=" + userId);
                return null;
            }
            if (!provider.asBinder().pingBinder()) {
                Ln.e("Root bootstrap provider is dead: " + authority + " user=" + userId);
                return null;
            }

            Bundle extras = new Bundle();
            extras.putString(RootServiceBootstrapRegistry.KEY_TOKEN, token);
            extras.putBinder(RootServiceBootstrapRegistry.KEY_SERVICE_BINDER, serviceBinder);

            Bundle reply = RootIContentProviderCompat.call(
                    provider,
                    null,
                    null,
                    authority,
                    RootServiceBootstrapRegistry.METHOD_ATTACH_REMOTE_SERVICE,
                    null,
                    extras
            );
            if (reply == null) {
                Ln.e("Root bootstrap provider returned null");
                return null;
            }

            IBinder lifecycleBinder = reply.getBinder(RootServiceBootstrapRegistry.KEY_APP_BINDER);
            if (lifecycleBinder == null || !lifecycleBinder.pingBinder()) {
                Ln.e("Root bootstrap app lifecycle binder missing");
                return null;
            }
            return lifecycleBinder;
        } catch (Throwable tr) {
            Ln.e("Failed to send binder back to app", tr);
            return null;
        } finally {
            if (provider != null) {
                ServiceManager.getActivityManager().removeContentProviderExternal(authority, providerToken);
            }
        }
    }
}
