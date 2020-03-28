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
 *     Björn Michael <b.michael@gmx.de> - [syntax highlighting] Syntax coloring for abstract classes - https://bugs.eclipse.org/331311
 *     Björn Michael <b.michael@gmx.de> - [syntax highlighting] Add highlight for inherited fields - https://bugs.eclipse.org/348368
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;

public class SemanticHighlightingTest extends AbstractSemanticHighlightingTest {
	@Rule
	public SemanticHighlightingTestSetup shts= new SemanticHighlightingTestSetup( "/SHTest/src/SHTest.java");

	@Test
	public void deprecatedMemberHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.DEPRECATED_MEMBER);
		Position[] expected= new Position[] {
				createPosition(0, 12, 13),
				createPosition(22, 5, 15),
				createPosition(24, 1, 13),
				createPosition(24, 15, 16),
				createPosition(25, 2, 15),
				createPosition(26, 2, 16),
				createPosition(27, 10, 10),
				createPosition(30, 7, 10),
				createPosition(30, 26, 13),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void staticFinalFieldHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.STATIC_FINAL_FIELD);
		Position[] expected= new Position[] {
				createPosition(6, 18, 16),
				createPosition(35, 37, 16),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void staticFieldHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.STATIC_FIELD);
		Position[] expected= new Position[] {
				createPosition(4, 12, 11),
				createPosition(6, 18, 16),
				createPosition(33, 32, 11),
				createPosition(35, 37, 16),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void fieldHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.FIELD);
		Position[] expected= new Position[] {
				createPosition( 3,  5,  5),
				createPosition( 4, 12, 11),
				createPosition( 5, 11, 10),
				createPosition( 6, 18, 16),
				createPosition(22,  5, 15),
				createPosition(25,  2, 15),
				createPosition(31,  9,  6),
				createPosition(32,  6, 11),
				createPosition(32, 31,  5),
				createPosition(33,  6, 17),
				createPosition(33, 32, 11),
				createPosition(34,  6, 16),
				createPosition(34, 36, 10),
				createPosition(35,  6, 22),
				createPosition(35, 37, 16),
				createPosition(48,  6, 14),
				createPosition(48, 22,  5),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void inheritedFieldHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.INHERITED_FIELD);
		Position[] expected= new Position[] {
				createPosition(48, 22,  5),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void numberHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.NUMBER);
		Position[] expected= new Position[] {
				createPosition(5, 23, 1),
				createPosition(6, 36, 1),
				createPosition(8, 21, 1),
				createPosition(13, 19, 1),
				createPosition(13, 31, 2),
			};

		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void methodDeclarationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.METHOD_DECLARATION);
		Position[] expected= new Position[] {
				createPosition(7, 6, 6),
				createPosition(19, 13, 12),
				createPosition(20, 15, 14),
				createPosition(24, 15, 16),
				createPosition(40, 4, 6),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void staticMethodInvocationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.STATIC_METHOD_INVOCATION);
		Position[] expected= new Position[] {
				createPosition(10, 2, 12),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	/*
	 * [syntax highlighting] 'Abstract Method Invocation' highlighting also matches declaration
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=73353
	 */
	@Test
	public void abstractMethodInvocationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.ABSTRACT_METHOD_INVOCATION);
		Position[] expected= new Position[] {
				createPosition(11, 2, 14),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void inheritedMethodInvocationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.INHERITED_METHOD_INVOCATION);
		Position[] expected= new Position[] {
				createPosition(12, 2, 8),
				createPosition(15, 17, 8),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void localVariableDeclarationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.LOCAL_VARIABLE_DECLARATION);
		Position[] expected= new Position[] {
				createPosition(7, 17, 5),
				createPosition(8, 6, 5),
				createPosition(13, 11, 6),
				createPosition(14, 26, 6),
				createPosition(41, 16, 4),
				createPosition(42, 20, 13),
				createPosition(43, 15, 7),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void localVariableHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.LOCAL_VARIABLE);
		Position[] expected= new Position[] {
				createPosition(7, 17, 5),
				createPosition(8, 6, 5),
				createPosition(8, 13, 5),
				createPosition(9, 2, 5),
				createPosition(13, 11, 6),
				createPosition(13, 22, 6),
				createPosition(13, 35, 6),
				createPosition(14, 26, 6),
				createPosition(15, 3, 5),
				createPosition(15, 10, 6),
				createPosition(16, 3, 6),
				createPosition(41, 16, 4),
				createPosition(42, 20, 13),
				createPosition(43, 15, 7),
		};
		Position[] actual= getSemanticHighlightingPositions();
		assertEqualPositions(expected, actual);
	}

	@Test
	public void parameterVariableHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.PARAMETER_VARIABLE);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(7, 17, 5),
				createPosition(8, 13, 5),
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void annotationElementHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.ANNOTATION_ELEMENT_REFERENCE);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(38, 19, 5),
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void typeParameterHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.TYPE_VARIABLE);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(39, 15, 1),
				createPosition(40, 2, 1),
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void typeArgumentHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.TYPE_ARGUMENT);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(41, 8, 6),
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void interfaceHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.INTERFACE);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(41, 3, 4),
				createPosition(42, 3, 16), // annotations are also interfaces
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void enumHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.ENUM);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(43, 3, 11),
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void annotationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.ANNOTATION);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(38, 2, 16),
				createPosition(42, 3, 16),
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void abstractClassHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.ABSTRACT_CLASS);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition( 2, 15,  6),
				createPosition(31,  2,  6),
				createPosition(31, 17,  6),
				createPosition(32, 19,  6),
				createPosition(33, 25,  6),
				createPosition(34, 24,  6),
				createPosition(35, 30,  6),
				createPosition(39, 25,  6),
				createPosition(47, 23, 14),
				createPosition(47, 46,  6),
		};
		assertEqualPositions(expected, actual);
	}

	@Test
	public void classHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.CLASS);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition( 2, 15,  6),
				createPosition(14, 16,  9),
				createPosition(24,  1, 13),
				createPosition(27, 10, 10),
				createPosition(30,  7, 10),
				createPosition(30, 26, 13),
				createPosition(31,  2,  6),
				createPosition(31, 17,  6),
				createPosition(32, 19,  6),
				createPosition(33, 25,  6),
				createPosition(34, 24,  6),
				createPosition(35, 30,  6),
				createPosition(39,  7,  7),
				createPosition(39, 25,  6),
				createPosition(41,  8,  6),
				createPosition(47, 23, 14),
				createPosition(47, 46,  6),
		};
		assertEqualPositions(expected, actual);
	}
}
