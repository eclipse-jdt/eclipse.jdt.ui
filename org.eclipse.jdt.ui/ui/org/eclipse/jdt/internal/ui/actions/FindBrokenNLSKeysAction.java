/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHintHelper;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.nls.search.SearchBrokenNLSKeysUtil;

public class FindBrokenNLSKeysAction extends SelectionDispatchAction {
	
	private static class SearchPatternData {

		private final IType fAccessorType;
		private final IFile fPropertyFile;
		
		public SearchPatternData(IType accessorType, IFile propertyFile) {
			fAccessorType= accessorType;
			fPropertyFile= propertyFile;
		}

		public IFile getPropertyFile() {
			return fPropertyFile;
		}

		public IType getWrapperClass() {
			return fAccessorType;
		}

	}

	//TODO: Add to API: IJavaEditorActionDefinitionIds
	public static final String FIND_BROKEN_NLS_KEYS_ACTION_ID= "org.eclipse.jdt.ui.edit.text.java.findNLSProblems"; //$NON-NLS-1$

	//TODO: Add to API: JdtActionConstants
	public static final String ACTION_HANDLER_ID= "org.eclipse.jdt.ui.actions.FindNLSProblems"; //$NON-NLS-1$
	
	private JavaEditor fEditor;
	
	public FindBrokenNLSKeysAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.FindNLSProblemsAction_Name); 
		setToolTipText(ActionMessages.FindNLSProblemsAction_ToolTip); 
		setDescription(ActionMessages.FindNLSProblemsAction_Description);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 */
	public FindBrokenNLSKeysAction(JavaEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(false);
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(ITextSelection selection) {
		try {
			ICompilationUnit compilationUnit= getCompilationUnit(fEditor);
			if (compilationUnit == null)
				return;
			
			SearchPatternData[] data= tryIfPropertyCuSelected(compilationUnit);
			run(data, compilationUnit.getElementName());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}		
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void run(IStructuredSelection selection) {
		SearchPatternData[] data= getNLSFiles(selection);
		if (data == null || data.length == 0) {
			MessageDialog.openInformation(getShell(), ActionMessages.FindNLSProblemsAction_ErrorDialogTitle, ActionMessages.FindNLSProblemsAction_NoPropertieFilesFoundErrorDescription);
			return;
		}
			
		String scope= "workspace"; //$NON-NLS-1$
		if (selection.size() == 1) {
			Object firstElement= selection.getFirstElement();
			if (firstElement instanceof IJavaElement) {
				scope= ((IJavaElement)firstElement).getElementName();
			} else if (firstElement instanceof IFile) {
				scope= ((IFile)firstElement).getName();
			} else if (firstElement instanceof IFolder) {
				scope= ((IFolder)firstElement).getName();
			}
		}
		run(data, scope);
	}
	
	private void run(SearchPatternData[] data, String scope) {
		List wrappers= new ArrayList();
		List properties= new ArrayList();
		for (int i= 0; i < data.length; i++) {
			SearchPatternData current= data[i];
			if (current.getWrapperClass() != null || current.getPropertyFile() != null) {
				wrappers.add(current.getWrapperClass());
				properties.add(current.getPropertyFile());
			}
		}
		IType[] accessorClasses= (IType[])wrappers.toArray(new IType[wrappers.size()]);
		IFile[] propertieFiles= (IFile[])properties.toArray(new IFile[properties.size()]);
		SearchBrokenNLSKeysUtil.search(scope, accessorClasses, propertieFiles);
	}

	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(ITextSelection selection) {
		try {
			setEnabled(tryIfPropertyCuSelected(getCompilationUnit(fEditor)) != null);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			setEnabled(false);
		}
	}
	
	/* (non-Javadoc)
	 * Method declared on SelectionDispatchAction.
	 */
	public void selectionChanged(IStructuredSelection selection) {
		try {
			if (selection.size() == 1 && selection.getFirstElement() instanceof ICompilationUnit) {
				setEnabled(true);
			} else {
				setEnabled(canEnable(selection.toArray()));
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
			setEnabled(false);
		}
	}
	
	private SearchPatternData[] getNLSFiles(IStructuredSelection selection) {
		Object[] selectedElements= selection.toArray();
		if (selectedElements.length == 1 && selectedElements[0] instanceof ICompilationUnit) {
			try {
				ICompilationUnit compilationUnit= (ICompilationUnit)selectedElements[0];
				return tryIfPropertyCuSelected(compilationUnit);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}

		}
		
		Hashtable result= new Hashtable();
		try {
			collectNLSFiles(selectedElements, result);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		Collection values= result.values();
		return (SearchPatternData[])values.toArray(new SearchPatternData[values.size()]);
	}
	
	private boolean canEnable(Object[] objects) throws CoreException {
		for (int i= 0; i < objects.length; i++) {
			if (objects[i] instanceof IFile) {
				IFile file= (IFile)objects[i];
				if ("properties".equalsIgnoreCase(file.getFileExtension())) //$NON-NLS-1$
					return true;
			} else if (objects[i] instanceof IFolder) {
				IFolder folder= (IFolder)objects[i];
				if (canEnable(folder.members()))
					return true;
			} else if (objects[i] instanceof IProject) {
				IProject project= (IProject)objects[i];
				if (project.exists() && project.isOpen() && canEnable(project.members()))
					return true;
			} else if (objects[i] instanceof IJavaProject) {
				IJavaProject project= (IJavaProject)objects[i];
				if (canEnable(project.getAllPackageFragmentRoots()))
					return true;
			} else if (objects[i] instanceof IJavaElement) {
				IJavaElement element= (IJavaElement)objects[i];
				IResource resource= element.getCorrespondingResource();
				if (resource != null) {
					if (canEnable(new Object[] {resource}))
						return true;
				}
			}
		}
		return false;
	}

	private void collectNLSFiles(Object[] objects, Hashtable result) throws CoreException {
		for (int i= 0; i < objects.length; i++) {
			if (objects[i] instanceof IFile) {
				if (!result.containsKey(objects[i])) {
					SearchPatternData data= tryIfPropertyFileSelected((IFile)objects[i]);
					if (data != null) {
						result.put(objects[i], data);
					} 
				}
			} else if (objects[i] instanceof IFolder) {
				IFolder folder= (IFolder)objects[i];
				collectNLSFiles(folder.members(), result);
			} else if (objects[i] instanceof IProject) {
				IProject project= ((IProject)objects[i]);
				if (project.exists() && project.isOpen())
					collectNLSFiles(project.members(), result);
			} else if (objects[i] instanceof IJavaProject) {
				IJavaProject project= (IJavaProject)objects[i];
				if (project.exists() && project.isOpen())
					collectNLSFiles(project.getAllPackageFragmentRoots(), result);
			} else if (objects[i] instanceof IJavaElement) {
				IJavaElement element= (IJavaElement)objects[i];
				IResource resource= element.getCorrespondingResource();
				if (resource != null) {
					collectNLSFiles(new Object[] {resource}, result);
				}
			}
		}
	}
	
	private SearchPatternData[] tryIfPropertyCuSelected(ICompilationUnit compilationUnit) throws JavaModelException {
		if (compilationUnit == null)
			return null;
		
		IType[] types= compilationUnit.getTypes();
		if (types.length > 1)
			return null;
		
		IStorage bundle= NLSHintHelper.getResourceBundle(compilationUnit);
		if (!(bundle instanceof IFile))
			return null;

		return new SearchPatternData[] {new SearchPatternData(types[0], (IFile)bundle)};
	}
	
	private SearchPatternData tryIfPropertyFileSelected(IFile file) throws JavaModelException {
		if (!"properties".equalsIgnoreCase(file.getFileExtension())) //$NON-NLS-1$
			return null;
		
		IPath propertyFullPath= file.getFullPath();
		// Try to find a corresponding CU
		String[] javaExtensions= JavaCore.getJavaLikeExtensions();
		for (int i= 0; i < javaExtensions.length; i++) { 
			String extension= javaExtensions[i];
			IPath cuPath= propertyFullPath.removeFileExtension().addFileExtension(extension);
			IFile cuFile= (IFile)JavaPlugin.getWorkspace().getRoot().findMember(cuPath);
			
			if (cuFile == null) { //try with uppercase first char
				String filename= cuPath.removeFileExtension().lastSegment();
				if (filename != null && filename.length() > 0) {
					filename= Character.toUpperCase(filename.charAt(0)) + filename.substring(1);
					IPath dirPath= propertyFullPath.removeLastSegments(1).addTrailingSeparator();
					cuPath= dirPath.append(filename).addFileExtension(extension);
					cuFile= (IFile)JavaPlugin.getWorkspace().getRoot().findMember(cuPath);
				}
			}

			if (cuFile != null && cuFile.exists()) {
				IJavaElement  element= JavaCore.create(cuFile);
				if (element != null && element.exists() && element.getElementType() == IJavaElement.COMPILATION_UNIT) {
					ICompilationUnit compilationUnit= (ICompilationUnit)element;
					IType type= (compilationUnit).findPrimaryType();
					if (type != null) {
						String resourceBundleName= NLSHintHelper.getResourceBundleName(compilationUnit);
						if (resourceBundleName != null) {
							String resourceName= resourceBundleName + NLSRefactoring.PROPERTY_FILE_EXT;
							String name= file.getName();
							if (resourceName.endsWith(name)) {
								return new SearchPatternData(type, file);
							}
						}
					}
				}
			}
		}

		return null;
	}
	
	private static ICompilationUnit getCompilationUnit(JavaEditor editor) {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit cu= manager.getWorkingCopy(editor.getEditorInput());
		return cu;
	}

}
