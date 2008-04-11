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
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureProcessor;

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

	private static void assertContainsMatches(List matches, String[] expectedHandleIdentifiers) {
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

	private static IType findType(String typeName) throws JavaModelException {
		return fgTestSetup.getSourceProject().findType(typeName);
	}

	private static IMethod findMethod(IType type, String methodName) throws JavaModelException {
		IMethod method= type.getMethod(methodName, new String[0]);
		if (! method.exists()) {
			IMethod[] methods= type.getMethods();
			for (int i= 0; i < methods.length; i++) {
				if (methods[i].getElementName().equals(methodName)) {
					method= methods[i];
					break;
				}
			}
		}
		return method;
	}

	private static List doRefactoring(RenameJavaElementDescriptor descriptor) throws CoreException {
		RefactoringStatus status= new RefactoringStatus();
		Refactoring refactoring= descriptor.createRefactoring(status);
		assertTrue(status.isOK());
		
		return doRefactoring(refactoring);
	}

	private static List doRefactoring(Refactoring refactoring) throws CoreException {
		CheckConditionsOperation op= new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
		op.run(null);
		RefactoringStatus validationStatus= op.getStatus();
		assertTrue(!validationStatus.hasFatalError());
		assertTrue(validationStatus.hasError());
		assertEquals(1, validationStatus.getEntries().length);
		
		ReferencesInBinaryContext context= (ReferencesInBinaryContext) validationStatus.getEntryAt(0).getContext();
		return context.getMatches();
	}

	public void testRenameType01() throws Exception {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE);
		descriptor.setJavaElement(findType("source.BaseClass"));
		descriptor.setNewName("RenamedBaseClass");
		descriptor.setUpdateReferences(true);
		
		List matches= doRefactoring(descriptor);
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass~compareTo~Lsource.BaseClass;"
		});
	}
	
	public void testRenameType02() throws Exception {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE);
		descriptor.setJavaElement(findType("source.Color"));
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
	
	private static List doRenameMethod(String typeName, String methodName) throws CoreException {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		IMethod method= findMethod(findType(typeName), methodName);
		descriptor.setJavaElement(method);
		descriptor.setNewName("newName");
		descriptor.setUpdateReferences(true);
		
		return doRefactoring(descriptor);
	}

	public void testRenameVirtualMethod01() throws Exception {
		List matches= doRenameMethod("source.BaseClass", "baseMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass~baseMethod"
		});
	}
	
	public void testRenameVirtualMethod02() throws Exception {
		List matches= doRenameMethod("source.BaseClass", "compareTo");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass~compareTo~Lsource.BaseClass;"
		});
	}
	
	public void testRenameVirtualMethod03() throws Exception {
		List matches= doRenameMethod("source.BaseClass", "referencedVirtualMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;",
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;",
				"=BinaryReference/binary<ref(SubClass.class[SubClass~baseMethod"
		});
	}
	
	public void testRenameNonVirtualMethod01() throws Exception {
		List matches= doRenameMethod("source.BaseClass", "referencedMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
	
	public void testRenameNonVirtualMethod02() throws Exception {
		List matches= doRenameMethod("source.BaseClass", "referencedStaticMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
	
	private static List doRenameField(String typeName, String fieldName) throws CoreException {
		IField field= findType(typeName).getField(fieldName);
		String refactoringID= field.isEnumConstant() ? IJavaRefactorings.RENAME_ENUM_CONSTANT : IJavaRefactorings.RENAME_FIELD;
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(refactoringID);
		descriptor.setJavaElement(field);
		descriptor.setNewName(field.isEnumConstant() ? "BLA" : "newName");
		descriptor.setUpdateReferences(true);
		
		return doRefactoring(descriptor);
	}

	public void testRenameField01() throws Exception {
		List matches= doRenameField("source.BaseClass", "fProtected");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass~SubClass~I"
		});
	}
	
	public void testRenameField02() throws Exception {
		List matches= doRenameField("source.BaseClass", "fPublic");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
	
	public void testRenameField03() throws Exception {
		List matches= doRenameField("source.Color", "RED");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
	
	public void testRenameField04() throws Exception {
		if (true) // https://bugs.eclipse.org/bugs/show_bug.cgi?id=226660
			return;
		List matches= doRenameField("source.Color", "GREEN");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
	
	public void testRenamePackage01() throws Exception {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		IPackageFragment pack= findType("source.BaseClass").getPackageFragment();
		descriptor.setJavaElement(pack);
		descriptor.setNewName("newName");
		descriptor.setUpdateReferences(true);
		descriptor.setUpdateHierarchy(false);
		
		List matches= doRefactoring(descriptor);
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass",
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass",
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
		});
	}

	public void testRenamePackage02() throws Exception {
		RenameJavaElementDescriptor descriptor= new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		IPackageFragment pack= findType("source.BaseClass").getPackageFragment();
		descriptor.setJavaElement(pack);
		descriptor.setNewName("newName");
		descriptor.setUpdateReferences(true);
		descriptor.setUpdateHierarchy(true);
		
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
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass",
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass",
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
				"=BinaryReference/binary<ref(SubClass.class[SubClass",
		});
		
		context= (ReferencesInBinaryContext) validationStatus.getEntryAt(1).getContext();
		matches= context.getMatches();
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;",
				"=BinaryReference/binary<ref(SubClass.class[SubClass"
		});
	}
	
	private static List doChangeSignature(String typeName, String methodName) throws JavaModelException, Exception, CoreException {
		IMethod method= findMethod(findType(typeName), methodName);
		ChangeSignatureProcessor processor= new ChangeSignatureProcessor(method);
		
		String[] newNames= { "x" };
		String[] newTypes= { "int" };
		String[] newDefaultValues= { "0" };
		ParameterInfo[] newParamInfos= ChangeSignatureTests.createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= { 0 };
		ChangeSignatureTests.addInfos(processor.getParameterInfos(), newParamInfos, newIndices);
		
		Refactoring refactoring= new ProcessorBasedRefactoring(processor);
		
		return doRefactoring(refactoring);
	}

	public void testChangeSignature01() throws Exception {
		List matches= doChangeSignature("source.BaseClass", "baseMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass~baseMethod",
				"=BinaryReference/binary<ref(SubClass.class[SubClass~baseMethod"
		});
	}
	
	public void testChangeSignature02() throws Exception {
		List matches= doChangeSignature("source.BaseClass", "compareTo");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass~compareTo~Lsource.BaseClass;"
		});
	}
	
	public void testChangeSignature03() throws Exception {
		List matches= doChangeSignature("source.BaseClass", "referencedMethod");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
	
	public void testChangeConstructorSignature01() throws Exception {
		List matches= doChangeSignature("source.BaseClass", "BaseClass");
		assertContainsMatches(matches, new String[] {
				"=BinaryReference/binary<ref(SubClass.class[SubClass~SubClass~I",
				"=BinaryReference/binary<ref(ReferenceClass.class[ReferenceClass~main~\\[Ljava.lang.String;"
		});
	}
}
