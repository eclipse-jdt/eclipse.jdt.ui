/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.core.search.TypeReferenceMatch;

import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;

public class BinaryReferencesTests extends TestCase {

	private static BinaryReferencesTestSetup fgTestSetup;
	
	public BinaryReferencesTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		fgTestSetup= new BinaryReferencesTestSetup(new TestSuite(BinaryReferencesTests.class));
		return fgTestSetup;
	}
	
	public static Test setUpTest(Test test) {
		fgTestSetup= new BinaryReferencesTestSetup(test);
		return fgTestSetup;
	}

	public void testRenameType01() throws Exception {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE);
		descriptor.setJavaElement(fgTestSetup.getSourceProject().findType("source.BaseClass"));
		descriptor.setNewName("RenamedBaseClass");
		descriptor.setUpdateReferences(true);
		
		RefactoringStatus status= new RefactoringStatus();
		Refactoring refactoring= descriptor.createRefactoring(status);
		assertTrue(status.isOK());
		
		CheckConditionsOperation op= new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
		op.run(null);
		RefactoringStatus validationStatus= op.getStatus();
		assertTrue(!validationStatus.hasFatalError());
		assertTrue(validationStatus.hasError());
		assertEquals(1, validationStatus.getEntries().length);
		
		ReferencesInBinaryContext context= (ReferencesInBinaryContext) validationStatus.getEntryAt(0).getContext();
		List matches= context.getMatches();
		assertEquals(3, matches.size());
		
		List expected= new ArrayList(Arrays.asList(new String[] {
			"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass",
			"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass",
			"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~compareTo~Lsource.BaseClass;"
		}));
		for (int i= 0; i < 3; i++) {
			TypeReferenceMatch match= (TypeReferenceMatch) matches.get(i);
			String handleIdentifier= ((IJavaElement) match.getElement()).getHandleIdentifier();
			assertTrue(expected.remove(handleIdentifier));
		}
	}
}
