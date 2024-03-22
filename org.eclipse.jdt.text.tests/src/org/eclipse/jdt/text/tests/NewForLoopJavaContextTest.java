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
 *     Lukas Hanke <hanke@yatta.de> - [templates][content assist] Content assist for 'for' loop should suggest member variables - https://bugs.eclipse.org/117215
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jface.text.templates.Template;

/**
 * Tests the automatic bracket insertion feature of the CUEditor. Also tests
 * linked mode along the way.
 *
 * @since 3.1
 */
public class NewForLoopJavaContextTest extends AbstractForLoopJavaContextTest {
	public static final String INNER_CLASS_DECLARATIONS= """
			private static class Inner {
			}
		\t
			private static abstract class Inner2<E> implements Iterable<E> {
			}
		\t
			private static abstract class Inner3 implements Iterable<Serializable> {
			}
		\t
			private static abstract class Inner4<T> implements Iterable<String> {
			}
		\t
			private static abstract class Transi1<T extends Collection> implements Iterable<T> {
			}
		\t
			private static abstract class Transi2<T extends List> extends Transi1<T> {
			}
		\t
		""";

	@Override
	protected String getInnerClasses() {
		return INNER_CLASS_DECLARATIONS;
	}

	@Override
	protected Template getForLoop() {
		return getTemplate("org.eclipse.jdt.ui.templates.for_iterable");
	}

	@Test
	public void array() throws Exception {
		String template= evaluateTemplateInMethod("void method(Number[] array)");
		assertEquals(
				"""
						for (Number number : array) {
						\t
						}\
					""", template);
	}

	@Test
	public void innerArray() throws Exception {
		String template= evaluateTemplateInMethod("void array(Inner[] array)");
		assertEquals(
				"""
						for (Inner inner : array) {
						\t
						}\
					""", template);
	}


	@Test
	public void superList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? super Number> list)");
		assertEquals(
				"""
						for (Object object : list) {
						\t
						}\
					""", template);
	}

	@Test
	public void superArrayList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? super Number[]> list)");
		assertEquals(
				"""
						for (Object object : list) {
						\t
						}\
					""", template);
	}

	@Test
	public void multiList() throws Exception {
		String template= evaluateTemplateInMethod("<T extends Comparable<T> & Iterable<? extends Number>> void method(List<T> list)");
		assertEquals(
				"""
						for (T t : list) {
						\t
						}\
					""", template);
	}

	@Test
	public void innerIterable() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner2<Number> inner)");
		assertEquals(
				"""
						for (Number number : inner) {
						\t
						}\
					""", template);
	}

	@Test
	public void innerIterable2() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner3 inner)");
		assertEquals(
				"""
						for (Serializable serializable : inner) {
						\t
						}\
					""", template);
	}

	@Test
	public void innerMixedParameters() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner4<Number> inner)");
		assertEquals(
				"""
						for (String string : inner) {
						\t
						}\
					""", template);
	}

	@Test
	public void genericList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<E> list)");
		assertEquals(
				"""
						for (E e : list) {
						\t
						}\
					""", template);
	}

	@Test
	public void wildcardList() throws Exception {
		String template= evaluateTemplateInMethod("void method(Transi1<?> transi)");
		assertEquals(
				"""
						for (Collection collection : transi) {
						\t
						}\
					""", template);
	}

	@Test
	public void concreteList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<String> list)");
		assertEquals(
				"""
						for (String string : list) {
						\t
						}\
					""", template);
	}

	@Test
	public void upperboundList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? extends Number> list)");
		assertEquals(
				"""
						for (Number number : list) {
						\t
						}\
					""", template);
	}

	@Test
	public void proposeField() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method()", "Collection<String> strings");
		assertEquals(
				"""
						for (String string : strings) {
						\t
						}\
					""", template);
	}

	@Test
	public void proposeParamWithField() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method(List<Number> numbers)", "Collection<String> strings");
		assertEquals(
				"""
						for (Number number : numbers) {
						\t
						}\
					""", template);
	}

}
