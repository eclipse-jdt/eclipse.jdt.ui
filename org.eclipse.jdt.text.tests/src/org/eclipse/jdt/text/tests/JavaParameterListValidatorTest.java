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
package org.eclipse.jdt.text.tests;


import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.tests.TestTextViewer;

import org.eclipse.jdt.internal.ui.text.java.JavaParameterListValidator;


public class JavaParameterListValidatorTest extends TestCase {
	
	protected TestTextViewer fTextViewer;
	protected IDocument fDocument;
	protected JavaParameterListValidator fValidator;
	
	public JavaParameterListValidatorTest(String name) {
		super(name);
	}
	
	protected void setUp() {
		fTextViewer= new TestTextViewer();
		fDocument= new Document();
		fValidator= new JavaParameterListValidator();
	}
	
	public static Test suite() {
		return new TestSuite(JavaParameterListValidatorTest.class); 
	}
	
	protected void tearDown () {
		fTextViewer= null;
		fDocument= null;
		fValidator= null;
	}
	
	protected void checkPresentation(TextPresentation shouldBe, TextPresentation is) {
		// check lengths
		assertTrue(shouldBe.getDenumerableRanges() == is.getDenumerableRanges());
		// check default range
		assertEquals(shouldBe.getDefaultStyleRange(), is.getDefaultStyleRange());
		// check rest
		Iterator e1= shouldBe.getAllStyleRangeIterator();
		Iterator e2= is.getAllStyleRangeIterator();
		while (e1.hasNext())
			assertEquals(e1.next(), e2.next());
	}
	
	protected String print(TextPresentation presentation) {
		StringBuffer buf= new StringBuffer();
		if (presentation != null) {
			// default range
			buf.append("Default style range: ");
			StyleRange range= presentation.getDefaultStyleRange();
			if (range != null)
				buf.append(range.toString());
			buf.append('\n');
			// rest
			Iterator e= presentation.getAllStyleRangeIterator();
			while (e.hasNext()) {
				buf.append(e.next().toString());
				buf.append('\n');
			}
		}
		return buf.toString();	
	}
	
	protected TextPresentation createSample(int position) {
		TextPresentation p= new TextPresentation();
		
		int entry= Math.round((float) Math.ceil(position/3));
		
		if (entry > 4) {
			p.addStyleRange(new StyleRange(0, 34, null, null, SWT.NORMAL));
			return p;
		}
		
		if (entry > 0)
			p.addStyleRange(new StyleRange(0, entry * 7, null, null, SWT.NORMAL));
		
		p.addStyleRange(new StyleRange(entry * 7, 6, null, null, SWT.BOLD));
		
		if (entry < 4) {
			int start= entry * 7 + 6;
			p.addStyleRange(new StyleRange(start, 34 - start, null, null, SWT.NORMAL));
		}
		
		return p;
	}
	
	public void testParameterStyling() {
		fDocument.set(" a, b, c, d, e");
		fTextViewer.setDocument(fDocument);
		
		IContextInformation info= new ContextInformation("context", " int a, int b, int c, int d, int e");
		fValidator.install(info, fTextViewer, 0);
		
		TextPresentation p= new TextPresentation();
		for (int i= 0; i < fDocument.getLength(); i++) {
			fValidator.updatePresentation(i, p);
			checkPresentation(createSample(i), p);
		}
	}
	
	public void testValidPositionsForward() {
		fDocument.set("(a, b, c) ");
		fTextViewer.setDocument(fDocument);
		
		IContextInformation info= new ContextInformation("context", "info");
		fValidator.install(info, fTextViewer, 1);
		
		assertTrue(!fValidator.isContextInformationValid(0));		
		for (int i= 1; i < 9; i++)
			assertTrue(fValidator.isContextInformationValid(i));
		assertTrue(!fValidator.isContextInformationValid(9));
	}
	
	public void testValidPositionsBackward() {
		fDocument.set("(a, b, c) ");
		fTextViewer.setDocument(fDocument);
		
		IContextInformation info= new ContextInformation("context", "info");
		fValidator.install(info, fTextViewer, 1);
		
		assertTrue(!fValidator.isContextInformationValid(9));
		for (int i= 8; i > 0; i--)
			assertTrue(fValidator.isContextInformationValid(i));
		assertTrue(!fValidator.isContextInformationValid(0));		
	}
}
