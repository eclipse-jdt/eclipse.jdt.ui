/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

/**
 * Add imports to a compilation unit.
 * The input is an array of full qualified type names. No elimination of unnecessary
 * imports is not done (use StubUtility for this). Dublicates are eliminated.
 * If the compilation unit is open in an editor, be sure to pass over its working copy.
 */
public class AddImportsOperation extends WorkspaceModifyOperation {
	
	private ICompilationUnit fCompilationUnit;
	private IJavaElement[] fImports;
	private boolean fDoSave;
	
	private IImportDeclaration[] fAddedImports;
	
	/**
	 * Generate import statements for the passed java elements
	 * Elements must be of type IType (-> single import) or IPackageFragment
	 * (on-demand-import). Other JavaElements are ignored
	 */
	public AddImportsOperation(ICompilationUnit cu, IJavaElement[] imports, boolean save) {
		super();
		fImports= imports;
		fCompilationUnit= cu;
		fAddedImports= null;
		fDoSave= save;
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			
			int nImports= fImports.length;
			monitor.beginTask(CodeManipulationMessages.getString("AddImportsOperation.description"), 2); //$NON-NLS-1$
			
			ImportsStructure impStructure= new ImportsStructure(fCompilationUnit);
			
			for (int i= 0; i < nImports; i++) {
				IJavaElement imp= fImports[i];
				if (imp instanceof IType) {
					IType type= (IType)imp;
					String packageName= type.getPackageFragment().getElementName();
					impStructure.addImport(packageName, type.getElementName());
				} else if (imp instanceof IPackageFragment) {
					String packageName= ((IPackageFragment)imp).getElementName();
					impStructure.addImport(packageName, "*"); //$NON-NLS-1$
				}
			}
			monitor.worked(1);
			fAddedImports= impStructure.create(fDoSave, null);
			monitor.worked(1);
		} finally {
			monitor.done();
		}
	}
	
	public IImportDeclaration[] getAddedImports() {
		return fAddedImports;
	}
		
}
