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
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Collection;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * @author rfuhrer, tip
 *
 */
public class ChangeTypeRefactoringTests extends RefactoringTest {
	private static final Class clazz= ChangeTypeRefactoringTests.class;
	private static final String REFACTORING_PATH= "ChangeTypeRefactoring/";

	public ChangeTypeRefactoringTests(String name) {
		super(name);
	} 
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	private String getSimpleTestFileName(boolean input) {
		String fileName= "A_" + getName() + (input ? "_in" : "_out") + ".java";

		return fileName;
	}
	
	private String getTestFileName(boolean positive, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();

		fileName += (positive ? "positive/": "negative/");
		fileName += getSimpleTestFileName(input);
		return fileName;
	}
	
	private ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean positive, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(input), getFileContents(getTestFileName(positive, input)));
	}
	
	private ICompilationUnit createAdditionalCU(String fileName, IPackageFragment pack) throws Exception {
		String fullName= TEST_PATH_PREFIX + getRefactoringPath() + "positive/" + fileName + ".java";
		return createCU(pack, fileName + ".java", getFileContents(fullName));
	}

	private ChangeTypeRefactoring helper1(int startLine, int startColumn, int endLine, int endColumn, String selectedTypeName)
		throws Exception {
		ICompilationUnit	cu= createCUfromTestFile(getPackageP(), true, true);
		IType selectedType= getType(selectedTypeName, cu);
		
		ISourceRange		selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ChangeTypeRefactoring		ref= ChangeTypeRefactoring.create(cu, selection.getOffset(), selection.getLength(), 
												   selectedType);
	
		// TODO Set parameters on your refactoring instance from arguments...
	
		RefactoringStatus	activationResult= ref.checkInitialConditions(new NullProgressMonitor());	
	
		assertTrue("activation was supposed to be successful:" + activationResult.toString(), activationResult.isOK());																
	
		RefactoringStatus	checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
	
		assertTrue("precondition was supposed to pass but was " + checkInputResult.toString(), checkInputResult.isOK());
	
		performChange(ref, false);	
		
		String newSource= cu.getSource();
	
		assertEqualLines(getName() + ": ", newSource, getFileContents(getTestFileName(true, false)));
	
		
		return ref;
	}

	private void failHelper1(int startLine, int startColumn, int endLine, int endColumn,
							 int expectedStatus, String selectedTypeName) throws Exception {
		ICompilationUnit	cu= createCUfromTestFile(getPackageP(), false, true);
		IType selectedType= getType(selectedTypeName, cu);
		ISourceRange		selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ChangeTypeRefactoring	ref= ChangeTypeRefactoring.create(cu, selection.getOffset(), selection.getLength(), 
												   selectedType);
		RefactoringStatus	result= performRefactoring(ref);

		assertNotNull("precondition was supposed to fail", result);
		assertEquals("status", expectedStatus, result.getSeverity());

		String	canonAfterSrcName= getTestFileName(false, false);

		assertEqualLines(cu.getSource(), getFileContents(canonAfterSrcName));
	}	
	

	public IType getType(String fullyQualifiedName, ICompilationUnit icu) throws JavaModelException {
		return JavaModelUtil.findType(icu.getJavaProject(), fullyQualifiedName);
	}

	//--- TESTS
	public void testLocalVarName() throws Exception {
		System.out.println("running testLocalVarName()");
		Collection types= helper1(5, 19, 5, 24, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Map"));
	}
	public void testLocalVarType() throws Exception {
		System.out.println("running testLocalVarType()");
		Collection types= helper1(5, 9, 5, 18, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Map"));		
	}
	public void testLocalVarDecl() throws Exception {
		System.out.println("running testLocalVarDecl()");
		Collection types= helper1(8, 9, 8, 23, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Map")); 		
	}
	public void testLocalSuperTypesOfArrayList() throws Exception {
		System.out.println("running testLocalSuperTypesOfArrayList()");
		Collection types= helper1(5, 19, 5, 23, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 7);
		Assert.assertTrue(types.contains("java.lang.Object"));
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.Collection"));
		Assert.assertTrue(types.contains("java.io.Serializable"));
		Assert.assertTrue(types.contains("java.lang.Cloneable"));		
	}
	public void testParameterName() throws Exception {
		System.out.println("running testParameterName()");
		Collection types= helper1(4, 31, 4, 36, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.Map"));
		Assert.assertTrue(types.contains("java.util.Dictionary"));		
	}
	public void testParameterType() throws Exception {
		System.out.println("running testParameterType()");
		Collection types= helper1(4, 21, 4, 29, "java.util.Dictionary").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.Map"));
		Assert.assertTrue(types.contains("java.util.Dictionary"));				
	}
	public void testParameterDecl() throws Exception {
		System.out.println("running testParameterDecl()");
		Collection types= helper1(4, 21, 4, 36, "java.util.Map").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.Map"));
		Assert.assertTrue(types.contains("java.util.Dictionary"));				
	}
	public void testFieldName() throws Exception {
		System.out.println("running testFieldName()");
		Collection types= helper1(10, 29, 10, 33, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testFieldType() throws Exception {
		System.out.println("running testFieldType()");
		Collection types= helper1(10, 19, 10, 27, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testFieldDecl() throws Exception {
		System.out.println("running testFieldDecl()");
		Collection types= helper1(10, 19, 10, 32, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testFieldUseSubtypesOfList() throws Exception {
		System.out.println("running testFieldUseSubtypesOfList()");
		Collection types= helper1(5, 22, 5, 26, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testFieldDeclSubtypesOfList() throws Exception {
		System.out.println("running testFieldDeclSubtypesOfList()");
		Collection types= helper1(8, 12, 8, 25, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));		
	}
	public void testLocalVarUse() throws Exception {
		System.out.println("running testLocalVarUse()");
		Collection types= helper1(6, 22, 6, 26, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));
	}
	public void testReturnTypeWithCall() throws Exception {
		System.out.println("running testReturnTypeWithCall()");
		Collection types= helper1(4, 12, 4, 20, "java.util.AbstractList").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.List"));		
	}
	public void testParameterNameWithOverride() throws Exception {
		System.out.println("running testParameterNameWithOverride()");
		Collection types= helper1(5, 38, 5, 40, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));
//		Assert.assertTrue(types.contains("java.util.ArrayList"));
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.Collection"));		
	}
	public void testParameterTypeWithOverride() throws Exception {
		System.out.println("running testParameterTypeWithOverride()");
		Collection types= helper1(10, 25, 10, 36, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));
//		Assert.assertTrue(types.contains("java.util.ArrayList"));
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.Collection"));		
	}
	public void testParameterDeclWithOverride() throws Exception {
		System.out.println("running testParameterDeclWithOverride()");
		Collection types= helper1(10, 25, 10, 39, "java.util.ArrayList").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));
//		Assert.assertTrue(types.contains("java.util.ArrayList"));
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.Collection"));		
	}
	public void testLocalVarCast() throws Exception {
		System.out.println("running testLocalVarCast()");
		Collection types= helper1(7, 24, 7, 24, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 7);
		Assert.assertTrue(types.contains("java.io.Serializable"));
		Assert.assertTrue(types.contains("java.util.Collection"));		
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.AbstractList"));		
		Assert.assertTrue(types.contains("java.lang.Object"));
		Assert.assertTrue(types.contains("java.lang.Cloneable"));
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));		
	}
	public void testReturnType() throws Exception {
		System.out.println("running testReturnType()");
		createAdditionalCU("A_testReturnType2", getPackageP());
		Collection types= helper1(6, 12, 6, 15, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.Collection"));
		Assert.assertTrue(types.contains("java.lang.Object"));
	}	
	public void testFieldWithAccess() throws Exception {
		System.out.println("running testFieldWithAccess()");
		createAdditionalCU("A_testFieldWithAccess2", getPackageP());
		Collection types= helper1(6, 12, 6, 21, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Collection"));
	}	
	public void testParameterTypeWithOverriding() throws Exception {
		System.out.println("running testParameterTypeWithOverriding()");
		createAdditionalCU("A_testParameterTypeWithOverriding2", getPackageP());
		Collection types= helper1(6, 21, 6, 24, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Collection"));
	}
	public void testMultiCU() throws Exception {
		System.out.println("running testMultiCU()");
		createAdditionalCU("A_testMultiCUInterface1", getPackageP());
		createAdditionalCU("A_testMultiCUInterface2", getPackageP());
		createAdditionalCU("A_testMultiCUClass1", getPackageP());
		createAdditionalCU("A_testMultiCUClass2", getPackageP());
		Collection types= helper1(6, 21, 6, 26, "java.util.Collection").getValidTypeNames();
		Assert.assertTrue(types.size() == 1);
		Assert.assertTrue(types.contains("java.util.Collection"));
	}
	public void testHashMap() throws Exception {
		System.out.println("running testHashMap()");
		Collection types= helper1(15, 17, 15, 19, "java.util.AbstractMap").getValidTypeNames();
		Assert.assertTrue(types.size() == 2);
		Assert.assertTrue(types.contains("java.util.AbstractMap"));
		Assert.assertTrue(types.contains("java.util.Map"));
	}
	public void testString() throws Exception {
		System.out.println("running testString()");
		Collection types= helper1(4, 9, 4, 14, "java.lang.Object").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.lang.Object"));
//		Assert.assertTrue(types.contains("java.lang.CharSequence")); // not in rtstubs.jar
		Assert.assertTrue(types.contains("java.lang.Comparable"));
		Assert.assertTrue(types.contains("java.io.Serializable"));	
	}
	public void testInterfaceTypes() throws Exception {
		System.out.println("running testInterfaceTypes()");
		Collection types= helper1(4, 11, 4, 11, "p.I").getValidTypeNames();
		Assert.assertTrue(types.size() == 3);
		Assert.assertTrue(types.contains("java.lang.Object"));
		Assert.assertTrue(types.contains("p.I"));
		Assert.assertTrue(types.contains("p.A"));
	}
	public void testImport() throws Exception {
		System.out.println("running testImport()");
		Collection types= helper1(11, 9, 11, 17, "java.util.List").getValidTypeNames();
		Assert.assertTrue(types.size() == 7);
		Assert.assertTrue(types.contains("java.io.Serializable"));
		Assert.assertTrue(types.contains("java.lang.Cloneable"));
		Assert.assertTrue(types.contains("java.lang.Object"));
		Assert.assertTrue(types.contains("java.util.List"));
		Assert.assertTrue(types.contains("java.util.AbstractList"));
		Assert.assertTrue(types.contains("java.util.Collection"));
		Assert.assertTrue(types.contains("java.util.AbstractCollection"));
	}
	
	
	// tests that are supposed to fail
	
	public void testInvalidSelection() throws Exception {
		System.out.println("running testInvalidSelection()");
		failHelper1(5, 23, 5, 37, 4, "java.lang.Object");
	}
	public void testBogusSelection() throws Exception {
		System.out.println("running testBogusSelection()");
		failHelper1(6, 23, 6, 35, 4, "java.lang.Object");
	}
	public void testMultiDeclaration() throws Exception {
		System.out.println("running testMultiDeclaration()");
		failHelper1(8, 22, 8, 26, 4, "java.util.List");		
	}
	public void testUpdateNotPossible() throws Exception {
		System.out.println("running testUpdateNotPossible()");
		failHelper1(5, 19, 5, 20, 4, "java.util.Hashtable");
	}
	public void testArray() throws Exception {
		System.out.println("running testArray()");
		failHelper1(5, 18, 5, 19, 4, "java.util.Object[]");
	}
	public void testPrimitive() throws Exception {
		System.out.println("running testPrimitive()");
		failHelper1(5, 13, 5, 13, 4, "java.util.Object");
	}
	public void testOverriddenBinaryMethod() throws Exception {
		System.out.println("running testOverriddenBinaryMethod()");
		failHelper1(3, 12, 3, 17, 4, "java.lang.Object");
	}
	public void testFieldOfLocalType() throws Exception {
		System.out.println("running testFieldOfLocalType()");
		failHelper1(5, 21, 5, 45, 4, "java.lang.Object");
	}
	public void testObjectReturnType() throws Exception {
		System.out.println("running testObjectReturnType()");
		failHelper1(3, 17, 3, 22, 4, "java.lang.Object");
	}
}
