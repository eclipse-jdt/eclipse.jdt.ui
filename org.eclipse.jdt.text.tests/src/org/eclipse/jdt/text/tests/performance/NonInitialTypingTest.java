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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

public class NonInitialTypingTest extends TestCase {
	
	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private PerformanceMeterFactory fPerformanceMeterFactory= new SystemTimePerformanceMeterFactory();

	private ITextEditor fEditor;
	
	private static final char[] METHOD= ("public int foobar(int iParam, Object oParam) {\r" +
			"return 42;\r" +
			"}\r").toCharArray();

	protected void setUp() throws PartInitException, BadLocationException {
		EditorTestHelper.runEventQueue();
		fEditor= (ITextEditor) EditorTestHelper.openInEditor(EditorTestHelper.findFile(FILE), true);
		// dirty editor to avoid initial dirtying / validate edit costs
		dirtyEditor();
	}
	
	private void dirtyEditor() {
		fEditor.getSelectionProvider().setSelection(new TextSelection(0, 0));
		EditorTestHelper.runEventQueue();
		sleep(1000);
		
		Display display= SWTEventHelper.getActiveDisplay();
		SWTEventHelper.pressCharacter(display, '{');
		SWTEventHelper.pressKeyCode(display, SWT.BS);
		
	}

	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
	}

	public void testTypeAMethod() throws BadLocationException {
		PerformanceMeter meter= fPerformanceMeterFactory.createPerformanceMeter(this);
		try {
			Display display= SWTEventHelper.getActiveDisplay();
			int offset= getInsertPosition();
			fEditor.getSelectionProvider().setSelection(new TextSelection(offset, 0));
			EditorTestHelper.runEventQueue();
			sleep(1000);
			
			meter.start();
			for (int i= 0; i < METHOD.length; i++) {
				SWTEventHelper.pressCharacter(display, METHOD[i]);
			}
			meter.stop();
			
		} finally {
			EditorTestHelper.revertEditor(fEditor, true);
			meter.commit();
		}
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
	
	private int getInsertPosition() throws BadLocationException {
		IDocument document= EditorTestHelper.getDocument(fEditor);
		int lines= document.getNumberOfLines();
		int offset= document.getLineOffset(lines - 2);
		return offset;
	}
}
