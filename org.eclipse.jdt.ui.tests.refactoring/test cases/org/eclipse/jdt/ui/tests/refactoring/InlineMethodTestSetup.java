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

import java.util.Hashtable;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

public class InlineMethodTestSetup extends TestSetup {

	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fRoot;
	private static final String CONTAINER= "src";

	private IPackageFragment fInvalid;
	private IPackageFragment fSimple;
	private IPackageFragment fArgument;
	private IPackageFragment fNameConflict;
	private IPackageFragment fCall;
	private IPackageFragment fExpression;
	private IPackageFragment fControlStatement;
	private IPackageFragment fReceiver;
	private IPackageFragment fImport;
	private IPackageFragment fCast;

	public InlineMethodTestSetup(Test test) {
		super(test);
	}

	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}
		
	protected void setUp() throws Exception {
		super.setUp();
		
		Hashtable options= TestOptions.getDefault();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "0");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);
		JavaPlugin.getDefault().getCodeTemplateStore().restoreDefaults();		
		
		fJavaProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fJavaProject);
		fRoot= JavaProjectHelper.addSourceContainer(fJavaProject, CONTAINER);
		
		RefactoringCore.getUndoManager().flush();
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description= workspace.getDescription();
		description.setAutoBuilding(false);
		workspace.setDescription(description);
		
		fInvalid= fRoot.createPackageFragment("invalid", true, null);
		fSimple= fRoot.createPackageFragment("simple_in", true, null);		
		fArgument= fRoot.createPackageFragment("argument_in", true, null);
		fNameConflict= fRoot.createPackageFragment("nameconflict_in", true, null);
		fCall= fRoot.createPackageFragment("call_in", true, null);
		fExpression= fRoot.createPackageFragment("expression_in", true, null);
		fControlStatement= fRoot.createPackageFragment("controlStatement_in", true, null);
		fReceiver= fRoot.createPackageFragment("receiver_in", true, null);
		fImport= fRoot.createPackageFragment("import_in", true, null);
		fCast= fRoot.createPackageFragment("cast_in", true, null);
		
		fImport.createCompilationUnit(
			"Provider.java",
			"package import_in;\n" +
			"\n" +
			"import import_use.List;\n" +
			"import java.io.File;\n" +
			"import java.util.ArrayList;\n" +
			"import java.util.Map;\n" +
			"\n" +
			"public class Provider {\n" +
			"	public File useAsReturn() {\n" +
			"		return null;\n" +
			"	}\n" +
			"	public void useInArgument(File file) {\n" +
			"		file= null;\n" +
			"	}\n" +
			"	public void useInDecl() {\n" +
			"		List list= null;\n" +
			"	}\n" +
			"	public int useInDecl2() {\n" +
		  	"		return new ArrayList().size();\n" +
			"	}\n" +	
			"	public Object useInDecl3() {\n" +
		  	"		return new java.util.HashMap();\n" +
			"	}\n" +	
			"	public void useInClassLiteral() {\n" +
			"		Class clazz= File.class;\n" +
			"	}\n" +
			"	public void useArray() {\n" +
			"		List[] lists= null;\n" +
			"	}\n" +
			"	public void useInLocalClass() {\n" +
			"		class Local extends File implements Comparable {\n" +
			"			public Local(String s) {\n" +
			"				super(s);\n" +
			"			}\n" +
			"			public void foo(Map map) {\n" +
			"			}\n" +
			"			public int compareTo(Object o) {\n" +
			"				return 0;\n" +
			"			}\n" +
			"		}\n" +
			"	}\n" +
			"}\n", 
			true, null);
			
			IPackageFragment importUse= fRoot.createPackageFragment("import_use", true, null);
			importUse.createCompilationUnit("List.java",
			"package import_use;" +
			"" +
			"public class List {" +
			"}", 
			true, null);
			
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		RefactoringTest.performDummySearch(fJavaProject);
		JavaProjectHelper.delete(fJavaProject);
	}
	
	public IPackageFragment getInvalidPackage() {
		return fInvalid;
	}

	public IPackageFragment getSimplePackage() {
		return fSimple;
	}

	public IPackageFragment getArgumentPackage() {
		return fArgument;
	}

	public IPackageFragment getNameConflictPackage() {
		return fNameConflict;
	}

	public IPackageFragment getCallPackage() {
		return fCall;
	}

	public IPackageFragment getExpressionPackage() {
		return fExpression;
	}
	
	public IPackageFragment getControlStatementPackage() {
		return fControlStatement;
	}
	
	public IPackageFragment getReceiverPackage() {
		return fReceiver;
	}
	
	public IPackageFragment getImportPackage() {
		return fImport;
	}	

	public IPackageFragment getCastPackage() {
		return fCast;
	}	
}
