/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import java.util.HashMap;import java.util.Map;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jface.wizard.Wizard;

/**
 * Wizard to allow source attachment at debug time.
 */
public class SourceAttachmentWizard extends Wizard {

	private static final String PREFIX = "source_attachment_wizard.";
	private static final String TITLE = PREFIX + "title";
	
	protected IPackageFragmentRoot fJar;
	protected SourceAttachmentWizardPage fPage;
	
	protected static Map fgNoPrompt = new HashMap();

	public SourceAttachmentWizard(IPackageFragmentRoot jar) {
		fJar = jar;
		setWindowTitle(DebugUIUtils.getResourceString(TITLE));
	}



	/**
	 * @see Wizard#addPages
	 */
	public void addPages() {
		fPage = new SourceAttachmentWizardPage(fJar);
		addPage(fPage);
	}

	public boolean performFinish() {
		if (fPage.isNoSource()) {
			fgNoPrompt.put(fJar, Boolean.FALSE);
			return true;
		}
		return fPage.performFinish();

	}

	public static boolean isOkToPrompt(IPackageFragmentRoot root) {
		return fgNoPrompt.get(root) == null;
	}
}
