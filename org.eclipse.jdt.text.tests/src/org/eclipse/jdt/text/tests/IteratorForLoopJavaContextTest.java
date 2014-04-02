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

import java.util.Collection;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.templates.Template;

/**
 * Tests functionality of the template based for loop completion to iterate over a
 * {@link Collection}, using an {@link Iterator} variable to fetch collection elements.
 */
public class IteratorForLoopJavaContextTest extends AbstractForLoopJavaContextTest {

	public static final String INNER_CLASS_DECLARATIONS= "	private static abstract class InnerCollection<E> implements Collection<E> {\n" +
			"	}\n" +
			"	\n" +
			"	private static abstract class InnerCollection2<E> implements Collection<Serializable> {\n" +
			"	}\n" +
			"	\n" +
			"	private static abstract class InnerCollection3<E> implements Collection<String> {\n" +
			"	}\n" +
			"	\n" +
			"	private static abstract class TransiCollection<E> implements Collection<E> {\n" +
			"	}\n" +
			"	\n";

	public static Test suite() {
		return new TestSuite(IteratorForLoopJavaContextTest.class);
	}

	@Override
	protected String getInnerClasses() {
		return INNER_CLASS_DECLARATIONS;
	}

	@Override
	protected Template getForLoop() {
		return getTemplate("org.eclipse.jdt.ui.templates.for_collection");
	}

	public void testCollection() throws Exception {
		String template= evaluateTemplateInMethod("void method(Collection<Number> numbers)");
		assertEquals(
				"	for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {\n" +
						"		Number number = (Number) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testInnerParameterized() throws Exception {
		String template= evaluateTemplateInMethod("void method(InnerCollection<Number> numbers)");
		assertEquals(
				"	for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {\n" +
						"		Number number = (Number) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testInnerFixTypedInterface() throws Exception {
		String template= evaluateTemplateInMethod("void method(InnerCollection2 numbers)");
		assertEquals(
				"	for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {\n" +
						"		Serializable serializable = (Serializable) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testInnerWithMixedTypes() throws Exception {
		String template= evaluateTemplateInMethod("void method(InnerCollection3<Number> numbers)");
		assertEquals(
				"	for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {\n" +
						"		String string = (String) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testSimpleTransitive() throws Exception {
		String template= evaluateTemplateInMethod("void method(TransiCollection<List<Number>> lists)");
		assertEquals(
				"	for (java.util.Iterator iterator = lists.iterator(); iterator.hasNext();) {\n" +
						"		List<Number> list = (List<Number>) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testWildcard() throws Exception {
		String template= evaluateTemplateInMethod("void method(Collection<?> objects)");
		assertEquals(
				"	for (java.util.Iterator iterator = objects.iterator(); iterator.hasNext();) {\n" +
						"		Object object = (Object) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testUpperboundWildcard() throws Exception {
		String template= evaluateTemplateInMethod("void method(Collection<? extends Number> numbers)");
		assertEquals(
				"	for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {\n" +
						"		Number number = (Number) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testLowerboundWildcard() throws Exception {
		String template= evaluateTemplateInMethod("void method(Collection<? super Number> objects)");
		assertEquals(
				"	for (java.util.Iterator iterator = objects.iterator(); iterator.hasNext();) {\n" +
						"		Object object = (Object) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T> method(Collection<T> objects)");
		assertEquals(
				"	for (java.util.Iterator iterator = objects.iterator(); iterator.hasNext();) {\n" +
						"		T t = (T) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testUppderboundGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T extends Number> method(Collection<T> numbers)");
		assertEquals(
				"	for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {\n" +
						"		T t = (T) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testLowerboundGeneric() throws Exception {
		String template= evaluateTemplateInMethod("void <T super Number> method(Collection<T> objects)");
		assertEquals(
				"	for (java.util.Iterator iterator = objects.iterator(); iterator.hasNext();) {\n" +
						"		T t = (T) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testProposalsWithField() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method()", "Collection<Number> numbers");
		assertEquals(
				"	for (java.util.Iterator iterator = numbers.iterator(); iterator.hasNext();) {\n" +
						"		Number number = (Number) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

	public void testProposalsWithFieldAndParam() throws Exception {
		String template= evaluateTemplateInMethodWithField("void method(Collection<String> strings)", "Collection<Number> numbers");
		assertEquals(
				"	for (java.util.Iterator iterator = strings.iterator(); iterator.hasNext();) {\n" +
						"		String string = (String) iterator.next();\n" +
						"		\n" +
						"	}", template);
	}

}
