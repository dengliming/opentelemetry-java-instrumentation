/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jdbc;

import io.opentelemetry.instrumentation.api.instrumenter.net.NetAttributesExtractor;
import org.checkerframework.checker.nullness.qual.Nullable;

final class JdbcNetAttributesExtractor extends NetAttributesExtractor<DbRequest, Void> {

  @Nullable
  @Override
  protected String transport(DbRequest dbRequest) {
    return null;
  }

  @Nullable
  @Override
  protected String peerName(DbRequest request, @Nullable Void response) {
    return request.getDbInfo().getHost();
  }

  @Nullable
  @Override
  protected Integer peerPort(DbRequest request, @Nullable Void response) {
    return request.getDbInfo().getPort();
  }

  @Nullable
  @Override
  protected String peerIp(DbRequest request, @Nullable Void response) {
    return null;
  }
}
