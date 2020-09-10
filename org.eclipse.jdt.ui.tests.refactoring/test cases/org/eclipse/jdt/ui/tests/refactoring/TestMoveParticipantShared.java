/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.IJavaElement;

public class TestMoveParticipantShared extends MoveParticipant implements ISharableParticipant {

	static TestMoveParticipantShared fgInstance;

	List<Object> fElements= new ArrayList<>(3);
	List<String> fHandles= new ArrayList<>(3);
	List<RefactoringArguments> fArguments= new ArrayList<>(3);

	@Override
	public boolean initialize(Object element) {
		fgInstance= this;
		fElements.add(element);
		fArguments.add(getArguments());
		if (element instanceof IJavaElement)
			fHandles.add(((IJavaElement)element).getHandleIdentifier());
		else
			fHandles.add(((IResource)element).getFullPath().toString());
		return true;
	}

	@Override
	public void addElement(Object element, RefactoringArguments args) {
		fElements.add(element);
		fArguments.add(args);
		if (element instanceof IJavaElement)
			fHandles.add(((IJavaElement)element).getHandleIdentifier());
		else
			fHandles.add(((IResource)element).getFullPath().toString());
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		return null;
	}

	public static void testNumberOfElements(int expected) {
		if (expected == 0) {
			Assert.assertNull(fgInstance);
		} else {
			Assert.assertEquals(expected, fgInstance.fElements.size());
			Assert.assertEquals(expected, fgInstance.fArguments.size());
		}
	}

	public static void testArguments(MoveArguments[] args) {
		testNumberOfElements(args.length);
		for (int i= 0; i < args.length; i++) {
			MoveArguments expected= args[i];
			MoveArguments actual= (MoveArguments)fgInstance.fArguments.get(i);
			Assert.assertEquals(expected.getDestination(), actual.getDestination());
			Assert.assertEquals(expected.getUpdateReferences(), actual.getUpdateReferences());
		}
	}

	public static void reset() {
		fgInstance= null;
	}
}
