package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeGenerationPreferencePage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class DeleteSourceReferencesAction extends SourceReferenceAction{
	
	public DeleteSourceReferencesAction(ISelectionProvider provider) {
		super(RefactoringMessages.getString("DeleteSourceReferencesAction.delete"), provider); //$NON-NLS-1$
	}

	protected void perform() throws CoreException {
		if (!confirmDelete())
			return;
			
		Map mapping= SourceReferenceUtil.groupByFile(getElementsToProcess()); //IFile -> List of ISourceReference (elements from that file)
		
		List emptyCuList= Arrays.asList(getCusLeftEmpty(mapping));
		
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			IFile file= (IFile)iter.next();
			if (emptyCuList.contains(JavaCore.create(file))) //do not delete in these files
				continue;
			deleteAll(mapping, file);
		}
		
		ICompilationUnit[] notDeleted= deleteEmptyCus(mapping);
		for (int i= 0; i < notDeleted.length; i++) {
			IFile file= (IFile)notDeleted[i].getUnderlyingResource();
			deleteAll(mapping, file);
		}
	}

	private static void deleteAll(Map mapping, IFile file) throws CoreException {
		List l= (List)mapping.get(file);
		ISourceReference[] refs= (ISourceReference[]) l.toArray(new ISourceReference[l.size()]);

		delete(file, getNonFields(refs));
		delete(getFields(refs));
	}

	private static void delete(IFile file, ISourceReference[] nonFields) throws CoreException{
		TextBuffer tb= TextBuffer.acquire(file);
		try{
			TextBufferEditor tbe= new TextBufferEditor(tb);
			for (int i= 0; i < nonFields.length; i++) {
				Assert.isTrue(! (nonFields[i] instanceof IField));
				tbe.add(createDeleteEdit(nonFields[i]));
			}
			if (! tbe.canPerformEdits())
				return; ///XXX can i assert here?
			tbe.performEdits(new NullProgressMonitor());	
			TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		} finally{
			if (tb != null)
				TextBuffer.release(tb);
		}	
	}
	
	private static void delete(IField[] fields) throws JavaModelException{
		for (int i= 0; i < fields.length; i++) {
			fields[i].delete(false, new NullProgressMonitor());
		}
	}
			
	private static TextEdit createDeleteEdit(ISourceReference ref) throws JavaModelException{
		return new DeleteSourceReferenceEdit(ref, SourceReferenceUtil.getCompilationUnit(ref));
	}
	
	/**
	 * returns cus that have <b>not</b> been deleted
	 */
	private ICompilationUnit[] deleteEmptyCus(Map mapping) throws JavaModelException {
		ICompilationUnit[] cusToDelete= getCusLeftEmpty(mapping);
		if (cusToDelete.length == 0)
			return cusToDelete;
			
		if (! confirmCusDelete(cusToDelete))
			return cusToDelete;
		
		List notDeletedCus= new ArrayList();
		notDeletedCus.addAll(Arrays.asList(cusToDelete));	
		
		for (int i= 0; i < cusToDelete.length; i++) {
			if (isReadOnly(cusToDelete[i]) && (! isOkToDeleteReadOnly(cusToDelete[i])))
				continue;
			cusToDelete[i].delete(false, new NullProgressMonitor());
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
		String message= "Compilation unit \'" + cu.getElementName() + "\' is read-only. Do you still want to delete it?"; 
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), RefactoringMessages.getString("DeleteSourceReferencesAction.delete1"), message); //$NON-NLS-1$
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
	protected ISourceReference[] getElementsToProcess() {
		ISourceReference[] elements= super.getElementsToProcess();
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
			ExceptionHandler.handle(e, JavaPlugin.getActiveWorkbenchShell(), RefactoringMessages.getString("DeleteSourceReferencesAction.delete_elements"), RefactoringMessages.getString("DeleteSourceReferencesAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			return new IMethod[0];
		}
	}

	//made protected for ui-less testing
	protected boolean confirmDelete() {
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= ReorgMessages.getString("deleteAction.confirm.message"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		return MessageDialog.openQuestion(parent, title, label);
	}

	//made protected for ui-less testing
	protected  boolean confirmCusDelete(ICompilationUnit[] cusToDelete) {
		String message;
		if (cusToDelete.length == 1) 
			message= "After the delete operation the compilation unit \'" + cusToDelete[0].getElementName() + "\' contains no types. \nOK to delete it as well?";
		else
			message= "After the delete operation " + cusToDelete.length + " compilation units contain no types. \nOK to delete them as well?";	
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), RefactoringMessages.getString("DeleteSourceReferencesAction.delete1"), message); //$NON-NLS-1$
	}
	
	//made protected for ui-less testing
	protected boolean confirmGetterSetterDelete() {
		String title= RefactoringMessages.getString("DeleteSourceReferencesAction.confirm_gs_delete"); //$NON-NLS-1$
		String label= RefactoringMessages.getString("DeleteSourceReferencesAction.delete_gs"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		return MessageDialog.openQuestion(parent, title, label);
	}
	
}

