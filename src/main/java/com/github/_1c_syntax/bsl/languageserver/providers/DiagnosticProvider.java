/*
 * This file is a part of BSL Language Server.
 *
 * Copyright © 2018-2020
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Gryzlov <nixel2007@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * BSL Language Server is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * BSL Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BSL Language Server.
 */
package com.github._1c_syntax.bsl.languageserver.providers;

import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.context.computer.DiagnosticIgnoranceComputer;
import com.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.DiagnosticSupplier;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class DiagnosticProvider {

  public static final String SOURCE = "bsl-language-server";

  private final Map<URI, List<Diagnostic>> computedDiagnostics;
  private final DiagnosticSupplier diagnosticSupplier;

  public DiagnosticProvider(DiagnosticSupplier diagnosticSupplier) {
    this.diagnosticSupplier = diagnosticSupplier;
    computedDiagnostics = new HashMap<>();
  }

  public void computeAndPublishDiagnostics(LanguageClient client, DocumentContext documentContext) {
    List<Diagnostic> diagnostics = computeDiagnostics(documentContext);

    client.publishDiagnostics(new PublishDiagnosticsParams(documentContext.getUri().toString(), diagnostics));
  }

  public void publishEmptyDiagnosticList(LanguageClient client, DocumentContext documentContext) {
    computedDiagnostics.put(documentContext.getUri(), Collections.emptyList());
    client.publishDiagnostics(
      new PublishDiagnosticsParams(documentContext.getUri().toString(), Collections.emptyList())
    );
  }

  public List<Diagnostic> computeDiagnostics(DocumentContext documentContext) {
    DiagnosticIgnoranceComputer.Data diagnosticIgnorance = documentContext.getDiagnosticIgnorance();

    List<Diagnostic> diagnostics =
      diagnosticSupplier.getDiagnosticInstances(documentContext)
        .parallelStream()
        .flatMap((BSLDiagnostic diagnostic) -> {
          try {
            return diagnostic.getDiagnostics(documentContext).stream();
          } catch (RuntimeException e) {
            String message = String.format(
              "Diagnostic computation error.%nFile: %s%nDiagnostic: %s",
              documentContext.getUri(),
              diagnostic.getInfo().getCode()
            );
            LOGGER.error(message, e);

            return Stream.empty();
          }
        })
        .filter(Predicate.not(diagnosticIgnorance::diagnosticShouldBeIgnored))
        .distinct()
        .collect(Collectors.toList());

    computedDiagnostics.put(documentContext.getUri(), diagnostics);

    return diagnostics;
  }

  public List<Diagnostic> getComputedDiagnostics(DocumentContext documentContext) {
    return computedDiagnostics.getOrDefault(documentContext.getUri(), Collections.emptyList());
  }

  public void clearComputedDiagnostics(DocumentContext documentContext) {
    computedDiagnostics.put(documentContext.getUri(), Collections.emptyList());
  }

  public void clearAllComputedDiagnostics() {
    computedDiagnostics.clear();
  }
}
