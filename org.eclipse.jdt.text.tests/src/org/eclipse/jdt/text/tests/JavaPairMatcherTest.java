/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christian Plesner Hansen (plesner@quenta.org) - integrated with the generic test for pair matchers
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.tests.AbstractPairMatcherTest;
/**
 * Tests for the java pair matcher
 * 
 * @since 3.3
 */
public class JavaPairMatcherTest extends AbstractPairMatcherTest {
	
	private static boolean BEFORE_MATCHES_DISABLED= true;
	
	protected IDocument fDocument;
	protected JavaPairMatcher fPairMatcher;
	
	
	public JavaPairMatcherTest(String name) {
		super(name);
	}
	
	protected String getDocumentPartitioning() {
		return IJavaPartitions.JAVA_PARTITIONING;
	}
	
	protected ICharacterPairMatcher createMatcher(String chars) {
		return new JavaPairMatcher(chars.toCharArray());
	}
	
	protected void setUp() {
		Document document= new Document("xx(yy(xx)yy)xx");
		String[] types= new String[] {
				IJavaPartitions.JAVA_DOC,
				IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
				IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
				IJavaPartitions.JAVA_STRING,
				IJavaPartitions.JAVA_CHARACTER,
				IDocument.DEFAULT_CONTENT_TYPE
		};
		FastPartitioner partitioner= new FastPartitioner(new FastJavaPartitionScanner(), types);
		partitioner.connect(document);
		document.setDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING, partitioner);

		fDocument= document;
		fPairMatcher= new JavaPairMatcher(new char[] { '(', ')' });
	}
	
	public static Test suite() {
		return new TestSuite(JavaPairMatcherTest.class); 
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
	
	public void testAngleBrackets1_4() {
		final JavaPairMatcher matcher= (JavaPairMatcher) createMatcher("(){}[]<>");
		matcher.setSourceVersion(JavaCore.VERSION_1_4);
		performMatch(matcher, " <>% ");
		performMatch(matcher, " <%> ");
		performMatch(matcher, " 2 < 3 || 4 >% 5 ");
		performMatch(matcher, " 2 <% 3 || 4 > 5 ");
		performMatch(matcher, " List<String>% ");
		performMatch(matcher, " foo < T >% ");
		performMatch(matcher, " foo <% T > ");
		performMatch(matcher, " foo < T >% ");
		performMatch(matcher, " final <% T > ");
		matcher.dispose();
	}
	
	public void testAngleBrackets1_5() {
		final JavaPairMatcher matcher= (JavaPairMatcher) createMatcher("(){}[]<>");
		matcher.setSourceVersion(JavaCore.VERSION_1_5);
		performMatch(matcher, " #<>% ");
		performMatch(matcher, " <%># ");
		performMatch(matcher, " 2 < 3 || 4 >% 5 ");
		performMatch(matcher, " 2 <% 3 || 4 > 5 ");
		performMatch(matcher, " List#<String>% ");
		performMatch(matcher, " foo < T >% ");
		performMatch(matcher, " foo <% T > ");
		performMatch(matcher, " foo < T >% ");
		performMatch(matcher, " final <% T ># ");
		matcher.dispose();
	}

}
