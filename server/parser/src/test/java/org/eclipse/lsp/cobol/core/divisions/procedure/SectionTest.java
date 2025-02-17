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
package org.eclipse.lsp.cobol.core.divisions.procedure;

import org.eclipse.lsp.cobol.core.cst.ProgramUnit;
import org.eclipse.lsp.cobol.core.cst.procedure.Paragraph;
import org.eclipse.lsp.cobol.core.cst.procedure.ProcedureDivision;
import org.eclipse.lsp.cobol.core.cst.procedure.Section;
import org.eclipse.lsp.cobol.core.hw.CobolLexer;
import org.eclipse.lsp.cobol.core.hw.CobolParser;
import org.eclipse.lsp.cobol.core.hw.ParseResult;
import org.eclipse.lsp.cobol.core.hw.ParserSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that HW parser can understand sections.
 */
class SectionTest {
  @Test
  void test() {
    String source = "IDENTIFICATION DIVISION.\n"
            + "       PROGRAM-ID.  SECTST.\n"
            + "       PROCEDURE DIVISION.\n"
            + "           DISPLAY 'OUT'.\n"
            + "           GO TO PARAG1.\n"
            + "       SECT1 SECTION.\n"
            + "           DISPLAY 'SECT1'.\n"
            + "       PARAG1.\n"
            + "           DISPLAY 'PARAG1'.\n"
            + "           PERFORM PARAG2 OF SECT2.\n"
            + "       SECT2 SECTION.\n"
            + "       PARAG2.\n"
            + "           DISPLAY 'PARAG2'.";
    ParseResult parseResult = new CobolParser(new CobolLexer(source), new ParserSettings()).parse();
    assertTrue(parseResult.getDiagnostics().isEmpty());
    ProgramUnit pu = (ProgramUnit) parseResult.getSourceUnit().getChildren().get(0);
    ProcedureDivision pd = (ProcedureDivision) pu.getChildren().get(1);
    assertEquals(2, pd.getChildren().stream().filter(Paragraph.class::isInstance).count());
    assertEquals(2, pd.getChildren().stream().filter(Section.class::isInstance).count());
    // TODO: check if everything is in the place

    assertEquals(source, parseResult.getSourceUnit().toText());
  }
}
