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

import org.eclipse.jdt.text.tests.performance.data.MeteringSession;
import org.eclipse.jdt.text.tests.performance.data.PerfMsrDimensions;
import org.eclipse.jdt.text.tests.performance.eval.AbsoluteBandChecker;
import org.eclipse.jdt.text.tests.performance.eval.Evaluator;
import org.eclipse.jdt.text.tests.performance.eval.IEvaluator;
import org.eclipse.jdt.text.tests.performance.eval.AssertChecker;
import org.eclipse.jdt.text.tests.performance.eval.RelativeBandChecker;

public abstract class RevertEditorTest extends TestCase {
	private static final int RUNS= 20;
	private static final String REPLACE_TEXT= "XXX"; //$NON-NLS-1$
	
	private PerformanceMeterFactory fPerformanceMeterFactory= Performance.createPerformanceMeterFactory();
	private PerformanceMeter fPerformanceMeter;
	private IEvaluator fEvaluator;
	
	
	protected void setUp() throws Exception {
		fPerformanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		fEvaluator= new Evaluator();
		AssertChecker cpuLessThan110Percent= new RelativeBandChecker(PerfMsrDimensions.CPU_TIME, 0.0F, 1.1F);
		AssertChecker workingSetWithin1MB= new AbsoluteBandChecker(PerfMsrDimensions.WORKING_SET, 1024*1024, 1024*1024);
		fEvaluator.setAssertCheckers(new AssertChecker[] {cpuLessThan110Percent, workingSetWithin1MB});
		fEvaluator.setReferenceFilterProperties("3.0-runtime2", fPerformanceMeter.getScenarioName(), null, "1092146708700");
	}
	
	protected void measureRevert(IFile file) throws PartInitException, BadLocationException {
		ITextEditor part= (ITextEditor) EditorTestHelper.openInEditor(file, true);
		for (int i= 0; i < RUNS; i++) {
			dirtyEditor(part);
			fPerformanceMeter.start();
			EditorTestHelper.revertEditor(part, true);
			fPerformanceMeter.stop();
			sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			EditorTestHelper.runEventQueue(part);
		}
		
		fPerformanceMeter.commit();
		MeteringSession session= fPerformanceMeter.getSessionData();
		fEvaluator.evaluate(session);
	}
	
	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}

	protected void dirtyEditor(ITextEditor part) throws BadLocationException {
		IDocument document= EditorTestHelper.getDocument(part);
		int line= document.getNumberOfLines() / 2; // dirty middle line
		int offset= document.getLineOffset(line);
		document.replace(offset, 0, REPLACE_TEXT);
		EditorTestHelper.runEventQueue(part);
	}
}
