package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;

/**
 * Interface to JUnitLaunchers
 */
public interface IJUnitLauncherDelegate {
	
	/**
	 * Returns the launched type.
	 */
	IType getLaunchedType();
	
	/**
	 * Returns the port the TestRunner will connect to.
	 */
	int getPort();
	
	/**
	 * Returns the launche mode
	 */
	String getRunMode();
	
}
