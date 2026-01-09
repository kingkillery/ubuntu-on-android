package com.udroid.app.service;

import com.udroid.app.rootfs.RootfsManager;
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
public final class RootfsDownloadService_MembersInjector implements MembersInjector<RootfsDownloadService> {
  private final Provider<RootfsManager> rootfsManagerProvider;

  public RootfsDownloadService_MembersInjector(Provider<RootfsManager> rootfsManagerProvider) {
    this.rootfsManagerProvider = rootfsManagerProvider;
  }

  public static MembersInjector<RootfsDownloadService> create(
      Provider<RootfsManager> rootfsManagerProvider) {
    return new RootfsDownloadService_MembersInjector(rootfsManagerProvider);
  }

  @Override
  public void injectMembers(RootfsDownloadService instance) {
    injectRootfsManager(instance, rootfsManagerProvider.get());
  }

  @InjectedFieldSignature("com.udroid.app.service.RootfsDownloadService.rootfsManager")
  public static void injectRootfsManager(RootfsDownloadService instance,
      RootfsManager rootfsManager) {
    instance.rootfsManager = rootfsManager;
  }
}
