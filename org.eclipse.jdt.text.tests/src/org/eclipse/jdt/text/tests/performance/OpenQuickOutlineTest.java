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

import java.io.IOException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

public class OpenQuickOutlineTest extends TestCase {
	
	private static final String PATH= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/";
	
	private static final String ORIG_NAME= "StyledText";
	
	private static final String ORIG_FILE= PATH + ORIG_NAME + ".java";

	private static final int N_OF_RUNS= 20;

	private PerformanceMeter fFirstMeter;

	private PerformanceMeter fSecondMeter;

	private static final String OUTLINE_VIEW= "org.eclipse.ui.views.ContentOutline";

	private boolean fWasOutlineViewShown;
	
	protected void setUp() throws Exception {
		fFirstMeter= Performance.createPerformanceMeterFactory().createPerformanceMeter(this, "cold");
		fSecondMeter= Performance.createPerformanceMeterFactory().createPerformanceMeter(this, "warm");
		fWasOutlineViewShown= EditorTestHelper.hideView(OUTLINE_VIEW);
		ResourceTestHelper.replicate(ORIG_FILE, PATH + ORIG_NAME, ".java", N_OF_RUNS, ORIG_NAME, ORIG_NAME);
		ResourceTestHelper.incrementalBuild();
	}
	
	protected void tearDown() throws Exception {
		for (int i= 0; i < N_OF_RUNS; i++)
			ResourceTestHelper.delete(PATH + ORIG_NAME + i + ".java");
		if (fWasOutlineViewShown)
			EditorTestHelper.showView(OUTLINE_VIEW);
	}

	public void testOpenQuickOutline1() throws IOException, CoreException {
		for (int i= 0; i < N_OF_RUNS; i++) {
			String name= ORIG_NAME + i;
			String file= PATH + name + ".java";
			ITextEditor editor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(file), true);
			EditorTestHelper.sleep(5000);
			EditorTestHelper.calmDown(5000, 100);
			
			measureOpenQuickOutline(editor, fFirstMeter);
			measureOpenQuickOutline(editor, fSecondMeter);
			
			EditorTestHelper.closeAllEditors();
		}
		fFirstMeter.commit();
		fSecondMeter.commit();
	}

	private void measureOpenQuickOutline(ITextEditor editor, PerformanceMeter performanceMeter) {
		IAction showOutline= editor.getAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		assertTrue(EditorTestHelper.calmDown(1000, 100));
		performanceMeter.start();
		runAction(showOutline);
		performanceMeter.stop();
		Shell shell= SWTEventHelper.getActiveDisplay().getActiveShell();
		assertEquals("", shell.getText());
		shell.close();
		shell= SWTEventHelper.getActiveDisplay().getActiveShell();
		assertFalse("".equals(shell.getText()));
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
