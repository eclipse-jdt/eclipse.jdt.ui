/*******************************************************************************
 * Copyright (c) 2024 GK Software SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingsCore;

public class Java23SemanticHighlightingTest extends AbstractSemanticHighlightingTest {

	@Rule
	public SemanticHighlightingTestSetup shts= new SemanticHighlightingTestSetup( "/SHTest/src/Java23.java");

	@Before
	public void updateCompliance() {
		shts.updateCompliance("23", true);
	}

	@Test
	public void restrictedKeywordHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightingsCore.RESTRICTED_KEYWORDS);
		Position[] expected= new Position[] {
				createPosition(0, 7, 6), // module (import modifier)
				createPosition(3, 1, 6), // sealed
				createPosition(3, 8, 6), // record
				createPosition(6, 1, 10),// non-sealed
				createPosition(8, 2, 3), // var
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}
}
