/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.Bundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This class contains some utility methods for J Search.
 */
public class SearchUtil {

	// LRU working sets
	public static int LRU_WORKINGSET_LIST_SIZE= 3;
	private static LRUWorkingSetsList fgLRUWorkingSets;

	// Settings store
	private static final String DIALOG_SETTINGS_KEY= "JavaElementSearchActions"; //$NON-NLS-1$
	private static final String STORE_LRU_WORKING_SET_NAMES= "lastUsedWorkingSetNames"; //$NON-NLS-1$
	
	private static IDialogSettings fgSettingsStore;

	private static final String BIN_PRIM_CONST_WARN_DIALOG_ID= "BinaryPrimitiveConstantWarningDialog"; //$NON-NLS-1$


	public static IJavaElement getJavaElement(IMarker marker) {
		if (marker == null || !marker.exists())
			return null;
		try {
			String handleId= (String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID);
			IJavaElement je= JavaCore.create(handleId);
			if (je == null)
				return null;

			if (!marker.getAttribute(IJavaSearchUIConstants.ATT_IS_WORKING_COPY, false)) {
				if (je.exists())
					return je;
			}
			
			ICompilationUnit cu= (ICompilationUnit) je.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (cu == null) {
				return je;
			}
			if (!cu.exists()) {
				IResource res= marker.getResource();
				if (res instanceof IFile) {
					cu= JavaCore.createCompilationUnitFrom((IFile) res);
					if (cu == null) {
						return je;
					}
					
				}
			}
			
			if (!je.exists()) {
				IJavaElement[] jElements= cu.findElements(je);
				if (jElements == null || jElements.length == 0)
					je= cu.getElementAt(marker.getAttribute(IMarker.CHAR_START, 0));
				else
					je= jElements[0];
			}
			return je;
		} catch (JavaModelException ex) {
			if (!ex.isDoesNotExist())
				ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.createJavaElement.title"), SearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.createJavaElement.title"), SearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
	}

	public static IJavaElement getJavaElement(Object entry) {
		if (entry != null && isISearchResultViewEntry(entry))
			return getJavaElement((ISearchResultViewEntry)entry);
		return null;
	}

	public static IResource getResource(Object entry) {
		if (entry != null && isISearchResultViewEntry(entry))
			return ((ISearchResultViewEntry)entry).getResource();
		return null;
	}

	public static IJavaElement getJavaElement(ISearchResultViewEntry entry) {
		if (entry != null)
			return getJavaElement(entry.getSelectedMarker());
		return null;
	}
	
	public static boolean isSearchPlugInActivated() {
		return Platform.getBundle("org.eclipse.search").getState() == Bundle.ACTIVE; //$NON-NLS-1$
	}

	public static boolean isISearchResultViewEntry(Object object) {
		return object != null && isSearchPlugInActivated() && (object instanceof ISearchResultViewEntry);
	}
	
	public static IMarker getMarkerFromPossibleSearchResultViewEntry(Object object) {
		if (object instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry= (ISearchResultViewEntry) object;
			return entry.getSelectedMarker();
		}
		return null;
	}
	
	/**
	 * This helper method with Object as parameter is needed to prevent the loading
	 * of the Search plug-in: the VM verifies the method call and hence loads the
	 * types used in the method signature, eventually triggering the loading of
	 * a plug-in (in this case ISearchQuery results in Search plug-in being loaded).
	 */
	public static void runQueryInBackground(Object query) {
		NewSearchUI.runQueryInBackground((ISearchQuery)query);
	}
	
	/**
	 * This helper method with Object as parameter is needed to prevent the loading
	 * of the Search plug-in: the VM verifies the method call and hence loads the
	 * types used in the method signature, eventually triggering the loading of
	 * a plug-in (in this case ISearchQuery results in Search plug-in being loaded).
	 */
	public static IStatus runQueryInForeground(IRunnableContext context, Object query) {
		return NewSearchUI.runQueryInForeground(context, (ISearchQuery)query);
	}
	
	public static Object getGroupByKeyFromPossibleSearchResultViewEntry(Object object) {
		if (object instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry= (ISearchResultViewEntry) object;
			return entry.getGroupByKey();
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
		return (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
	}

	/*
	 * Copied from JavaModelUtil and patched to allow members which do not exist.
	 * The only case where this is a problem is for methods which have same name and
	 * paramters as a constructor. The constructor will win in such a situation.
	 * 
	 * @see JavaModelUtil#findMemberInCompilationUnit(ICompilationUnit, IMember)
	 */		
	public static IMember findInCompilationUnit(ICompilationUnit cu, IMember member) throws JavaModelException {
		if (member.getElementType() == IJavaElement.TYPE) {
			return JavaModelUtil.findTypeInCompilationUnit(cu, JavaModelUtil.getTypeQualifiedName((IType)member));
		} else {
			IType declaringType= JavaModelUtil.findTypeInCompilationUnit(cu, JavaModelUtil.getTypeQualifiedName(member.getDeclaringType()));
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
					result= JavaModelUtil.findMethod(meth.getElementName(), meth.getParameterTypes(), isConstructor, declaringType);
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
			return findInCompilationUnit(cu, (IMember)element);
		
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

	public static String toString(IWorkingSet[] workingSets) {
		Arrays.sort(workingSets, new WorkingSetComparator());
		String result= ""; //$NON-NLS-1$
		if (workingSets != null && workingSets.length > 0) {
			boolean firstFound= false;
			for (int i= 0; i < workingSets.length; i++) {
				String workingSetName= workingSets[i].getName();
				if (firstFound)
					result= SearchMessages.getFormattedString("SearchUtil.workingSetConcatenation", new String[] {result, workingSetName}); //$NON-NLS-1$
				else {
					result= workingSetName;
					firstFound= true;
				}
			}
		}
		return result;
	}

	// ---------- LRU working set handling ----------

	/**
	 * Updates the LRU list of working sets.
	 * 
	 * @param workingSets	the workings sets to be added to the LRU list
	 */
	public static void updateLRUWorkingSets(IWorkingSet[] workingSets) {
		if (workingSets == null || workingSets.length < 1)
			return;
		
		getLRUWorkingSets().add(workingSets);
		saveState();
	}

	private static void saveState() {
		IWorkingSet[] workingSets;
		Iterator iter= fgLRUWorkingSets.iterator();
		int i= 0;
		while (iter.hasNext()) {
			workingSets= (IWorkingSet[])iter.next();
			String[] names= new String[workingSets.length];
			for (int j= 0; j < workingSets.length; j++)
				names[j]= workingSets[j].getName();
			fgSettingsStore.put(STORE_LRU_WORKING_SET_NAMES + i, names);
			i++;
		}
	}

	public static LRUWorkingSetsList getLRUWorkingSets() {
		if (fgLRUWorkingSets == null) {
			restoreState();
		}
		return fgLRUWorkingSets;
	}

	static void restoreState() {
		fgLRUWorkingSets= new LRUWorkingSetsList(LRU_WORKINGSET_LIST_SIZE);
		fgSettingsStore= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_KEY);
		if (fgSettingsStore == null)
			fgSettingsStore= JavaPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS_KEY);
		
		boolean foundLRU= false;
		for (int i= LRU_WORKINGSET_LIST_SIZE - 1; i >= 0; i--) {
			String[] lruWorkingSetNames= fgSettingsStore.getArray(STORE_LRU_WORKING_SET_NAMES + i);
			if (lruWorkingSetNames != null) {
				Set workingSets= new HashSet(2);
				for (int j= 0; j < lruWorkingSetNames.length; j++) {
					IWorkingSet workingSet= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(lruWorkingSetNames[j]);
					if (workingSet != null) {
						workingSets.add(workingSet);
					}
				}
				foundLRU= true;
				if (!workingSets.isEmpty())
					fgLRUWorkingSets.add((IWorkingSet[])workingSets.toArray(new IWorkingSet[workingSets.size()]));
			}
		}
		if (!foundLRU)
			// try old preference format
			restoreFromOldFormat();
	}

	private static void restoreFromOldFormat() {
		fgLRUWorkingSets= new LRUWorkingSetsList(LRU_WORKINGSET_LIST_SIZE);
		fgSettingsStore= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_KEY);
		if (fgSettingsStore == null)
			fgSettingsStore= JavaPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS_KEY);

		boolean foundLRU= false;
		String[] lruWorkingSetNames= fgSettingsStore.getArray(STORE_LRU_WORKING_SET_NAMES);
		if (lruWorkingSetNames != null) {
			for (int i= lruWorkingSetNames.length - 1; i >= 0; i--) {
				IWorkingSet workingSet= PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(lruWorkingSetNames[i]);
				if (workingSet != null) {
					foundLRU= true;
					fgLRUWorkingSets.add(new IWorkingSet[]{workingSet});
				}
			}
		}
		if (foundLRU)
			// save in new format
			saveState();
	}

	public static void warnIfBinaryConstant(IJavaElement element, Shell shell) {
		if (isBinaryPrimitveConstantOrString(element))
			OptionalMessageDialog.open(
				BIN_PRIM_CONST_WARN_DIALOG_ID,
				shell,
				SearchMessages.getString("Search.FindReferencesAction.BinPrimConstWarnDialog.title"), //$NON-NLS-1$
				null,
				SearchMessages.getString("Search.FindReferencesAction.BinPrimConstWarnDialog.message"), //$NON-NLS-1$
				MessageDialog.INFORMATION,
				new String[] { IDialogConstants.OK_LABEL },
				0);
	}
	
	private static boolean isBinaryPrimitveConstantOrString(IJavaElement element) {
		if (element != null && element.getElementType() == IJavaElement.FIELD) {
			IField field= (IField)element;
			int flags;
			try {
				flags= field.getFlags();
			} catch (JavaModelException ex) {
				return false;
			}
			return field.isBinary() && Flags.isStatic(flags) && Flags.isFinal(flags) && isPrimitiveOrString(field);
		}
		return false;
	}

	private static boolean isPrimitiveOrString(IField field) {
		String fieldType;
		try {
			fieldType= field.getTypeSignature();
		} catch (JavaModelException ex) {
			return false;
		}
		char first= fieldType.charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED && first != Signature.C_ARRAY)
			|| (first == Signature.C_RESOLVED && fieldType.substring(1, fieldType.length() - 1).equals(String.class.getName()));
	}
}
