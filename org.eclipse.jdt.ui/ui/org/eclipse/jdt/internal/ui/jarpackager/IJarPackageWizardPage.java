/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import org.eclipse.jface.wizard.IWizardPage;

/**
 * Common interface for all JAR package wizard pages.
 */
public interface IJarPackageWizardPage extends IWizardPage {
	/**
	 * Tells the page that the user has pressed finish.
	 */
	void finish();
}
