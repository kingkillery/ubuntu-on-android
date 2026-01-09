package com.udroid.app.session;

import com.udroid.app.native.NativeBridge;
import com.udroid.app.storage.SessionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class UbuntuSessionManagerImpl_Factory implements Factory<UbuntuSessionManagerImpl> {
  private final Provider<SessionRepository> sessionRepositoryProvider;

  private final Provider<NativeBridge> nativeBridgeProvider;

  public UbuntuSessionManagerImpl_Factory(Provider<SessionRepository> sessionRepositoryProvider,
      Provider<NativeBridge> nativeBridgeProvider) {
    this.sessionRepositoryProvider = sessionRepositoryProvider;
    this.nativeBridgeProvider = nativeBridgeProvider;
  }

  @Override
  public UbuntuSessionManagerImpl get() {
    return newInstance(sessionRepositoryProvider.get(), nativeBridgeProvider.get());
  }

  public static UbuntuSessionManagerImpl_Factory create(
      Provider<SessionRepository> sessionRepositoryProvider,
      Provider<NativeBridge> nativeBridgeProvider) {
    return new UbuntuSessionManagerImpl_Factory(sessionRepositoryProvider, nativeBridgeProvider);
  }

  public static UbuntuSessionManagerImpl newInstance(SessionRepository sessionRepository,
      NativeBridge nativeBridge) {
    return new UbuntuSessionManagerImpl(sessionRepository, nativeBridge);
  }
}
