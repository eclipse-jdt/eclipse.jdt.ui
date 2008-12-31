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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import junit.framework.Test;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;

public class InlineMethodTestSetup extends RefactoringTestSetup {

	private IPackageFragment fInvalid;
	private IPackageFragment fBugs;
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
	private IPackageFragment fBinary;
	private IPackageFragment fOperator;

	public InlineMethodTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fInvalid= root.createPackageFragment("invalid", true, null);
		fBugs= root.createPackageFragment("bugs_in", true, null);
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
		fBinary= root.createPackageFragment("binary_in", true, null);
		fOperator= root.createPackageFragment("operator_in", true, null);

		IJavaProject javaProject= getProject();
		IProject project= javaProject.getProject();
		copyFilesFromResources(project, "binary/classes", "*.class");
		copyFilesFromResources(project, "binary_src/classes", "*.java");

		IClasspathEntry[] classpath= javaProject.getRawClasspath();
		IClasspathEntry[] newClasspath= new IClasspathEntry[classpath.length + 1];
		System.arraycopy(classpath, 0, newClasspath, 0, classpath.length);
		IClasspathEntry binaryFolder= JavaCore.newLibraryEntry(javaProject.getPath().append("binary"), javaProject.getPath().append("binary_src"), null);
		newClasspath[classpath.length]= binaryFolder;
		javaProject.setRawClasspath(newClasspath, null);

		fImport.createCompilationUnit(
			"Provider.java",
			"package import_in;\n" +
			"\n" +
			"import import_use.List;\n" +
			"import java.io.File;\n" +
			"import java.util.ArrayList;\n" +
			"import java.util.Map;\n" +
			"import static java.lang.Math.PI;\n" +
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
			"	public void useStaticImport() {\n" +
			"		double i= PI;\n" +
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

	private static void copyFilesFromResources(IProject project, String pathInRoot, String filePattern) throws CoreException, IOException {
		String[] folders= pathInRoot.split("/");
		IFolder folder= project.getFolder(folders[0]);
		folder.create(true, true, null);
		for (int i= 1; i < folders.length; i++) {
			folder= folder.getFolder(folders[i]);
			folder.create(true, true, null);
		}

		Bundle bundle= RefactoringTestPlugin.getDefault().getBundle();
		Enumeration/*URL*/ classUrls= bundle.findEntries("/resources/InlineMethodWorkspace/TestCases/" + pathInRoot, filePattern, false);
		while (classUrls.hasMoreElements()) {
			URL classUrl= (URL) classUrls.nextElement();
			String urlFile= classUrl.getFile();
			String fileName= urlFile.substring(urlFile.lastIndexOf('/') + 1);

			IFile file= folder.getFile(new Path(fileName));
			file.create(classUrl.openStream(), true, null);
		}
	}

	public IPackageFragment getInvalidPackage() {
		return fInvalid;
	}

	public IPackageFragment getBugsPackage() {
		return fBugs;
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

	public IPackageFragment getBinaryPackage() {
		return fBinary;
	}

	public IPackageFragment getOperatorPackage() {
		return fOperator;
	}
}