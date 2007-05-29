/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

public class AbstractDocumentLineDifferTest extends TextPerformanceTestCase {
	private static final class TestReferenceProvider implements IQuickDiffReferenceProvider {

		private final IDocument fDocument;
		
		public TestReferenceProvider(IDocument reference) {
			fDocument= reference;
		}
		
		public IDocument getReference(IProgressMonitor monitor) throws CoreException {
			return fDocument;
		}

		public void dispose() {
		}

		public String getId() {
			return "testProvider";
		}

		public void setActiveEditor(ITextEditor editor) {
		}

		public boolean isEnabled() {
			return true;
		}

		public void setId(String id) {
		}

	}
	

	protected static final String FAUST1;
	protected static final String FAUST_FEW_CHANGES;
	protected static final String SMALL_FAUST1;
	protected static final String SMALL_FAUST_MANY_CHANGES;
	protected static final String SMALL_FAUST_MANY_CHANGES_SAME_SIZE;
	
	static {
		String faust;
		try {
			faust= FileTool.read(new InputStreamReader(AbstractDocumentLineDifferTest.class.getResourceAsStream("faust1.txt"))).toString();
		} catch (IOException x) {
			faust= "";
			x.printStackTrace();
		}
		FAUST1= faust;
		
		FAUST_FEW_CHANGES= FAUST1.replaceAll("MARGARETE", "GRETCHEN");
		
		SMALL_FAUST1= FAUST1.substring(0, 20000);
		SMALL_FAUST_MANY_CHANGES= SMALL_FAUST1.replaceAll("\n", "\n_");
		SMALL_FAUST_MANY_CHANGES_SAME_SIZE= SMALL_FAUST1.replaceAll(".\n", "_\n");
		
	}
	protected static final long MAX_WAIT= 10000; // wait 10 seconds at most
	
	private Exception fFirstException;
	private TestReferenceProvider fReferenceProvider;

	protected void setUpFast() throws Exception {
		setWarmUpRuns(10);
		setMeasuredRuns(10);

		IDocument reference= new Document(FAUST1);
		fReferenceProvider= new TestReferenceProvider(reference);
	}
	
	protected void setUpSlow() throws Exception {
		setWarmUpRuns(2);
		setMeasuredRuns(3);
		
		IDocument reference= new Document(SMALL_FAUST1);
		fReferenceProvider= new TestReferenceProvider(reference);
	}
	
	protected void tearDown() throws Exception {
		if (fFirstException != null)
			throw fFirstException;
		
		super.tearDown();
	}
	
	protected final void setUpDiffer(DocumentLineDiffer differ) {
		differ.setReferenceProvider(fReferenceProvider);
	}

}
