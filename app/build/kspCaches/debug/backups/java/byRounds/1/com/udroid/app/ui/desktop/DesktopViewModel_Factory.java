package com.udroid.app.ui.desktop;

import androidx.lifecycle.SavedStateHandle;
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
public final class DesktopViewModel_Factory implements Factory<DesktopViewModel> {
  private final Provider<UbuntuSessionManager> sessionManagerProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public DesktopViewModel_Factory(Provider<UbuntuSessionManager> sessionManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public DesktopViewModel get() {
    return newInstance(sessionManagerProvider.get(), savedStateHandleProvider.get());
  }

  public static DesktopViewModel_Factory create(
      Provider<UbuntuSessionManager> sessionManagerProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new DesktopViewModel_Factory(sessionManagerProvider, savedStateHandleProvider);
  }

  public static DesktopViewModel newInstance(UbuntuSessionManager sessionManager,
      SavedStateHandle savedStateHandle) {
    return new DesktopViewModel(sessionManager, savedStateHandle);
  }
}
