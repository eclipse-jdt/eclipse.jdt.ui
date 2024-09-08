package org.eclipse.jdt.ui.tests.quickfix.rules;

import org.eclipse.jdt.core.JavaCore;

public class EclipseJava10 extends AbstractEclipseJava {
	private static final String TESTRESOURCES_RTSTUBS_10_JAR= "testresources/rtstubs10.jar"; //$NON-NLS-1$

	public EclipseJava10() {
		super(TESTRESOURCES_RTSTUBS_10_JAR, JavaCore.VERSION_10);
	}
}
