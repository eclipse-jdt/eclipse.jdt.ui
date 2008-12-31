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
package org.eclipse.jdt.internal.junit.buildpath;


import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

public class JUnitHomeInitializer extends ClasspathVariableInitializer {

	/*
	 * @see ClasspathVariableInitializer#initialize(String)
	 */
	public void initialize(String variable) {
		if (JUnitPlugin.JUNIT_HOME.equals(variable)) {
			initializeHome();
		} else if (JUnitPlugin.JUNIT_SRC_HOME.equals(variable)) {
			initializeSource();
		}
	}

	private void initializeHome() {
		try {
			IPath location= BuildPathSupport.getBundleLocation(BuildPathSupport.JUNIT3_PLUGIN);
			if (location != null) {
				JavaCore.setClasspathVariable(JUnitPlugin.JUNIT_HOME, location, null);
			} else {
				JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
			}
		} catch (JavaModelException e1) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
		}
	}

	private void initializeSource() {
		try {
			IPath sourceLocation= BuildPathSupport.getSourceLocation(BuildPathSupport.JUNIT3_PLUGIN);
			if (sourceLocation != null) {
				JavaCore.setClasspathVariable(JUnitPlugin.JUNIT_SRC_HOME, sourceLocation, null);
			} else {
				JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_SRC_HOME, null);
			}
		} catch (JavaModelException e1) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_SRC_HOME, null);
		}
	}
}