/*******************************************************************************
 * Copyright (c) 2025 Vector Informatik GmbH and others.
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
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public final class FoldingTestUtils {

	private FoldingTestUtils() {
	}

	public static List<IRegion> getProjectionRangesOfFile(IPackageFragment packageFragment, String fileName, String code) throws Exception {
		ICompilationUnit cu= packageFragment.createCompilationUnit(fileName, code, true, null);
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);
		ProjectionAnnotationModel model= editor.getAdapter(ProjectionAnnotationModel.class);

		List<IRegion> regions= extractRegions(model);
		editor.close(false);
		return regions;
	}

	public static List<IRegion> extractRegions(ProjectionAnnotationModel model) {
		List<IRegion> regions= new ArrayList<>();
		Iterator<Annotation> it= model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation a= it.next();
			if (a instanceof ProjectionAnnotation) {
				Position p= model.getPosition(a);
				regions.add(new Region(p.getOffset(), p.getLength()));
			}
		}
		return regions;
	}

	public static void assertCodeHasRegions(IPackageFragment packageFragment, String fileName, String code, int regionsCount) throws Exception {
		List<IRegion> regions= FoldingTestUtils.getProjectionRangesOfFile(packageFragment, fileName, code);
		assertEquals(regionsCount, regions.size(), String.format("Expected %d regions but saw %d.", regionsCount, regions.size()));
	}

	public static void assertContainsRegionUsingStartAndEndLine(List<IRegion> projectionRanges, String input, int startLine, int endLine) {
		assertTrue(startLine <= endLine, "start line must be smaller or equal to end line");
		int startLineBegin= findLineStartIndex(input, startLine);
		int endLineBegin= findLineStartIndex(input, endLine);
		int endLineEnd= findNextLineStart(input, endLineBegin);
		endLineEnd= getLengthIfNotFound(input, endLineEnd);
		int expectedRegionOffset= startLineBegin + 1;
		int expectedRegionLength= endLineEnd + 1;
		assertContainsRegionWithOffsetAndLength(projectionRanges, startLine, endLine, expectedRegionOffset, expectedRegionLength);
	}

	static void assertContainsRegionWithOffsetAndLength(List<IRegion> projectionRanges, int startLine, int endLine, int expectedRegionOffset, int expectedRegionLength) {
		for (IRegion region : projectionRanges) {
			if (region.getOffset() == expectedRegionOffset && region.getOffset() + region.getLength() == expectedRegionLength) {
				return;
			}
		}
		fail(
				"missing region from line " + startLine + " (index " + expectedRegionOffset + ") " +
						"to line " + endLine + " (index " + expectedRegionLength + ")" +
						", actual regions: " + projectionRanges
		);
	}

	private static int getLengthIfNotFound(String input, int startLineEnd) {
		if (startLineEnd == -1) {
			startLineEnd= input.length();
		}
		return startLineEnd;
	}

	static int findLineStartIndex(String input, int lineNumber) {
		int currentInputIndex= 0;
		for (int i= 0; i < lineNumber; i++) {
			currentInputIndex= findNextLineStart(input, currentInputIndex);
			if (currentInputIndex == -1) {
				fail("line number is greater than the total number of lines");
			}
		}
		return currentInputIndex;
	}

	private static int findNextLineStart(String input, int currentInputIndex) {
		return input.indexOf('\n', currentInputIndex + 1);
	}
}
