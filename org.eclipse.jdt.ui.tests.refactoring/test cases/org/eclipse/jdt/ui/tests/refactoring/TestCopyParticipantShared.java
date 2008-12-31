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
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

public class TestCopyParticipantShared extends CopyParticipant implements ISharableParticipant {

	static TestCopyParticipantShared fgInstance;

	List fElements= new ArrayList(3);
	List fHandles= new ArrayList(3);
	List fArguments= new ArrayList(3);

	public boolean initialize(Object element) {
		fgInstance= this;
		fElements.add(element);
		fArguments.add(getArguments());
		if (element instanceof IJavaElement) {
			fHandles.add(((IJavaElement)element).getHandleIdentifier());
		} else if (element instanceof IResource) {
			fHandles.add(((IResource)element).getFullPath().toString());
		} else if (element instanceof JavaElementResourceMapping) {
			fHandles.add(((JavaElementResourceMapping)element).
				getJavaElement().getHandleIdentifier() + "_mapping");
		}
		return true;
	}

	public void addElement(Object element, RefactoringArguments args) {
		fElements.add(element);
		fArguments.add(args);
		if (element instanceof IJavaElement) {
			fHandles.add(((IJavaElement)element).getHandleIdentifier());
		} else if (element instanceof IResource) {
			fHandles.add(((IResource)element).getFullPath().toString());
		} else if (element instanceof JavaElementResourceMapping) {
			fHandles.add(((JavaElementResourceMapping)element).getJavaElement().getHandleIdentifier() + "_mapping");
		}
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

	public static void testNumberOfElements(int expected) {
		if (expected == 0) {
			Assert.assertTrue(fgInstance == null);
		} else {
			Assert.assertEquals(expected, fgInstance.fElements.size());
			Assert.assertEquals(expected, fgInstance.fArguments.size());
		}
	}

	public static void testArguments(CopyArguments[] args) {
		testNumberOfElements(args.length);
		for (int i= 0; i < args.length; i++) {
			CopyArguments expected= args[i];
			CopyArguments actual= (CopyArguments)fgInstance.fArguments.get(i);
			compareArguments(expected, actual);
		}
	}

	public static void compareArguments(CopyArguments expected, CopyArguments actual) {
		Assert.assertEquals("Destination: ", expected.getDestination(), actual.getDestination());
		compareExecutionLog(expected.getExecutionLog(), actual.getExecutionLog());
	}

	private static void compareExecutionLog(ReorgExecutionLog expected, ReorgExecutionLog actual) {
		Assert.assertEquals("Canceled: ", expected.isCanceled(), actual.isCanceled());
		Object[] expectedRenamed= expected.getRenamedElements();
		Object[] actualRenamed= actual.getRenamedElements();
		Assert.assertEquals(expectedRenamed.length, actualRenamed.length);
		for (int j= 0; j < expectedRenamed.length; j++) {
			Assert.assertEquals(expected.getNewName(expectedRenamed[j]), actual.getNewName(actualRenamed[j]));
		}
		Object[] expectedProcessed= expected.getProcessedElements();
		Object[] actualProcessed= actual.getProcessedElements();
		Assert.assertEquals(expectedProcessed.length, actualProcessed.length);
		for (int j= 0; j < expectedProcessed.length; j++) {
			Assert.assertEquals(expectedProcessed[j], actualProcessed[j]);
		}
	}

	public static void reset() {
		fgInstance= null;
	}
}
