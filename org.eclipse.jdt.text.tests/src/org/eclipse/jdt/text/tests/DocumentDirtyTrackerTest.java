/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.ui.javaeditor.DocumentDirtyTracker;

/**
 * Tests for DocumentDirtyTracker to ensure it correctly tracks dirty lines
 * and prevents race conditions in format-on-save operations.
 */
public class DocumentDirtyTrackerTest {

	private IDocument document;
	private DocumentDirtyTracker tracker;

	@Before
	public void setUp() {
		document = new Document();
		tracker = DocumentDirtyTracker.get(document);
	}

	@After
	public void tearDown() {
		if (tracker != null) {
			tracker.dispose();
		}
	}

	@Test
	public void testInitiallyNoDirtyRegions() {
		IRegion[] regions = tracker.getDirtyRegions();
		assertNull("Should have no dirty regions initially", regions);
	}

	@Test
	public void testSingleLineEdit() {
		document.set("line1\nline2\nline3\n");
		tracker.clearDirtyLines(); // Clear initial dirty marks

		// Edit line 1
		document.set("modified1\nline2\nline3\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should have dirty regions after edit", regions);
		assertEquals("Should have 1 dirty region", 1, regions.length);
	}

	@Test
	public void testMultipleConsecutiveLines() {
		document.set("line1\nline2\nline3\nline4\n");
		tracker.clearDirtyLines();

		// Edit lines 1 and 2
		document.set("modified1\nmodified2\nline3\nline4\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should have dirty regions", regions);
		assertEquals("Should merge consecutive lines into 1 region", 1, regions.length);
	}

	@Test
	public void testNonConsecutiveLines() {
		document.set("line1\nline2\nline3\nline4\n");
		tracker.clearDirtyLines();

		// Mark lines 0 and 2 as dirty manually
		tracker.markLinesDirty(0, 2);

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should have dirty regions", regions);
		assertEquals("Should have 2 separate regions", 2, regions.length);
	}

	@Test
	public void testLineInsertion() {
		document.set("line1\nline2\nline3\n");
		tracker.clearDirtyLines();

		// Mark line 1 as dirty
		tracker.markLinesDirty(1);

		// Insert a line before line 1
		document.set("line1\ninserted\nline2\nline3\n");

		// The dirty line should have shifted from 1 to 2
		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should still have dirty regions after insertion", regions);
	}

	@Test
	public void testLineDeletion() {
		document.set("line1\nline2\nline3\nline4\n");
		tracker.clearDirtyLines();

		// Mark line 2 as dirty
		tracker.markLinesDirty(2);

		// Delete line 1
		document.set("line1\nline3\nline4\n");

		// The dirty line should have shifted from 2 to 1
		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should still have dirty regions after deletion", regions);
	}

	@Test
	public void testClearDirtyLines() {
		document.set("line1\nline2\nline3\n");
		tracker.clearDirtyLines();

		// Edit a line
		document.set("modified1\nline2\nline3\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should have dirty regions before clear", regions);

		// Clear dirty lines
		tracker.clearDirtyLines();

		regions = tracker.getDirtyRegions();
		assertNull("Should have no dirty regions after clear", regions);
	}

	@Test
	public void testUTF8Characters() {
		// Test with UTF-8 characters including emojis
		document.set("Hello 世界\n你好 World\nEmoji 😀🎉\n");
		tracker.clearDirtyLines();

		// Edit the emoji line
		document.set("Hello 世界\n你好 World\nModified 🚀✨\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should handle UTF-8 characters correctly", regions);
		assertEquals("Should have 1 dirty region", 1, regions.length);
	}

	@Test
	public void testRapidSuccessiveEdits() {
		document.set("line1\nline2\nline3\n");
		tracker.clearDirtyLines();

		// Simulate rapid successive edits on different lines
		document.set("mod1\nline2\nline3\n");
		document.set("mod1\nmod2\nline3\n");
		document.set("mod1\nmod2\nmod3\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should track all rapid edits", regions);
		// All lines should be marked as dirty
		assertEquals("All consecutive lines should be in 1 region", 1, regions.length);
	}

	@Test
	public void testEmptyDocument() {
		document.set("");
		tracker.clearDirtyLines();

		IRegion[] regions = tracker.getDirtyRegions();
		assertNull("Empty document should have no dirty regions", regions);
	}

	@Test
	public void testSingleLineDocument() {
		document.set("single line");
		tracker.clearDirtyLines();

		// Edit the single line
		document.set("modified line");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should have dirty regions for single line edit", regions);
		assertEquals("Should have 1 dirty region", 1, regions.length);
	}

	@Test
	public void testRegionBounds() {
		document.set("line1\nline2\nline3\n");
		tracker.clearDirtyLines();

		// Edit line 1
		document.set("modified1\nline2\nline3\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull("Should have regions", regions);

		// Verify region is within document bounds
		for (IRegion region : regions) {
			int offset = region.getOffset();
			int length = region.getLength();
			int docLength = document.getLength();

			assertEquals("Offset should be non-negative", true, offset >= 0);
			assertEquals("Length should be non-negative", true, length >= 0);
			assertEquals("Region should be within document bounds", true, offset + length <= docLength);
		}
	}

	@Test
	public void testMultipleDocuments() {
		// Test that different documents have independent trackers
		IDocument doc1 = new Document("doc1 line1\ndoc1 line2\n");
		IDocument doc2 = new Document("doc2 line1\ndoc2 line2\n");

		DocumentDirtyTracker tracker1 = DocumentDirtyTracker.get(doc1);
		DocumentDirtyTracker tracker2 = DocumentDirtyTracker.get(doc2);

		tracker1.clearDirtyLines();
		tracker2.clearDirtyLines();

		// Edit only doc1
		doc1.set("doc1 modified\ndoc1 line2\n");

		IRegion[] regions1 = tracker1.getDirtyRegions();
		IRegion[] regions2 = tracker2.getDirtyRegions();

		assertNotNull("Doc1 should have dirty regions", regions1);
		assertNull("Doc2 should not have dirty regions", regions2);

		tracker1.dispose();
		tracker2.dispose();
	}

	@Test
	public void testSameDocumentReturnsSameTracker() {
		DocumentDirtyTracker tracker1 = DocumentDirtyTracker.get(document);
		DocumentDirtyTracker tracker2 = DocumentDirtyTracker.get(document);

		assertEquals("Same document should return same tracker instance", tracker1, tracker2);
	}
}
