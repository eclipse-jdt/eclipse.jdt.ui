/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.testplugin;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IWorkspaceRoot;


public class JavaTestSetup extends TestSetup {
	
	/**
	 * @deprecated
	 * Not needed anymore. No added value
	 */
	public JavaTestSetup(Test test) {
		super(test);
	}	
	
	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}
	

	
	
}