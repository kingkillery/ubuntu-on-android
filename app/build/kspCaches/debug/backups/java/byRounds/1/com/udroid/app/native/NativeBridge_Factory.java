package com.udroid.app.native;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class NativeBridge_Factory implements Factory<NativeBridge> {
  @Override
  public NativeBridge get() {
    return newInstance();
  }

  public static NativeBridge_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static NativeBridge newInstance() {
    return new NativeBridge();
  }

  private static final class InstanceHolder {
    private static final NativeBridge_Factory INSTANCE = new NativeBridge_Factory();
  }
}
