/*******************************************************************************
 * Copyright (c) 2014 Yatta Solutions GmbH, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lukas Hanke <hanke@yatta.de> - initial API and implementation - https://bugs.eclipse.org/117215
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.templates.Template;

/**
 * Tests functionality of the template based for loop completion to iterate over an array, using a
 * temporary variable inside the for loop's body.
 */
public class ArrayWithTempVarForLoopJavaContextTest extends AbstractForLoopJavaContextTest {

	public static Test suite() {
		return new TestSuite(ArrayWithTempVarForLoopJavaContextTest.class);
	}

	@Override
	protected String getInnerClasses() {
		return NewForLoopJavaContextTest.INNER_CLASS_DECLARATIONS;
	}

	@Override
	protected Template getForLoop() {
		return getTemplate("org.eclipse.jdt.ui.templates.for_temp");
	}

	public void testSimpleArray() throws Exception {
		String template= evaluateTemplateInMethod("void method(Number[] numbers)");
		assertEquals(
				"	for (int i = 0; i < numbers.length; i++) {\n" +
						"		Number number = numbers[i];\n" +
						"		\n" +
						"	}", template);
	}

	public void testInnerClassArray() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner[] inners)");
		assertEquals(
				"	for (int i = 0; i < inners.length; i++) {\n" +
						"		Inner inner = inners[i];\n" +
						"		\n" +
						"	}", template);
	}

	public void testInnerClassParameterized() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner2<Number>[] inners)");
		assertEquals(
				"	for (int i = 0; i < inners.length; i++) {\n" +
						"		Inner2<Number> inner2 = inners[i];\n" +
						"		\n" +
						"	}", template);
	}

	public void testGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T> method(T[] generics)");
		assertEquals(
				"	for (int i = 0; i < generics.length; i++) {\n" +
						"		T t = generics[i];\n" +
						"		\n" +
						"	}", template);
	}

	public void testUppderboundGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T extends Number> method(T[] numbers)");
		assertEquals(
				"	for (int i = 0; i < numbers.length; i++) {\n" +
						"		T t = numbers[i];\n" +
						"		\n" +
						"	}", template);
	}

	public void testLowerboundGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T super Number> method(T[] objects)");
		assertEquals(
				"	for (int i = 0; i < objects.length; i++) {\n" +
						"		T t = objects[i];\n" +
						"		\n" +
						"	}", template);
	}

	public void testProposalsWithField() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method()", "Number[] numbers");
		assertEquals(
				"	for (int i = 0; i < numbers.length; i++) {\n" +
						"		Number number = numbers[i];\n" +
						"		\n" +
						"	}", template);
	}

	public void testProposalsWithFieldAndParam() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method(String[] strings)", "Number[] numbers");
		assertEquals(
				"	for (int i = 0; i < strings.length; i++) {\n" +
						"		String string = strings[i];\n" +
						"		\n" +
						"	}", template);
	}

}
