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
package org.eclipse.jdt.ui.text.tests;

import junit.awtui.TestRunner;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;

public class PairMatcherTest extends TestCase {
	
	private static boolean BEFORE_MATCHES_DISABLED= true;
	
	protected IDocument fDocument;
	protected JavaPairMatcher fPairMatcher;
	
	
	public PairMatcherTest(String name) {
		super(name);
	}
	
	public static void main(String args[]) {
		String a[] = { "org.eclipse.jdt.ui.text.tests.PairMatcherTest"};
		TestRunner.main(a);
	}
	
	protected void setUp() {
		fDocument= new Document("xx(yy(xx)yy)xx");
		fPairMatcher= new JavaPairMatcher(new char[] { '(', ')' });
	}
	
	public static Test suite() {
		return new TestSuite(PairMatcherTest.class); 
	}
	
	protected void tearDown () {
		fDocument= null;
		fPairMatcher= null;
	}
	
	public void testBeforeOpeningMatch() {
		IRegion match= fPairMatcher.match(fDocument, 2);
		if (BEFORE_MATCHES_DISABLED) {
			assertNull(match);
		} else {
			assertNotNull(match);
			assertTrue(match.getOffset() == 2 && match.getLength() == 10);
		}
		
		match= fPairMatcher.match(fDocument, 5);
		if (BEFORE_MATCHES_DISABLED) {
			assertNull(match);
		} else {
			assertNotNull(match);
			assertTrue(match.getOffset() == 5 && match.getLength() == 4);		
		}
	}
	
	public void testAfterOpeningMatch() {
		IRegion match= fPairMatcher.match(fDocument, 3);
		assertNotNull(match);
		assertTrue(match.getOffset() == 2 && match.getLength() == 10);
		
		match= fPairMatcher.match(fDocument, 6);
		assertNotNull(match);
		assertTrue(match.getOffset() == 5 && match.getLength() == 4);		
	}
	
	public void testBeforeClosingMatch() {
		IRegion match= fPairMatcher.match(fDocument, 11);
		if (BEFORE_MATCHES_DISABLED) {
			assertNull(match);
		} else {
			assertNotNull(match);
			assertTrue(match.getOffset() == 2 && match.getLength() == 10);
		}
		
		match= fPairMatcher.match(fDocument, 8);
		if (BEFORE_MATCHES_DISABLED) {
			assertNull(match);
		} else {
			assertNotNull(match);
			assertTrue(match.getOffset() == 5 && match.getLength() == 4);		
		}
	}
	
	public void testAfterClosingMatch() {
		IRegion match= fPairMatcher.match(fDocument, 12);
		assertNotNull(match);
		assertTrue(match.getOffset() == 2 && match.getLength() == 10);
		
		match= fPairMatcher.match(fDocument, 9);
		assertNotNull(match);
		assertTrue(match.getOffset() == 5 && match.getLength() == 4);		
	}	
	
	public void testBeforeClosingMatchWithNL() {
		fDocument.set("x(y\ny)x");
		IRegion match= fPairMatcher.match(fDocument, 5);
		if (BEFORE_MATCHES_DISABLED) {
			assertNull(match);
		} else {
			assertNotNull(match);
			assertTrue(match.getOffset() == 1 && match.getLength() == 5);
		}
	}	
	
	public void testAfterClosingMatchWithNL() {
		fDocument.set("x(y\ny)x");
		IRegion match= fPairMatcher.match(fDocument, 6);
		assertNotNull(match);
		assertTrue(match.getOffset() == 1 && match.getLength() == 5);
	}
	
	public void testBeforeClosingMatchWithNLAndSingleLineComment() {
		fDocument.set("x\nx(y\nx //(x\ny)x");
		IRegion match= fPairMatcher.match(fDocument, 14);
		if (BEFORE_MATCHES_DISABLED) {
			assertNull(match);
		} else {
			assertNotNull(match);
			assertTrue(match.getOffset() == 3 && match.getLength() == 12);
		}
	}	
	
	public void testAfterClosingMatchWithNLAndSingleLineComment() {
		fDocument.set("x\nx(y\nx //(x\ny)x");
		IRegion match= fPairMatcher.match(fDocument, 15);
		assertNotNull(match);
		assertTrue(match.getOffset() == 3 && match.getLength() == 12);
	}	
}
