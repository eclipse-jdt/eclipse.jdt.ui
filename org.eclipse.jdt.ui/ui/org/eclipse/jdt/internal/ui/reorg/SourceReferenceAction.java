package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public abstract class SourceReferenceAction extends SelectionDispatchAction {

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
		BusyIndicator.showWhile(JavaPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
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
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}

}
