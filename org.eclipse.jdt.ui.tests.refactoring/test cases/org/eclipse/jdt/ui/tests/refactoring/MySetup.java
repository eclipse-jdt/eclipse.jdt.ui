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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class MySetup extends TestSetup {
	
	public MySetup(Test test) {
		super(test);
	}
	public static final String CONTAINER= "src";
	private static IPackageFragmentRoot fgRoot;
	private static IPackageFragment fgPackageP;
	private static IJavaProject fgJavaTestProject;
	
	public static IPackageFragmentRoot getDefaultSourceFolder() throws Exception {
		if (fgRoot != null) 
			return fgRoot;
		throw new Exception("MySetup not initialized");
	}
	
	public static IJavaProject getProject()throws Exception {
		if (fgJavaTestProject != null)
			return fgJavaTestProject;
		throw new Exception("MySetup not initialized");
	}
	
	public static IPackageFragment getPackageP()throws Exception {
		if (fgPackageP != null) 
			return fgPackageP;
		throw new Exception("MySetup not initialized");
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		JavaProjectHelper.setAutoBuilding(false);
		if (JavaPlugin.getActivePage() != null)
			JavaPlugin.getActivePage().close();
		fgJavaTestProject= JavaProjectHelper.createJavaProject("TestProject"+System.currentTimeMillis(), "bin");
		JavaProjectHelper.addRTJar(fgJavaTestProject);
		fgRoot= JavaProjectHelper.addSourceContainer(fgJavaTestProject, CONTAINER);
		fgPackageP= fgRoot.createPackageFragment("p", true, null);
		

		Hashtable options= JavaCore.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "0");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);
		
		StringBuffer comment= new StringBuffer();
		comment.append("/**\n");
		comment.append(" * ${tags}\n");
		comment.append(" */");
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT).setPattern(comment.toString());
	}
	
	protected void tearDown() throws Exception {
		if (fgPackageP.exists())
			fgPackageP.delete(true, null);
		JavaProjectHelper.removeSourceContainer(fgJavaTestProject, CONTAINER);
		JavaProjectHelper.delete(fgJavaTestProject);
		super.tearDown();
	}
	
}

