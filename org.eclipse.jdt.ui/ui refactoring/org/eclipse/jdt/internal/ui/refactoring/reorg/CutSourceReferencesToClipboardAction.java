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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.ResourceTransfer;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class CutSourceReferencesToClipboardAction extends SourceReferenceAction {

	CopySourceReferencesToClipboardAction fCopy;
	DeleteSourceReferencesAction fDelete;
	
	protected CutSourceReferencesToClipboardAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction, String helpContextID) {
		super(site);
		setText(ReorgMessages.getString("CutSourceReferencesToClipboardAction.cut")); //$NON-NLS-1$
		fCopy= new CopySourceReferencesToClipboardAction(site, clipboard, pasteAction);
		fDelete= new DeleteSourceReferencesAction(site);
		update(getSelection());
		WorkbenchHelp.setHelp(this, helpContextID);
	}
	
	protected void perform(IStructuredSelection selection) throws CoreException {
		fCopy.perform(selection);
		fDelete.perform(selection);
	}

	public void selectionChanged(IStructuredSelection selection) {
		if (true) {
			setEnabled(false);//XXX for now
			return;
		}
		
		/*
		 * cannot cut top-level types. this deltes the cu and then you cannot paste because the cu is gone. 
		 */
		if (containsTopLevelTypes(selection)){
			setEnabled(false);
			return;
		}	
		fCopy.selectionChanged(selection);
		fDelete.selectionChanged(selection);
		setEnabled(fCopy.isEnabled() && fDelete.isEnabled());
	}

	private static boolean containsTopLevelTypes(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object each= iter.next();
			if ((each instanceof IType) && ((IType)each).getDeclaringType() == null)
				return true;
		}
		return false;
	}
	
	//TODO delete this class after reorg transition
	public static class DeleteSourceReferencesAction extends SourceReferenceAction {

		private boolean fCanDeleteGetterSetter;
		private boolean fAskForDeleteConfirmation;
	
		public DeleteSourceReferencesAction(IWorkbenchSite site) {
			super(site);
			fCanDeleteGetterSetter= true;
			fAskForDeleteConfirmation= true;
		}

		public void setCanDeleteGetterSetter(boolean canDelete){
			fCanDeleteGetterSetter= canDelete;
		}

		public void setAskForDeleteConfirmation(boolean ask){
			fAskForDeleteConfirmation= ask;
		}
	
		protected void perform(IStructuredSelection selection) throws CoreException {
			if (fAskForDeleteConfirmation && !confirmDelete(selection))
				return;
			
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, true, createDeleteOperation(selection));
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), ReorgMessages.getString("DeleteSourceReferenceAction.error.title"), ReorgMessages.getString("DeleteSourceReferenceAction.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InterruptedException e) {
				//nothing. action interrupted
			}
		}
	
		private IRunnableWithProgress createDeleteOperation(final IStructuredSelection selection){
			return new IRunnableWithProgress(){
				public void run(IProgressMonitor pm) throws InvocationTargetException, InterruptedException {
					try {
						JavaCore.run(createDeleteRunnable(selection), pm);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						pm.done();
					}
				}
			};
		}
	
		private IWorkspaceRunnable createDeleteRunnable(final IStructuredSelection selection){
			return new IWorkspaceRunnable(){
				public void run(IProgressMonitor pm) throws CoreException {
					Map mapping= SourceReferenceUtil.groupByFile(getElementsToProcess(selection)); //IFile -> List of ISourceReference (elements from that file)
					int size= mapping.keySet().size();
					pm.beginTask(ReorgMessages.getString("DeleteSourceReferenceAction.deleting"), 3 * size); //$NON-NLS-1$
		
					if (areAllFilesReadOnly(mapping)){
						String title= ReorgMessages.getString("DeleteSourceReferencesAction.title"); //$NON-NLS-1$
						String label= ReorgMessages.getString("DeleteSourceReferencesAction.read_only");  //$NON-NLS-1$
						MessageDialog.openInformation(getShell(), title, label);
						return;
					}	
				
					List emptyCuList= Arrays.asList(getCusLeftEmpty(mapping));
		
					for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
						IFile file= (IFile)iter.next();
						if (emptyCuList.contains(JavaCore.create(file))) //do not delete in these files
							continue;
						if (isReadOnly(file))
							continue;
						deleteAll(mapping, file, new SubProgressMonitor(pm, 1));
					}
		
					ICompilationUnit[] notDeleted= deleteEmptyCus(mapping, new SubProgressMonitor(pm, size));
					for (int i= 0; i < notDeleted.length; i++) {
						IFile file= (IFile)notDeleted[i].getResource();
						if (isReadOnly(file))
							continue;
						deleteAll(mapping, file, new SubProgressMonitor(pm, 1));
					}		
				}
			};
		}

		private static boolean isReadOnly(IFile file){
			if (! file.isReadOnly())
				return false;
			if (ResourcesPlugin.getWorkspace().validateEdit(new IFile[]{file}, null).isOK())
				return false;
			return true;
		}
	
		private static boolean areAllFilesReadOnly(Map mapping){
			for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
				if (! isReadOnly((IFile)iter.next()))
					return false;
			}
			return true;
		}
	
		private static void deleteAll(Map mapping, IFile file, IProgressMonitor pm) throws CoreException {
			List l= (List)mapping.get(file);
			ISourceReference[] refs= (ISourceReference[]) l.toArray(new ISourceReference[l.size()]);
			pm.beginTask("", refs.length); //$NON-NLS-1$
		
			ISourceReference[] nonFields= getNonFields(refs);
			delete(file, nonFields, new SubProgressMonitor(pm, nonFields.length));
		
			IField[] fields= getFields(refs);
			delete(fields, new SubProgressMonitor(pm, fields.length));
			pm.done();
		}

		private static void delete(IFile file, ISourceReference[] nonFields, IProgressMonitor pm) throws CoreException{
			pm.beginTask("", 2); //$NON-NLS-1$
			TextBuffer tb= TextBuffer.acquire(file);
			try{
				TextBufferEditor tbe= new TextBufferEditor(tb);
				for (int i= 0; i < nonFields.length; i++) {
					Assert.isTrue(! (nonFields[i] instanceof IField));
					tbe.add(createDeleteEdit(nonFields[i]));
					if (pm.isCanceled())
						throw new OperationCanceledException();			
				}
				if (! tbe.canPerformEdits().isOK())
					return; ///XXX can i assert here?
				tbe.performEdits(new SubProgressMonitor(pm, 1));	
				TextBuffer.commitChanges(tb, false, new SubProgressMonitor(pm, 1));
			} finally{
				if (tb != null)
					TextBuffer.release(tb);
				pm.done();
			}	
		}
	
		private static void delete(IField[] fields, IProgressMonitor pm) throws JavaModelException{
			if (fields.length != 0)
				getJavaModel().delete(fields, false, pm);
		}

		private static IJavaModel getJavaModel() {
			return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		}
			
		private static TextEdit createDeleteEdit(ISourceReference ref) throws CoreException {
			ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(ref);
			cu.reconcile();
			return DeleteSourceReferenceEdit.create(ref, cu);
		}
	
		/**
		 * returns cus that have <b>not</b> been deleted
		 */
		private ICompilationUnit[] deleteEmptyCus(Map mapping, IProgressMonitor pm) throws JavaModelException {
			ICompilationUnit[] cusToDelete= getCusLeftEmpty(mapping);
			if (cusToDelete.length == 0)
				return cusToDelete;
			
			if (! confirmCusDelete(cusToDelete))
				return cusToDelete;
		
			List notDeletedCus= new ArrayList();
			notDeletedCus.addAll(Arrays.asList(cusToDelete));	
		
			pm.beginTask("", cusToDelete.length); //$NON-NLS-1$
			for (int i= 0; i < cusToDelete.length; i++) {
				if (isReadOnly(cusToDelete[i]) && (! isOkToDeleteReadOnly(cusToDelete[i])))
					continue;
				cusToDelete[i].delete(false, new SubProgressMonitor(pm, 1));
				notDeletedCus.remove(cusToDelete[i]);
			}
			pm.done();
			return (ICompilationUnit[]) notDeletedCus.toArray(new ICompilationUnit[notDeletedCus.size()]);
		}
	
		private static boolean isReadOnly(ICompilationUnit cu) throws JavaModelException{
			if (cu.isReadOnly())
				return true;
			if (cu.getResource().isReadOnly())	
				return true;
			return false;	
		}


		private boolean isOkToDeleteReadOnly(ICompilationUnit cu){
			String message= ReorgMessages.getFormattedString("DeleteSourceReferencesAction.cu_read_only", getName(cu));//$NON-NLS-1$
			return MessageDialog.openQuestion(getShell(), ReorgMessages.getString("DeleteSourceReferencesAction.delete1"), message); //$NON-NLS-1$
		}
	
		/*
		 * @param Map mapping //IFile -> List of ISourceReference (elements from that file)
		 */
		private static ICompilationUnit[] getCusLeftEmpty(Map mapping) throws JavaModelException{
			List cuList= new ArrayList();
			for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
				IFile file= (IFile) iter.next();
				IJavaElement el= JavaCore.create(file);
				if (el == null || el.getElementType() != IJavaElement.COMPILATION_UNIT)
					continue;
				ICompilationUnit cu= (ICompilationUnit)el;
				List sourceReferences= (List)mapping.get(file);
				if (willBeLeftEmpty(cu, sourceReferences))
					cuList.add(cu);
			}
			return (ICompilationUnit[]) cuList.toArray(new ICompilationUnit[cuList.size()]);
		}
	
		private static boolean willBeLeftEmpty(ICompilationUnit cu, List sourceReferences) throws JavaModelException{
			IType[] cuTypes= WorkingCopyUtil.getWorkingCopyIfExists(cu).getTypes();
			for (int i= 0; i < cuTypes.length; i++) {
				if (! sourceReferences.contains(cuTypes[i]))
					return false;
			}
			return true;
		}	
	
		//overridden to add getters/setters
		protected ISourceReference[] getElementsToProcess(IStructuredSelection selection) {
			ISourceReference[] elements= super.getElementsToProcess(selection);
			if (! fCanDeleteGetterSetter)
				return elements;
			IField[] fields= getFields(elements);
			if (fields.length == 0)
				return elements;
			IMethod[] gettersSetters= getGettersSettersForFields(fields);		
			if (gettersSetters.length == 0)
				return elements;
			Set getterSetterSet= new HashSet(Arrays.asList(gettersSetters));
			getterSetterSet.removeAll(Arrays.asList(elements));
			if (getterSetterSet.isEmpty())
				return elements;
			if (! confirmGetterSetterDelete())
				return elements;
		
			Set newElementSet= new HashSet(Arrays.asList(elements));
			newElementSet.addAll(getterSetterSet);
			return (ISourceReference[]) newElementSet.toArray(new ISourceReference[newElementSet.size()]);
		}

		private static ISourceReference[] getNonFields(ISourceReference[] elements){
			List nonFields= new ArrayList();
			for (int i= 0; i < elements.length; i++) {
				if (! (elements[i] instanceof IField))
					nonFields.add(elements[i]);
			}
			return (ISourceReference[]) nonFields.toArray(new ISourceReference[nonFields.size()]);
		}
	
		private static IField[] getFields(ISourceReference[] elements){
			List fields= new ArrayList();
			for (int i= 0; i < elements.length; i++) {
				if (elements[i] instanceof IField)
					fields.add(elements[i]);
			}
			return (IField[]) fields.toArray(new IField[fields.size()]);
		}
	
		private IMethod[] getGettersSettersForFields(IField[] fields) {
			try {
				List gettersSetters= new ArrayList();
				for (int i= 0; i < fields.length; i++) {
					IMethod getter= GetterSetterUtil.getGetter(fields[i]);
					if (getter != null && getter.exists())
						gettersSetters.add(getter);
					IMethod setter= GetterSetterUtil.getSetter(fields[i]);
					if (setter != null && setter.exists())
						gettersSetters.add(setter);			
				}
				return  (IMethod[]) gettersSetters.toArray(new IMethod[gettersSetters.size()]);
			} catch(JavaModelException e) {
				ExceptionHandler.handle(e, getShell(), ReorgMessages.getString("DeleteSourceReferencesAction.delete_elements"), ReorgMessages.getString("DeleteSourceReferencesAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
				return new IMethod[0];
			}
		}
		private boolean confirmDelete(IStructuredSelection selection) {
			String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
			String label;
			if (selection.size() == 1){
				String[] keys= {getName(selection.getFirstElement())};
				label= ReorgMessages.getFormattedString("DeleteSourceReferencesAction.sure", keys); //$NON-NLS-1$
			} else {
				String[] keys= {String.valueOf(selection.size())};
				label= ReorgMessages.getFormattedString("DeleteSourceReferencesAction.sure_elements", keys); //$NON-NLS-1$
			}
			return MessageDialog.openQuestion(getShell(), title, label);
		}

		private static String getName(Object element) {
			if (element instanceof IJavaElement){
				ILabelProvider lp= new JavaElementLabelProvider();
				String text= lp.getText(element);
				lp.dispose();
				return text;
			} else if (element instanceof IResource)
				return ReorgUtils.getName((IResource)element);
			Assert.isTrue(false);
			return null;
		}

		//made protected for ui-less testing
		protected  boolean confirmCusDelete(ICompilationUnit[] cusToDelete) {
			String message;
			if (cusToDelete.length == 1){
				message= ReorgMessages.getFormattedString("DeleteSourceReferencesAction.cu_empty", getName(cusToDelete[0]));//$NON-NLS-1$
			} else {
				message= ReorgMessages.getFormattedString("DeleteSourceReferencesAction.cus_empty", String.valueOf(cusToDelete.length));//$NON-NLS-1$
			}	
			return MessageDialog.openQuestion(getShell(), ReorgMessages.getString("DeleteSourceReferencesAction.delete1"), message); //$NON-NLS-1$
		}
	
		//made protected for ui-less testing
		protected boolean confirmGetterSetterDelete() {
			String title= ReorgMessages.getString("DeleteSourceReferencesAction.confirm_gs_delete"); //$NON-NLS-1$
			String label= ReorgMessages.getString("DeleteSourceReferencesAction.delete_gs"); //$NON-NLS-1$
			Shell parent= getShell();
			return MessageDialog.openQuestion(parent, title, label);
		}
	
		/*
		 * @see SourceReferenceAction#canWorkOn(Object)
		 */
		protected boolean canWorkOn(Object element) throws JavaModelException {
			if (! super.canWorkOn(element))
				return false;
			if (element instanceof IMember && ((IMember)element).isBinary())
				return false;
			return true;	
		}
	}
	
	//TODO delete this class after reorg transition
	private static class CopySourceReferencesToClipboardAction extends SourceReferenceAction{

	private Clipboard fClipboard;
	private SelectionDispatchAction fPasteAction;

	protected CopySourceReferencesToClipboardAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction) {
		super(site);
		Assert.isNotNull(clipboard);
		fClipboard= clipboard;
		fPasteAction= pasteAction;
	}

	protected void perform(IStructuredSelection selection) throws JavaModelException {
		copyToOSClipbard(getElementsToProcess(selection));
	}
	
	private void copyToOSClipbard(ISourceReference[] refs)  throws JavaModelException {
		try{
			fClipboard.setContents(createClipboardInput(refs), createTransfers());
					
			// update the enablement of the paste action
			// workaround since the clipboard does not suppot callbacks				
			if (fPasteAction != null && fPasteAction.getSelection() != null)
				fPasteAction.update(fPasteAction.getSelection());
			
		} catch (SWTError e){
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(getShell(), ReorgMessages.getString("CopyToClipboardProblemDialog.title"), ReorgMessages.getString("CopyToClipboardProblemDialog.message"))) //$NON-NLS-1$ //$NON-NLS-2$
				copyToOSClipbard(refs);
		}	
	}
		
	private static Object[] createClipboardInput(ISourceReference[] refs) throws JavaModelException {
		TypedSource[] typedSources= convertToTypedSourceArray(refs);
		return new Object[] { convertToInputForTextTransfer(typedSources), typedSources, getResourcesForMainTypes(refs)};
	}
	private static Transfer[] createTransfers() {
		return new Transfer[] { TextTransfer.getInstance(), TypedSourceTransfer.getInstance(), ResourceTransfer.getInstance()};
	}

	private static String convertToInputForTextTransfer(TypedSource[] typedSources) throws JavaModelException {
		String lineDelim= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < typedSources.length; i++) {
			buff.append(typedSources[i].getSource()).append(lineDelim);
		}
		return buff.toString();
	}

	private static TypedSource[] convertToTypedSourceArray(ISourceReference[] refs) throws JavaModelException {
		TypedSource[] elems= new TypedSource[refs.length];
		for (int i= 0; i < refs.length; i++) {
			elems[i]= TypedSource.create(refs[i]);
		}
		return elems;
	}
	
	private static IResource[] getResourcesForMainTypes(ISourceReference[] refs){
		IType[] mainTypes= getMainTypes(refs);
		List resources= new ArrayList();
		for (int i= 0; i < mainTypes.length; i++) {
			IResource resource= getResource(mainTypes[i]);
			if (resource != null)
				resources.add(resource);
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}
	
	private static IType[] getMainTypes(ISourceReference[] refs){
		List mainTypes= new ArrayList();
		for (int i= 0; i < refs.length; i++) {
			try {
				if ((refs[i] instanceof IType) && JavaElementUtil.isMainType((IType)refs[i]))
					mainTypes.add(refs[i]);
			} catch(JavaModelException e) {
				JavaPlugin.log(e);//cannot show dialog
			}
		}
		return (IType[]) mainTypes.toArray(new IType[mainTypes.size()]);
	}
	
	private static IResource getResource(IType type){
		return ResourceUtil.getResource(type);
	}

}
}
abstract class SourceReferenceAction extends SelectionDispatchAction {

	//workaround for bug 18311
	private static final ISourceRange fgUnknownRange= new SourceRange(-1, 0);

	protected SourceReferenceAction(IWorkbenchSite site) {
		super(site);
	}

	protected ISourceReference[] getElementsToProcess(IStructuredSelection selection) {
		return SourceReferenceUtil.removeAllWithParentsSelected(getSelectedElements(selection));
	}	
	
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public final void run(final IStructuredSelection selection) {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					perform(selection);
				} catch (CoreException e) {
					ExceptionHandler.handle(e, getText(), ReorgMessages.getString("SourceReferenceAction.exception")); //$NON-NLS-1$
				}
			}
		});
	}
	
	protected abstract void perform(IStructuredSelection selection) throws CoreException;
	
	private boolean canOperateOn(IStructuredSelection selection) {
		try{
			if (selection.isEmpty())			
				return false;
			Object[] elems= selection.toArray();
			for (int i= 0; i < elems.length; i++) {
				Object elem= elems[i];
				if (! canWorkOn(elem))
					return false;
			}
			return true;
		} catch (JavaModelException e){
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e);
			return false;
		}	
	}
	
	private ISourceReference[] getSelectedElements(IStructuredSelection selection){
		return getWorkingCopyElements(selection.toList());
	}
	
	protected boolean canWorkOn(Object elem) throws JavaModelException{
		if (elem == null)
			return false;
			
		if (! (elem instanceof ISourceReference)) 
			return false;
			
		if (! (elem instanceof IJavaElement)) 
			return false;
								
		if (elem instanceof IClassFile) 
			return false;

		if (elem instanceof ICompilationUnit)
			return false;

		if (elem instanceof IMember){ 
			IMember member= (IMember)elem;
			if (member.isBinary() && (member.getSourceRange() == null || fgUnknownRange.equals(member.getSourceRange())))
				return false;
		}
		
		if (elem instanceof IMember){
			/* feature in jdt core - initializers from class files are not binary but have no cus
			 * see bug 37199
			 * we just say 'no' to them
			 */
			IMember member= (IMember)elem;
			if (! member.isBinary() && SourceReferenceUtil.getCompilationUnit(member) == null)
				return false;					
		}
		
		if (isDeletedFromEditor((ISourceReference)elem))
			return false;			
			
		if (elem instanceof IMember) //binary excluded before
			return true;

		if (elem instanceof IImportContainer)
			return true;

		if (elem instanceof IImportDeclaration)
			return true;

		if (elem instanceof IPackageDeclaration)
			return true;			
		
		//we never get here normally
		return false;	
	}

	private static boolean isDeletedFromEditor(ISourceReference elem) throws JavaModelException{
		if (elem instanceof IMember && ((IMember)elem).isBinary())
			return false;
		ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(elem);
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		if (wc.equals(cu))
			return false;
		IJavaElement element= (IJavaElement)elem;
		IJavaElement wcElement= JavaModelUtil.findInCompilationUnit(wc, element);
		return wcElement == null || ! wcElement.exists();
	}
			
	private static ISourceReference[] getWorkingCopyElements(List l) {
		List wcList= new ArrayList(l.size());
		for (Iterator iter= l.iterator(); iter.hasNext();) {
			ISourceReference element= (ISourceReference) iter.next();
			if (! (element instanceof IJavaElement)) //can this happen ?
				wcList.add(element); 
			ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(element);
			if (cu == null){
				wcList.add(element);
			} else if (cu.isWorkingCopy()){
				wcList.add(element);
			} else {
				ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
				try {
					IJavaElement wcElement= JavaModelUtil.findInCompilationUnit(wc, (IJavaElement)element);
					if (wcElement != null && wcElement.exists())
						wcList.add(wcElement);
				} catch(JavaModelException e) {
					JavaPlugin.log(e); //cannot show dialog here
					//do nothing - do not add to selection (?)
				}
			}	
		}
		return (ISourceReference[]) wcList.toArray(new ISourceReference[wcList.size()]);
	}	

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}

}

