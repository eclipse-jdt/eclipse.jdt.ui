/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.util.Iterator;

import org.eclipse.swt.custom.StyleRange;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.tests.TestTextViewer;

import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;



public class JavaColoringTest extends TestCase {

	protected TestTextViewer fTextViewer;
	protected IDocument fDocument;
	protected JavaTextTools fTextTools;

	public JavaColoringTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() {

		IPreferenceStore store= new PreferenceStore();
		fTextTools= new JavaTextTools(store);

		fTextViewer= new TestTextViewer();

		fDocument= new Document();
		IDocumentPartitioner partitioner= fTextTools.createDocumentPartitioner();
		partitioner.connect(fDocument);
		fDocument.setDocumentPartitioner(partitioner);

		IPreferenceStore generalTextStore= EditorsUI.getPreferenceStore();
		IPreferenceStore combinedStore= new ChainedPreferenceStore(new IPreferenceStore[] { store, generalTextStore });

		SourceViewerConfiguration conf= new JavaSourceViewerConfiguration(fTextTools.getColorManager(), combinedStore, null, null);
		IPresentationReconciler reconciler= conf.getPresentationReconciler(fTextViewer);
		reconciler.install(fTextViewer);

		System.out.print("------ next ---------\n");

	}

	public static Test suite() {
		return new TestSuite(JavaColoringTest.class);
	}

	@Override
	protected void tearDown () {

		fTextTools.dispose();
		fTextTools= null;

		fTextViewer= null;
		fDocument= null;
	}

	String print(TextPresentation presentation) {

		StringBuffer buf= new StringBuffer();

		if (presentation != null) {

			buf.append("Default style range: ");
			StyleRange range= presentation.getDefaultStyleRange();
			if (range != null)
				buf.append(range.toString());
			buf.append('\n');

			Iterator<StyleRange> e= presentation.getAllStyleRangeIterator();
			while (e.hasNext()) {
				buf.append(e.next().toString());
				buf.append('\n');
			}
		}

		return buf.toString();
	}

	public void testSimple() {
		fDocument.set("xx //");
		fTextViewer.setDocument(fDocument);
		System.out.print(print(fTextViewer.getTextPresentation()));
	}

	public void testTypingWithPartitionChange() {
		try {
			fTextViewer.setDocument(fDocument);
			fDocument.replace(0, 0, "x/");
			System.out.print(print(fTextViewer.getTextPresentation()));
			fDocument.replace(2,0, "/");
			System.out.print(print(fTextViewer.getTextPresentation()));
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}

	public void testTogglingPartitions() {
		try {
			fTextViewer.setDocument(fDocument);
			fDocument.replace(0, 0, "\t/*\n\tx\n\t/*/\n\ty\n//\t*/");
			System.out.print(print(fTextViewer.getTextPresentation()));
			fDocument.replace(0,0, "//");
			System.out.print(print(fTextViewer.getTextPresentation()));
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
}
