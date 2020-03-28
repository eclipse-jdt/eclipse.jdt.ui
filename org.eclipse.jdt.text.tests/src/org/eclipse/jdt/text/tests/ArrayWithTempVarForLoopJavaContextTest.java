/*******************************************************************************
 * Copyright (c) 2014, 2020 Yatta Solutions GmbH, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lukas Hanke <hanke@yatta.de> - initial API and implementation - https://bugs.eclipse.org/117215
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jface.text.templates.Template;

/**
 * Tests functionality of the template based for loop completion to iterate over an array, using a
 * temporary variable inside the for loop's body.
 */
public class ArrayWithTempVarForLoopJavaContextTest extends AbstractForLoopJavaContextTest {

	@Override
	protected String getInnerClasses() {
		return NewForLoopJavaContextTest.INNER_CLASS_DECLARATIONS;
	}

	@Override
	protected Template getForLoop() {
		return getTemplate("org.eclipse.jdt.ui.templates.for_temp");
	}

	@Test
	public void simpleArray() throws Exception {
		String template= evaluateTemplateInMethod("void method(Number[] numbers)");
		assertEquals(
				"	for (int i = 0; i < numbers.length; i++) {\n" +
						"		Number number = numbers[i];\n" +
						"		\n" +
						"	}", template);
	}

	@Test
	public void innerClassArray() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner[] inners)");
		assertEquals(
				"	for (int i = 0; i < inners.length; i++) {\n" +
						"		Inner inner = inners[i];\n" +
						"		\n" +
						"	}", template);
	}

	@Test
	public void innerClassParameterized() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner2<Number>[] inners)");
		assertEquals(
				"	for (int i = 0; i < inners.length; i++) {\n" +
						"		Inner2<Number> inner2 = inners[i];\n" +
						"		\n" +
						"	}", template);
	}

	@Test
	public void generic() throws Exception {
		String template= evaluateTemplateInMethod("void <T> method(T[] generics)");
		assertEquals(
				"	for (int i = 0; i < generics.length; i++) {\n" +
						"		T t = generics[i];\n" +
						"		\n" +
						"	}", template);
	}

	@Test
	public void uppderboundGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T extends Number> method(T[] numbers)");
		assertEquals(
				"	for (int i = 0; i < numbers.length; i++) {\n" +
						"		T t = numbers[i];\n" +
						"		\n" +
						"	}", template);
	}

	@Test
	public void lowerboundGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T super Number> method(T[] objects)");
		assertEquals(
				"	for (int i = 0; i < objects.length; i++) {\n" +
						"		T t = objects[i];\n" +
						"		\n" +
						"	}", template);
	}

	@Test
	public void proposalsWithField() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method()", "Number[] numbers");
		assertEquals(
				"	for (int i = 0; i < numbers.length; i++) {\n" +
						"		Number number = numbers[i];\n" +
						"		\n" +
						"	}", template);
	}

	@Test
	public void proposalsWithFieldAndParam() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method(String[] strings)", "Number[] numbers");
		assertEquals(
				"	for (int i = 0; i < strings.length; i++) {\n" +
						"		String string = strings[i];\n" +
						"		\n" +
						"	}", template);
	}

}
