package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;

public class CopyCompilationUnitChange extends CompilationUnitReorgChange {
	
	public CopyCompilationUnitChange(ICompilationUnit cu, IPackageFragment dest, INewNameQuery newNameQuery){
		super(cu, dest, newNameQuery);
	}
		
	/* non java-doc
	 * @see CompilationUnitReorgChange#doPeform(IProgressMonitor)
	 */
	void doPeform(IProgressMonitor pm) throws JavaModelException{
		getCu().copy(getDestinationPackage(), null, getNewName(), true, pm);
	}

	/* non java-doc
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return null;
	}

	/* non java-doc
	 * @see IChange#isUndoable()
	 */	
	public boolean isUndoable(){
		return false;
	}

	/* non java-doc
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getFormattedString("CopyCompilationUnitChange.copy", //$NON-NLS-1$
			new String[]{getCu().getElementName(), getPackageName(getDestinationPackage())});
	}

}

