/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lukas Hanke <hanke@yatta.de> - [templates][content assist] Content assist for 'for' loop should suggest member variables - https://bugs.eclipse.org/117215 
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.templates.Template;

/**
 * Tests the automatic bracket insertion feature of the CUEditor. Also tests
 * linked mode along the way.
 *
 * @since 3.1
 */
public class NewForLoopJavaContextTest extends AbstractForLoopJavaContextTest {

	public static final String INNER_CLASS_DECLARATIONS= "	private static class Inner {\n" +
			"	}\n" +
			"	\n" +
			"	private static abstract class Inner2<E> implements Iterable<E> {\n" +
			"	}\n" +
			"	\n" +
			"	private static abstract class Inner3 implements Iterable<Serializable> {\n" +
			"	}\n" +
			"	\n" +
			"	private static abstract class Inner4<T> implements Iterable<String> {\n" +
			"	}\n" +
			"	\n" +
			"	private static abstract class Transi1<T extends Collection> implements Iterable<T> {\n" +
			"	}\n" +
			"	\n" +
			"	private static abstract class Transi2<T extends List> extends Transi1<T> {\n" +
			"	}\n" +
			"	\n";

	public static Test suite() {
		return new TestSuite(NewForLoopJavaContextTest.class);
	}

	@Override
	protected String getInnerClasses() {
		return INNER_CLASS_DECLARATIONS;
	}

	@Override
	protected Template getForLoop() {
		return getTemplate("org.eclipse.jdt.ui.templates.for_iterable");
	}

	public void testArray() throws Exception {
		String template= evaluateTemplateInMethod("void method(Number[] array)");
		assertEquals(
				"	for (Number number : array) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testInnerArray() throws Exception {
		String template= evaluateTemplateInMethod("void array(Inner[] array)");
		assertEquals(
				"	for (Inner inner : array) {\n" +
				"		\n" +
				"	}", template);
	}


	public void testSuperList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? super Number> list)");
		assertEquals(
				"	for (Object object : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testSuperArrayList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? super Number[]> list)");
		assertEquals(
				"	for (Object object : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testMultiList() throws Exception {
		String template= evaluateTemplateInMethod("<T extends Comparable<T> & Iterable<? extends Number>> void method(List<T> list)");
		assertEquals(
				"	for (T t : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testInnerIterable() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner2<Number> inner)");
		assertEquals(
				"	for (Number number : inner) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testInnerIterable2() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner3 inner)");
		assertEquals(
				"	for (Serializable serializable : inner) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testInnerMixedParameters() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner4<Number> inner)");
		assertEquals(
				"	for (String string : inner) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testGenericList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<E> list)");
		assertEquals(
				"	for (E e : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testWildcardList() throws Exception {
		String template= evaluateTemplateInMethod("void method(Transi1<?> transi)");
		assertEquals(
				"	for (Collection collection : transi) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testConcreteList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<String> list)");
		assertEquals(
				"	for (String string : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testUpperboundList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? extends Number> list)");
		assertEquals(
				"	for (Number number : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testProposeField() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method()", "Collection<String> strings");
		assertEquals(
				"	for (String string : strings) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testProposeParamWithField() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method(List<Number> numbers)", "Collection<String> strings");
		assertEquals(
				"	for (Number number : numbers) {\n" +
				"		\n" +
				"	}", template);
	}

}
