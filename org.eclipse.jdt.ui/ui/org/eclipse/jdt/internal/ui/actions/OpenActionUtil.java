/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

import org.eclipse.jdt.ui.JavaElementLabelProvider;


public abstract class OpenActionUtil {
		
	/**
	 * Opens the editor on the given element and subsequently selects it.
	 */
	public static void open(IJavaElement element) throws JavaModelException, PartInitException {
		IEditorPart part= EditorUtility.openInEditor(element);
		EditorUtility.revealInEditor(part, element);
	}
	
	/**
	 * Filters out source references from the given code resolve results.
	 * A utility method that can be called by subclassers. 
	 */
	public static List filterResolveResults(IJavaElement[] codeResolveResults) {
		int nResults= codeResolveResults.length;
		List refs= new ArrayList(nResults);
		for (int i= 0; i < nResults; i++) {
			if (codeResolveResults[i] instanceof ISourceReference)
				refs.add(codeResolveResults[i]);
		}
		return refs;
	}
						
	/**
	 * Shows a dialog for resolving an ambigous java element.
	 * Utility method that can be called by subclassers.
	 */
	public static IJavaElement selectJavaElement(List elements, Shell shell, String title, String message) {
		
		int nResults= elements.size();
		
		if (nResults == 0)
			return null;
		
		if (nResults == 1)
			return (IJavaElement) elements.get(0);
		
		int flags= JavaElementLabelProvider.SHOW_DEFAULT
						| JavaElementLabelProvider.SHOW_QUALIFIED
						| JavaElementLabelProvider.SHOW_ROOT;
						
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(elements.toArray());
		
		if (dialog.open() == dialog.OK) {
			Object[] selection= dialog.getResult();
			if (selection != null && selection.length > 0) {
				nResults= selection.length;
				for (int i= 0; i < nResults; i++) {
					Object current= selection[i];
					if (current instanceof IJavaElement)
						return (IJavaElement) current;
				}
			}
		}		
		return null;
	}
	
	public static IJavaElement getElementToOpen(IJavaElement element) throws JavaModelException {
		if (element == null)
			return null;
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_DECLARATION:
				// select package fragment
				element= JavaModelUtil.getPackageFragmentRoot(element);
				break;
			case IJavaElement.IMPORT_DECLARATION:
				// select referenced element: package fragment or cu/classfile of referenced type
				IImportDeclaration declaration= (IImportDeclaration) element;
				if (declaration.isOnDemand()) {
					element= JavaModelUtil.findTypeContainer(element.getJavaProject(), Signature.getQualifier(element.getElementName()));
				} else {
					element= JavaModelUtil.findType(element.getJavaProject(), element.getElementName());
				}
				if (element instanceof IType) {
					element= (IJavaElement) JavaModelUtil.getOpenable(element);
				}
				break;
			case IJavaElement.IMPORT_CONTAINER:
			case IJavaElement.TYPE:
			case IJavaElement.METHOD:
			case IJavaElement.FIELD:
			case IJavaElement.INITIALIZER:
				// select parent cu/classfile
				element= (IJavaElement) JavaModelUtil.getOpenable(element);
				break;
			case IJavaElement.JAVA_MODEL:
				element= null;
				break;
			default:
		}
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) element;
			if (cu.isWorkingCopy()) {
				element= cu.getOriginalElement();
			}
		}
		return element;
	}		
}
