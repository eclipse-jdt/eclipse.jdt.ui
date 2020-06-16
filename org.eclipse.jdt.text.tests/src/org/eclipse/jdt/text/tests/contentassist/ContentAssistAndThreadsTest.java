/*******************************************************************************
 * Copyright (c) 2019, 2020 Red Hat Inc., and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.contentassist.ContentAssistant;

import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProcessor;

public class ContentAssistAndThreadsTest extends AbstractCompletionTest {
	@After
	public void resetPreference() {
		JavaPlugin.getDefault().getPreferenceStore().setToDefault(PreferenceConstants.CODEASSIST_NONUITHREAD_COMPUTATION);
	}

	@Test
	public void testComputeCompletionInNonUIThread() throws Exception {
		IJavaProject fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("Blah.java", "", true, new NullProgressMonitor());
		JavaEditor part= (JavaEditor) JavaUI.openInEditor(cu);
		ContentAssistant assistant= new ContentAssistant();
		assistant.setDocumentPartitioning(IJavaPartitions.JAVA_PARTITIONING);
		JavaCompletionProcessor javaProcessor= new JavaCompletionProcessor(part, assistant, getContentType());
		AtomicReference<Throwable> exception = new AtomicReference<>();
		List<IStatus> errors = new ArrayList<>();
		JavaPlugin.getDefault().getLog().addLogListener((status, plugin) -> {
			if (status.getSeverity() >= IStatus.WARNING) {
				errors.add(status);
			}
		});
		Thread thread = new Thread(() -> {
			try {
				javaProcessor.computeCompletionProposals(part.getViewer(), 0);
				// a popup can be shown and block the thread in case of error
			} catch (Exception e) {
				exception.set(e);
			}
		});
		thread.start();
		thread.join();
		if (exception.get() != null) {
			exception.get().printStackTrace();
		}
		assertNull(exception.get());
		assertEquals(Collections.emptyList(), errors);
	}

	@Test
	public void testLongNonUIThreadContentAssistDoesntFreezeUI() throws Exception {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.CODEASSIST_NONUITHREAD_COMPUTATION, true);
		IJavaProject fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("Blah.java", LongCompletionProposalComputer.CONTENT_TRIGGER_STRING, true, new NullProgressMonitor());
		JavaEditor part= (JavaEditor) JavaUI.openInEditor(cu);
		final Set<Shell> beforeShells = Arrays.stream(part.getSite().getShell().getDisplay().getShells()).filter(Shell::isVisible).collect(Collectors.toSet());
		Display display= part.getViewer().getTextWidget().getDisplay();
		ContentAssistAction action = (ContentAssistAction) part.getAction(ITextEditorActionConstants.CONTENT_ASSIST);
		action.update();
		CheckUIThreadReactivityThread thread = new CheckUIThreadReactivityThread(display);
		thread.start();
		display.asyncExec(() -> action.run()); // mustn't be synchronous or CheckUIThreadReactivityTest can miss it.
		try {
			assertTrue("Missing completion proposal", new org.eclipse.jdt.text.tests.performance.DisplayHelper() {
				@Override
				protected boolean condition() {
					Set<Shell> newShells = Arrays.stream(part.getSite().getShell().getDisplay().getShells()).filter(Shell::isVisible).collect(Collectors.toSet());
					newShells.removeAll(beforeShells);
					if (!newShells.isEmpty()) {
						Table completionTable = findCompletionSelectionControl(newShells.iterator().next());
						return Arrays.stream(completionTable.getItems()).map(TableItem::getText).anyMatch(LongCompletionProposalComputer.CONTENT_TRIGGER_STRING::equals);
					}
					return false;
				}
			}.waitForCondition(display, 3000));
		} finally {
			thread.interrupt();
		}
		assertTrue("UI was frozen for " + thread.getMaxDuration(), thread.getMaxDuration() < 1000);
	}

	private Table findCompletionSelectionControl(Widget control) {
		if (control instanceof Table) {
			return (Table)control;
		} else if (control instanceof Composite) {
			for (Widget child : ((Composite)control).getChildren()) {
				Table res = findCompletionSelectionControl(child);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}
}
