/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.reorg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockWorkbenchSite;

import org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.PasteAction;


public class PasteActionTest extends RefactoringTest{

	private Clipboard fClipboard;
	private static final Class clazz= PasteActionTest.class;

	public PasteActionTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected void setUp() throws Exception {
		super.setUp();
		fClipboard= new Clipboard(Display.getDefault());
	}
	protected void tearDown() throws Exception {
		super.tearDown();
		fClipboard.dispose();
	}

	private static Object[] merge(Object[] array1, Object[] array2) {
		Set elements= new HashSet(array1.length + array2.length);
		elements.addAll(Arrays.asList(array1));
		elements.addAll(Arrays.asList(array2));
		return elements.toArray();
	}
	
	private void verifyDisabled(IResource[] copySelectedResources, IJavaElement[] copySelectedJavaElements, IResource[] pasteSelectedResources, IJavaElement[] pasteSelectedJavaElements) throws JavaModelException {
		Object[] pasteSelection= merge(pasteSelectedResources, pasteSelectedJavaElements);
		
		PasteAction pasteAction= new PasteAction(new MockWorkbenchSite(pasteSelection), fClipboard);
		CopyToClipboardAction copyToClipboardAction= new CopyToClipboardAction(new MockWorkbenchSite(merge(copySelectedResources, copySelectedJavaElements)), fClipboard, pasteAction);
		copyToClipboardAction.setAutoRepeatOnFailure(true);
		copyToClipboardAction.update(copyToClipboardAction.getSelection());
		assertTrue("copy not enabled", copyToClipboardAction.isEnabled());
		copyToClipboardAction.run();
		
		pasteAction.update(pasteAction.getSelection());
		assertTrue("paste should be disabled", ! pasteAction.isEnabled());
	}

	private void verifyEnabled(IResource[] copySelectedResources, IJavaElement[] copySelectedJavaElements, IResource[] pasteSelectedResources, IJavaElement[] pasteSelectedJavaElements) throws JavaModelException {
		Object[] pasteSelection= merge(pasteSelectedResources, pasteSelectedJavaElements);
		
		PasteAction pasteAction= new PasteAction(new MockWorkbenchSite(pasteSelection), fClipboard);
		CopyToClipboardAction copyToClipboardAction= new CopyToClipboardAction(new MockWorkbenchSite(merge(copySelectedResources, copySelectedJavaElements)), fClipboard, pasteAction);
		copyToClipboardAction.setAutoRepeatOnFailure(true);
		copyToClipboardAction.update(copyToClipboardAction.getSelection());
		assertTrue("copy not enabled", copyToClipboardAction.isEnabled());
		copyToClipboardAction.run();
		
		pasteAction.update(pasteAction.getSelection());
		assertTrue("paste should be enabled", pasteAction.isEnabled());
	}

	public void testEnabled_javaProject() throws Exception {
		System.out.println("testEnabled_javaProject - disabled due to failure under Motif");
		if (true)
			return;
		IJavaElement[] javaElements= {MySetup.getProject()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements, new IResource[0], new IJavaElement[0]);
	}

	public void testEnabled_project() throws Exception {
		IJavaElement[] javaElements= {};
		IResource[] resources= {MySetup.getProject().getProject()};
		verifyEnabled(resources, javaElements, new IResource[0], new IJavaElement[0]);
	}
}
