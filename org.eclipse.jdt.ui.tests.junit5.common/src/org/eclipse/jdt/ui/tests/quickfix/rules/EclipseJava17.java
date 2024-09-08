package org.eclipse.jdt.ui.tests.quickfix.rules;

import org.eclipse.jdt.core.JavaCore;

public class EclipseJava17 extends AbstractEclipseJava {
	private static final String TESTRESOURCES_RTSTUBS_17_JAR= "testresources/rtstubs_17.jar"; //$NON-NLS-1$

	public EclipseJava17() {
		super(TESTRESOURCES_RTSTUBS_17_JAR, JavaCore.VERSION_17);
	}
}
