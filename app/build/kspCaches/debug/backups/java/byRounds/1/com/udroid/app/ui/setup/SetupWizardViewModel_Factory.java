package com.udroid.app.ui.setup;

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
public final class SetupWizardViewModel_Factory implements Factory<SetupWizardViewModel> {
  private final Provider<UbuntuSessionManager> sessionManagerProvider;

  public SetupWizardViewModel_Factory(Provider<UbuntuSessionManager> sessionManagerProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public SetupWizardViewModel get() {
    return newInstance(sessionManagerProvider.get());
  }

  public static SetupWizardViewModel_Factory create(
      Provider<UbuntuSessionManager> sessionManagerProvider) {
    return new SetupWizardViewModel_Factory(sessionManagerProvider);
  }

  public static SetupWizardViewModel newInstance(UbuntuSessionManager sessionManager) {
    return new SetupWizardViewModel(sessionManager);
  }
}
