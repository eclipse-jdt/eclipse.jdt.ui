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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Property;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.TypeExtensionManager;

public class PropertyTesterTests extends TestCase {
	
	private A a;
	private B b;
	private I i;

	protected void setUp() throws Exception {
		a= new A();
		b= new B();
		i= b;
	}
	
	public void testSimple() throws Exception {
		assertTrue(test(a, "simple", null,"simple"));
	}
	
	public void testInherited() throws Exception {
		assertTrue(test(b, "simple", null, "simple"));
		assertTrue(test(i, "simple", null, "simple"));
	}
	
	public void testUnknown() throws Exception {
		try {
			test(a, "unknown", null, null);
		} catch (CoreException e) {
			return;
		}
		assertTrue(false);
	}
	
	public void testOverridden() throws Exception {
		assertTrue(test(a, "overridden", null, "A"));
		assertTrue(test(b, "overridden", null, "B"));
		A b_as_a= b;
		assertTrue(test(b_as_a, "overridden", null, "B"));
		assertTrue(test(i, "overridden", null, "B"));
	}
	
	public void testOdering() throws Exception {
		assertTrue(test(b, "ordering", null, "A"));
		I other= new I() {};
		assertTrue(test(other, "ordering", null, "I"));
	}
	
	public void testChaining() throws Exception {
		assertTrue(test(a, "chaining", null, "A2"));
	}
	
	// This test is questionable. It depends on if core runtime can
	// guaratee any ordering in the plug-in registry.
	public void testChainOrdering() throws Exception {
		assertTrue(test(a, "chainOrdering", null, "A"));
	}
	
	public void testWrongNameSpace() throws Exception {
		try {
			test(a, "differentNamespace", null, null);
		} catch (CoreException e) {
			return;
		}
		assertTrue(false);		
	}
	
	public void testDifferentNameSpace() throws Exception {
		assertTrue(test("org.eclipse.jdt.ui.tests.refactoring2", a, "differentNamespace", null, "A3"));		
	}
	
	private boolean test(Object receiver, String property, Object[] args, Object expectedValue) throws CoreException {
		TypeExtensionManager manager= new TypeExtensionManager("propertyTesters");
		Property p= manager.getProperty(receiver, "org.eclipse.jdt.ui.tests.refactoring", property);
		return p.test(receiver, args, expectedValue);
	}
	
	private boolean test(String namespace, Object receiver, String property, Object[] args, Object expectedValue) throws CoreException {
		TypeExtensionManager manager= new TypeExtensionManager("propertyTesters");
		Property p= manager.getProperty(receiver, namespace, property);
		return p.test(receiver, args, expectedValue);
	}	
}
