/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

public class RevertEditorTest extends TestCase {
	
	public static final int N_OF_COPIES= 20;

	public static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	public static final String FILE_PREFIX= "TextLayout";

	public static final String FILE_SUFFIX= ".java";
	
	private PerformanceMeterFactory fPerformanceMeterFactory= new OSPerformanceMeterFactory();

	private static final String REPLACE_TEXT= "XXX";

	protected void setUp() {
		EditorTestHelper.runEventQueue();
	}
	
	public void testRevertJavaEditor() throws PartInitException, BadLocationException {
		// cold run
		measure();
	}

	private void measure() throws PartInitException, BadLocationException {
		PerformanceMeter performanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		try {
			IFile[] files= EditorTestHelper.findFiles(OpenEditorTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, N_OF_COPIES);
			for (int i= 0, n= files.length; i < n; i++) {
				ITextEditor part= (ITextEditor) EditorTestHelper.openInEditor(files[i], true);
				dirtyEditor(part);
				performanceMeter.start();
				EditorTestHelper.revertEditor(part);
				performanceMeter.stop();
				sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			}
		} finally {
			EditorTestHelper.closeAllEditors();
			performanceMeter.commit();
		}
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}

	private void dirtyEditor(ITextEditor part) throws BadLocationException {
		IDocument document= EditorTestHelper.getDocument(part);
		int line= document.getNumberOfLines() / 2; // dirty middle line
		int offset= document.getLineOffset(line);
		document.replace(offset, 0, REPLACE_TEXT);
		EditorTestHelper.runEventQueue(part);
	}
}
