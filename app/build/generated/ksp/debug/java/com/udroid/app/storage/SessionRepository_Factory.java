package com.udroid.app.storage;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SessionRepository_Factory implements Factory<SessionRepository> {
  private final Provider<Context> contextProvider;

  public SessionRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SessionRepository get() {
    return newInstance(contextProvider.get());
  }

  public static SessionRepository_Factory create(Provider<Context> contextProvider) {
    return new SessionRepository_Factory(contextProvider);
  }

  public static SessionRepository newInstance(Context context) {
    return new SessionRepository(context);
  }
}
