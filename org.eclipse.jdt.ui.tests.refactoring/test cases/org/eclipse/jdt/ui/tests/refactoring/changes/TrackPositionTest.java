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
package org.eclipse.jdt.ui.tests.refactoring.changes;

import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ltk.core.refactoring.DocumentChange;

import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;

public class TrackPositionTest {
	private static final String NN= "N.N";

	private IDocument fDocument;
	private DocumentChange fChange;

	@Before
	public void setUp() throws Exception {
		fDocument= new Document("0123456789");
		fChange= new DocumentChange(NN, fDocument);
		fChange.setKeepPreviewEdits(true);
		fChange.initializeValidationData(new NullProgressMonitor());
	}

	@After
	public void tearDown() throws Exception {
		fChange= null;
	}

	@Test
	public void test1() throws Exception {
		TextEdit edit= new ReplaceEdit(2, 2, "xyz");
		TextChangeCompatibility.addTextEdit(fChange, NN, edit);
		executeChange();
		assertEquals(edit.getRegion(), 2, 3);
	}

	@Test
	public void test2() throws Exception {
		TextEdit edit= new ReplaceEdit(5, 3, "xy");
		TextChangeCompatibility.addTextEdit(fChange, NN, edit);
		IDocument preview= fChange.getPreviewDocument(new NullProgressMonitor());
		Assert.assertEquals("0123456789", fDocument.get());
		Assert.assertEquals("01234xy89", preview.get());
		assertEquals(fChange.getPreviewEdit(edit).getRegion(), 5, 2);
	}

	private void executeChange() throws Exception {
		try {
			assertFalse(fChange.isValid(new NullProgressMonitor()).hasFatalError());
			fChange.perform(new NullProgressMonitor());
		} finally {
			fChange.dispose();
		}
	}

	private static void assertEquals(IRegion r, int offset, int length) {
		Assert.assertEquals("Offset", offset, r.getOffset());
		Assert.assertEquals("Length", length, r.getLength());
	}
}
