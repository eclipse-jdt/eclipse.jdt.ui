/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.template.CodeTemplates;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

public class SurroundWithTestSetup extends TestSetup {
	
	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fRoot;
	private static final String CONTAINER= "src";

	private IPackageFragment fTryCatchPackage;
	
	public SurroundWithTestSetup(Test test) {
		super(test);
	}	
	
	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}
		
	protected void setUp() throws Exception {
		super.setUp();
		fJavaProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fJavaProject);
		fRoot= JavaProjectHelper.addSourceContainer(fJavaProject, CONTAINER);
		
		Refactoring.getUndoManager().flush();
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description= workspace.getDescription();
		description.setAutoBuilding(false);
		workspace.setDescription(description);
		
		fTryCatchPackage= getRoot().createPackageFragment("trycatch_in", true, null);
		CodeTemplates.getInstance().getTemplates(CodeTemplateContextType.CATCHBLOCK_NAME)[0].setPattern("");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		RefactoringTest.performDummySearch(fJavaProject);
		JavaProjectHelper.delete(fJavaProject);
	}
	
	public IPackageFragment getTryCatchPackage() {
		return fTryCatchPackage;
	}
}

