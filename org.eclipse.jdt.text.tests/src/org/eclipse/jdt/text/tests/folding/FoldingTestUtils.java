/*******************************************************************************
 * Copyright (c) 2025, 2026 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *     Daniel Schmid - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.folding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public final class FoldingTestUtils {
	private record StartEnd(int start, int end) {
	}

	public record ProjectionRegion(int offset, int length, boolean collapsed) {
	}

	private FoldingTestUtils() {
	}

	public static List<ProjectionRegion> getProjectionRangesOfPackage(IPackageFragment packageFragment, String code) throws Exception {
		ICompilationUnit cu= packageFragment.createCompilationUnit("A.java", code, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);
		try {
			ProjectionAnnotationModel model= editor.getAdapter(ProjectionAnnotationModel.class);

			return extractRegions(model);
		} finally {
			editor.close(false);
		}
	}

	public static List<ProjectionRegion> extractRegions(ProjectionAnnotationModel model) {
		List<ProjectionRegion> regions= new ArrayList<>();
		Iterator<Annotation> it= model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation a= it.next();
			if (a instanceof ProjectionAnnotation projectionAnnotation) {
				Position p= model.getPosition(a);
				regions.add(new ProjectionRegion(p.getOffset(), p.getLength(), projectionAnnotation.isCollapsed()));
			}
		}
		assertNoDuplicatedRegions(regions);
		assertNoRegionsStartInTheSameOffset(regions);
		return regions;
	}

	private static void assertNoDuplicatedRegions(List<ProjectionRegion> regions) {
		long distinctRegions= regions.stream()
				.map(r -> Map.entry(r.offset(), r.length())) // map to offset-length pairs
				.distinct()
				.count();
		assertEquals(regions.size(), distinctRegions,
				"Some regions are duplicated: " + sorted(regions));
	}

	private static void assertNoRegionsStartInTheSameOffset(Collection<ProjectionRegion> regions) {
		long distinctOffsets= regions.stream()
				.map(ProjectionRegion::offset)
				.distinct()
				.count();

		assertEquals(regions.size(), distinctOffsets,
				"Some regions start in the same offset: Regions: " + sorted(regions));
	}

	public static void assertCodeHasRegions(IPackageFragment packageFragment, String code, int regionsCount) throws Exception {
		List<ProjectionRegion> regions= FoldingTestUtils.getProjectionRangesOfPackage(packageFragment, code);
		assertEquals(regionsCount, regions.size(), String.format("Expected %d regions but saw %d.", regionsCount, regions.size()));
	}

	public static void assertDoesNotContainRegionUsingStartLine(List<ProjectionRegion> projectionRanges, String input, int startLine) {
		int startLineBegin= findLineStartIndex(input, startLine);
		for (ProjectionRegion region : projectionRanges) {
			if (region.offset() == startLineBegin) {
				fail("found unexpected region at offset=" + region.offset() + ", length=" + region.length() +
						" starting at line " + startLine + " (line offset: " + startLineBegin + ")");
			}
		}
	}

	public static void assertDoesNotContainRegionUsingStartAndEndLine(List<ProjectionRegion> projectionRanges, String input, int startLine, int endLine) {
		StartEnd startEnd= getStartEnd(input, startLine, endLine);
		assertDoesNotContainRegionWithOffsetAndLength(projectionRanges, startLine, endLine, startEnd.start(), startEnd.end());
	}

	public static void assertContainsRegionUsingStartAndEndLine(List<ProjectionRegion> projectionRanges, String input, int startLine, int endLine) {
		StartEnd startEnd= getStartEnd(input, startLine, endLine);
		assertContainsRegionWithOffsetAndLength(projectionRanges, startLine, endLine, startEnd.start(), startEnd.end());
	}

	public static void assertContainsCollapsedRegionUsingStartAndEndLine(List<ProjectionRegion> projectionRanges, String input, int startLine, int endLine) {
		assertContainsRegionWithCollapsedState(projectionRanges, input, startLine, endLine, true);
	}

	public static void assertContainsExpandedRegionUsingStartAndEndLine(List<ProjectionRegion> projectionRanges, String input, int startLine, int endLine) {
		assertContainsRegionWithCollapsedState(projectionRanges, input, startLine, endLine, false);
	}

	private static void assertContainsRegionWithCollapsedState(List<ProjectionRegion> projectionRanges, String input, int startLine, int endLine, boolean collapsed) {
		StartEnd startEnd= getStartEnd(input, startLine, endLine);
		int expectedRegionBegin= startEnd.start();
		int expectedRegionLength= startEnd.end() - startEnd.start() + 1;

		for (ProjectionRegion projectionRegion : projectionRanges) {
			if (projectionRegion.offset() == expectedRegionBegin && projectionRegion.length() == expectedRegionLength) {
				assertEquals(collapsed, projectionRegion.collapsed(),
						"incorrect collapsed state for region from line " + startLine + " to line " + endLine + " (offset: " + expectedRegionBegin
								+ ", length: " + expectedRegionLength + ")");
				return;
			}
		}

		fail(
				"missing region from line " + startLine + " to line " + endLine + " (offset: " + expectedRegionBegin
						+ ", length: " + expectedRegionLength + ")" +
						", actual regions: " + sorted(projectionRanges));
	}

	private static StartEnd getStartEnd(String input, int startLine, int endLine) {
		assertTrue(startLine <= endLine, "start line must be smaller or equal to end line");
		int startLineBegin= findLineStartIndex(input, startLine);
		int endLineBegin= findLineStartIndex(input, endLine);
		int endLineEnd= findNextLineStart(input, endLineBegin);
		endLineEnd= getLastIndexIfNotFound(input, endLineEnd);
		int expectedRegionBegin= startLineBegin;
		int expectedRegionEnd= endLineEnd;
		return new StartEnd(expectedRegionBegin, expectedRegionEnd);
	}

	static void assertDoesNotContainRegionWithOffsetAndLength(List<ProjectionRegion> projectionRanges, int startLine, int endLine, int expectedRegionBegin, int expectedRegionEnd) {
		int expectedRegionLength= expectedRegionEnd - expectedRegionBegin + 1;

		for (ProjectionRegion region : projectionRanges) {
			if (region.offset() == expectedRegionBegin && region.length() == expectedRegionLength) {
				fail(
						"The region from line " + startLine + " to line " + endLine + " (offset: " + expectedRegionBegin
								+ ", length: " + expectedRegionLength + ")" +
								" shouldn't exist, actual regions: " + sorted(projectionRanges));
			}
		}
	}

	static void assertContainsRegionWithOffsetAndLength(List<ProjectionRegion> projectionRanges, int startLine, int endLine, int expectedRegionBegin, int expectedRegionEnd) {
		int expectedRegionLength= expectedRegionEnd - expectedRegionBegin + 1;

		for (ProjectionRegion region : projectionRanges) {
			if (region.offset() == expectedRegionBegin && region.length() == expectedRegionLength) {
				return;
			}
		}

		fail(
				"missing region from line " + startLine + " to line " + endLine + " (offset: " + expectedRegionBegin
						+ ", length: " + expectedRegionLength + ")" +
						", actual regions: " + sorted(projectionRanges));
	}

	/**
	 * @return a sorted copy of the original regions, for better debugging:
	 *         <ul>
	 *         <li>first by offset (ascending)</li>
	 *         <li>Then by length (descending i.e. longer regions first)</li>
	 *         </ul>
	 */
	private static Collection<ProjectionRegion> sorted(Collection<ProjectionRegion> regions) {
		List<ProjectionRegion> sortedRegions= new ArrayList<>(regions);
		sortedRegions.sort(Comparator.comparingInt(ProjectionRegion::offset).thenComparing(Comparator.comparingInt(ProjectionRegion::length).reversed()));
		return sortedRegions;
	}

	private static int getLastIndexIfNotFound(String input, int startLineEnd) {
		if (startLineEnd == -1) {
			startLineEnd= input.length() - 1;
		}
		return startLineEnd;
	}

	static int findLineStartIndex(String input, int lineNumber) {
		int currentInputIndex= -1;
		for (int i= 0; i < lineNumber; i++) {
			currentInputIndex= findNextLineStart(input, currentInputIndex + 1);
			if (currentInputIndex == -1) {
				fail("line number is greater than the total number of lines");
			}
		}
		return currentInputIndex + 1;
	}

	private static int findNextLineStart(String input, int currentInputIndex) {
		return input.indexOf('\n', currentInputIndex);
	}
}
