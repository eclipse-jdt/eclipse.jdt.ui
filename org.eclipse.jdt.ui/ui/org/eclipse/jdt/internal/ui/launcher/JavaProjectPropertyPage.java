/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/*
 * The page for setting java runtime
 */
public abstract class JavaProjectPropertyPage extends PropertyPage {

	protected static final String CORE_EXCEPTION="java_project_propertypage.core_exception";
	protected static final String NO_JAVA="java_project_propertypage.no_java";
	
	public final Control createContents(Composite parent) {
		if (getJavaProject() == null) {
			return createNoJavaContents(parent);
		} else {
			return createJavaContents(parent);
		}
	}
	
	protected abstract Control createJavaContents(Composite parent);
	
	protected Control createNoJavaContents(Composite parent) {
		noDefaultAndApplyButton();
		Label label= new Label(parent, SWT.LEFT);
		label.setText(JavaLaunchUtils.getResourceString(NO_JAVA));
		return parent;
	};

	protected boolean isJavaProject(IProject proj) {
		try {
			return proj.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			ErrorDialog.openError(getControl().getShell(), JavaLaunchUtils.getResourceString(CORE_EXCEPTION), null, e.getStatus());
		}
		return false;
	}
	
	protected IJavaProject getJavaProject() {
		Object o= getElement();
		if (o instanceof IProject) {
			IProject p= (IProject)o;
			if (isJavaProject(p))
				o= JavaCore.create((IProject)o);
		}
		if (o instanceof IJavaProject)
			return (IJavaProject)o;
		return null;
	}
}