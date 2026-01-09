package com.udroid.app.ui.session;

import com.udroid.app.session.UbuntuSessionManager;
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
public final class SessionListViewModel_Factory implements Factory<SessionListViewModel> {
  private final Provider<UbuntuSessionManager> sessionManagerProvider;

  public SessionListViewModel_Factory(Provider<UbuntuSessionManager> sessionManagerProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public SessionListViewModel get() {
    return newInstance(sessionManagerProvider.get());
  }

  public static SessionListViewModel_Factory create(
      Provider<UbuntuSessionManager> sessionManagerProvider) {
    return new SessionListViewModel_Factory(sessionManagerProvider);
  }

  public static SessionListViewModel newInstance(UbuntuSessionManager sessionManager) {
    return new SessionListViewModel(sessionManager);
  }
}
