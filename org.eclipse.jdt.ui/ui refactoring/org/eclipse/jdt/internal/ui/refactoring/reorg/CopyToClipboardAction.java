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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.TypedSource;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaElementTransfer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ParentChecker;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


public class CopyToClipboardAction extends SelectionDispatchAction{

	private final Clipboard fClipboard;
	private boolean fAutoRepeatOnFailure= false;

	public CopyToClipboardAction(IWorkbenchSite site) {
		this(site, null);
	}

	public CopyToClipboardAction(IWorkbenchSite site, Clipboard clipboard) {
		super(site);
		setText(ReorgMessages.CopyToClipboardAction_text);
		setDescription(ReorgMessages.CopyToClipboardAction_description);
		fClipboard= clipboard;
		ISharedImages workbenchImages= getWorkbenchSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		update(getSelection());

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.COPY_ACTION);
	}

	public void setAutoRepeatOnFailure(boolean autorepeatOnFailure){
		fAutoRepeatOnFailure= autorepeatOnFailure;
	}

	private static ISharedImages getWorkbenchSharedImages() {
		return JavaPlugin.getDefault().getWorkbench().getSharedImages();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		List elements= selection.toList();
		IResource[] resources= ReorgUtils.getResources(elements);
		IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
		if (elements.size() != resources.length + javaElements.length)
			setEnabled(false);
		else
			setEnabled(canEnable(resources, javaElements));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		try {
			List elements= selection.toList();
			IResource[] resources= ReorgUtils.getResources(elements);
			IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
			if (elements.size() == resources.length + javaElements.length && canEnable(resources, javaElements))
				doRun(resources, javaElements);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), ReorgMessages.CopyToClipboardAction_2, ReorgMessages.CopyToClipboardAction_3);
		}
	}

	private void doRun(IResource[] resources, IJavaElement[] javaElements) throws CoreException {
		ClipboardCopier copier= new ClipboardCopier(resources, javaElements, getShell(), fAutoRepeatOnFailure);

		if (fClipboard != null) {
			copier.copyToClipboard(fClipboard);
		} else {
			Clipboard clipboard= new Clipboard(getShell().getDisplay());
			try {
				copier.copyToClipboard(clipboard);
			} finally {
				clipboard.dispose();
			}
		}
	}

	private boolean canEnable(IResource[] resources, IJavaElement[] javaElements) {
		return new CopyToClipboardEnablementPolicy(resources, javaElements).canEnable();
	}

	//----------------------------------------------------------------------------------------//

	private static class ClipboardCopier{
		private final boolean fAutoRepeatOnFailure;
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;
		private final Shell fShell;
		private final ILabelProvider fLabelProvider;

		private ClipboardCopier(IResource[] resources, IJavaElement[] javaElements, Shell shell, boolean autoRepeatOnFailure) {
			Assert.isNotNull(resources);
			Assert.isNotNull(javaElements);
			Assert.isNotNull(shell);
			fResources= resources;
			fJavaElements= javaElements;
			fShell= shell;
			fLabelProvider= createLabelProvider();
			fAutoRepeatOnFailure= autoRepeatOnFailure;
		}

		public void copyToClipboard(Clipboard clipboard) throws CoreException {
			//Set<String> fileNames
			Set fileNames= new HashSet(fResources.length + fJavaElements.length);
			StringBuffer namesBuf = new StringBuffer();
			processResources(fileNames, namesBuf);
			processJavaElements(fileNames, namesBuf);

			IType[] mainTypes= ReorgUtils.getMainTypes(fJavaElements);
			ICompilationUnit[] cusOfMainTypes= ReorgUtils.getCompilationUnits(mainTypes);
			IResource[] resourcesOfMainTypes= ReorgUtils.getResources(cusOfMainTypes);
			addFileNames(fileNames, resourcesOfMainTypes);

			IResource[] cuResources= ReorgUtils.getResources(getCompilationUnits(fJavaElements));
			addFileNames(fileNames, cuResources);

			IResource[] resourcesForClipboard= ReorgUtils.union(fResources, ReorgUtils.union(cuResources, resourcesOfMainTypes));
			IJavaElement[] javaElementsForClipboard= ReorgUtils.union(fJavaElements, cusOfMainTypes);

			TypedSource[] typedSources= TypedSource.createTypedSources(javaElementsForClipboard);
			String[] fileNameArray= (String[]) fileNames.toArray(new String[fileNames.size()]);
			copyToClipboard(resourcesForClipboard, fileNameArray, namesBuf.toString(), javaElementsForClipboard, typedSources, 0, clipboard);
		}

		private static IJavaElement[] getCompilationUnits(IJavaElement[] javaElements) {
			List cus= ReorgUtils.getElementsOfType(javaElements, IJavaElement.COMPILATION_UNIT);
			return (ICompilationUnit[]) cus.toArray(new ICompilationUnit[cus.size()]);
		}

		private void processResources(Set fileNames, StringBuffer namesBuf) {
			for (int i= 0; i < fResources.length; i++) {
				IResource resource= fResources[i];
				addFileName(fileNames, resource);

				if (i > 0)
					namesBuf.append('\n');
				namesBuf.append(getName(resource));
			}
		}

		private void processJavaElements(Set fileNames, StringBuffer namesBuf) {
			for (int i= 0; i < fJavaElements.length; i++) {
				IJavaElement element= fJavaElements[i];
				switch (element.getElementType()) {
					case IJavaElement.JAVA_PROJECT :
					case IJavaElement.PACKAGE_FRAGMENT_ROOT :
					case IJavaElement.PACKAGE_FRAGMENT :
					case IJavaElement.COMPILATION_UNIT :
					case IJavaElement.CLASS_FILE :
						addFileName(fileNames, ReorgUtils.getResource(element));
						break;
					default :
						break;
				}

				if (fResources.length > 0 || i > 0)
					namesBuf.append('\n');
				namesBuf.append(getName(element));
			}
		}

		private static void addFileNames(Set fileName, IResource[] resources) {
			for (int i= 0; i < resources.length; i++) {
				addFileName(fileName, resources[i]);
			}
		}

		private static void addFileName(Set fileName, IResource resource){
			if (resource == null)
				return;
			IPath location = resource.getLocation();
			if (location != null) {
				fileName.add(location.toOSString());
			} else {
				// not a file system path. skip file.
			}
		}

		private void copyToClipboard(IResource[] resources, String[] fileNames, String names, IJavaElement[] javaElements, TypedSource[] typedSources, int repeat, Clipboard clipboard) {
			final int repeat_max_count= 10;
			try{
				clipboard.setContents(createDataArray(resources, javaElements, fileNames, names, typedSources),
										createDataTypeArray(resources, javaElements, fileNames, typedSources));
			} catch (SWTError e) {
				if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD || repeat >= repeat_max_count)
					throw e;
				if (fAutoRepeatOnFailure) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						// do nothing.
					}
				}
				if (fAutoRepeatOnFailure || MessageDialog.openQuestion(fShell, ReorgMessages.CopyToClipboardAction_4, ReorgMessages.CopyToClipboardAction_5))
					copyToClipboard(resources, fileNames, names, javaElements, typedSources, repeat + 1, clipboard);
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

	private static class CopyToClipboardEnablementPolicy {
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;
		public CopyToClipboardEnablementPolicy(IResource[] resources, IJavaElement[] javaElements){
			Assert.isNotNull(resources);
			Assert.isNotNull(javaElements);
			fResources= resources;
			fJavaElements= javaElements;
		}

		public boolean canEnable() {
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

		private boolean canCopyAllToClipboard() {
			for (int i= 0; i < fResources.length; i++) {
				if (! canCopyToClipboard(fResources[i])) return false;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (! canCopyToClipboard(fJavaElements[i])) return false;
			}
			return true;
		}

		private static boolean canCopyToClipboard(IJavaElement element) {
			if (element == null || ! element.exists())
				return false;

			if (JavaElementUtil.isDefaultPackage(element))
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
				if (ReorgUtils.isProject(fResources[i])) return true;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (ReorgUtils.isProject(fJavaElements[i])) return true;
			}
			return false;
		}

		private boolean hasNonProjects() {
			for (int i= 0; i < fResources.length; i++) {
				if (! ReorgUtils.isProject(fResources[i])) return true;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (! ReorgUtils.isProject(fJavaElements[i])) return true;
			}
			return false;
		}
	}
}
