package org.eclipse.jdt.internal.ui.search;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Defines an action which searches for implementors of Java interfaces.
 */
public class FindImplementorsAction extends ElementSearchAction {

	public FindImplementorsAction() {
		super(JavaPlugin.getResourceString("Search.FindImplementorsAction.label"), new Class[] {IType.class});
		setToolTipText(JavaPlugin.getResourceString("Search.FindImplementorsAction.tooltip"));
	}

	public boolean canOperateOn(ISelection sel) {
		if (!super.canOperateOn(sel))
			return false;

		IJavaElement element= getJavaElement(sel);
		if (element.getElementType() == IJavaElement.TYPE)
			try {
				return ((IType) element).isInterface();
			} catch (JavaModelException ex) {
				ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.javaElementAccess.");
				return false;
			}
		// should not happen: handled by super.canOperateOn
		return false;
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.IMPLEMENTORS;
	}

}

