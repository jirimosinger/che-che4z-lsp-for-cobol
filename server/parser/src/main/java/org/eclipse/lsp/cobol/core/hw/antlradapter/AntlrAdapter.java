/*
 * Copyright (c) 2024 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Broadcom, Inc. - initial API and implementation
 *    DAF Trucks NV – implementation of DaCo COBOL statements
 *    and DAF development standards
 *
 */
package org.eclipse.lsp.cobol.core.hw.antlradapter;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.eclipse.lsp.cobol.core.*;
import org.eclipse.lsp.cobol.core.CobolParser;
import org.eclipse.lsp.cobol.core.cst.*;
import org.eclipse.lsp.cobol.core.cst.base.CstNode;
import org.eclipse.lsp.cobol.core.cst.IdentificationDivision;
import org.eclipse.lsp.cobol.core.cst.procedure.ProcedureDivision;
import org.eclipse.lsp.cobol.core.hw.TokenType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.lsp.cobol.core.hw.antlradapter.Utils.*;

/**
 * Reconstruct the AST
 */
public class AntlrAdapter {
  public static final int INT = 6;
  private final BaseErrorListener errorListener;
  private final DefaultErrorStrategy errorStrategy;
  private final ParseTreeListener treeListener;
  private CharStream charStream;

  public AntlrAdapter(BaseErrorListener errorListener, DefaultErrorStrategy errorStrategy, ParseTreeListener treeListener) {
    this.errorListener = errorListener;
    this.errorStrategy = errorStrategy;
    this.treeListener = treeListener;
  }

  /**
   * Convert HW source unit to ANTLR Start rule
   *
   * @param su root of CST
   * @return ANTLR AST
   */
  public CobolParser.StartRuleContext sourceUnitToStartRule(SourceUnit su) {
    this.charStream = CharStreams.fromString(su.toText());
    traversePrograms(su, p -> replaceWithAntlr(p, convertProgramNode(p)));
    CobolParser.StartRuleContext startRuleContext = convertSourceUnit(su);
    startRuleContext.addChild(new TerminalNodeImpl(new CommonToken(Token.EOF, "")));
    return startRuleContext;
  }

  private static CobolParser.ProgramUnitContext replaceWithAntlr(ProgramUnit p, CobolParser.ProgramUnitContext result) {
    org.eclipse.lsp.cobol.core.hw.Token token = Utils.findStartToken(p).get();
    int startLine = token.getLine();
    int startPos = token.getStartPositionInLine();
    char[] chars = p.toText().toCharArray();
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] != '\n') {
        chars[i] = ' ';
      }
    }
    String lexeme = new String(chars);
    p.getChildren().clear();
    AntlrAdapted antlr = new AntlrAdapted(result);
    antlr.getChildren().add(new org.eclipse.lsp.cobol.core.hw.Token(lexeme, startLine, startPos, token.getIndex(), TokenType.WHITESPACE));
    p.getChildren().add(antlr);
    return result;
  }

  private void traversePrograms(CstNode node, Function<ProgramUnit, CobolParser.ProgramUnitContext> processor) {
    node.getChildren().forEach(s -> traversePrograms(s, processor));
    if (node instanceof ProgramUnit) {
      processor.apply((ProgramUnit) node);
    }
  }

  private CobolParser.StartRuleContext convertSourceUnit(SourceUnit cstNode) {
    CobolParser.StartRuleContext start = new CobolParser.StartRuleContext(null, 0);
    start.children = new ArrayList<>();
    start.start = toAntlrToken(findStartToken(cstNode, true).get(), charStream);
    start.stop = toAntlrToken(findStopToken(cstNode, true).get(), charStream);
    CobolParser.CompilationUnitContext compilationUnit = new CobolParser.CompilationUnitContext(start, 0);
    initNode(cstNode, compilationUnit, charStream);
    start.addChild(compilationUnit);
    processChildNodes(cstNode, compilationUnit);
    return start;
  }

  private AntlrAdapted findAntlrNode(CstNode cstNode) {
    for (CstNode node : cstNode.getChildren()) {
      if (node instanceof AntlrAdapted) {
        return (AntlrAdapted) node;
      } else {
        AntlrAdapted a = findAntlrNode(node);
        if (a != null) {
          return a;
        }
      }
    }
    return null;
  }

  private CobolParser.ProgramUnitContext convertProgramNode(CstNode programUnit) {
    CobolParser.ProgramUnitContext program = new CobolParser.ProgramUnitContext(null, 0);
    initNode(programUnit, program, charStream);
    processChildNodes(programUnit, program);
    Optional<List<org.eclipse.lsp.cobol.core.hw.Token>> endProgramName = getEndProgramName(programUnit);
    if (endProgramName.isPresent()) {
      CobolParser.EndProgramStatementContext endProgramStatementContext = new CobolParser.EndProgramStatementContext(program, 0);
      endProgramStatementContext.start = toAntlrToken(findStartToken(programUnit).get(), charStream);
      endProgramStatementContext.stop = toAntlrToken(findStopToken(programUnit).get(), charStream);
      CobolParser.ProgramNameContext nameContext = new CobolParser.ProgramNameContext(endProgramStatementContext, 0);
      org.eclipse.lsp.cobol.core.hw.Token nameToken = endProgramName.get().get(0);
      nameContext.start = toAntlrToken(nameToken, charStream);
      nameContext.stop = toAntlrToken((endProgramName.get().get(endProgramName.get().size() - 1)), charStream);
      endProgramStatementContext.children = new ArrayList<>();
      endProgramStatementContext.children.add(nameContext);
      nameContext.addChild(new TerminalNodeImpl(toAntlrToken(nameToken, charStream)));
      program.addChild(endProgramStatementContext);
    }
    return program;
  }

  private ParserRuleContext convertNode(CstNode cstNode) {
    if (cstNode instanceof ProgramUnit) {
      // All programs should be adapted to this point
      return ((AntlrAdapted) cstNode.getChildren().get(0)).getRuleContext();
    } else if (cstNode instanceof DataDivision) {
      return antlrDataDivisionParser(cstNode).dataDivision();
    } else if (cstNode instanceof IdentificationDivision) {
      return antlrIdDivisionParser(cstNode).identificationDivision();
    } else if (cstNode instanceof EnvironmentDivision) {
      return antlrParser(cstNode).environmentDivision();
    } else if (cstNode instanceof ProcedureDivision) {
      ProcedureDivisionAntlrAdapter adapter = new ProcedureDivisionAntlrAdapter(charStream,
              errorListener,
              errorStrategy,
              treeListener);
      return adapter.processProcedureDivisionContext((ProcedureDivision) cstNode);
    } else {
      return null;
    }
  }

  void processChildNodes(CstNode cstNode, ParserRuleContext parent) {
    parent.children = new ArrayList<>();
    for (CstNode child : cstNode.getChildren()) {
      ParserRuleContext ruleContext = convertNode(child);
      if (ruleContext == null) {
        continue;
      }
      parent.addChild(ruleContext);
    }
  }

  private CobolIdentificationDivisionParser antlrIdDivisionParser(CstNode node) {
    org.eclipse.lsp.cobol.core.hw.Token startToken = findStartToken(node).get();
    String input = generatePrefix(charStream, startToken) + node.toText();
    CobolIdentificationDivisionLexer antlrLexer = new CobolIdentificationDivisionLexer(CharStreams.fromString(input));
    antlrLexer.removeErrorListeners();
    antlrLexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(antlrLexer);
    CobolIdentificationDivisionParser antlrParser = new CobolIdentificationDivisionParser(tokens);
    antlrParser.removeErrorListeners();
    antlrParser.addErrorListener(errorListener);
    antlrParser.setErrorHandler(errorStrategy);
    antlrParser.addParseListener(treeListener);
    return antlrParser;
  }

  private CobolDataDivisionParser antlrDataDivisionParser(CstNode node) {
    org.eclipse.lsp.cobol.core.hw.Token startToken = findStartToken(node).get();
    String input = generatePrefix(charStream, startToken) + node.toText();
    CobolDataDivisionLexer antlrLexer = new CobolDataDivisionLexer(CharStreams.fromString(input));
    antlrLexer.removeErrorListeners();
    antlrLexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(antlrLexer);
    CobolDataDivisionParser antlrParser = new CobolDataDivisionParser(tokens);
    antlrParser.removeErrorListeners();
    antlrParser.addErrorListener(errorListener);
    antlrParser.setErrorHandler(errorStrategy);
    antlrParser.addParseListener(treeListener);
    return antlrParser;
  }

  private CobolParser antlrParser(CstNode node) {
    org.eclipse.lsp.cobol.core.hw.Token startToken = findStartToken(node).get();
    String input = generatePrefix(charStream, startToken) + node.toText();
    org.eclipse.lsp.cobol.core.CobolLexer antlrLexer = new org.eclipse.lsp.cobol.core.CobolLexer(CharStreams.fromString(input));
    antlrLexer.removeErrorListeners();
    antlrLexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(antlrLexer);
    org.eclipse.lsp.cobol.core.CobolParser antlrParser = new org.eclipse.lsp.cobol.core.CobolParser(tokens);
    antlrParser.removeErrorListeners();
    antlrParser.addErrorListener(errorListener);
    antlrParser.setErrorHandler(errorStrategy);
    antlrParser.addParseListener(treeListener);
    return antlrParser;
  }

  /**
   * Collect tokens
   *
   * @param su CST root
   * @return ANTLR token stream
   */
  public CommonTokenStream adaptTokens(SourceUnit su) {
    List<CstNode> tokens = new ArrayList<>();
    collectTokens(su, tokens);
    CommonTokenStream commonTokenStream = new CommonTokenStream(new ListTokenSource(tokens.stream()
            .filter(t -> ((org.eclipse.lsp.cobol.core.hw.Token) t).getType() != TokenType.WHITESPACE)
            .map(org.eclipse.lsp.cobol.core.hw.Token.class::cast)
            .map(token -> Utils.toAntlrToken(token, charStream)).collect(Collectors.toList())));
    commonTokenStream.fill();
    return commonTokenStream;
  }

  private void collectTokens(CstNode su, List<CstNode> result) {
    if (su instanceof org.eclipse.lsp.cobol.core.hw.Token) {
      result.add(su);
    }
    for (CstNode node : su.getChildren()) {
      collectTokens(node, result);
    }
  }

  static Optional<List<org.eclipse.lsp.cobol.core.hw.Token>> getEndProgramName(CstNode cstNode) {
    List<org.eclipse.lsp.cobol.core.hw.Token> result = new ArrayList<>();
    if (!(cstNode instanceof ProgramUnit)) {
      return Optional.empty();
    }
    boolean nextName = false;
    int skip = 1;
    for (CstNode n : cstNode.getChildren()) {
      if (!(n instanceof org.eclipse.lsp.cobol.core.hw.Token) || ((org.eclipse.lsp.cobol.core.hw.Token) n).getType() == TokenType.WHITESPACE) {
        continue;
      }
      if (nextName) {
        if (skip == 0) {
          result.add((org.eclipse.lsp.cobol.core.hw.Token) n);
        } else {
          skip--;
        }
      }
      if ("END".equalsIgnoreCase(((org.eclipse.lsp.cobol.core.hw.Token) n).getLexeme())) {
        nextName = true;
      }
    }
    return result.isEmpty() ? Optional.empty() : Optional.of(result);
  }
}
