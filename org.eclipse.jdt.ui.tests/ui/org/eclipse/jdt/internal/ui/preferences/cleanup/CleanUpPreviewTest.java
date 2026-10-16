/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;

public class CleanUpPreviewTest {

	private Shell fShell;
	private CleanUpPreview fPreview;
	private String fPreviewText;

	@BeforeEach
	public void setUp() {
		fShell= new Shell(Display.getDefault());
		fPreview= new CleanUpPreview(fShell, new ICleanUpConfigurationUI() {
			@Override
			public void setOptions(CleanUpOptions options) {
			}

			@Override
			public Composite createContents(Composite parent) {
				return parent;
			}

			@Override
			public int getCleanUpCount() {
				return 0;
			}

			@Override
			public int getSelectedCleanUpCount() {
				return 0;
			}

			@Override
			public String getPreview() {
				return fPreviewText;
			}
		});
	}

	@AfterEach
	public void tearDown() {
		fShell.dispose();
	}

	@Test
	public void testFirstPreviewIsNotHighlighted() {
		showPreview("a\nb\nc\n");
		assertEquals(List.of(), highlightedLines());
	}

	@Test
	public void testChangedLinesAreHighlighted() {
		showPreview("a\nb\nc\nd\n");
		showPreview("a\nB\nc\nD\n");
		assertEquals(List.of(1, 3), highlightedLines());
	}

	@Test
	public void testDeletionHighlightsFollowingLine() {
		showPreview("a\nb\nc\n");
		showPreview("a\nc\n");
		assertEquals(List.of(1), highlightedLines());
	}

	@Test
	public void testDeletionAtEndHighlightsLastLine() {
		showPreview("a\nb\nc");
		showPreview("a\nb");
		assertEquals(List.of(1), highlightedLines());
	}

	@Test
	public void testUnchangedPreviewClearsHighlight() {
		showPreview("a\nb\n");
		showPreview("a\nB\n");
		showPreview("a\nB\n");
		assertEquals(List.of(), highlightedLines());
	}

	private void showPreview(String text) {
		fPreviewText= text;
		fPreview.update();
		Display display= fShell.getDisplay();
		while (display.readAndDispatch()) {
			// process the asynchronous update
		}
	}

	private List<Integer> highlightedLines() {
		StyledText widget= (StyledText) fPreview.getControl();
		List<Integer> lines= new ArrayList<>();
		for (int i= 0; i < widget.getLineCount(); i++) {
			if (widget.getLineBackground(i) != null)
				lines.add(i);
		}
		return lines;
	}
}
