/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.reorg.TypedSource;
import org.eclipse.jdt.internal.ui.reorg.TypedSourceTransfer;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;


public class CopyToClipboardAction extends SelectionDispatchAction{

	private final Clipboard fClipboard;
	private SelectionDispatchAction fPasteAction;//may be null

	public CopyToClipboardAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction) {
		super(site);
		Assert.isNotNull(clipboard);
		fClipboard= clipboard;
		fPasteAction= pasteAction;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		try {
			List elements= selection.toList();
			IResource[] resources= ReorgUtils2.getResources(elements);
			IJavaElement[] javaElements= ReorgUtils2.getJavaElements(elements);
			if (elements.size() != resources.length + javaElements.length)
				setEnabled(false);
			else
				setEnabled(canEnable(resources, javaElements));
		} catch (JavaModelException e) {
			//no ui here - this happens on selection changes
			setEnabled(false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		try {
			List elements= selection.toList();
			IResource[] resources= ReorgUtils2.getResources(elements);
			IJavaElement[] javaElements= ReorgUtils2.getJavaElements(elements);
			if (elements.size() == resources.length + javaElements.length && canEnable(resources, javaElements)) 
				doRun(resources, javaElements);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(), "Copy To Clipboard", "Internal error. See log for details.");
		}
	}

	private void doRun(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		new ClipboardCopier(resources, javaElements, fClipboard, getShell()).copyToClipboard();

		// update the enablement of the paste action
		// workaround since the clipboard does not support callbacks				
		if (fPasteAction != null && fPasteAction.getSelection() != null)
			fPasteAction.update(fPasteAction.getSelection());
	}

	private boolean canEnable(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		return new CopyToClipboardEnablementPolicy(resources, javaElements).canEnable();
	}
	
	//----------------------------------------------------------------------------------------//
	
	private static class ClipboardCopier{
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;
		private final Clipboard fClipboard;
		private final Shell fShell;
		private final ILabelProvider fLabelProvider;
		
		private ClipboardCopier(IResource[] resources, IJavaElement[] javaElements, Clipboard clipboard, Shell shell){
			Assert.isNotNull(resources);
			Assert.isNotNull(javaElements);
			Assert.isNotNull(clipboard);
			Assert.isNotNull(shell);
			fResources= resources;
			fJavaElements= javaElements;
			fClipboard= clipboard;
			fShell= shell;
			fLabelProvider= createLabelProvider();
		}

		public void copyToClipboard() throws JavaModelException{
			//List<String> fileNameList
			List fileNameList= new ArrayList(fResources.length + fJavaElements.length);
			StringBuffer namesBuf = new StringBuffer();
			processResources(fileNameList, namesBuf);
			processJavaElements(fileNameList, namesBuf);

			IType[] mainTypes= ReorgUtils2.getMainTypes(fJavaElements);
			processMainTypes(fileNameList, mainTypes);
			
			ICompilationUnit[] cusOfMainTypes= ReorgUtils2.getCompilationUnits(mainTypes);
			IResource[] resourcesForClipboard= ReorgUtils2.union(fResources, ReorgUtils2.getResources(cusOfMainTypes));
			IJavaElement[] javaElementsForClipboard= ReorgUtils2.union(fJavaElements, cusOfMainTypes);
			
			TypedSource[] typedSources= TypedSource.createTypeSources(javaElementsForClipboard);
			String[] fileNames= (String[]) fileNameList.toArray(new String[fileNameList.size()]);
			copyToClipboard(resourcesForClipboard, fileNames, namesBuf.toString(), javaElementsForClipboard, typedSources);
		}

		private void processResources(List fileNameList, StringBuffer namesBuf) {
			for (int i= 0; i < fResources.length; i++) {
				IResource resource= fResources[i];
				addFileNameToList(fileNameList, resource);

				if (i > 0)
					namesBuf.append('\n');
				namesBuf.append(getName(resource));
			}
		}

		private void processJavaElements(List fileNameList, StringBuffer namesBuf) {
			for (int i= 0; i < fJavaElements.length; i++) {
				IJavaElement element= fJavaElements[i];
				if (! ReorgUtils2.isInsideCompilationUnit(element))
					addFileNameToList(fileNameList, ReorgUtils2.getResource(element));

				if (fResources.length > 0 || i > 0)
					namesBuf.append('\n');
				namesBuf.append(getName(element));
			}
		}

		private void processMainTypes(List fileNameList, IType[] mainTypes) {
			for (int i= 0; i < mainTypes.length; i++) {
				IType mainType= mainTypes[i];
				IResource resource= ReorgUtils2.getResource(mainType.getCompilationUnit());
				addFileNameToList(fileNameList, resource);
			}
		}

		private static void addFileNameToList(List fileNameList, IResource resource){
			if (resource == null)
				return;
			IPath location = resource.getLocation();
			// location may be null. See bug 29491.
			if (location != null)
				fileNameList.add(location.toOSString());			
		}
		
		private void copyToClipboard(IResource[] resources, String[] fileNames, String names, IJavaElement[] javaElements, TypedSource[] typedSources){
			try{
				fClipboard.setContents( createDataArray(resources, javaElements, fileNames, names, typedSources),
										createDataTypeArray(resources, javaElements, fileNames, typedSources));
			} catch (SWTError e) {
				if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
					throw e;
				if (MessageDialog.openQuestion(fShell, "Problem Copying to Clipboard", "There was a problem when accessing the system clipboard. Retry?"))
					copyToClipboard(resources, fileNames, names, javaElements, typedSources);
			}
		}
		
		private static Transfer[] createDataTypeArray(IResource[] resources, IJavaElement[] javaElements, String[] fileNames, TypedSource[] typedSources) {
			List result= new ArrayList(4);
			if (resources.length != 0)
				result.add(ResourceTransfer.getInstance());
			if (javaElements.length != 0)
				result.add(JavaElementTransfer.getInstance());
			if (fileNames.length != 0)
				result.add(FileTransfer.getInstance());
			if (typedSources.length != 0)
				result.add(TypedSourceTransfer.getInstance());
			result.add(TextTransfer.getInstance());			
			return (Transfer[]) result.toArray(new Transfer[result.size()]);
		}

		private static Object[] createDataArray(IResource[] resources, IJavaElement[] javaElements, String[] fileNames, String names, TypedSource[] typedSources) {
			List result= new ArrayList(4);
			if (resources.length != 0)
				result.add(resources);
			if (javaElements.length != 0)
				result.add(javaElements);
			if (fileNames.length != 0)
				result.add(fileNames);
			if (typedSources.length != 0)
				result.add(typedSources);
			result.add(names);
			return result.toArray();
		}

		private static ILabelProvider createLabelProvider(){
			return new JavaElementLabelProvider(
				JavaElementLabelProvider.SHOW_VARIABLE
				+ JavaElementLabelProvider.SHOW_PARAMETERS
				+ JavaElementLabelProvider.SHOW_TYPE
			);		
		}
		private String getName(IResource resource){
			return fLabelProvider.getText(resource);
		}
		private String getName(IJavaElement javaElement){
			return fLabelProvider.getText(javaElement);
		}
	}
	
	private static class CopyToClipboardEnablementPolicy implements IReorgEnablementPolicy{
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;
		public CopyToClipboardEnablementPolicy(IResource[] resources, IJavaElement[] javaElements){
			Assert.isNotNull(resources);
			Assert.isNotNull(javaElements);
			fResources= resources;
			fJavaElements= javaElements;
		}

		public boolean canEnable() throws JavaModelException{
			if (fResources.length + fJavaElements.length == 0)
				return false;
			if (hasProjects() && hasNonProjects())
				return false;
			if (! canCopyAllToClipboard())
				return false;
			if (! new ParentChecker(fResources, fJavaElements).haveCommonParent())
				return false;
			return true;
		}

		private boolean canCopyAllToClipboard() throws JavaModelException {
			for (int i= 0; i < fResources.length; i++) {
				if (! canCopyToClipboard(fResources[i])) return false;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (! canCopyToClipboard(fJavaElements[i])) return false;
			}
			return true;
		}

		private static boolean canCopyToClipboard(IJavaElement element) throws JavaModelException {
			if (element == null)
				return false;
				
			if (! element.exists())
				return false;
				
			if (element instanceof IJavaModel)
				return false;
				
			if (JavaElementUtil.isDefaultPackage(element))		
				return false;
			
			if (element instanceof IMember && ! ReorgUtils2.hasSourceAvailable((IMember)element))
				return false;
			
			if (element instanceof IMember){
				/* feature in jdt core - initializers from class files are not binary but have no cus
				 * see bug 37199
				 * we just say 'no' to them
				 */
				IMember member= (IMember)element;
				if (! member.isBinary() && ReorgUtils2.getCompilationUnit(member) == null)
					return false;
			}
			
			if (ReorgUtils2.isDeletedFromEditor(element))
				return false;

			return true;
		}

		private static boolean canCopyToClipboard(IResource resource) {
			return 	resource != null && 
					resource.exists() &&
					! resource.isPhantom() &&
					resource.getType() != IResource.ROOT;
		}

		private boolean hasProjects() {
			for (int i= 0; i < fResources.length; i++) {
				if (ReorgUtils2.isProject(fResources[i])) return true;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (ReorgUtils2.isProject(fJavaElements[i])) return true;
			}
			return false;
		}

		private boolean hasNonProjects() {
			for (int i= 0; i < fResources.length; i++) {
				if (! ReorgUtils2.isProject(fResources[i])) return true;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (! ReorgUtils2.isProject(fJavaElements[i])) return true;
			}
			return false;
		}
	}
}
