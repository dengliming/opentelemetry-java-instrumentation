/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.guava;

import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.AbstractFuture;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.instrumentation.api.Java8BytecodeBridge;
import io.opentelemetry.javaagent.instrumentation.api.concurrent.ExecutorInstrumentationUtils;
import io.opentelemetry.javaagent.instrumentation.api.concurrent.RunnableWrapper;
import io.opentelemetry.javaagent.instrumentation.api.concurrent.State;
import java.util.List;
import java.util.concurrent.Executor;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

@AutoService(InstrumentationModule.class)
public class GuavaInstrumentationModule extends InstrumentationModule {

  public GuavaInstrumentationModule() {
    super("guava", "guava-10.0");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new ListenableFutureInstrumentation());
  }

  public static class ListenableFutureInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("com.google.common.util.concurrent.AbstractFuture");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          isConstructor(), GuavaInstrumentationModule.class.getName() + "$AbstractFutureAdvice");
      transformer.applyAdviceToMethod(
          named("addListener").and(ElementMatchers.takesArguments(Runnable.class, Executor.class)),
          GuavaInstrumentationModule.class.getName() + "$AddListenerAdvice");
    }
  }

  public static class AbstractFutureAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onConstruction() {
      InstrumentationHelper.initialize();
    }
  }

  public static class AddListenerAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static State addListenerEnter(
        @Advice.Argument(value = 0, readOnly = false) Runnable task) {
      final Context context = Java8BytecodeBridge.currentContext();
      final Runnable newTask = RunnableWrapper.wrapIfNeeded(task);
      if (ExecutorInstrumentationUtils.shouldAttachStateToTask(newTask)) {
        task = newTask;
        final ContextStore<Runnable, State> contextStore =
            InstrumentationContext.get(Runnable.class, State.class);
        return ExecutorInstrumentationUtils.setupState(contextStore, newTask, context);
      }
      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void addListenerExit(
        @Advice.Enter final State state, @Advice.Thrown final Throwable throwable) {
      ExecutorInstrumentationUtils.cleanUpOnMethodExit(state, throwable);
    }

    private static void muzzleCheck(final AbstractFuture<?> future) {
      future.addListener(null, null);
    }
  }
}
