package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class DeleteSourceReferencesAction extends SourceReferenceAction{
	
	public DeleteSourceReferencesAction(ISelectionProvider provider) {
		super("&Delete", provider);
	}

	//void perform(ISourceReference[] elements) throws CoreException {
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
		delete(file, refs);
	}

	private static void delete(IFile file, ISourceReference[] elems) throws CoreException{
		TextBuffer tb= TextBuffer.acquire(file);
		try{
			TextBufferEditor tbe= new TextBufferEditor(tb);
			for (int i= 0; i < elems.length; i++) {
				tbe.add(createDeleteEdit(elems[i]));
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
			
		if (! isOkToDeleteCus(cusToDelete))
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

	//made protected for ui-less testing
	protected boolean isOkToDeleteCus(ICompilationUnit[] cusToDelete) {
		String message;
		if (cusToDelete.length == 1) 
			message= "After the delete operation the compilation unit \'" + cusToDelete[0].getElementName() + "\' contains no types. \nOK to delete it as well?";
		else
			message= "After the delete operation " + cusToDelete.length + " compilation units contain no types. \nOK to delete them as well?";	
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), "Delete", message);
	}

	private static boolean isOkToDeleteReadOnly(ICompilationUnit cu){
		String message= "Compilation unit \'" + cu.getElementName() + "\' is read-only. Do you still want to delete it?"; 
		return MessageDialog.openQuestion(JavaPlugin.getActiveWorkbenchShell(), "Delete", message);
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
	
	private boolean confirmDelete() {
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= ReorgMessages.getString("deleteAction.confirm.message"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		return MessageDialog.openQuestion(parent, title, label);
	}
	
}

