/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;

import org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;

/**
 * Compares two <code>IParitionTokenScanner</code>s for conformance and performance.
 */
public class PartitionTokenScannerTest extends TestCase {

	private IPartitionTokenScanner fReference;
	private IPartitionTokenScanner fTestee;

	public PartitionTokenScannerTest(String name) {
		super(name);
	}

	protected void setUp() {
		fReference= new JavaPartitionScanner();
		fTestee= new FastJavaPartitionScanner(true);
	}

	// read sample java file
	private IDocument getDocument(String name, String lineDelimiter) {
		try {
			InputStream stream= getClass().getResourceAsStream(name);
			BufferedReader reader= new BufferedReader(new InputStreamReader(stream));
			
			StringBuffer buffer= new StringBuffer();
			String line= reader.readLine();
			while (line != null) {
				buffer.append(line);
				buffer.append(lineDelimiter);
				line= reader.readLine();
			}

			return new Document(buffer.toString());

		} catch (IOException e) {
		}
		
		return null;
	}
	
	public static IDocument getRandomDocument(int size) {
		final char[] characters= {'/', '*', '\'', '"', '\r', '\n', '\\'};
		final StringBuffer buffer= new StringBuffer();
		
		for (int i= 0; i < size; i++) {
			final int randomIndex= (int) (Math.random() * characters.length);
			buffer.append(characters[randomIndex]);
		}
		
		return new Document(buffer.toString());
	}

	public static Test suite() {
		return new TestSuite(PartitionTokenScannerTest.class);
	}

	public void testTestCaseLF() {
		testConformance(getDocument("TestCase.txt", "\n"));
	}

	public void testTestCaseCRLF() {
		testConformance(getDocument("TestCase.txt", "\r\n"));
	}

	public void testTestCaseCR() {
		testConformance(getDocument("TestCase.txt", "\r"));
	}

	public void testTestCase2LF() {
		testConformance(getDocument("TestCase2.txt", "\n"));
	}

	public void testTestCase2CRLF() {
		testConformance(getDocument("TestCase2.txt", "\r\n"));
	}

	public void testTestCase2CR() {
		testConformance(getDocument("TestCase2.txt", "\r"));
	}

//	XXX not fully passing because of "\<LF> and '\<LF>
//	public void testRandom() {
//		testConformance(getRandomDocument(2048));
//	}

	/**
	 * Tests performance of the testee against the reference IPartitionTokenScanner.
	 */
	public void testPerformance() {
		final int COUNT= 5000;
		final IDocument document= getDocument("TestCase.txt", "\n");

		final long referenceTime= getTime(fReference, document, COUNT);
		final long testeeTime= getTime(fTestee, document, COUNT);

		if (false) {
			System.out.println("reference time = " + referenceTime / 1000.0f);
			System.out.println("testee time = " + testeeTime / 1000.0f);
			System.out.println("factor = " + (float) referenceTime / testeeTime);
		}
		
		// dangerous: assert no regression in performance
		// assertTrue(testeeTime <= referenceTime);
	}
	
	public void test_bug57903() {
		final Document document= new Document("<%/**f%>");
		fReference.setRange(document, 2, 4);
		fTestee.setRange(document, 2, 4);
		
		IToken refToken= null;
		while (refToken == null || !refToken.isEOF()) {
			refToken=fReference.nextToken();
			IToken testeeToken=fTestee.nextToken();
			assertTokenEquals(refToken, testeeToken);
		}
	}
	
	private long getTime(IPartitionTokenScanner scanner, IDocument document, int count) {
		final long start= System.currentTimeMillis();

		for (int i= 0; i < count; i++)
			testPerformance(scanner, document);

		final long end= System.currentTimeMillis();
		
		return end - start;
	}
	
	private void testConformance(final IDocument document) {
		
		final StringBuffer message= new StringBuffer();
		
		fReference.setRange(document, 0, document.getLength());
		fTestee.setRange(document, 0, document.getLength());
		
		while (true) {
			
			message.setLength(0);
			
			final IToken referenceToken= fReference.nextToken();
			final IToken testeeToken= fTestee.nextToken();
			assertTokenEquals(referenceToken, testeeToken);
			
			final int referenceOffset= fReference.getTokenOffset();
			final int testeeOffset= fTestee.getTokenOffset();
			message.append(", offset = " + referenceOffset);
			message.append(", " + extractString(document, referenceOffset));
			assertEquals(message.toString(), referenceOffset, testeeOffset);

			final int referenceLength= fReference.getTokenLength();
			final int testeeLength= fTestee.getTokenLength();
			message.append(", length = " + referenceLength);
			assertEquals(message.toString(), referenceLength, testeeLength);

			if (referenceToken.isEOF())
				break;
		}
	}

	private static void testPerformance(final IPartitionTokenScanner scanner, final IDocument document) {
		
		scanner.setRange(document, 0, document.getLength());

		IToken token;
		do {
			token= scanner.nextToken();
			scanner.getTokenOffset();
			scanner.getTokenLength();

		} while (!token.isEOF());
	}
	
	private void assertTokenEquals(IToken expected, IToken actual) {
		assertEquals(expected.isEOF(), actual.isEOF());
		assertEquals(expected.isOther(), actual.isOther());
		assertEquals(expected.isUndefined(), actual.isUndefined());
		assertEquals(expected.isWhitespace(), actual.isWhitespace());
	}
		
	private static String extractString(IDocument document, int offset) {
		final StringBuffer buffer= new StringBuffer();

		try {
			IRegion region= document.getLineInformationOfOffset(offset);
			String line= document.get(region.getOffset(), region.getLength());
			
			int offsetIndex= offset - region.getOffset();

			// XXX kludge
			if (offsetIndex > line.length())
				offsetIndex= line.length();
			
			buffer.append("line = " + document.getLineOfOffset(offset) + ": [");
			buffer.append(line.substring(0, offsetIndex));
			buffer.append("<POS>");
			buffer.append(line.substring(offsetIndex));
			buffer.append(']');
			
		} catch (BadLocationException e) {
		}
		
		return buffer.toString();
	}

	/**
	 * Escapes CR, LF and TAB in a string.
	 * 
	 * @param string the string to escape
	 * @return the escaped string
	 */
	public static String escape(String string) {
		final StringBuffer buffer= new StringBuffer();
		
		final int length= string.length();
		for (int i= 0; i < length; i++) {
			final char character= string.charAt(i);
			switch (character) {
			case '\t':
				buffer.append("\\t");
				break;

			case '\r':
				buffer.append("\\r");
				break;
				
			case '\n':
				buffer.append("\\n");
				break;
			
			default:
				buffer.append(character);
				break;
			}
		}
		
		return buffer.toString();
	}
	
}
