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

import junit.framework.Test;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class InlineMethodTestSetup extends RefactoringTestSetup {

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
	private IPackageFragment fEnum;
	private IPackageFragment fGeneric;

	public InlineMethodTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();
				
		IPackageFragmentRoot root= getDefaultSourceFolder();
		fInvalid= root.createPackageFragment("invalid", true, null);
		fSimple= root.createPackageFragment("simple_in", true, null);		
		fArgument= root.createPackageFragment("argument_in", true, null);
		fNameConflict= root.createPackageFragment("nameconflict_in", true, null);
		fCall= root.createPackageFragment("call_in", true, null);
		fExpression= root.createPackageFragment("expression_in", true, null);
		fControlStatement= root.createPackageFragment("controlStatement_in", true, null);
		fReceiver= root.createPackageFragment("receiver_in", true, null);
		fImport= root.createPackageFragment("import_in", true, null);
		fCast= root.createPackageFragment("cast_in", true, null);
		fEnum= root.createPackageFragment("enum_in", true, null);
		fGeneric= root.createPackageFragment("generic_in", true, null);
		
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
			"		class Local extends File {\n" +
			"			private static final long serialVersionUID = 1L;\n" +
			"			public Local(String s) {\n" +
			"				super(s);\n" +
			"			}\n" +
			"			public void foo(Map map) {\n" +
			"			}\n" +
			"			public void bar(Byte b) {\n" +
			"			}\n" +
			"		}\n" +
			"	}\n" +
			"}\n", 
			true, null);
			
			IPackageFragment importUse= root.createPackageFragment("import_use", true, null);
			importUse.createCompilationUnit("List.java",
			"package import_use;" +
			"" +
			"public class List {" +
			"}", 
			true, null);
			
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
	
	public IPackageFragment getEnumPackage() {
		return fEnum;
	}
	
	public IPackageFragment getGenericPackage() {
		return fGeneric;
	}
}
