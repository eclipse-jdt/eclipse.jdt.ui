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
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;
import org.eclipse.jdt.ui.tests.refactoring.infra.TestExceptionHandler;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public abstract class RefactoringTest extends TestCase {

	private IPackageFragmentRoot fRoot;
	private IPackageFragment fPackageP;
	private IJavaProject fJavaProject;
	
	public boolean fIsVerbose= false;

	public static final String TEST_PATH_PREFIX= "";

	protected static final String TEST_INPUT_INFIX= "/in/";
	protected static final String TEST_OUTPUT_INFIX= "/out/";
	protected static final String CONTAINER= "src";
	
	public RefactoringTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		fJavaProject= MySetup.getProject();
		fRoot= MySetup.getDefaultSourceFolder();
		fPackageP= MySetup.getPackageP();
		
		if (fIsVerbose){
			System.out.println("\n---------------------------------------------");
			System.out.println("\nTest:" + getClass() + "." + getName());
		}	
		Refactoring.getUndoManager().flush();
	}

	protected void performDummySearch() throws Exception {
		performDummySearch(fPackageP);
	}	

	public static void performDummySearch(IJavaElement element) throws Exception{
		new SearchEngine().searchAllTypeNames(
		 	ResourcesPlugin.getWorkspace(),
			null,
			null,
			IJavaSearchConstants.EXACT_MATCH,
			IJavaSearchConstants.CASE_SENSITIVE,
			IJavaSearchConstants.CLASS,
			SearchEngine.createJavaSearchScope(new IJavaElement[]{element}),
			new Requestor(),
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
			null);
	}
	
	protected void tearDown() throws Exception {
		performDummySearch();
		
		if (fPackageP.exists()){	
			IJavaElement[] kids= fPackageP.getChildren();
			for (int i= 0; i < kids.length; i++){
				if (kids[i] instanceof ISourceManipulation){
					try{
						if (kids[i].exists() && ! kids[i].isReadOnly())
							((ISourceManipulation)kids[i]).delete(true, null);
					}	catch (JavaModelException e){
						//try to delete'em all
					}
				}	
			}
		}	
		
		if (fRoot.exists()){
			IJavaElement[] packages= fRoot.getChildren();
			for (int i= 0; i < packages.length; i++){
				try{
					IPackageFragment pack= (IPackageFragment)packages[i];
					if (! pack.equals(fPackageP) && pack.exists() && ! pack.isReadOnly())
						pack.delete(true, null);
				}	catch (JavaModelException e){
					//try to delete'em all
				}	
			}
		}
	}

	protected IPackageFragmentRoot getRoot() {
		return fRoot;
	}

	protected IPackageFragment getPackageP() {
		return fPackageP;
	}

	protected final RefactoringStatus performRefactoring(IRefactoring ref) throws JavaModelException {
		RefactoringStatus status= ref.checkPreconditions(new NullProgressMonitor());
		if (!status.isOK())
			return status;

		IChange change= ref.createChange(new NullProgressMonitor());
		performChange(change);
		
		// XXX: this should be done by someone else
		Refactoring.getUndoManager().addUndo(ref.getName(), change.getUndoChange());

		return null;
	}
	
	protected void performChange(IChange change) throws JavaModelException{
		change.aboutToPerform(new ChangeContext(new TestExceptionHandler()), new NullProgressMonitor());
		try {
			change.perform(new ChangeContext(new TestExceptionHandler()), new NullProgressMonitor());
		} finally {
			change.performed();
		}
	}

	/****************  helpers  ******************/
	/**** mostly not general, just shortcuts *****/

	/**
	 * @param pack
	 * @param name
	 * @param contents
	 */
	protected ICompilationUnit createCU(IPackageFragment pack, String name, String contents) throws Exception {
		if (pack.getCompilationUnit(name).exists())
			return pack.getCompilationUnit(name);
		ICompilationUnit cu= pack.createCompilationUnit(name, contents, true, null);
		cu.save(null, true);
		return cu;
	}

	/**
	 * BOGUS: this might be already implemented somewhere else (JDK? Core?)
	 */
	protected String getFileContents(String fileName) throws IOException {
		if (fIsVerbose)
			System.out.println("loading:" + fileName);
			
		InputStream in= getFileInputStream(fileName);
		BufferedReader br= new BufferedReader(new InputStreamReader(in));
		
		StringBuffer sb= new StringBuffer(300);
		try {
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		} finally {
			br.close();
		}
		return sb.toString();
	}

	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}

	protected IType getType(ICompilationUnit cu, String name) throws JavaModelException {
		IType[] types= cu.getAllTypes();
		for (int i= 0; i < types.length; i++)
			if (JavaModelUtil.getTypeQualifiedName(types[i]).equals(name) ||
			    types[i].getElementName().equals(name))
				return types[i];
		return null;
	}
	
	/**
	 * subclasses override to inform about the location of their test cases
	 */
	protected String getRefactoringPath() {
		return "";
	}

	/**
	 *  example "RenameType/"
	 */
	protected String getTestPath() {
		return TEST_PATH_PREFIX + getRefactoringPath();
	}

	/**
	 * @param cuName
	 * @param infix
	 * example "RenameTest/test0 + infix + cuName.java"
	 */
	protected String createTestFileName(String cuName, String infix) {
		return getTestPath() + getName() + infix + cuName + ".java";
	}
	
	protected String getInputTestFileName(String cuName) {
		return createTestFileName(cuName, TEST_INPUT_INFIX);
	}
	
	/**
	 * @param subDirName example "p/" or "org/eclipse/jdt/"
	 */
	protected String getInputTestFileName(String cuName, String subDirName) {
		return createTestFileName(cuName, TEST_INPUT_INFIX + subDirName);
	}

	protected String getOutputTestFileName(String cuName) {
		return createTestFileName(cuName, TEST_OUTPUT_INFIX);
	}
	
	/**
	 * @param subDirName example "p/" or "org/eclipse/jdt/"
	 */
	protected String getOutputTestFileName(String cuName, String subDirName) {
		return createTestFileName(cuName, TEST_OUTPUT_INFIX + subDirName);
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName) throws Exception {
		return createCUfromTestFile(pack, cuName, true);
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, String subDirName) throws Exception {
		return createCUfromTestFile(pack, cuName, subDirName, true);
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, boolean input) throws Exception {
		String contents= input 
					? getFileContents(getInputTestFileName(cuName))
					: getFileContents(getOutputTestFileName(cuName));
		return createCU(pack, cuName + ".java", contents);
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName, String subDirName, boolean input) throws Exception {
		String contents= input 
			? getFileContents(getInputTestFileName(cuName, subDirName))
			: getFileContents(getOutputTestFileName(cuName, subDirName));
		
		return createCU(pack, cuName + ".java", contents);
	}
	
	protected void printTestDisabledMessage(String explanation){
		System.out.println("\n" +getClass().getName() + "::"+ getName() + " disabled (" + explanation + ")");
	}
	
	private static class Requestor implements ITypeNameRequestor{
		
		public void acceptClass(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
		}

		public void acceptInterface(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
		}
	}
}

