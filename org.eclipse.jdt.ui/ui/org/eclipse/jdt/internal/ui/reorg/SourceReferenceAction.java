package org.eclipse.jdt.internal.ui.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;

class SourceReferenceAction extends RefactoringAction {

	protected SourceReferenceAction(String name, StructuredSelectionProvider provider) {
		super(name, provider);
	}
	
	protected SourceReferenceAction(String name, ISelectionProvider provider) {
		super(name, provider);
	}

	/*
	 * @see RefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
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
			JavaPlugin.log(e);
			return false;
		}	
	}
	
	private static boolean canWorkOn(Object elem) throws JavaModelException{
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

		if ((elem instanceof IMember) && ((IMember)elem).isBinary())
			return false;
			
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
		ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(elem);
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		if (wc.equals(cu))
			return false;
		IJavaElement element= (IJavaElement)elem;
		IJavaElement wcElement= JavaModelUtil.findInCompilationUnit(wc, element);
		return wcElement == null || ! wcElement.exists();
	}
			
	protected ISourceReference[] getSelectedElements(){
		return getWorkingCopyElements(getStructuredSelection().toList());
	}
	
	private static ISourceReference[] getWorkingCopyElements(List l) {
		List wcList= new ArrayList(l.size());
		for (Iterator iter= l.iterator(); iter.hasNext();) {
			ISourceReference element= (ISourceReference) iter.next();
			if (! (element instanceof IJavaElement)) //can this happen ?
				wcList.add(element); 
			ICompilationUnit cu= SourceReferenceUtil.getCompilationUnit(element);
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
		return (ISourceReference[]) wcList.toArray(new ISourceReference[wcList.size()]);
	}	
}

