/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.testplugin;

import java.lang.reflect.Method;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IPath;

import org.eclipse.ui.internal.Workbench;

public class TestWorkbench extends Workbench {

	/**
	 * Run an event loop for the workbench.
	 */
	protected void runEventLoop() {
		// Dispatch all events.
		Display display = Display.getCurrent();
		while (true) {
			try {
				if (!display.readAndDispatch())
					break;
			} catch (Throwable e) {
				break;
			}
		}
		IPath location= JavaTestPlugin.getDefault().getWorkspace().getRoot().getLocation();
		System.out.print("Workspace-location: " + location.toString());
				
		
		try {
			String[] args= getCommandLineArgs();
			if (args.length > 0) {
				Test test= getTest(args[0]);
				TestRunner.run(test);
			} else {
				System.out.println("TestWorkbench: Argument must be class name");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		
		// Close the workbench.
		close();
	}
	
	public Test getTest(String className) throws Exception {
		Class testClass= getClass().getClassLoader().loadClass(className);
		try {
			Method suiteMethod= testClass.getMethod(TestRunner.SUITE_METHODNAME, new Class[0]);
			return (Test) suiteMethod.invoke(null, new Class[0]); // static method
		} catch (NoSuchMethodException e) {
			Object obj= testClass.newInstance();
			return (Test) obj;
		}

	}
	
	
}