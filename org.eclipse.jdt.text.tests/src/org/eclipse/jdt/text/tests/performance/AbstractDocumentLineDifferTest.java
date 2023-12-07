/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.performance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

public abstract class AbstractDocumentLineDifferTest extends TextPerformanceTestCase {
	private static final class TestReferenceProvider implements IQuickDiffReferenceProvider {

		private final IDocument fDocument;

		public TestReferenceProvider(IDocument reference) {
			fDocument= reference;
		}

		@Override
		public IDocument getReference(IProgressMonitor monitor) throws CoreException {
			return fDocument;
		}

		@Override
		public void dispose() {
		}

		@Override
		public String getId() {
			return "testProvider";
		}

		@Override
		public void setActiveEditor(ITextEditor editor) {
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public void setId(String id) {
		}

	}


	protected static final String FAUST1;
	protected static final String FAUST_FEW_CHANGES;
	protected static final String SMALL_FAUST1;
	protected static final String SMALL_FAUST_MANY_CHANGES;
	protected static final String SMALL_FAUST_MANY_CHANGES_SAME_SIZE;

	static {
		FAUST1= AbstractDocumentLineDifferTest.getFaust();

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

	@Override
	protected void tearDown() throws Exception {
		if (fFirstException != null)
			throw fFirstException;

		super.tearDown();
	}

	@SuppressWarnings("restriction")
	protected final void setUpDiffer(org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer differ) {
		differ.setReferenceProvider(fReferenceProvider);
	}

	static String getFaust() {
		try (InputStream resourceAsStream= AbstractDocumentLineDifferTest.class.getResourceAsStream("faust1.txt")) {
			return new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
