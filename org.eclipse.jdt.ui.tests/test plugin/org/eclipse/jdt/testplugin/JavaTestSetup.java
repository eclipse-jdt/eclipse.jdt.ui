/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.testplugin;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IWorkspaceRoot;


public class JavaTestSetup extends TestSetup {
	
	private static JavaTestProject fgTestProject;
	
	public static JavaTestProject getTestProject() throws Exception {
		if (fgTestProject != null) {
			return fgTestProject;
		}
		throw new Exception("JavaTestSetup not initialized");
	}
		
	public JavaTestSetup(Test test) {
		super(test);
	}	
	
	protected void setUp() throws Exception {
		IWorkspaceRoot root= JavaTestPlugin.getWorkspace().getRoot();
		fgTestProject= new JavaTestProject(root, "TestProject", "bin");
		
		if (fgTestProject.addRTJar() == null) {
			throw new Exception("JDK not found");
		}
		
	}

	protected void tearDown() throws Exception {
		fgTestProject.remove();
	}
	

	
	
}