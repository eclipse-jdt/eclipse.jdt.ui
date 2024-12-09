/*******************************************************************************
 * Copyright (c) 2024 Broadcom Inc. and others.
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

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.text.tests.AbstractSemanticHighlightingTest;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;

public class SemanticTokensProviderTest extends AbstractSemanticHighlightingTest {

	@Rule
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


}
