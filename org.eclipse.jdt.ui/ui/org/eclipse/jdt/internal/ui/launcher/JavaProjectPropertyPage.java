/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/*
 * The page for setting java runtime
 */
public abstract class JavaProjectPropertyPage extends PropertyPage {
	
	private boolean fHasJavaContents;
	
	public final Control createContents(Composite parent) {
		IJavaProject jproject= getJavaProject();
		if (jproject != null && jproject.getProject().isOpen()) {
			fHasJavaContents= true;
			return createJavaContents(parent);
		} else {
			return createClosedContents(parent);
		}
	}
	
	protected abstract boolean performJavaOk();
	protected abstract Control createJavaContents(Composite parent);
		
	protected Control createClosedContents(Composite parent) {
		Label label= new Label(parent, SWT.LEFT);
		label.setText(LauncherMessages.getString("javaProjectPropertyPage.closed"));
		return label;
	}
		
	final public boolean performOk() {
		if (fHasJavaContents) {
			return performJavaOk();
		}
		return true;
	}
		
	protected IJavaProject getJavaProject() {
		Object obj= getElement();
		if (obj instanceof IProject) {
			IProject p= (IProject)obj;
			try {
				if (p.hasNature(JavaCore.NATURE_ID)) {
					return JavaCore.create(p);
				}
			} catch (CoreException e) {
				JavaPlugin.log(e.getStatus());
			}
		} else if (obj instanceof IJavaProject) {
			return (IJavaProject)obj;
		}
		return null;
	}
}