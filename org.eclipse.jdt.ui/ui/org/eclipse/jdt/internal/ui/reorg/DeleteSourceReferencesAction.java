package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeGenerationPreferencePage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class DeleteSourceReferencesAction extends SourceReferenceAction {

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
                    performDeletion(selection, pm);
                } catch (CoreException e) {
                	throw new InvocationTargetException(e);
                }
            }
		};
	}
	
	private void performDeletion(IStructuredSelection selection, IProgressMonitor pm) throws CoreException{
		Map mapping= SourceReferenceUtil.groupByFile(getElementsToProcess(selection)); //IFile -> List of ISourceReference (elements from that file)
        int size= mapping.keySet().size();
		pm.beginTask(ReorgMessages.getString("DeleteSourceReferenceAction.deleting"), 3 * size); //$NON-NLS-1$
		
		if (areAllFilesReadOnly(mapping)){
			String title= ReorgMessages.getString("DeleteSourceReferencesAction.title"); //$NON-NLS-1$
			String label= ReorgMessages.getString("DeleteSourceReferencesAction.read_only");  //$NON-NLS-1$
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), title, label);
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
			IFile file= (IFile)notDeleted[i].getUnderlyingResource();
			if (isReadOnly(file))
				continue;
			deleteAll(mapping, file, new SubProgressMonitor(pm, 1));
		}		
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
		}	
	}
	
	private static void delete(IField[] fields, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fields.length); //$NON-NLS-1$
		for (int i= 0; i < fields.length; i++) {
			fields[i].delete(false, new SubProgressMonitor(pm, 1));
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
	}
			
	private static TextEdit createDeleteEdit(ISourceReference ref) throws JavaModelException{
		ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(ref);
		cu.reconcile();
		return new DeleteSourceReferenceEdit(ref, cu);
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
		return (ICompilationUnit[]) notDeletedCus.toArray(new ICompilationUnit[notDeletedCus.size()]);
	}
	
	private static boolean isReadOnly(ICompilationUnit cu) throws JavaModelException{
		if (cu.isReadOnly())
			return true;
		if (cu.getUnderlyingResource() != null && cu.getUnderlyingResource().isReadOnly())	
			return true;
		return false;	
	}


	private static boolean isOkToDeleteReadOnly(ICompilationUnit cu){
		String message= ReorgMessages.getFormattedString("DeleteSourceReferencesAction.cu_read_only", cu.getElementName());//$NON-NLS-1$
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), ReorgMessages.getString("DeleteSourceReferencesAction.delete1"), message); //$NON-NLS-1$
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
	
	private static IMethod[] getGettersSettersForFields(IField[] fields) {
		try {
			String[] namePrefixes= CodeGenerationPreferencePage.getGetterStetterPrefixes();
			String[] nameSuffixes= CodeGenerationPreferencePage.getGetterStetterSuffixes();
			
			List gettersSetters= new ArrayList();
			for (int i= 0; i < fields.length; i++) {
				IMethod getter= GetterSetterUtil.getGetter(fields[i], namePrefixes, nameSuffixes);
				if (getter != null && getter.exists())
					gettersSetters.add(getter);
				IMethod setter= GetterSetterUtil.getSetter(fields[i], namePrefixes, nameSuffixes);
				if (setter != null && setter.exists())
					gettersSetters.add(setter);			
			}
			return  (IMethod[]) gettersSetters.toArray(new IMethod[gettersSetters.size()]);
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, JavaPlugin.getActiveWorkbenchShell(), ReorgMessages.getString("DeleteSourceReferencesAction.delete_elements"), ReorgMessages.getString("DeleteSourceReferencesAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			return new IMethod[0];
		}
	}
	private static boolean confirmDelete(IStructuredSelection selection) {
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label;
		if (selection.size() == 1){
			String pattern= "Are you sure you want to delete ''{0}''?";
			label= MessageFormat.format(pattern, new String[]{ReorgUtils.getName(selection.getFirstElement())});
		} else {
			String pattern= "Are you sure you want to delete these {0} elements?";
			label= MessageFormat.format(pattern, new String[]{String.valueOf(selection.size())});
		}
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), title, label);
	}

	//made protected for ui-less testing
	protected  boolean confirmCusDelete(ICompilationUnit[] cusToDelete) {
		String message;
		if (cusToDelete.length == 1){
			message= ReorgMessages.getFormattedString("DeleteSourceReferencesAction.cu_empty", cusToDelete[0].getElementName());//$NON-NLS-1$
		} else {
			message= ReorgMessages.getFormattedString("DeleteSourceReferencesAction.cus_empty", String.valueOf(cusToDelete.length));//$NON-NLS-1$
		}	
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), ReorgMessages.getString("DeleteSourceReferencesAction.delete1"), message); //$NON-NLS-1$
	}
	
	//made protected for ui-less testing
	protected boolean confirmGetterSetterDelete() {
		String title= ReorgMessages.getString("DeleteSourceReferencesAction.confirm_gs_delete"); //$NON-NLS-1$
		String label= ReorgMessages.getString("DeleteSourceReferencesAction.delete_gs"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
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
