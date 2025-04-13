/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;

public class AutoboxingSemanticHighlightingTest extends AbstractSemanticHighlightingTest {

	@RegisterExtension
	public SemanticHighlightingTestSetup shts=new SemanticHighlightingTestSetup("/SHTest/src/Autoboxing.java");

	@Test
	public void autoboxingHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.AUTOBOXING);
		Position[] expected= new Position[] {
			createPosition(3, 15, 5),
			createPosition(4, 21, 1),
			createPosition(8, 10, 4),
			createPosition(10, 18, 4),
			createPosition(10, 24, 1),
			createPosition(12, 10, 4),
			createPosition(12, 16, 3),
			createPosition(15, 14, 2),
			createPosition(16, 14, 1),
			createPosition(19, 14, 2),
			createPosition(20, 14, 1),
			createPosition(22, 11, 5),
			createPosition(24, 11, 5),
			createPosition(26, 18, 5),
			createPosition(26, 30, 4),
			createPosition(26, 42, 5),
			createPosition(28, 21, 5),
			createPosition(28, 37, 5),
			createPosition(29, 21, 5),
			createPosition(29, 29, 1),
			createPosition(30, 21, 1),
			createPosition(30, 25, 5),
			createPosition(32, 29, 4),
			createPosition(34, 18, 4),
			createPosition(35, 14, 4),
			createPosition(37, 14, 5),
			createPosition(37, 20, 4),
			createPosition(39, 24, 4),
			createPosition(39, 38, 5),
			createPosition(39, 53, 4),
			createPosition(43, 17, 4),
			createPosition(43, 28, 5),
			createPosition(45, 12, 4),
			createPosition(46, 12, 4),
			createPosition(47, 15, 2),
			createPosition(49, 14, 3),
			createPosition(51, 21, 6),
		};

		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}

}
