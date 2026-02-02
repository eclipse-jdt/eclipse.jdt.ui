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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

	@BeforeEach
	public void setUp() {
		document = new Document();
		tracker = DocumentDirtyTracker.get(document);
	}

	@AfterEach
	public void tearDown() {
		if (tracker != null) {
			tracker.dispose();
		}
	}

	@Test
	public void testInitiallyNoDirtyRegions() {
		IRegion[] regions = tracker.getDirtyRegions();
		assertNull(regions, "Should have no dirty regions initially");
	}

	@Test
	public void testSingleLineEdit() {
		document.set("line1\nline2\nline3\n");
		tracker.clearDirtyLines(); // Clear initial dirty marks

		// Edit line 1
		document.set("modified1\nline2\nline3\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull(regions, "Should have dirty regions after edit");
		assertEquals(1, regions.length, "Should have 1 dirty region");
	}

	@Test
	public void testMultipleConsecutiveLines() {
		document.set("line1\nline2\nline3\nline4\n");
		tracker.clearDirtyLines();

		// Edit lines 1 and 2
		document.set("modified1\nmodified2\nline3\nline4\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull(regions, "Should have dirty regions");
		assertEquals(1, regions.length, "Should merge consecutive lines into 1 region");
	}

	@Test
	public void testNonConsecutiveLines() {
		document.set("line1\nline2\nline3\nline4\n");
		tracker.clearDirtyLines();

		// Mark lines 0 and 2 as dirty manually
		tracker.markLinesDirty(0, 2);

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull(regions, "Should have dirty regions");
		assertEquals(2, regions.length, "Should have 2 separate regions");
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
		assertNotNull(regions, "Should still have dirty regions after insertion");
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
		assertNotNull(regions, "Should still have dirty regions after deletion");
	}

	@Test
	public void testClearDirtyLines() {
		document.set("line1\nline2\nline3\n");
		tracker.clearDirtyLines();

		// Edit a line
		document.set("modified1\nline2\nline3\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull(regions, "Should have dirty regions before clear");

		// Clear dirty lines
		tracker.clearDirtyLines();

		regions = tracker.getDirtyRegions();
		assertNull(regions, "Should have no dirty regions after clear");
	}

	@Test
	public void testUTF8Characters() {
		// Test with UTF-8 characters including emojis
		document.set("Hello 世界\n你好 World\nEmoji 😀🎉\n");
		tracker.clearDirtyLines();

		// Edit the emoji line
		document.set("Hello 世界\n你好 World\nModified 🚀✨\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull(regions, "Should handle UTF-8 characters correctly");
		assertEquals(1, regions.length, "Should have 1 dirty region");
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
		assertNotNull(regions, "Should track all rapid edits");
		// All lines should be marked as dirty
		assertEquals(1, regions.length, "All consecutive lines should be in 1 region");
	}

	@Test
	public void testEmptyDocument() {
		document.set("");
		tracker.clearDirtyLines();

		IRegion[] regions = tracker.getDirtyRegions();
		assertNull(regions, "Empty document should have no dirty regions");
	}

	@Test
	public void testSingleLineDocument() {
		document.set("single line");
		tracker.clearDirtyLines();

		// Edit the single line
		document.set("modified line");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull(regions, "Should have dirty regions for single line edit");
		assertEquals(1, regions.length, "Should have 1 dirty region");
	}

	@Test
	public void testRegionBounds() {
		document.set("line1\nline2\nline3\n");
		tracker.clearDirtyLines();

		// Edit line 1
		document.set("modified1\nline2\nline3\n");

		IRegion[] regions = tracker.getDirtyRegions();
		assertNotNull(regions, "Should have regions");

		// Verify region is within document bounds
		for (IRegion region : regions) {
			int offset = region.getOffset();
			int length = region.getLength();
			int docLength = document.getLength();

			assertTrue(offset >= 0, "Offset should be non-negative");
			assertTrue(length >= 0, "Length should be non-negative");
			assertTrue(offset + length <= docLength, "Region should be within document bounds");
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

		assertNotNull(regions1, "Doc1 should have dirty regions");
		assertNull(regions2, "Doc2 should not have dirty regions");

		tracker1.dispose();
		tracker2.dispose();
	}

	@Test
	public void testSameDocumentReturnsSameTracker() {
		DocumentDirtyTracker tracker1 = DocumentDirtyTracker.get(document);
		DocumentDirtyTracker tracker2 = DocumentDirtyTracker.get(document);

		assertEquals(tracker1, tracker2, "Same document should return same tracker instance");
	}
}
