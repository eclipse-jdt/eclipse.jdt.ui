package org.eclipse.jdt.internal.ui.search;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class FindDeclarationsAction extends ElementSearchAction {

	public FindDeclarationsAction() {
		this(JavaPlugin.getResourceString("Search.FindDeclarationAction.label"), new Class[] {IField.class, IMethod.class, IType.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class});
		setToolTipText(JavaPlugin.getResourceString("Search.FindDeclarationAction.tooltip"));
	}

	public FindDeclarationsAction(String label, Class[] validTypes) {
		super(label, validTypes);
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		if (element.getElementType() == IJavaElement.TYPE) {
			IType type= (IType)element;
			int searchFor= IJavaSearchConstants.TYPE;
			String pattern= PrettySignature.getUnqualifiedTypeSignature(type);
			return new JavaSearchOperation(JavaPlugin.getWorkspace(), pattern,
				searchFor, getLimitTo(), getScope(type), getCollector());
		}
		else if (element.getElementType() == IJavaElement.METHOD) {
			IMethod method= (IMethod)element;
			int searchFor= IJavaSearchConstants.METHOD;
			if (method.isConstructor())
				searchFor= IJavaSearchConstants.CONSTRUCTOR;
			IType type= method.getDeclaringType();
			String pattern= PrettySignature.getUnqualifiedMethodSignature(method);
			return new JavaSearchOperation(JavaPlugin.getWorkspace(), pattern,
				searchFor, getLimitTo(), getScope(type), getCollector());
		}
		else
			return super.makeOperation(element);
	}

	protected int getLimitTo() {
		return IJavaSearchConstants.DECLARATIONS;
	}

	protected IJavaSearchScope getScope(IType type) throws JavaModelException {
		return getScope();
	}

	protected boolean shouldUserBePrompted() {
		return false;
	}
}