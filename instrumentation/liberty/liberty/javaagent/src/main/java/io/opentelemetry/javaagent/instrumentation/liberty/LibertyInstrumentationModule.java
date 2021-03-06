/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.liberty;

import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.List;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Instrumenting request handling in Liberty.
 *
 * <ul>
 *   <li>On entry to WebApp.handleRequest remember request. {@link LibertyHandleRequestAdvice}
 *   <li>On call to WebApp.isForbidden (called from WebApp.handleRequest) start span based on
 *       remembered request. We don't start span immediately at the start or handleRequest because
 *       HttpServletRequest isn't usable yet. {@link LibertyStartSpanAdvice}
 *   <li>On exit from WebApp.handleRequest close the span. {@link LibertyHandleRequestAdvice}
 * </ul>
 */
@AutoService(InstrumentationModule.class)
public class LibertyInstrumentationModule extends InstrumentationModule {

  public LibertyInstrumentationModule() {
    super("liberty");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new WebAppInstrumentation());
  }

  public static class WebAppInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("com.ibm.ws.webcontainer.webapp.WebApp");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      // https://github.com/OpenLiberty/open-liberty/blob/master/dev/com.ibm.ws.webcontainer/src/com/ibm/ws/webcontainer/webapp/WebApp.java
      transformer.applyAdviceToMethod(
          named("handleRequest")
              .and(takesArgument(0, named("javax.servlet.ServletRequest")))
              .and(takesArgument(1, named("javax.servlet.ServletResponse")))
              .and(takesArgument(2, named("com.ibm.wsspi.http.HttpInboundConnection"))),
          LibertyHandleRequestAdvice.class.getName());

      // isForbidden is called from handleRequest
      transformer.applyAdviceToMethod(
          named("isForbidden").and(takesArgument(0, named(String.class.getName()))),
          LibertyStartSpanAdvice.class.getName());
    }
  }
}
