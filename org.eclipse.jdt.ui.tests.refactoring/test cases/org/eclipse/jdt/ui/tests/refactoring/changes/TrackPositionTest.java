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
package org.eclipse.jdt.ui.tests.refactoring.changes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ltk.core.refactoring.DocumentChange;

import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;

public class TrackPositionTest extends TestCase {

	private static final Class THIS= TrackPositionTest.class;
	private static final String NN= "N.N";

	private IDocument fDocument;
	private DocumentChange fChange;

	public TrackPositionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(THIS);
	}

	protected void setUp() throws Exception {
		fDocument= new Document("0123456789");
		fChange= new DocumentChange(NN, fDocument);
		fChange.setKeepPreviewEdits(true);
		fChange.initializeValidationData(new NullProgressMonitor());
	}

	protected void tearDown() throws Exception {
		fChange= null;
	}

	public void test1() throws Exception {
		TextEdit edit= new ReplaceEdit(2, 2, "xyz");
		TextChangeCompatibility.addTextEdit(fChange, NN, edit);
		executeChange();
		assertEquals(edit.getRegion(), 2, 3);
	}

	public void test2() throws Exception {
		TextEdit edit= new ReplaceEdit(5, 3, "xy");
		TextChangeCompatibility.addTextEdit(fChange, NN, edit);
		IDocument preview= fChange.getPreviewDocument(new NullProgressMonitor());
		assertEquals(fDocument.get(), "0123456789");
		assertEquals(preview.get(), "01234xy89");
		assertEquals(fChange.getPreviewEdit(edit).getRegion(), 5, 2);
	}

	private void executeChange() throws Exception {
		try {
			assertTrue(!fChange.isValid(new NullProgressMonitor()).hasFatalError());
			fChange.perform(new NullProgressMonitor());
		} finally {
			fChange.dispose();
		}
	}

	private static void assertEquals(IRegion r, int offset, int length) {
		assertEquals("Offset", offset, r.getOffset());
		assertEquals("Length", length, r.getLength());
	}
}
