/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.xml;

import junit.framework.TestCase;

import org.eclipse.jdt.internal.corext.refactoring.participants.xml.ExpressionException;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Method;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.TypeExtension;

public class TypeExtenderTests extends TestCase {
	
	private A a;
	private B b;
	private I i;

	protected void setUp() throws Exception {
		a= new A();
		b= new B();
		i= b;
	}
	
	public void testSimple() throws Exception {
		assertTrue(call(a, "simple", null).equals("simple"));
	}
	
	public void testInherited() throws Exception {
		assertTrue(call(b, "simple", null).equals("simple"));
		assertTrue(call(i, "simple", null).equals("simple"));
	}
	
	public void testUnknown() throws Exception {
		try {
			call(a, "unknown", null);
		} catch (ExpressionException e) {
			return;
		}
		assertTrue(false);
	}
	
	public void testOverridden() throws Exception {
		assertTrue(call(a, "overridden", null).equals("A"));
		assertTrue(call(b, "overridden", null).equals("B"));
		A b_as_a= b;
		assertTrue(call(b_as_a, "overridden", null).equals("B"));
		assertTrue(call(i, "overridden", null).equals("B"));
	}
	
	public void testOdering() throws Exception {
		assertTrue(call(b, "ordering", null).equals("A"));
		I other= new I() {};
		assertTrue(call(other, "ordering", null).equals("I"));
	}
	
	public void testChaining() throws Exception {
		assertTrue(call(a, "chaining", null).equals("A2"));
	}
	
	// This test is questionable. It depends on if core runtime can
	// guaratee any ordering in the plug-in registry.
	public void testChainOrdering() throws Exception {
		assertTrue(call(a, "chainOrdering", null).equals("A"));
	}
	
	private Object call(Object receiver, String method, Object[] args) throws ExpressionException {
		Method m= TypeExtension.getMethod(receiver, method);
		return m.invoke(receiver, args);
	}	
}
