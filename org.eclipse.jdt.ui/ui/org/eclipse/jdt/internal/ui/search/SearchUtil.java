/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This class contains some utility methods for J Search.
 */
class SearchUtil extends JavaModelUtil {

	static IJavaElement getJavaElement(IMarker marker) {
		try {
			IJavaElement je= JavaCore.create((String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
			if (!marker.getAttribute(IJavaSearchUIConstants.ATT_IS_WORKING_COPY, false))
				return je;

			ICompilationUnit cu= findCompilationUnit(je);
			if (cu == null)
				return null;
			// Find working copy element
			IWorkingCopy[] workingCopies= getWorkingCopies();
			int i= 0;
			while (i < workingCopies.length) {
				if (workingCopies[i].getOriginalElement().equals(cu)) {
					je= findInWorkingCopy(workingCopies[i], je, true);
					break;
				}
				i++;
			}
			if (je != null && !je.exists())
				je= cu.getElementAt(marker.getAttribute(IMarker.CHAR_START, 0));
			return je;
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.createJavaElement.title"), SearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
	}

	// --------------- Util methods needed for working copies ---------------

	static IWorkingCopy[] getWorkingCopies() {
		IWorkingCopyManager wcManager= JavaPlugin.getDefault().getWorkingCopyManager();
		IEditorPart[] editorParts= getEditors();
		ArrayList workingCopies= new ArrayList(editorParts.length);
		for (int i= 0; i < editorParts.length; i++) {
			IWorkingCopy workingCopy= wcManager.getWorkingCopy(editorParts[i].getEditorInput());
			if (workingCopy != null)
				workingCopies.add(workingCopy);
		}
		return (IWorkingCopy[])workingCopies.toArray(new IWorkingCopy[workingCopies.size()]);
	}

	/**
	 * Returns an array of all editors. If the identical content is presented in
	 * more than one editor, only one of those editor parts is part of the result.
	 * 
	 * @return an array of all editor parts.
	 */
	public static IEditorPart[] getEditors() {
		Set inputs= new HashSet(7);
		List result= new ArrayList(0);
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorPart[] editors= pages[x].getEditors();
				for (int z= 0; z < editors.length; z++) {
					IEditorPart editor= editors[z];
					IEditorInput input= editor.getEditorInput();
					if (!inputs.contains(input)) {
						inputs.add(input);
						result.add(editor);
					}
				}
			}
		}
		return (IEditorPart[])result.toArray(new IEditorPart[result.size()]);
	}

	/** 
	 * Returns the working copy of the given java element.
	 * @param javaElement the javaElement for which the working copyshould be found
	 * @param reconcile indicates whether the working copy must be reconcile prior to searching it
	 * @return the working copy of the given element or <code>null</code> if none
	 */	
	private static IJavaElement findInWorkingCopy(IWorkingCopy workingCopy, IJavaElement element, boolean reconcile) throws JavaModelException {
		if (workingCopy != null) {
			if (reconcile) {
				synchronized (workingCopy) {
					workingCopy.reconcile();
					return SearchUtil.findInCompilationUnit((ICompilationUnit)workingCopy, element);
				}
			} else {
					return SearchUtil.findInCompilationUnit((ICompilationUnit)workingCopy, element);
			}
		}
		return null;
	}

	/**
	 * Returns the compilation unit for the given java element.
	 * 
	 * @param	element the java element whose compilation unit is searched for
	 * @return	the compilation unit of the given java element
	 */
	static ICompilationUnit findCompilationUnit(IJavaElement element) {
		if (element == null)
			return null;

		if (element.getElementType() == IJavaElement.COMPILATION_UNIT)
			return (ICompilationUnit)element;
			
		if (element instanceof IMember)
			return ((IMember)element).getCompilationUnit();

		return findCompilationUnit(element.getParent());
	}

	/*
	 * Copied from JavaModelUtil and patched to allow members which do not exist.
	 * The only case where this is a problem is for methods which have same name and
	 * paramters as a constructor. The constructor will win in such a situation.
	 * 
	 * @see JavaModelUtil#findMemberInCompilationUnit(ICompilationUnit, IMember)
	 */		
	public static IMember findMemberInCompilationUnit(ICompilationUnit cu, IMember member) throws JavaModelException {
		if (member.getElementType() == IJavaElement.TYPE) {
			return findTypeInCompilationUnit(cu, getTypeQualifiedName((IType)member));
		} else {
			IType declaringType= findTypeInCompilationUnit(cu, getTypeQualifiedName(member.getDeclaringType()));
			if (declaringType != null) {
				IMember result= null;
				switch (member.getElementType()) {
				case IJavaElement.FIELD:
					result= declaringType.getField(member.getElementName());
					break;
				case IJavaElement.METHOD:
					IMethod meth= (IMethod) member;
					// XXX: Begin patch ---------------------
					boolean isConstructor;
					if (meth.exists())
						isConstructor= meth.isConstructor();
					else
						isConstructor= declaringType.getElementName().equals(meth.getElementName());
					// XXX: End patch -----------------------
					result= findMethod(meth.getElementName(), meth.getParameterTypes(), isConstructor, declaringType);
					break;
				case IJavaElement.INITIALIZER:
					result= declaringType.getInitializer(1);
					break;					
				}
				if (result != null && result.exists()) {
					return result;
				}
			}
		}
		return null;
	}

	/*
	 * XXX: Unchanged copy from JavaModelUtil
	 */
	public static IJavaElement findInCompilationUnit(ICompilationUnit cu, IJavaElement element) throws JavaModelException {
		
		if (element instanceof IMember)
			return findMemberInCompilationUnit(cu, (IMember) element);
		
		int type= element.getElementType();
		switch (type) {
			case IJavaElement.IMPORT_CONTAINER:
				return cu.getImportContainer();
			
			case IJavaElement.PACKAGE_DECLARATION:
				return find(cu.getPackageDeclarations(), element.getElementName());
			
			case IJavaElement.IMPORT_DECLARATION:
				return find(cu.getImports(), element.getElementName());
			
			case IJavaElement.COMPILATION_UNIT:
				return cu;
		}
		
		return null;
	}
	
	/*
	 * XXX: Unchanged copy from JavaModelUtil
	 */
	private static IJavaElement find(IJavaElement[] elements, String name) {
		if (elements == null || name == null)
			return null;
			
		for (int i= 0; i < elements.length; i++) {
			if (name.equals(elements[i].getElementName()))
				return elements[i];
		}
		
		return null;
	}
}
