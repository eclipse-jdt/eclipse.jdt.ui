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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class RevertEditorTest extends TestCase {
	
	public static final int N_OF_COPIES= 20;

	public static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	public static final String FILE_PREFIX= "TextLayout";

	public static final String FILE_SUFFIX= ".java";
	
	private PerformanceMeterFactory fPerformanceMeterFactory= new OSPerformanceMeterFactory();

	private static final String REPLACE_TEXT= "XXX";

	protected void setUp() {
		runEventQueue(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
	}
	
	public void testRevertJavaEditor() throws PartInitException, BadLocationException {
		// cold run
		measure();
	}

	private void measure() throws PartInitException, BadLocationException {
		PerformanceMeter performanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		try {
			IFile[] files= findFiles(OpenEditorTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, N_OF_COPIES);
			for (int i= 0, n= files.length; i < n; i++) {
				IEditorPart part= openInEditor(files[i]);
				dirtyEditor(part);
				performanceMeter.start();
				revertEditor(part);
				performanceMeter.stop();
				sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			}
		} finally {
			getActivePage().closeAllEditors(false);
			performanceMeter.commit();
		}
	}

	private IEditorPart openInEditor(IFile file) throws PartInitException {
		IEditorPart part= IDE.openEditor(getActivePage(), file);
		runEventQueue(part.getSite().getShell());
		return part;
	}

	private IFile[] findFiles(String prefix, String suffix, int i, int n) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		List types= new ArrayList(n);
		for (int j= i; j < i + n; j++)
			types.add(root.getFile(new Path(root.getLocation().toString() + "/" + prefix + j + suffix)));
		return (IFile[]) types.toArray(new IFile[types.size()]);
	}
	
	private void revertEditor(IEditorPart part) {
		if (part instanceof ITextEditor) {
			ITextEditor editor= (ITextEditor) part;
			editor.doRevertToSaved();
		}
	}

	private void dirtyEditor(IEditorPart part) throws BadLocationException {
		IDocument document= getDocument(part);
		int line= document.getNumberOfLines() / 2; // dirty middle line
		int offset= document.getLineOffset(line);
		document.replace(offset, 0, REPLACE_TEXT);
		runEventQueue(part.getSite().getShell());
	}

	private IDocument getDocument(IEditorPart part) throws BadLocationException {
		if (part instanceof ITextEditor) {
			ITextEditor editor= (ITextEditor) part;
			IDocumentProvider provider= editor.getDocumentProvider();
			IEditorInput input= editor.getEditorInput();
			return provider.getDocument(input);
		} else
			throw new BadLocationException();
	}

	private void runEventQueue(Shell shell) {
		while (shell.getDisplay().readAndDispatch());
	}

	private IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
}
