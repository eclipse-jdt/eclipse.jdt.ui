/*******************************************************************************
 * Copyright (c) 2024, 2026 Broadcom Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alex Boyko (Broadcom Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.semantictokens;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.eclipse.jdt.text.tests.AbstractSemanticHighlightingTest;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;

public class SemanticTokensProviderTest extends AbstractSemanticHighlightingTest {

	@RegisterExtension
	public SemanticHighlightingTestSetup shts= new SemanticHighlightingTestSetup( "/SHTest/src/STTest.java");

	@Test
	public void contributedHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.CLASS);
		setUpSemanticHighlighting(SemanticHighlightings.NUMBER);
		setUpSemanticHighlighting(SemanticHighlightings.LOCAL_VARIABLE);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(0, 6, 1),
				createPosition(1, 1, 6),
				createPosition(1, 20, 6),
				createPosition(1, 27, 1),
				createPosition(1, 29, 4),
				createPosition(1, 34, 1),
				createPosition(1, 36, 5),
				createPosition(1, 42, 1),
				createPosition(1, 44, 2),
				createPosition(1, 47, 3),
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void disabledHighlightingNoError() throws Exception {
		List<IStatus> errors= new ArrayList<>();
		ILogListener logListener= (status, plugin) -> {
			if (status.getSeverity() == IStatus.ERROR) {
				errors.add(status);
			}
		};
		Platform.addLogListener(logListener);
		try {
			// CLASS is disabled - T in the SQL should not produce a position and no error should be logged
			setUpSemanticHighlighting(SemanticHighlightings.NUMBER);
			setUpSemanticHighlighting(SemanticHighlightings.LOCAL_VARIABLE);
			Position[] actual= getSemanticHighlightingPositions();
			Position[] expected= new Position[] {
					createPosition(1, 20, 6),      // SELECT - KEYWORD (syntax, always enabled)
					createPosition(1, 27, 1),      // * - OPERATOR (syntax, always enabled)
					createPosition(1, 29, 4),      // FROM - KEYWORD (syntax, always enabled)
					createPosition(1, 36, 5),      // WHERE - KEYWORD (syntax, always enabled)
					createPosition(1, 42, 1),      // a - LOCAL_VARIABLE (enabled)
					createPosition(1, 44, 2),      // == - OPERATOR (syntax, always enabled)
					createPosition(1, 47, 3),      // 567 - NUMBER (enabled)
			};
			assertEqualPositions(expected, actual);

			List<IStatus> semanticErrors= errors.stream()
					.filter(s -> s.getMessage().contains("Cannot find semantic highlighting")) //$NON-NLS-1$
					.toList();
			assertTrue(semanticErrors.isEmpty(), () -> "Unexpected error log for disabled highlighting: " + semanticErrors); //$NON-NLS-1$
		} finally {
			Platform.removeLogListener(logListener);
		}
	}

	@Test
	public void enabledHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.CLASS);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(0, 6, 1),       // A - CLASS (built-in)
				createPosition(1, 1, 6),       // String - CLASS (built-in)
				createPosition(1, 20, 6),      // SELECT - KEYWORD (syntax, always enabled)
				createPosition(1, 27, 1),      // * - OPERATOR (syntax, always enabled)
				createPosition(1, 29, 4),      // FROM - KEYWORD (syntax, always enabled)
				createPosition(1, 34, 1),      // T - CLASS (contributed, enabled)
				createPosition(1, 36, 5),      // WHERE - KEYWORD (syntax, always enabled)
				createPosition(1, 44, 2),      // == - OPERATOR (syntax, always enabled)
		};
		assertEqualPositions(expected, actual);
	}

}
