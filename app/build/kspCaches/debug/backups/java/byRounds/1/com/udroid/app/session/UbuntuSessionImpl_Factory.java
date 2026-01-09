package com.udroid.app.session;

import com.udroid.app.native.NativeBridge;
import com.udroid.app.storage.SessionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class UbuntuSessionImpl_Factory implements Factory<UbuntuSessionImpl> {
  private final Provider<String> idProvider;

  private final Provider<SessionConfig> configProvider;

  private final Provider<SessionRepository> sessionRepositoryProvider;

  private final Provider<NativeBridge> nativeBridgeProvider;

  public UbuntuSessionImpl_Factory(Provider<String> idProvider,
      Provider<SessionConfig> configProvider, Provider<SessionRepository> sessionRepositoryProvider,
      Provider<NativeBridge> nativeBridgeProvider) {
    this.idProvider = idProvider;
    this.configProvider = configProvider;
    this.sessionRepositoryProvider = sessionRepositoryProvider;
    this.nativeBridgeProvider = nativeBridgeProvider;
  }

  @Override
  public UbuntuSessionImpl get() {
    return newInstance(idProvider.get(), configProvider.get(), sessionRepositoryProvider.get(), nativeBridgeProvider.get());
  }

  public static UbuntuSessionImpl_Factory create(Provider<String> idProvider,
      Provider<SessionConfig> configProvider, Provider<SessionRepository> sessionRepositoryProvider,
      Provider<NativeBridge> nativeBridgeProvider) {
    return new UbuntuSessionImpl_Factory(idProvider, configProvider, sessionRepositoryProvider, nativeBridgeProvider);
  }

  public static UbuntuSessionImpl newInstance(String id, SessionConfig config,
      SessionRepository sessionRepository, NativeBridge nativeBridge) {
    return new UbuntuSessionImpl(id, config, sessionRepository, nativeBridge);
  }
}
