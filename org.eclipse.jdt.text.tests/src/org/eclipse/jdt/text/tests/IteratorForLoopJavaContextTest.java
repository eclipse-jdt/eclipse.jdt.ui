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

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import org.eclipse.jface.text.templates.Template;

/**
 * Tests functionality of the template based for loop completion to iterate over a
 * {@link Collection}, using an {@link Iterator} variable to fetch collection elements.
 */
public class IteratorForLoopJavaContextTest extends AbstractForLoopJavaContextTest {

	public static final String INNER_CLASS_DECLARATIONS= """
			private static abstract class InnerCollection<E> implements Collection<E> {
			}
		\t
			private static abstract class InnerCollection2<E> implements Collection<Serializable> {
			}
		\t
			private static abstract class InnerCollection3<E> implements Collection<String> {
			}
		\t
			private static abstract class TransiCollection<E> implements Collection<E> {
			}
		\t
		""";

	@Override
	protected String getInnerClasses() {
		return INNER_CLASS_DECLARATIONS;
	}

	@Override
	protected Template getForLoop() {
		return getTemplate("org.eclipse.jdt.ui.templates.for_collection");
	}

	@Test
	public void collection() throws Exception {
		String template= evaluateTemplateInMethod("void method(Collection<Number> numbers)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {
							Number number = (Number) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void innerParameterized() throws Exception {
		String template= evaluateTemplateInMethod("void method(InnerCollection<Number> numbers)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {
							Number number = (Number) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void innerFixTypedInterface() throws Exception {
		String template= evaluateTemplateInMethod("void method(InnerCollection2 numbers)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {
							Serializable serializable = (Serializable) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void innerWithMixedTypes() throws Exception {
		String template= evaluateTemplateInMethod("void method(InnerCollection3<Number> numbers)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {
							String string = (String) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void simpleTransitive() throws Exception {
		String template= evaluateTemplateInMethod("void method(TransiCollection<List<Number>> lists)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = lists.iterator(); iterator.hasNext();) {
							List<Number> list = (List<Number>) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void wildcard() throws Exception {
		String template= evaluateTemplateInMethod("void method(Collection<?> objects)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = objects.iterator(); iterator.hasNext();) {
							Object object = (Object) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void upperboundWildcard() throws Exception {
		String template= evaluateTemplateInMethod("void method(Collection<? extends Number> numbers)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {
							Number number = (Number) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void lowerboundWildcard() throws Exception {
		String template= evaluateTemplateInMethod("void method(Collection<? super Number> objects)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = objects.iterator(); iterator.hasNext();) {
							Object object = (Object) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void generic() throws Exception {
		String template= evaluateTemplateInMethod("void <T> method(Collection<T> objects)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = objects.iterator(); iterator.hasNext();) {
							T t = (T) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void uppderboundGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T extends Number> method(Collection<T> numbers)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {
							T t = (T) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void lowerboundGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T super Number> method(Collection<T> objects)");
		assertEquals(
				"""
						for (java.util.Iterator iterator = objects.iterator(); iterator.hasNext();) {
							T t = (T) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void proposalsWithField() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method()", "Collection<Number> numbers");
		assertEquals(
				"""
						for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {
							Number number = (Number) iterator.next();
						\t
						}\
					""", template);
	}

	@Test
	public void proposalsWithFieldAndParam() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method(Collection<String> strings)", "Collection<Number> numbers");
		assertEquals(
				"""
						for (java.util.Iterator iterator = strings.iterator(); iterator.hasNext();) {
							String string = (String) iterator.next();
						\t
						}\
					""", template);
	}

}
