/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Defines an action which searches for implementors of Java interfaces.
 */
public class FindImplementorsAction extends ElementSearchAction {

	public FindImplementorsAction() {
		super(SearchMessages.getString("Search.FindImplementorsAction.label"), new Class[] {IType.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindImplementorsAction.tooltip")); //$NON-NLS-1$
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_DECL);
	}

	public boolean canOperateOn(ISelection sel) {
		if (!super.canOperateOn(sel))
			return false;

		IJavaElement element= getJavaElement(sel, true);
		if (element.getElementType() == IJavaElement.TYPE)
			try {
				return ((IType) element).isInterface();
			} catch (JavaModelException ex) {
				ExceptionHandler.log(ex, SearchMessages.getString("Search.Error.javaElementAccess.message")); //$NON-NLS-2$ //$NON-NLS-1$
				return false;
			}
		// should not happen: handled by super.canOperateOn
		return false;
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.IMPLEMENTORS;
	}

}

