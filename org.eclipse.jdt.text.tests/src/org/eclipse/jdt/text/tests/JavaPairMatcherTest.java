/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christian Plesner Hansen (plesner@quenta.org) - integrated with the generic test for pair matchers
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.tests.AbstractPairMatcherTest;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;
/**
 * Tests for the java pair matcher
 *
 * @since 3.3
 */
public class JavaPairMatcherTest extends AbstractPairMatcherTest {

	protected IDocument fDocument;
	protected JavaPairMatcher fPairMatcher;


	public JavaPairMatcherTest() {
		super(true);
	}

	@Override
	protected String getDocumentPartitioning() {
		return IJavaPartitions.JAVA_PARTITIONING;
	}

	@Override
	protected ICharacterPairMatcher createMatcher(String chars) {
		return new JavaPairMatcher(chars.toCharArray());
	}

	@Before
	public void setUp() {
		Document document= new Document("xx(yy(xx)yy)xx()/*  */");
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

	@After
	public void tearDown () {
		fDocument= null;
		fPairMatcher= null;
	}

	@Test
	public void testBeforeOpeningMatch() {
		IRegion match= fPairMatcher.match(fDocument, 2);
		assertNotNull(match);
		assertTrue(match.getOffset() == 2 && match.getLength() == 10);

		match= fPairMatcher.match(fDocument, 5);
		assertNotNull(match);
		assertTrue(match.getOffset() == 5 && match.getLength() == 4);
	}

	@Test
	public void testAfterOpeningMatch() {
		IRegion match= fPairMatcher.match(fDocument, 3);
		assertNotNull(match);
		assertTrue(match.getOffset() == 2 && match.getLength() == 10);

		match= fPairMatcher.match(fDocument, 6);
		assertNotNull(match);
		assertTrue(match.getOffset() == 5 && match.getLength() == 4);
	}
	@Test
	public void testBeforeClosingMatch() {
		IRegion match= fPairMatcher.match(fDocument, 11);
		assertNotNull(match);
		assertTrue(match.getOffset() == 2 && match.getLength() == 10);

		match= fPairMatcher.match(fDocument, 8);
		assertNotNull(match);
		assertTrue(match.getOffset() == 5 && match.getLength() == 4);

	}
	@Test
	public void testAfterClosingMatch() {
		IRegion match= fPairMatcher.match(fDocument, 12);
		assertNotNull(match);
		assertTrue(match.getOffset() == 2 && match.getLength() == 10);

		match= fPairMatcher.match(fDocument, 9);
		assertNotNull(match);
		assertTrue(match.getOffset() == 5 && match.getLength() == 4);
	}
	@Test
	public void testBeforeClosingMatchWithNL() {
		fDocument.set("x(y\ny)x");
		IRegion match= fPairMatcher.match(fDocument, 5);
		assertNotNull(match);
		assertTrue(match.getOffset() == 1 && match.getLength() == 5);
	}
	@Test
	public void testAfterClosingMatchWithNL() {
		fDocument.set("x(y\ny)x");
		IRegion match= fPairMatcher.match(fDocument, 6);
		assertNotNull(match);
		assertTrue(match.getOffset() == 1 && match.getLength() == 5);
	}
	@Test
	public void testBeforeClosingMatchWithNLAndSingleLineComment() {
		fDocument.set("x\nx(y\nx //(x\ny)x");
		IRegion match= fPairMatcher.match(fDocument, 14);
		assertNotNull(match);
		assertTrue(match.getOffset() == 3 && match.getLength() == 12);
	}
	@Test
	public void testAfterClosingMatchWithNLAndSingleLineComment() {
		fDocument.set("x\nx(y\nx //(x\ny)x");
		IRegion match= fPairMatcher.match(fDocument, 15);
		assertNotNull(match);
		assertTrue(match.getOffset() == 3 && match.getLength() == 12);
	}
	@Test
	public void testEnclosingMatch() {
		IRegion match= fPairMatcher.findEnclosingPeerCharacters(fDocument, 4, 0);
		assertNotNull(match);
		assertTrue(match.getOffset() == 2 && match.getLength() == 10);

		match= fPairMatcher.findEnclosingPeerCharacters(fDocument, 7, 0);
		assertNotNull(match);
		assertTrue(match.getOffset() == 5 && match.getLength() == 4);
	}
	@Test
	public void testAngleBrackets1_5() {
		final JavaPairMatcher matcher= (JavaPairMatcher) createMatcher("(){}[]<>");
		matcher.setSourceVersion(JavaCore.VERSION_1_5);
		performMatch(matcher, " #< %> ");
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
	@Test
	public void testBug209505() {
		fDocument.set("(xny)/*  */");
		IRegion match= fPairMatcher.match(fDocument, 4);
		assertNotNull(match);
		assertTrue(match.getOffset() == 0 && match.getLength() == 5);
	}


}
