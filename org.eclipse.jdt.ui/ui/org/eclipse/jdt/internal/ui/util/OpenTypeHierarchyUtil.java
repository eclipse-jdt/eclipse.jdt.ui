/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;

public class OpenTypeHierarchyUtil {

	private OpenTypeHierarchyUtil() {
	}

	public static TypeHierarchyViewPart open(IJavaElement element, IWorkbenchWindow window) {
		IJavaElement[] candidates= getCandidates(element);
		if (candidates != null) {
			return open(candidates, window);
		}
		return null;
	}

	public static TypeHierarchyViewPart open(IJavaElement[] candidates, IWorkbenchWindow window) {
		Assert.isTrue(candidates != null && candidates.length != 0);

		IJavaElement input= null;
		if (candidates.length > 1) {
			String title= JavaUIMessages.OpenTypeHierarchyUtil_selectionDialog_title;
			String message= JavaUIMessages.OpenTypeHierarchyUtil_selectionDialog_message;
			input= SelectionConverter.selectJavaElement(candidates, window.getShell(), title, message);
		} else {
			input= candidates[0];
		}
		if (input == null)
			return null;

		try {
			if (PreferenceConstants.OPEN_TYPE_HIERARCHY_IN_PERSPECTIVE.equals(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.OPEN_TYPE_HIERARCHY))) {
				return openInPerspective(window, input);
			} else {
				return openInViewPart(window, input);
			}

		} catch (WorkbenchException e) {
			ExceptionHandler.handle(e, window.getShell(),
				JavaUIMessages.OpenTypeHierarchyUtil_error_open_perspective,
				e.getMessage());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, window.getShell(),
				JavaUIMessages.OpenTypeHierarchyUtil_error_open_editor,
				e.getMessage());
		}
		return null;
	}

	private static TypeHierarchyViewPart openInViewPart(IWorkbenchWindow window, IJavaElement input) {
		IWorkbenchPage page= window.getActivePage();
		try {
			TypeHierarchyViewPart result= (TypeHierarchyViewPart) page.findView(JavaUI.ID_TYPE_HIERARCHY);
			if (result != null) {
				result.clearNeededRefresh(); // avoid refresh of old hierarchy on 'becomes visible'
			}
			result= (TypeHierarchyViewPart) page.showView(JavaUI.ID_TYPE_HIERARCHY);
			result.setInputElement(input);
			return result;
		} catch (CoreException e) {
			ExceptionHandler.handle(e, window.getShell(),
				JavaUIMessages.OpenTypeHierarchyUtil_error_open_view, e.getMessage());
		}
		return null;
	}

	private static TypeHierarchyViewPart openInPerspective(IWorkbenchWindow window, IJavaElement input) throws WorkbenchException, JavaModelException {
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		// The problem is that the input element can be a working copy. So we first convert it to the original element if
		// it exists.
		IJavaElement perspectiveInput= input;

		if (input instanceof IMember) {
			if (input.getElementType() != IJavaElement.TYPE) {
				perspectiveInput= ((IMember)input).getDeclaringType();
			} else {
				perspectiveInput= input;
			}
		}
		IWorkbenchPage page= workbench.showPerspective(JavaUI.ID_HIERARCHYPERSPECTIVE, window, perspectiveInput);

		TypeHierarchyViewPart part= (TypeHierarchyViewPart) page.findView(JavaUI.ID_TYPE_HIERARCHY);
		if (part != null) {
			part.clearNeededRefresh(); // avoid refresh of old hierarchy on 'becomes visible'
		}
		part= (TypeHierarchyViewPart) page.showView(JavaUI.ID_TYPE_HIERARCHY);
		part.setInputElement(input);
		if (input instanceof IMember) {
			if (page.getEditorReferences().length == 0) {
				JavaUI.openInEditor(input, false, false); // only open when the perspecive has been created
			}
		}
		return part;
	}


	/**
	 * Converts the input to a possible input candidates
	 * @param input input
	 * @return the possible candidates
	 */
	public static IJavaElement[] getCandidates(Object input) {
		if (!(input instanceof IJavaElement)) {
			return null;
		}
		try {
			IJavaElement elem= (IJavaElement) input;
			switch (elem.getElementType()) {
				case IJavaElement.INITIALIZER:
				case IJavaElement.METHOD:
				case IJavaElement.FIELD:
				case IJavaElement.TYPE:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.JAVA_PROJECT:
					return new IJavaElement[] { elem };
				case IJavaElement.PACKAGE_FRAGMENT:
					if (((IPackageFragment)elem).containsJavaResources())
						return new IJavaElement[] {elem};
					break;
				case IJavaElement.PACKAGE_DECLARATION:
					return new IJavaElement[] { elem.getAncestor(IJavaElement.PACKAGE_FRAGMENT) };
				case IJavaElement.IMPORT_DECLARATION:
					IImportDeclaration decl= (IImportDeclaration) elem;
					if (decl.isOnDemand()) {
						elem= JavaModelUtil.findTypeContainer(elem.getJavaProject(), Signature.getQualifier(elem.getElementName()));
					} else {
						elem= elem.getJavaProject().findType(elem.getElementName());
					}
					if (elem == null)
						return null;
					return new IJavaElement[] {elem};

				case IJavaElement.CLASS_FILE:
					return new IJavaElement[] { ((IClassFile)input).getType() };
				case IJavaElement.COMPILATION_UNIT: {
					ICompilationUnit cu= (ICompilationUnit) elem.getAncestor(IJavaElement.COMPILATION_UNIT);
					if (cu != null) {
						IType[] types= cu.getTypes();
						if (types.length > 0) {
							return types;
						}
					}
					break;
				}
				default:
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
}
