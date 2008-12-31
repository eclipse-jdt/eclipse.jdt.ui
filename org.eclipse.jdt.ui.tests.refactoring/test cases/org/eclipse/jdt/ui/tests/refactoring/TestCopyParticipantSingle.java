/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.CopyArguments;
import org.eclipse.ltk.core.refactoring.participants.CopyParticipant;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

public class TestCopyParticipantSingle extends CopyParticipant {

	static List fgInstances= new ArrayList();

	private Object fElement;
	private String fHandle;

	public boolean initialize(Object element) {
		fgInstances.add(this);
		fElement= element;
		ref(fElement);
		if (fElement instanceof IJavaElement) {
			fHandle= ((IJavaElement)fElement).getHandleIdentifier();
		} else if (fElement instanceof IResource) {
			fHandle= ((IResource)fElement).getFullPath().toString();
		} else if (element instanceof JavaElementResourceMapping) {
			fHandle= ((JavaElementResourceMapping)element).
				getJavaElement().getHandleIdentifier() + "_mapping";
		}
		return true;
	}

	public String getName() {
		return getClass().getName();
	}

	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		return null;
	}

	public static void testNumberOfInstances(int instances) {
		Assert.assertEquals(instances, fgInstances.size());
	}

	public static void testElements(String[] handles) {
		testNumberOfInstances(handles.length);
		List l1= new ArrayList(Arrays.asList(handles));
		for (int i= 0; i < l1.size(); i++) {
			Assert.assertTrue(l1.contains(getInstance(i).fHandle));
		}
	}

	public static void testArguments(CopyArguments[] args) {
		testNumberOfInstances(args.length);
		for (int i= 0; i < args.length; i++) {
			CopyArguments expected= args[i];
			CopyArguments actual= getInstance(i).getArguments();
			TestCopyParticipantShared.compareArguments(expected, actual);
		}
	}

	public static void reset() {
		fgInstances= new ArrayList();
	}

	private static TestCopyParticipantSingle getInstance(int i) {
		return ((TestCopyParticipantSingle)fgInstances.get(i));
	}

	/* package */ void ref(Object element) {
	}
}
