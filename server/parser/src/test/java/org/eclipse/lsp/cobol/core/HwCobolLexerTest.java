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
package org.eclipse.lsp.cobol.core;

import org.eclipse.lsp.cobol.core.hw.CobolLexer;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.lsp.cobol.core.hw.GrammarRule;
import org.eclipse.lsp.cobol.core.hw.Token;
import org.junit.jupiter.api.Test;

/**
 * test hw lexer
 */
class HwCobolLexerTest {

  @Test
  void identificationDivision() {
    CobolLexer lexer = new CobolLexer("Id DIVISION. PROGRAM-ID. Pr1.");
    assertEquals("Id", tokenContent(lexer));
    assertEquals(" ", tokenContent(lexer));
    assertEquals("DIVISION", tokenContent(lexer));
    assertEquals(".", tokenContent(lexer));
    assertEquals(" ", tokenContent(lexer));
    assertEquals("PROGRAM-ID", tokenContent(lexer));
    assertEquals(".", tokenContent(lexer));
    assertEquals(" ", tokenContent(lexer));
    assertEquals("Pr1", tokenContent(lexer));
    assertEquals(".", tokenContent(lexer));
    assertFalse(lexer.hasMore());
  }

  @Test
  void newLine() {
    CobolLexer lexer = new CobolLexer("A \n B");
    assertToken(lexer.forward(GrammarRule.ProgramUnit).get(0), "A", 0, 0, 0);
    assertToken(lexer.forward(GrammarRule.ProgramUnit).get(0), " ", 0, 1, 1);
    assertToken(lexer.forward(GrammarRule.ProgramUnit).get(0), "\n", 0, 2, 2);
    assertToken(lexer.forward(GrammarRule.ProgramUnit).get(0), " ", 1, 0, 3);
    assertToken(lexer.forward(GrammarRule.ProgramUnit).get(0), "B", 1, 1, 4);
    assertFalse(lexer.hasMore());
  }

  @Test
  void peekTest() {
    CobolLexer lexer = new CobolLexer("Aa\n  B");
    assertToken(lexer.peek(GrammarRule.ProgramUnit).get(0), "Aa", 0, 0, 0);
    assertToken(lexer.peek(GrammarRule.ProgramUnit).get(0), "Aa", 0, 0, 0);
    assertToken(lexer.forward(GrammarRule.ProgramUnit).get(0), "Aa", 0, 0, 0);
    assertToken(lexer.peek(GrammarRule.ProgramUnit).get(0), "\n", 0, 2, 2);
    assertToken(lexer.forward(GrammarRule.ProgramUnit).get(0), "\n", 0, 2, 2);
    assertToken(lexer.peek(GrammarRule.ProgramUnit).get(0), "  ", 0, 0, 3);
  }

  private static void assertToken(Token token, String lexeme, int line, int charPos, int index) {
    assertEquals(lexeme, token.getLexeme(), "lexeme");
    assertEquals(line, token.getLine(), "line");
    assertEquals(charPos, token.getStartPositionInLine(), "char in line position");
    assertEquals(index, token.getIndex(), "index");
  }

  private static String tokenContent(CobolLexer lexer) {
    return lexer.forward(GrammarRule.ProgramUnit).get(0).getLexeme();
  }
}
