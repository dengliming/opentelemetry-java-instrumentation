/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.tracer

import org.spockframework.util.ReflectionUtil
import spock.lang.Specification

class SpanNamesTest extends Specification {

  def "test spanNameForClass"() {
    when:
    String result = SpanNames.spanNameForClass(clazz)

    then:
    result == expected

    where:
    clazz                   | expected
    SpanNamesTest.class     | "SpanNamesTest"
    SpanNames.class         | "SpanNames"
  }

  def "test spanNameForMethod"() {
    when:
    String result = SpanNames.spanNameForMethod(method)

    then:
    result == expected

    where:
    method                                                                   | expected
    ReflectionUtil.getMethodByName(SpanNames.class, "spanNameForClass")      | "SpanNames.spanNameForClass"
    ReflectionUtil.getMethodByName(String.class, "length")                   | "String.length"
  }

  def "test spanNameForMethod with class"() {
    when:
    String result = SpanNames.spanNameForMethod(clazz, method)

    then:
    result == expected

    where:
    clazz            | method                                                              | expected
    SpanNames.class  | ReflectionUtil.getMethodByName(SpanNames.class, "spanNameForClass") | "SpanNames.spanNameForClass"
    SpanNames.class  | "test"                                                              |  "SpanNames.test"
  }
}
