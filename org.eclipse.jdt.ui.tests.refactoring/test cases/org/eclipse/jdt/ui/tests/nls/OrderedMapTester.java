/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.nls;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.ui.refactoring.nls.OrderedMap;

public class OrderedMapTester extends TestCase {

	/**
	 * Constructor for OrderedMapTester
	 */
	public OrderedMapTester(String name) {
		super(name);
	}

	public static void main (String[] args) {
		junit.textui.TestRunner.run (suite());
	}
	
	public static Test suite() {
		return new TestSuite(OrderedMapTester.class);
	}
	
	private OrderedMap m;
	
	public void setUp(){
		m= new OrderedMap();
	}
	
	public void test0(){
		assertTrue(m.peek() == null);
	}
	
	public void test1(){
		String key= "key"; //$NON-NLS-1$
		m.push(key, this);
		assertTrue(m.peek() != null);
	}
	
	public void test2(){
		String key= "key"; //$NON-NLS-1$
		m.push(key, this);
		m.remove(key);
		assertTrue(m.peek() == null);
	}
	
	public void test3(){
		String key= "key"; //$NON-NLS-1$
		m.push(key, this);
		m.push(key, key);
		assertTrue("A", m.peek() != null); //$NON-NLS-1$
		m.remove(key);
		assertTrue("B", m.peek() == null); //$NON-NLS-1$
	}
	
	public void test4(){
		m.remove(this);
	}
	
	public void test5(){
		String key1= "key1"; //$NON-NLS-1$
		String key2= "key2"; //$NON-NLS-1$
		String v1= "v1"; //$NON-NLS-1$
		String v2= "v2"; //$NON-NLS-1$
		m.push(key1, v1);
		m.push(key2, v2);
		assertTrue("A", m.peek().equals(v2)); //$NON-NLS-1$
		m.remove(key2);
		assertTrue("B", m.peek().equals(v1)); //$NON-NLS-1$
	}

	public void test6(){
		String key1= "key1"; //$NON-NLS-1$
		String key2= "key2"; //$NON-NLS-1$
		String v1= "v1"; //$NON-NLS-1$
		String v2= "v2"; //$NON-NLS-1$
		m.push(key1, v1);
		m.push(key2, v2);
		assertEquals("A", v2, m.peek()); //$NON-NLS-1$
		m.remove(key1);
		assertEquals("B", v2, m.peek()); //$NON-NLS-1$
		m.remove(key2);
		assertEquals("B", null, m.peek()); //$NON-NLS-1$
	}
	
	public void test7(){
		String key1= "key1"; //$NON-NLS-1$
		String key2= "key2"; //$NON-NLS-1$
		String v1= "v1"; //$NON-NLS-1$
		String v2= "v2"; //$NON-NLS-1$
		m.push(key1, v1);
		m.push(key2, v2);
		assertEquals("A", v2, m.peek()); //$NON-NLS-1$
		m.remove(key2);
		assertEquals("B", v1, m.peek()); //$NON-NLS-1$
		m.remove(key1);
		assertEquals("B", null, m.peek()); //$NON-NLS-1$
	}
	
}

