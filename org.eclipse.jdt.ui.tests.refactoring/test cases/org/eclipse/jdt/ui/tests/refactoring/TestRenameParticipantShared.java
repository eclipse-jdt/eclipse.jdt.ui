/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.refactoring.tagging.IDerivedElementUpdating;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IDerivedElementRefactoringProcessor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

public class TestRenameParticipantShared extends RenameParticipant implements ISharableParticipant {

	static TestRenameParticipantShared fgInstance;
	List fElements= new ArrayList(3);
	List fHandles= new ArrayList(3);
	List fArguments= new ArrayList(3);
	Map fDerived= new HashMap();

	public boolean initialize(Object element) {
		fgInstance= this;
		fElements.add(element);
		fArguments.add(getArguments());
		if (element instanceof IJavaElement)
			fHandles.add(((IJavaElement)element).getHandleIdentifier());
		else
			fHandles.add(((IResource)element).getFullPath().toString());
		
		IDerivedElementRefactoringProcessor updating= (IDerivedElementRefactoringProcessor)getProcessor().getAdapter(IDerivedElementUpdating.class);
		if ((updating != null) && (getArguments().getUpdateDerivedElements())) { 
			Object[] elements= updating.getDerivedElements();
			for (int i= 0; i < elements.length; i++) {
				IJavaElement updated= (IJavaElement)updating.getRefactoredElement(elements[i]);
				if (updated!=null) 
					fDerived.put(((IJavaElement) elements[i]).getHandleIdentifier(), updated.getElementName());
			}
		}
		
		return true;
	}

	public void addElement(Object element, RefactoringArguments args) {
		fElements.add(element);
		fArguments.add(args);
		if (element instanceof IJavaElement)
			fHandles.add(((IJavaElement)element).getHandleIdentifier());
		else
			fHandles.add(((IResource)element).getFullPath().toString());
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
	
	public static void testArguments(RenameArguments[] args) {
		testNumberOfElements(args.length);
		for (int i= 0; i < args.length; i++) {
			RenameArguments expected= args[i];
			RenameArguments actual= (RenameArguments)fgInstance.fArguments.get(i);
			Assert.assertEquals(expected.getNewName(), actual.getNewName());
			Assert.assertEquals(expected.getUpdateReferences(), actual.getUpdateReferences());
		}
	}
	
	public static void reset() {
		fgInstance= null;
	}

	public static void testNumberOfDerivedElements(int expected) {
		if (expected == 0)
			Assert.assertTrue(fgInstance == null);
		else
			Assert.assertEquals(expected, fgInstance.fDerived.size());
	}

	public static void testDerivedElements(List derivedList, List derivedNewNameList) {
		for (int i=0; i< derivedList.size(); i++) {
			String handle= (String) derivedList.get(i);
			String newName= (String)derivedNewNameList.get(i);
			String actualNewName= (String)fgInstance.fDerived.get(handle);
			Assert.assertNotNull(actualNewName);
			Assert.assertEquals(newName, actualNewName);
		}
		Assert.assertEquals(derivedList.size(), fgInstance.fDerived.size());
	}
}
