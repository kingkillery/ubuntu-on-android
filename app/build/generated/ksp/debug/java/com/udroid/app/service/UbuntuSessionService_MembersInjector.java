package com.udroid.app.service;

import com.udroid.app.session.UbuntuSessionManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class UbuntuSessionService_MembersInjector implements MembersInjector<UbuntuSessionService> {
  private final Provider<UbuntuSessionManager> sessionManagerProvider;

  public UbuntuSessionService_MembersInjector(
      Provider<UbuntuSessionManager> sessionManagerProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
  }

  public static MembersInjector<UbuntuSessionService> create(
      Provider<UbuntuSessionManager> sessionManagerProvider) {
    return new UbuntuSessionService_MembersInjector(sessionManagerProvider);
  }

  @Override
  public void injectMembers(UbuntuSessionService instance) {
    injectSessionManager(instance, sessionManagerProvider.get());
  }

  @InjectedFieldSignature("com.udroid.app.service.UbuntuSessionService.sessionManager")
  public static void injectSessionManager(UbuntuSessionService instance,
      UbuntuSessionManager sessionManager) {
    instance.sessionManager = sessionManager;
  }
}
