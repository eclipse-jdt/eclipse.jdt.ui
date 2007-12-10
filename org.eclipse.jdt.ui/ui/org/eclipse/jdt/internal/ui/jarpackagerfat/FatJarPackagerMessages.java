/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 211045 [jar application] program arguments are ignored
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import org.eclipse.osgi.util.NLS;

public final class FatJarPackagerMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackagerMessages";//$NON-NLS-1$

	public static String JarPackageWizard_jarExport_title;
	public static String JarPackageWizard_jarExportError_message;
	public static String JarPackageWizard_jarExportError_title;
	public static String JarPackageWizard_windowTitle;

	public static String JarPackageWizardPage_title;

	public static String FatJarBuilder_error_readingArchiveFile;

	public static String FatJarPackageWizard_JarExportProblems_message;

	public static String FatJarPackageWizardPage_destinationGroupTitle;
	public static String FatJarPackageWizardPage_error_missingClassFile;
	public static String FatJarPackageWizard_IPIssueDialog_message;

	public static String FatJarPackageWizard_IPIssueDialog_title;

	public static String FatJarPackageWizardPage_warning_launchConfigContainsProgramArgs;
	public static String FatJarPackageWizardPage_warning_launchConfigContainsVMArgs;
	public static String FatJarPackageWizardPage_error_noMainMethod;
	public static String FatJarPackageWizardPage_launchConfigGroupTitle;
	public static String FatJarPackageWizardPage_LaunchConfigurationWithoutMainType_warning;
	public static String FatJarPackageWizardPage_description;

	static {
		NLS.initializeMessages(BUNDLE_NAME, FatJarPackagerMessages.class);
	}

	private FatJarPackagerMessages() {
		// Do not instantiate
	}
}
