package org.eclipse.jdt.internal.ui.jarpackager;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import org.eclipse.jface.wizard.IWizardPage;

/**
 * Common interface for all JAR package wizard pages.
 */
public interface IJarPackageWizardPage extends IWizardPage {
	/**
	 * Persists resource specification control setting that are to be restored
	 * in the next instance of this page. Subclasses wishing to persist
	 * settings for their controls should extend the hook method 
	 * <code>internalSaveWidgetValues</code>.
	 */
	void saveWidgetValues();
	/**
	 * Computes whether this page is complete.
	 *
	 * @return	<code>true</code> if this page is complete,
	 * 			<code>false</code> if it is incomplete
	 */
	boolean computePageCompletion();
}
