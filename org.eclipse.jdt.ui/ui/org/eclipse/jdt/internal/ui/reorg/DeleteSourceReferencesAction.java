package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.DebugUtils;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class DeleteSourceReferencesAction extends SourceReferenceAction{
	
	public DeleteSourceReferencesAction(ISelectionProvider provider) {
		super("&Delete", provider);
	}
	
	/*
	 * @see Action#run
	 */
	public void run() {
		new BusyIndicator().showWhile(JavaPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					perform(getSelectedElements());
				} catch (CoreException e) {
					ExceptionHandler.handle(e, "Delete", "Unexpected exception. See log for details.");
				}
			}
		});
	}

	static void perform(ISourceReference[] elements) throws CoreException {
		ISourceReference[] childrenRemoved= SourceReferenceUtil.removeAllWithParentsSelected(elements);
		Map mapping= SourceReferenceUtil.groupByFile(childrenRemoved); //IFile -> List of ISourceReference (elements from that file)
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			IFile file= (IFile)iter.next();
			List l= (List)mapping.get(file);
			ISourceReference[] refs= (ISourceReference[]) l.toArray(new ISourceReference[l.size()]);
			delete(file, refs);
		}
		deleteEmptyCus(mapping);
	}

	private static void delete(IFile file, ISourceReference[] elems) throws CoreException{
		TextBuffer tb= TextBuffer.acquire(file);
		TextBufferEditor tbe= new TextBufferEditor(tb);
		for (int i= 0; i < elems.length; i++) {
			tbe.add(createDeleteEdit(elems[i]));
		}
		if (! tbe.canPerformEdits())
			return; ///XXX can i assert here?
		tbe.performEdits(new NullProgressMonitor());	
		TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		TextBuffer.release(tb);
	}
			
	private static TextEdit createDeleteEdit(ISourceReference ref) throws JavaModelException{
		return new DeleteSourceReferenceEdit(ref, SourceReferenceUtil.getCompilationUnit(ref));
	}
	
	private static void deleteEmptyCus(Map mapping) throws JavaModelException {
		ICompilationUnit[] cusToDelete= getCusLeftEmpty(mapping);
		if (cusToDelete.length == 0)
			return;
			
		if (! isOkToDeleteCus(cusToDelete))
			return;
			
		for (int i= 0; i < cusToDelete.length; i++) {
			if (isReadOnly(cusToDelete[i]) && (! isOkToDeleteReadOnly(cusToDelete[i])))
				continue;
			cusToDelete[i].delete(false, new NullProgressMonitor());
		}	
	}
	
	private static boolean isReadOnly(ICompilationUnit cu) throws JavaModelException{
		if (cu.isReadOnly())
			return true;
		if (cu.getUnderlyingResource() != null && cu.getUnderlyingResource().isReadOnly())	
			return true;
		return false;	
	}

	private static boolean isOkToDeleteCus(ICompilationUnit[] cusToDelete) {
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
}

