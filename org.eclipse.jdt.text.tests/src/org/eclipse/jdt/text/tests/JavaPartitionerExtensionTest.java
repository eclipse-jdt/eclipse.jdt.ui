/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jface.preference.PreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IDocumentPartitioningListenerExtension;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;

import org.eclipse.jdt.ui.text.JavaTextTools;

public class JavaPartitionerExtensionTest {
	class PartitioningListener implements IDocumentPartitioningListener, IDocumentPartitioningListenerExtension {
		/*
		 * @see IDocumentPartitioningListener#documentPartitioningChanged(IDocument)
		 */
		@Override
		public void documentPartitioningChanged(IDocument document) {
			fDocumentPartitioningChanged= true;
		}

		/*
		 * @see IDocumentPartitioningListenerExtension#documentPartitioningChanged(IDocument, IRegion)
		 */
		@Override
		public void documentPartitioningChanged(IDocument document, IRegion region) {
			fDocumentPartitioningChanged= true;
			fChangedDocumentPartitioning= region;
		}
	}

	private JavaTextTools fTextTools;
	private Document fDocument;
	protected boolean fDocumentPartitioningChanged;
	protected IRegion fChangedDocumentPartitioning;

	@Before
	public void setUp() {

		fTextTools= new JavaTextTools(new PreferenceStore());

		fDocument= new Document();
		IDocumentPartitioner partitioner= fTextTools.createDocumentPartitioner();
		partitioner.connect(fDocument);
		fDocument.setDocumentPartitioner(partitioner);

		fDocumentPartitioningChanged= false;
		fChangedDocumentPartitioning= null;
		fDocument.addDocumentPartitioningListener(new PartitioningListener());
	}

	@After
	public void tearDown () {
		fTextTools.dispose();
		fTextTools= null;

		IDocumentPartitioner partitioner= fDocument.getDocumentPartitioner();
		partitioner.disconnect();
		fDocument= null;
	}


	protected String print(ITypedRegion r) {
		return r != null ? "[" + r.getOffset() + "," + r.getLength() + "," + r.getType() + "]" : "null";
	}

	protected String print(IRegion r) {
		return r != null ? "[" + r.getOffset() + "," + r.getLength() + "]" : "null";
	}

	protected void check(int offset, int length) {
		assertTrue(fDocumentPartitioningChanged);
		assertNotNull(fChangedDocumentPartitioning);
		assertEquals(fChangedDocumentPartitioning.getOffset(), offset);
		assertEquals(fChangedDocumentPartitioning.getLength(), length);

		fDocumentPartitioningChanged= false;
		fChangedDocumentPartitioning= null;
	}

	protected void check() {
		assertFalse(fDocumentPartitioningChanged);
		assertNull(fChangedDocumentPartitioning);
	}

	@Test
	public void testConvertPartition() {

		try {
			fDocument.set("/*xxx*/");
			check(0, 7);
			fDocument.replace(0,0,"//");
			check(0, 9);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testTransformPartition() {

		try {
			fDocument.set("/*\nx\nx\nx\n*/");
			check(0, 11);
			fDocument.replace(0,0,"//");
			check(0, 13);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testToggleMultiPartition() {

		try {
			fDocument.set("/*\nCode version 1\n/*/\nCode version 2\n//*/");
			check(0, 41);
			fDocument.replace(0,0,"//");
			check(0, 43);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testSplitPartition() {
		try {
			fDocument.set("class X {}");
			check();
			fDocument.replace(9, 0, "/**/");
			check(9, 4);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testShortenDocument() {
		try {
			fDocument.set("class x {\n/***/\n}");
			check(10, 5);
			fDocument.replace(0, fDocument.getLength(), "/**/");
			check(0, 4);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testDeletePartition() {
		try {
			fDocument.set("class x {\n/***/\n}");
			check(10, 5);
			fDocument.replace(10, 5,  "");
			check(10, 0);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testDeletePartition2() {
		try {
			fDocument.set("class x {\n/***/\n}");
			check(10, 5);
			fDocument.replace(10, 7,  "");
			check(10, 0);
		} catch (BadLocationException x) {
			fail();
		}
	}
}
