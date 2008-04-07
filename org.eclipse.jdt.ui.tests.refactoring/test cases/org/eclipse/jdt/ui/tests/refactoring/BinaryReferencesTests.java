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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.core.search.SearchMatch;

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
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass~compareTo~Lsource.BaseClass;"
		});
	}
	
	public void testRenameType02() throws Exception {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE);
		descriptor.setJavaElement(fgTestSetup.getSourceProject().findType("source.Color"));
		descriptor.setNewName("Colour");
		descriptor.setUpdateSimilarDeclarations(true);
		descriptor.setMatchStrategy(RenameJavaElementDescriptor.STRATEGY_SUFFIX);
		descriptor.setUpdateReferences(true);
		
		RefactoringStatus status= new RefactoringStatus();
		Refactoring refactoring= descriptor.createRefactoring(status);
		assertTrue(status.isOK());
		
		CheckConditionsOperation op= new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
		op.run(null);
		RefactoringStatus validationStatus= op.getStatus();
		assertTrue(!validationStatus.hasFatalError());
		assertTrue(validationStatus.hasError());
		assertEquals(2, validationStatus.getEntries().length);
		
		ReferencesInBinaryContext context= (ReferencesInBinaryContext) validationStatus.getEntryAt(0).getContext();
		List matches= context.getMatches();
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass~paintColor~Lsource.Color;"
		});
		
		context= (ReferencesInBinaryContext) validationStatus.getEntryAt(1).getContext();
		matches= context.getMatches();
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
	
	private List doTestRenameMethod(String typeName, String methodName) throws JavaModelException, Exception, CoreException {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		IType baseClass= fgTestSetup.getSourceProject().findType(typeName);
		IMethod method= baseClass.getMethod(methodName, new String[0]);
		if (! method.exists()) {
			IMethod[] methods= baseClass.getMethods();
			for (int i= 0; i < methods.length; i++) {
				if (methods[i].getElementName().equals(methodName)) {
					method= methods[i];
					break;
				}
			}
		}
		descriptor.setJavaElement(method);
		descriptor.setNewName("newName");
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
		return context.getMatches();
	}

	private void assertContainsMatches(List matches, String[] expectedHandleIdentifiers) {
		int matchCount= matches.size();
		assertTrue("match count too small: " + matchCount, matchCount >= expectedHandleIdentifiers.length);
		
		List actual= new ArrayList();
		for (int i= 0; i < matchCount; i++) {
			SearchMatch match= (SearchMatch) matches.get(i);
			String handleIdentifier= ((IJavaElement) match.getElement()).getHandleIdentifier();
			actual.add(handleIdentifier);
		}
		List expected= new ArrayList(Arrays.asList(expectedHandleIdentifiers));
		expected.removeAll(actual);
		if (expected.size() != 0)
			assertEquals("not all expected matches", expected.toString(), actual.toString());
	}

	public void testRenameVirtualMethod01() throws Exception {
		List matches= doTestRenameMethod("source.BaseClass", "baseMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass~baseMethod"
		});
	}
	
	public void testRenameVirtualMethod02() throws Exception {
		List matches= doTestRenameMethod("source.BaseClass", "compareTo");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass~compareTo~Lsource.BaseClass;"
		});
	}
	
	public void testRenameVirtualMethod03() throws Exception {
		List matches= doTestRenameMethod("source.BaseClass", "referencedVirtualMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;",
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;",
				"=BinaryReference/binary<ref(SubClass.class[SubClass~baseMethod"
		});
	}
	
	public void testRenameNonVirtualMethod01() throws Exception {
		List matches= doTestRenameMethod("source.BaseClass", "referencedMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
	
	public void testRenameNonVirtualMethod02() throws Exception {
		List matches= doTestRenameMethod("source.BaseClass", "referencedStaticMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
}
