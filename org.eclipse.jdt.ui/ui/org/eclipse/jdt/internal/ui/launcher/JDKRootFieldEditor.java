/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.DirectoryFieldEditor;

public class JDKRootFieldEditor extends DirectoryFieldEditor {
	private String[] fPathSuffixes;
	
	protected final static String ERROR_NOT_ROOT= "jdkrooteditor.error";
	
	public JDKRootFieldEditor(String preferenceKey, String label, String[] pathSuffixes, Composite parent) {
		super(preferenceKey, label, parent);
		fPathSuffixes= pathSuffixes;
	}

	protected boolean doCheckState() {
		if (!super.doCheckState())
			return false;
		String fileName= getTextControl().getText();
		if (!"".equals(fileName)) {
			fileName= fileName.trim();
			File javaExe= null;
			for (int i= 0; i < fPathSuffixes.length; i++) {
				javaExe= new File(fileName, fPathSuffixes[i]);
				if (javaExe.isFile()) {
					return true;
				}
			}
			String format= JavaLaunchUtils.getResourceString(ERROR_NOT_ROOT);
			String msg= MessageFormat.format(format, new Object[] { javaExe.getAbsolutePath() });
			setErrorMessage(msg);
			return false;
		}
		return true;
	}
}