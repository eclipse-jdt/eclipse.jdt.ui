/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class FindDeclarationsAction extends JavaElementSearchAction {
	
	public FindDeclarationsAction(IWorkbenchSite site) {
		this(site, SearchMessages.getString("Search.FindDeclarationAction.label"), new Class[] {IField.class, IMethod.class, IType.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationAction.tooltip")); //$NON-NLS-1$
	}

	public FindDeclarationsAction(JavaEditor editor) {
		this(editor, SearchMessages.getString("Search.FindDeclarationAction.label"), new Class[] {IField.class, IMethod.class, IType.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationAction.tooltip")); //$NON-NLS-1$
	}

	FindDeclarationsAction(IWorkbenchSite site, String label, Class[] validTypes) {
		super(site, label, validTypes);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_DECL);
	}

	FindDeclarationsAction(JavaEditor editor, String label, Class[] validTypes) {
		super(editor, label, validTypes);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_DECL);
	}
	
	JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		if (element.getElementType() == IJavaElement.METHOD) {
			IMethod method= (IMethod)element;
			int searchFor= IJavaSearchConstants.METHOD;
			if (method.isConstructor())
				searchFor= IJavaSearchConstants.CONSTRUCTOR;
			IType type= getType(element);
			String pattern= PrettySignature.getUnqualifiedMethodSignature(method);
			return new JavaSearchOperation(JavaPlugin.getWorkspace(), pattern, true,
				searchFor, getLimitTo(), getScope(type), getScopeDescription(type), getCollector());
		}
		else
			return super.makeOperation(element);
	}

	int getLimitTo() {
		return IJavaSearchConstants.DECLARATIONS;
	}
}