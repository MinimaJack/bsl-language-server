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
package com.github._1c_syntax.bsl.languageserver.context;

import com.github._1c_syntax.bsl.languageserver.context.computer.CognitiveComplexityComputer;
import com.github._1c_syntax.bsl.languageserver.context.computer.ComplexityData;
import com.github._1c_syntax.bsl.languageserver.context.computer.Computer;
import com.github._1c_syntax.bsl.languageserver.context.computer.CyclomaticComplexityComputer;
import com.github._1c_syntax.bsl.languageserver.context.computer.DiagnosticIgnoranceComputer;
import com.github._1c_syntax.bsl.languageserver.context.computer.SymbolTreeComputer;
import com.github._1c_syntax.bsl.languageserver.context.symbol.MethodSymbol;
import com.github._1c_syntax.bsl.languageserver.context.symbol.Symbol;
import com.github._1c_syntax.bsl.languageserver.context.symbol.SymbolTree;
import com.github._1c_syntax.bsl.languageserver.utils.StringUtils;
import com.github._1c_syntax.bsl.languageserver.utils.Trees;
import com.github._1c_syntax.bsl.parser.BSLLexer;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.bsl.parser.BSLParserRuleContext;
import com.github._1c_syntax.bsl.parser.Tokenizer;
import com.github._1c_syntax.mdclasses.mdo.MDObjectBase;
import com.github._1c_syntax.mdclasses.metadata.SupportConfiguration;
import com.github._1c_syntax.mdclasses.metadata.additional.ModuleType;
import com.github._1c_syntax.mdclasses.metadata.additional.SupportVariant;
import com.github._1c_syntax.utils.Absolute;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.SneakyThrows;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.runtime.tree.Tree;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.lsp4j.Range;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.antlr.v4.runtime.Token.DEFAULT_CHANNEL;

public class DocumentContext {

  private final URI uri;
  private final ServerContext context;
  private final Cache<CacheKey, Object> cache;
  private String content;

  public DocumentContext(URI uri, String content, ServerContext context) {
    this.uri = Absolute.uri(uri);
    this.content = content;
    this.context = context;
    this.cache = CacheBuilder.newBuilder()
      .softValues()
      .build();
  }

  public void invalidateCache(CacheKey key){
    this.cache.invalidate(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T getIfPresentInCache(CacheKey key){
    return (T) this.cache.getIfPresent(key);
  }

  private static boolean mustCovered(Tree node) {
    return node instanceof BSLParser.StatementContext
      || node instanceof BSLParser.GlobalMethodCallContext
      || node instanceof BSLParser.Var_nameContext;
  }

  public ServerContext getServerContext() {
    return context;
  }

  @SneakyThrows
  public Tokenizer getTokenizer() {
    requireNonNull(content);
    return (Tokenizer) this.cache.get(CacheKey.Tokenizer, () -> new Tokenizer(content));
  }

  public String getContent() {
    requireNonNull(content);
    return content;
  }

  @SneakyThrows
  public String[] getContentList() {
    return (String[]) this.cache.get(CacheKey.ContentList, this::computeContentList);
  }

  @SneakyThrows
  public BSLParser.FileContext getAst() {
    return (BSLParser.FileContext) this.cache.get(CacheKey.Ast, getTokenizer()::getAst);
  }

  @SneakyThrows
  public SymbolTree getSymbolTree() {
    return (SymbolTree) this.cache.get(CacheKey.SymbolTree, this::computeSymbolTree);
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  public List<Token> getTokens() {
    return (List<Token>) this.cache.get(CacheKey.Tokens, getTokenizer()::getTokens);
  }

  public List<Token> getTokensFromDefaultChannel() {
    return getTokens().stream()
      .filter(token -> token.getChannel() == DEFAULT_CHANNEL)
      .collect(Collectors.toList());
  }

  public List<Token> getComments() {
    return getTokens().stream()
      .filter(token -> token.getType() == BSLLexer.LINE_COMMENT)
      .collect(Collectors.toList());
  }

  public String getText(Range range) {
    return StringUtils.getTextRange(getContentList(), range);
  }

  @SneakyThrows
  public MetricStorage getMetrics() {
    return (MetricStorage) this.cache.get(CacheKey.Metrics, this::computeMetrics);
  }

  public URI getUri() {
    return uri;
  }

  @SneakyThrows
  public FileType getFileType() {
    return (FileType) this.cache.get(CacheKey.FileType, () -> {
      String uriPath = uri.getPath();
      if (uriPath == null) {
        return FileType.BSL;
      }

      return Arrays.stream(FileType.values())
        .filter(Predicate.isEqual(FilenameUtils.getExtension(uriPath).toUpperCase(Locale.ENGLISH)))
        .findAny()
        .orElse(FileType.BSL);
    });
  }

  @SneakyThrows
  public ComplexityData getCognitiveComplexityData() {
    return (ComplexityData) this.cache.get(CacheKey.CognitiveComplexityData, this::computeCognitiveComplexity);
  }

  @SneakyThrows
  public ComplexityData getCyclomaticComplexityData() {
    return (ComplexityData) this.cache.get(CacheKey.CyclomaticComplexityData, this::computeCyclomaticComplexity);
  }

  @SneakyThrows
  public DiagnosticIgnoranceComputer.Data getDiagnosticIgnorance() {
    return (DiagnosticIgnoranceComputer.Data) this.cache.get(CacheKey.DiagnosticIgnorance, this::computeDiagnosticIgnorance);
  }

  @SneakyThrows
  public ModuleType getModuleType() {
    return (ModuleType) this.cache.get(CacheKey.ModuleType, this::computeModuleType);
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  public Map<SupportConfiguration, SupportVariant> getSupportVariants() {
    return (Map<SupportConfiguration, SupportVariant>) this.cache.get(CacheKey.SupportVariants, this::computeSupportVariants);
  }

  public synchronized void rebuild(String content) {
    clear();
    this.content = content;
    this.cache.invalidate(CacheKey.Tokenizer);
  }

  public void clearSecondaryData() {
    this.cache.invalidate(CacheKey.ContentList);
    this.cache.invalidate(CacheKey.Tokens);
    this.cache.invalidate(CacheKey.Ast);
    this.cache.invalidate(CacheKey.Tokenizer);
    this.content = null;

    Optional.ofNullable(this.cache.getIfPresent(CacheKey.SymbolTree))
      .map(SymbolTree.class::cast)
      .ifPresent(symbolTree -> symbolTree.getChildrenFlat().forEach(Symbol::clearParseTreeData));

    this.cache.invalidate(CacheKey.CognitiveComplexityData);
    this.cache.invalidate(CacheKey.CyclomaticComplexityData);
    this.cache.invalidate(CacheKey.Metrics);
    this.cache.invalidate(CacheKey.DiagnosticIgnorance);
  }

  private void clear() {
    clearSecondaryData();
    this.cache.invalidate(CacheKey.SymbolTree);
  }

  private String[] computeContentList() {
    return getContent().split("\n", -1);
  }

  private SymbolTree computeSymbolTree() {
    return new SymbolTreeComputer(this).compute();
  }

  private ModuleType computeModuleType() {
    return context.getConfiguration().getModuleType(uri);
  }

  private Map<SupportConfiguration, SupportVariant> computeSupportVariants() {
    return context.getConfiguration().getModuleSupport(uri);
  }

  private ComplexityData computeCognitiveComplexity() {
    Computer<ComplexityData> cognitiveComplexityComputer = new CognitiveComplexityComputer(this);
    return cognitiveComplexityComputer.compute();
  }

  private ComplexityData computeCyclomaticComplexity() {
    Computer<ComplexityData> cyclomaticComplexityComputer = new CyclomaticComplexityComputer(this);
    return cyclomaticComplexityComputer.compute();
  }

  private MetricStorage computeMetrics() {
    MetricStorage metricsTemp = new MetricStorage();
    final List<MethodSymbol> methodsUnboxed = getSymbolTree().getMethods();

    metricsTemp.setFunctions(Math.toIntExact(methodsUnboxed.stream().filter(MethodSymbol::isFunction).count()));
    metricsTemp.setProcedures(methodsUnboxed.size() - metricsTemp.getFunctions());


    int[] nclocData = getTokensFromDefaultChannel().stream()
      .mapToInt(Token::getLine)
      .distinct()
      .toArray();
    metricsTemp.setNclocData(nclocData);
    metricsTemp.setNcloc(nclocData.length);
    metricsTemp.setCovlocData(computeCovlocData());

    int lines;
    final List<Token> tokensUnboxed = getTokens();
    if (tokensUnboxed.isEmpty()) {
      lines = 0;
    } else {
      lines = tokensUnboxed.get(tokensUnboxed.size() - 1).getLine();
    }
    metricsTemp.setLines(lines);

    int comments = (int) getComments()
      .stream()
      .map(Token::getLine)
      .distinct()
      .count();
    metricsTemp.setComments(comments);

    int statements = Trees.findAllRuleNodes(getAst(), BSLParser.RULE_statement).size();
    metricsTemp.setStatements(statements);

    metricsTemp.setCognitiveComplexity(getCognitiveComplexityData().getFileComplexity());
    metricsTemp.setCyclomaticComplexity(getCyclomaticComplexityData().getFileComplexity());

    return metricsTemp;
  }

  public Optional<MDObjectBase> getMdObject() {
    return Optional.ofNullable(getServerContext().getConfiguration().getModulesByURI().get(getUri()));
  }

  private int[] computeCovlocData() {

    return Trees.getDescendants(getAst()).stream()
      .filter(Predicate.not(TerminalNodeImpl.class::isInstance))
      .filter(DocumentContext::mustCovered)
      .mapToInt(node -> ((BSLParserRuleContext) node).getStart().getLine())
      .distinct()
      .toArray();

  }

  private DiagnosticIgnoranceComputer.Data computeDiagnosticIgnorance() {
    Computer<DiagnosticIgnoranceComputer.Data> diagnosticIgnoranceComputer = new DiagnosticIgnoranceComputer(this);
    return diagnosticIgnoranceComputer.compute();
  }

}