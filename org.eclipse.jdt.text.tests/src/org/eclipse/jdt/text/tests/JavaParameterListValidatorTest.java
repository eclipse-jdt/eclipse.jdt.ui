/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

	public void testParameterStyling() {
		final String code= "a, b, c, d, e";
		assertParameterInfoStyles(code, "int a, int b, int c, int d, int e", computeCommaPositions(code));
    }

	public void testParameterStylingWithString() {
		final String code= "a, b, \"foo, bar, and such\", d, e";
		assertParameterInfoStyles(code, "int a, int b, String c, int d, int e", computeCommaPositions(code));
	}

	public void testParameterStylingWithEscapedString() {
		final String code= "a, b, \"foo, b\\\"ar, and such\\\\\", d, e";
		assertParameterInfoStyles(code, "int a, int b, String c, int d, int e", computeCommaPositions(code));
	}

	public void testParameterStylingWithComment() {
		final String code= "a, b, c /* the ( argument */, d, e";
		assertParameterInfoStyles(code, "int a, int b, String c, int d, int e", computeCommaPositions(code));
	}

	public void testParameterStylingWithGenerics() {
		final String code= "new HashMap <String, HashMap<String,Integer[]>>(), p1, p2";
		assertParameterInfoStyles(code, "Map<String, Map> m, int p1, int p2", computeCommaPositions(code));
	}

	public void testParameterStylingWithGenericsAndComments() {
		final String code= "new HashMap <String, /* comment > */HashMap<String,Integer[]>>(), p1, p2";
		assertParameterInfoStyles(code, "Map<String, Map> m, int p1, int p2", new int[] {64, 68});
	}

	public void testParameterStylingWithConstants() {
		final String code= "MAX < MIN, MAX > MIN";
		assertParameterInfoStyles(code, "boolean b1, boolean b2", new int[] {9});
	}

	public void testParameterStylingWithGenericsThatLookLikeConstants() {
		final String code= "new MAX < MIN, MAX >";
		assertParameterInfoStyles(code, "MAX<?,?>", new int[0]);
	}

	public void testParameterStylingWithNestedGenerics() {
		final String code= "null, null";
		assertParameterInfoStyles(code, "ICallback<List<ETypedElement>, T2, D> callback, Shell shell", computeCommaPositions(code));
	}
	private void assertParameterInfoStyles(String code, String infostring, int[] argumentCommas) {
		fDocument.set(code);
		fTextViewer.setDocument(fDocument);

		int[] parameterCommas= computeCommaPositions(infostring);
		IContextInformation info= new ContextInformation("context", infostring);
		fValidator.install(info, fTextViewer, 0);

		TextPresentation p= new TextPresentation();
		for (int i= 0; i < fDocument.getLength(); i++) {
			fValidator.updatePresentation(i, p);
			assertEquals(createPresentation(i, parameterCommas, argumentCommas, infostring), p);
		}
    }

	private int[] computeCommaPositions(String code) {
	    int angleLevel= 0;
	    int pos= 0;
		List positions= new ArrayList();
		while (pos < code.length() && pos != -1) {
			char ch= code.charAt(pos);
			switch (ch) {
	            case ',':
	            	if (angleLevel == 0)
	            		positions.add(new Integer(pos));
		            break;
	            case '<':
	        	    angleLevel++;
	        	    break;
	            case '>':
	            	angleLevel--;
	            	break;
	            case '(':
	            	pos= code.indexOf(')', pos);
	            	break;
	            case '[':
	            	pos= code.indexOf(']', pos);
	            	break;
	            case '{':
	            	pos= code.indexOf('}', pos);
	            	break;
	            case '"':
	            	pos= findLiteralEnd(code, pos + 1, '"');
	            	break;
	            case '/':
	            	if (pos < code.length() - 1 && code.charAt(pos + 1) == '*')
	            		pos= findCommentEnd(code, pos + 2);
	            	break;
	            case '\'':
	            	pos= findLiteralEnd(code, pos + 1, '\'');
	            	break;
	            default:
	            	break;
            }
			if (pos != -1)
				pos++;
		}

		int[] fields= new int[positions.size()];
		for (int i= 0; i < fields.length; i++)
	        fields[i]= ((Integer) positions.get(i)).intValue();
	    return fields;
    }

    private int findCommentEnd(String code, int pos) {
    	while (pos < code.length()) {
    		pos= code.indexOf('*', pos);
    		if (pos == -1 || pos == code.length() - 1)
    			break;
    		if (code.charAt(pos + 1) == '/')
    			return pos;
    		pos++;
    	}
	    return -1;
    }

	private int findLiteralEnd(String code, int pos, char peer) {
    	while (pos < code.length()) {
    		char ch= code.charAt(pos);
    		if (ch == peer)
    			return pos;
    		if (ch == '\\')
    			pos += 2;
    		else
    			pos++;
    	}
	    return -1;
    }

	private TextPresentation createPresentation(int position, int[] parameterCommas, int[] argumentCommas, String contextInfo) {
		int length= contextInfo.length();
		TextPresentation p= new TextPresentation();


		int boldStart= 0;
		int boldEnd= length;

		for (int i= 0; i < argumentCommas.length; i++) {
	        int argumentComma= argumentCommas[i];
	        int parameterComma= parameterCommas[i];

	        if (argumentComma < position)
	        	boldStart= parameterComma + 1;
	        if (argumentComma >= position) {
	        	boldEnd= parameterComma;
	        	break;
	        }
        }

		if (boldStart > 0)
			p.addStyleRange(new StyleRange(0, boldStart, null, null, SWT.NORMAL));

		p.addStyleRange(new StyleRange(boldStart, boldEnd - boldStart, null, null, SWT.BOLD));

		if (boldEnd < length)
			p.addStyleRange(new StyleRange(boldEnd, length - boldEnd, null, null, SWT.NORMAL));

		// TODO handle no range at all

		return p;
	}

	private static void assertEquals(TextPresentation expected, TextPresentation actual) {
		// check lengths
		assertTrue(expected.getDenumerableRanges() == actual.getDenumerableRanges());
		// check default range
		assertEquals(expected.getDefaultStyleRange(), actual.getDefaultStyleRange());
		// check rest
		Iterator e1= expected.getAllStyleRangeIterator();
		Iterator e2= actual.getAllStyleRangeIterator();
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
	public void testValidPositions() {
		assertValidPositions("(a, b, c)");
	}

	public void testValidPositionsWithComment() {
		assertValidPositions("(a, b /* the ( argument */, c)");
	}

	public void testValidPositionsWithString() {
		assertValidPositions("(a, \"foo, bar, and such\", c)");
	}

	public void testValidGenericPositions() throws Exception {
	    assertValidPositions("(new A<T>(), new B<T>())");
    }

	public void testValidAllPositions() throws Exception {
		assertValidPositions("(new HashMap<String, HashMap<String,Integer[]>>(), p1, p2)");
	}

	public void testValidArrayPositions() throws Exception {
		assertValidPositions("(foo[], bar[13])");
	}

	/**
	 * Asserts that the context information is invalid both at both borders of the passed string,
	 * but valid at each position within the string.
	 *
	 * @param code the code to test, typically a parenthesized expression such as
	 *        <code>(a, b, c)</code>
	 */
	private void assertValidPositions(final String code) {
	    fDocument.set(code + " ");
		fTextViewer.setDocument(fDocument);

		IContextInformation info= new ContextInformation("context", "info");
		fValidator.install(info, fTextViewer, 1);

		assertTrue(!fValidator.isContextInformationValid(0));
		final int length= code.length();
		final int firstInnerPosition= 1;
		final int lastInnerPosition= length - 1;

		// forward
		for (int pos= firstInnerPosition; pos <= lastInnerPosition; pos++)
			assertTrue(fValidator.isContextInformationValid(pos));
		assertTrue(!fValidator.isContextInformationValid(length));

		// backward
		for (int pos= lastInnerPosition; pos >= firstInnerPosition; pos--)
			assertTrue(fValidator.isContextInformationValid(pos));
		assertTrue(!fValidator.isContextInformationValid(0));
    }
}
