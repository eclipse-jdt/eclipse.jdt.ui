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
package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.refactoring.CheckConditionsOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringErrorDialogUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;

public abstract class ReorgDestinationAction extends SelectionDispatchAction {

	protected ReorgDestinationAction(IWorkbenchSite site) {
		super(site);
	}

	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}

	private final boolean canOperateOn(IStructuredSelection selection) {
		try {
			if (selection.isEmpty())
				return false;
			if (ClipboardActionUtil.hasOnlyProjects(selection)) {
				if (selection.size() != 1)
					return false;
				IProject project= (IProject)((IAdaptable)selection.getFirstElement()).getAdapter(IProject.class);
				return project.isAccessible();
			} else {
				return createRefactoring(selection.toList()) != null;
			}
		} catch (JavaModelException e) {
			return false;
		}
	}

	public void run(IStructuredSelection selection) {
		if (!needsSaving(selection)) {
			doRun(selection);
		} else {
			RefactoringSaveHelper helper= new RefactoringSaveHelper();
			try {
				if (helper.saveEditors()) {
					doRun(selection);
				}
			} finally {
				helper.triggerBuild();
			}
		}
	}
	
	private void doRun(IStructuredSelection selection) {
		try{
			List elements= selection.toList();

			//XXX workaround bug 31998
			for (Iterator iter= elements.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (ActionUtil.mustDisableJavaModelAction(getShell(), element))
					return;
			}
			
			if (!ensureSaved(elements, getActionName()))
				return;
			ReorgRefactoring refactoring= createRefactoring(elements);
			if (refactoring == null)
				return;
			
			setUnsavedFileList(refactoring, elements);
			Object destination= selectDestination(refactoring);
			if (destination == null)
				return;
			String duplicate= getDuplicatedElementName(elements);
			if (duplicate != null){
				String message= ReorgMessages.getFormattedString("ReorgDestinationAction.duplicate", duplicate);//$NON-NLS-1$
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell().getShell(), ReorgMessages.getString("ReorgDestinationAction.duplicate_name"),	message); //$NON-NLS-1$
				return;
			}
			
			refactoring.setDestination(destination);
			
			Set excluded= getExcluded(refactoring);
			if (excluded == null) //canceled
				return;
			if (excluded.size() == elements.size())
				return;
			refactoring.setExcludedElements(excluded);	
				
			if (! isOkToProceed(refactoring))
				return;
			reorg(refactoring);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, ReorgMessages.getString("ReorgDestinationAction.exception_title"), ReorgMessages.getString("ReorgDestinationAction.exception")); //$NON-NLS-2$ //$NON-NLS-1$
		}	
	}
	
	/**
	 * returns null if no 2 elements have the same name
	 */
	private static  String getDuplicatedElementName(List elements){
		String[] names= getElementNames(elements);
		if (names.length == 0)
			return null;
		Arrays.sort(names);	
		String last= names[0];	
		for (int i= 1; i < names.length; i++){ //non standard loop
			if (last.equals(names[i]))
				return last;
			last= names[i];	
		}
		return null;
	}
	
	abstract String getActionName();
	abstract String getDestinationDialogMessage();
	abstract ReorgRefactoring createRefactoring(List elements) throws JavaModelException;

	//returns null iff canceled
	private static Set getExcluded(ReorgRefactoring refactoring) throws JavaModelException{
		Map elements= refactoring.getElementsThatExistInTarget();
		
		ReorgQueries.OverwriteQuery overwriteQuery= new ReorgQueries.OverwriteQuery();
		
		Set result= new HashSet();
		for (Iterator iter= elements.keySet().iterator(); iter.hasNext(); ){
			Object element= iter.next();
			Object conflicting= elements.get(element);
			if (! overwriteQuery.overwrite(conflicting))
				result.add(element);
		}
		if (overwriteQuery.isCanceled())
			return null;
		return result;
	}
	
	//hook to override
	protected boolean isOkToProceed(ReorgRefactoring refactoring) throws JavaModelException{
		return true;
	}
	
	private boolean needsSaving(IStructuredSelection selection) {
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= (Object) iter.next();
			if (element instanceof ICompilationUnit || element instanceof IType)
				return true;
		}
		return false;
	}
	
 	void reorg(ReorgRefactoring refactoring) throws JavaModelException{
		CheckConditionsOperation runnable= new CheckConditionsOperation(refactoring, CheckConditionsOperation.PRECONDITIONS);
		try {
			new ProgressMonitorDialog(getShell()).run(false, false, runnable);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), getActionName(), ReorgMessages.getString("ReorgDestinationAction.error_occurred"));  //$NON-NLS-1$
			return;
		} catch (InterruptedException e) {
			Assert.isTrue(false); //cannot happen - not cancelable
		}
		RefactoringStatus status= runnable.getStatus();           
		if (status == null)
			return;
		if (status.hasFatalError())
			RefactoringErrorDialogUtil.open(getActionName(), status);//$NON-NLS-1$
		else
			doReorg(refactoring);
	}
	
	private void doReorg(ReorgRefactoring refactoring) throws JavaModelException{
		MultiStatus status= ClipboardActionUtil.perform(refactoring);
		if (status.isOK()) 
			return;
		JavaPlugin.log(status);
		ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), getActionName(), ReorgMessages.getString("ReorgDestinationAction.error"), status); //$NON-NLS-1$
	}
		
	private static boolean ensureSaved(List elements, String actionName) {
		List unsavedEditors= new ArrayList();
		List unsavedElements= new ArrayList();
		
		collectUnsavedEditors(elements, unsavedEditors, unsavedElements);
		
		if (unsavedEditors.isEmpty())
			return true;
			
		ListSelectionDialog dialog = createUnsavedEditorDialog(unsavedElements);
		if (dialog.open() != Window.OK)
			return false;
		
		IEditorPart[] unsavedEditorArray= (IEditorPart[]) unsavedEditors.toArray(new IEditorPart[unsavedEditors.size()]);
		IRunnableWithProgress r= createSaveEditorOperation(dialog.getResult(), unsavedEditorArray);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, r);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, actionName, ReorgMessages.getString("ReorgAction.exception.saving")); //$NON-NLS-1$
			return false;
		} catch (InterruptedException e) {
		}
		return true;
	}
	
	private static void setUnsavedFileList(ReorgRefactoring refactoring, List elements){
		List unsavedEditors= new ArrayList(0);
		collectUnsavedEditors(elements, unsavedEditors, new ArrayList(0));
		refactoring.setUnsavedFiles(getFiles(unsavedEditors));
	}

	private static IFile[] getFiles(List editorParts){
		List result= new ArrayList(editorParts.size());
		for (Iterator iter= editorParts.iterator(); iter.hasNext(); ){
			IEditorPart each= (IEditorPart)iter.next();
			IEditorInput input= each.getEditorInput();
			if (input instanceof IFileEditorInput)
				result.add(((IFileEditorInput)input).getFile());
		}
		return (IFile[]) result.toArray(new IFile[result.size()]);
	}

	private static IRunnableWithProgress createSaveEditorOperation(final Object[] elementsToSave, final IEditorPart[] unsavedEditors) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				pm.beginTask(ReorgMessages.getString("ReorgAction.task.saving"), elementsToSave.length); //$NON-NLS-1$
				for (int i= 0; i < elementsToSave.length; i++) {
					IEditorPart editor= findEditor(elementsToSave[i], unsavedEditors);
					if (editor != null)
						editor.doSave(new SubProgressMonitor(pm, 1));
					else
						pm.worked(1);
				}
				pm.done();
			}
		};
	}
	
	private static IEditorPart findEditor(Object element, IEditorPart[] unsavedEditors){
		for (int i= 0; i < unsavedEditors.length; i++) {
			if (EditorUtility.isEditorInput(element, unsavedEditors[i]))
				return unsavedEditors[i];
		}
		return null;
	}

	private static ListSelectionDialog createUnsavedEditorDialog(List unsavedElements) {
		int labelFlags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_POST_QUALIFIED;
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		String msg= ReorgMessages.getString("ReorgAction.checkSaveTargets"); //$NON-NLS-1$
		ListSelectionDialog dialog= new ListSelectionDialog(parent, unsavedElements, new ListContentProvider(), 
			new JavaElementLabelProvider(labelFlags), msg);
		dialog.setTitle(ReorgMessages.getString("ReorgAction.checkSaveTargets.title")); //$NON-NLS-1$
		dialog.setInitialSelections(unsavedElements.toArray());
		return dialog;
	}
	
	private static void collectUnsavedEditors(List elements, List unsavedEditors, List unsavedElements) {
		IEditorPart[] editors= JavaPlugin.getDirtyEditors();
		for (int i= 0; i < editors.length; i++) {
			for (Iterator iter= elements.iterator(); iter.hasNext(); ){
				Object element= iter.next();
				if (EditorUtility.isEditorInput(element, editors[i])) {
					unsavedEditors.add(editors[i]);
					unsavedElements.add(element);
				}
			}
		}
	}
	
	private static String[] getElementNames(List elements){
		String[] result= new String[elements.size()];
		int i= 0;
		for (Iterator iter= elements.iterator(); iter.hasNext(); ){
			result[i]= ReorgUtils.getName(iter.next());
			i++;
		}   
		return result;
	}
	
	//overriden by d'n'd - must be protected
	protected Object selectDestination(ReorgRefactoring refactoring) {
		StandardJavaElementContentProvider cp= new DestinationContentProvider();
		ElementTreeSelectionDialog dialog= createDestinationSelectionDialog(JavaPlugin.getActiveWorkbenchShell(), new DestinationRenderer(JavaElementLabelProvider.SHOW_SMALL_ICONS), cp, refactoring);
		initDialog(dialog, getActionName(), getDestinationDialogMessage(), refactoring, null);
		
		return openDialog(dialog);
	}
	
	protected Object openDialog(ElementTreeSelectionDialog dialog) {
		if (dialog.open() != Window.CANCEL)
			return dialog.getFirstResult();
		return null;
	}

	public static void initDialog(ElementTreeSelectionDialog dialog, String title, String message, ReorgRefactoring refactoring, Object selection) {
		dialog.setTitle(title);
		dialog.setValidator(new ReorgSelectionValidator(refactoring));
		dialog.addFilter(new ContainerFilter(refactoring));
		dialog.setSorter(new JavaElementSorter());
		dialog.setMessage(message);
		dialog.setSize(60, 18);
		dialog.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
		if (selection == null)
			selection= computeCommonParent(refactoring.getElementsToReorg());
		dialog.setInitialSelection(selection);
	}
	
 	ElementTreeSelectionDialog createDestinationSelectionDialog(Shell parent, ILabelProvider labelProvider, StandardJavaElementContentProvider cp, ReorgRefactoring refactoring){
		return new ElementTreeSelectionDialog(parent, labelProvider, cp);
	}
	
	private static Object computeCommonParent(List elements){
		if (elements.isEmpty())
			return null;
		Object parent= elements.get(0);
		for (Iterator iter= elements.iterator(); iter.hasNext(); ){
			parent= computeCommonParent(parent, iter.next());
		}
		IResource parentRes= getResource(parent);
		IJavaElement parentElement= JavaCore.create(parentRes);
		if (parentElement != null)
			return parentElement;
		return getResource(parent);	
	}
	
	private static Object computeCommonParent(Object e1, Object e2){
		IResource r1= getResource(e1);
		IResource r2= getResource(e2);	
		if (r1 == null && r2 == null)
			return null;
		if (r1 == null)
			return r2.getParent();
		if (r2 == null)
			return r1.getParent();
		
		if (r1.equals(r2))
			return r1.getParent();
			
		if (r1.getFullPath().isPrefixOf(r2.getFullPath()))
			return r1;

		if (r2.getFullPath().isPrefixOf(r1.getFullPath()))
			return r2;
						
		IPath p1= r1.getParent().getFullPath();
		IPath p2= r2.getParent().getFullPath();
		IPath commonPath= new Path(""); //$NON-NLS-1$
		int segCount= Math.min(p1.segmentCount(), p2.segmentCount());
		for (int i= 0; i < segCount; i++){
			if (p1.segment(i).equals(p2.segment(i)))
				commonPath= commonPath.append(p1.segment(i));
			else
				break;	
		}
		return ResourcesPlugin.getWorkspace().getRoot().findMember(commonPath);
	}
	
	private static IResource getResource(Object o) {
		try{
			if (o instanceof IResource)
				return (IResource)o;
			else if (o instanceof IJavaElement)
				return ((IJavaElement)o).getCorrespondingResource();
			else
				return null;	
		} catch (JavaModelException e){
			JavaPlugin.log(e);
			return null;
		}		
	}
	
	//-----
	private static class ContainerFilter extends ViewerFilter {
		private ReorgRefactoring fRefactoring;
	
		ContainerFilter(ReorgRefactoring refactoring) {
			Assert.isNotNull(refactoring);
			fRefactoring= refactoring;
		}
	
		public boolean select(Viewer viewer, Object parent, Object o) {
			if (fRefactoring.getElementsToReorg().contains(o))
				return false;
			return fRefactoring.canBeAncestor(o);
		}
	}
	
	//-----
	static class DestinationRenderer extends JavaElementLabelProvider {
		public DestinationRenderer(int flags) {
			super(flags);
		}
	
		public String getText(Object element) {
			if (element instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot)element;
				if (root.getResource() instanceof IProject)
					return ReorgMessages.getString("DestinationRenderer.packages"); //$NON-NLS-1$
			}
			return super.getText(element);
		}
	}
	//------
	private static class ReorgSelectionValidator implements ISelectionStatusValidator {
		private ReorgRefactoring fRefactoring;
		
		public ReorgSelectionValidator(ReorgRefactoring refactoring) {
			Assert.isNotNull(refactoring);
			fRefactoring= refactoring;
		}
		
		public IStatus validate(Object[] selection)  {
			if (selection.length != 1)
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			try{	
				if (fRefactoring.isValidDestination(selection[0]))
					return new StatusInfo();
				return new StatusInfo(IStatus.ERROR, "");	 //$NON-NLS-1$
			} catch (JavaModelException e){
				ExceptionHandler.handle(e, ReorgMessages.getString("ReorgDestinationAction.exception_title"), ReorgMessages.getString("ReorgDestinationAction.exception")); //$NON-NLS-2$ //$NON-NLS-1$
				return new StatusInfo(IStatus.ERROR, ""); //$NON-NLS-1$
			}	
		}
	}
}
