/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.ISavePolicy;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Shell;


public class CUSavePolicy implements ISavePolicy {
	
	private static final String DEFAULT_PACKAGE= ""; //$NON-NLS-1$

	private String fNewTypeName;

	protected IType getMainType(ICompilationUnit cu) throws JavaModelException {
		IType[] p= cu.getTypes();
		if (p.length == 1)
			return p[0];
		for (int i= 0; i < p.length; i++) {
			if (Flags.isPublic(p[i].getFlags()))
				return p[i];
		}
		return null;
	}
	
	protected String makeMainTypeName(String cuName) {
		if (cuName.endsWith(".java")) //$NON-NLS-1$
			return cuName.substring(0, cuName.length()-5);
		return cuName;
	}
	
	protected String handleTypeNameChanged(final Shell shell, ICompilationUnit cu, String oldName) throws JavaModelException {
		IType publicType= getMainType(cu);
		if (publicType != null) {
			String typeName= publicType.getElementName();
			if (typeName.equals(oldName))
				return null;
			renameConstructors(publicType, oldName, typeName);
			if (!typeName.equals(makeMainTypeName(cu.getElementName()))) {
				if (shouldRenameCU(shell, typeName)) {
					return typeName+".java"; //$NON-NLS-1$
				}
			}
		}
		return null;
	}
	
	private boolean shouldRenameCU(final Shell shell, String typeName) {
		final String cuName= typeName+".java"; //$NON-NLS-1$
		String message= ReorgMessages.getString("cuSavePolicy.confirmRenameCU"); //$NON-NLS-1$
		String title= ReorgMessages.getString("cuSavePolicy.save.title"); //$NON-NLS-1$
		return confirm(shell, title, message, new Object[] { cuName });
	}
	
	private boolean confirm(final Shell shell,final String title, final String format, final Object[] args) {
		final String message= MessageFormat.format(format, args);
		final boolean[] confirmed= new boolean[] {false};
		Runnable r= new Runnable() {
			public void run() {
				if (MessageDialog.openQuestion(shell, title, message)) {
					confirmed[0]= true;
				}
			}
		};
		
		
		shell.getDisplay().syncExec(r);
		
		return confirmed[0];
	}

	protected void renameConstructors(IType t, String oldName, String newName) throws JavaModelException {
		if (oldName == null)
			return;
		IMethod[] p= t.getMethods();
		for (int i= 0; i < p.length; i++) {
			if (oldName.equals(p[i].getElementName()))
				p[i].rename(newName, true, null);
		}
	}
	
	protected String checkOverwriteCU(final Shell shell, final ICompilationUnit cu, final IPackageFragment newPackage, final String newName) {
		class Runner implements Runnable {
			public String fNewName= null;
			
			public void run() {
					NameClashDialog dialog= new NameClashDialog(shell, new IInputValidator() {
						public String isValid(String newText) {
							return isValidNewName(cu, newPackage, newText);
						}
					}, newName, true);
					if (dialog.open() == dialog.CANCEL)
						return;
					if (!dialog.isReplace())
						fNewName= dialog.getNewName();
					else
						fNewName= newName;
				}
		};
		if (isValidNewName(cu, newPackage, newName) != null) {
			Runner r= new Runner();
			shell.getDisplay().syncExec(r);
			return r.fNewName;
		}
		return newName;
	}
	
	
	private static IPackageFragment getDestination(Object dest) {
		IPackageFragment result= null;
		try {
			result= getDestinationAsPackageFragement(dest);
			if (result != null && !result.isReadOnly())
				return result;
		} catch (JavaModelException e) {
		}
		return null;	
	}
	
	public static String isValidNewName(Object original, Object destination, String name) {
		IPackageFragment pkg= getDestination(destination);
		if (pkg == null)
			return null;
			
		// the order is important here since getCompilationUnit() throws an exception
		// if the name is invalid.
		if (original instanceof ICompilationUnit) {
			if (!name.endsWith(".java")) //$NON-NLS-1$
				return ReorgMessages.getString("cuFileReorgSupport.error.invalidEnding"); //$NON-NLS-1$
			if (!JavaConventions.validateCompilationUnitName(name).isOK())
				return ReorgMessages.getString("cuFileReorgSupport.error.invalidName"); //$NON-NLS-1$
		}
		try {
			if (pkg.getCompilationUnit(name).exists() || getResource(pkg, name) != null)
				return ReorgMessages.getString("cuFileReorgSupport.error.duplicate"); //$NON-NLS-1$
		} catch (JavaModelException e) {
		}
		return null;
	}
	
	
	public void preSave(ICompilationUnit workingCopy) {
		
		String oldTypeName= null;
		try {
			
			ICompilationUnit original= (ICompilationUnit) workingCopy.getOriginalElement();
			if (original != null) {
				IType type= getMainType(original);
				if (type != null)
					oldTypeName= type.getElementName();
			}
					
			if (oldTypeName == null)
				return;
				
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			fNewTypeName= handleTypeNameChanged(shell, workingCopy, oldTypeName);
		} catch (JavaModelException e) {
		}
	}
	
	private IPackageFragment handlePackageChanged(final Shell shell, ICompilationUnit cu) throws JavaModelException {
		IPackageFragment oldPackage= (IPackageFragment)cu.getParent();
		String oldPkgName= oldPackage.getElementName();
		String newPkgName= getPackageDeclaration(cu);
	
		if (oldPkgName.equals(newPkgName))
			return null;
		
		IPackageFragment pack= null;
		IPackageFragment[] packs= getNewPackages(cu, newPkgName);
		
		if (packs.length > 1) {
			pack= pickPackage(shell, packs);
		} else if (packs.length == 1) {
			//if (!packs[0].getElementName().equals(oldPackage.getElementName())) {
				int flags= JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_CONTAINER
					| JavaElementLabelProvider.SHOW_ROOT;
				ILabelProvider renderer= new JavaElementLabelProvider(flags);
				String packageLabel= renderer.getText(packs[0]);
				if (shouldMoveCU(shell, cu, packageLabel))
					pack= packs[0];
			//}
		} else {
			return tryCreatePackage(shell, (IPackageFragmentRoot)oldPackage.getParent(), newPkgName);
		}
		
		return pack;
	}
	
	private IPackageFragment[] getNewPackages(ICompilationUnit cu, String name) throws JavaModelException {
		List v= new ArrayList();
		IPackageFragment[] packages= cu.getJavaProject().getPackageFragments();
		for (int i= 0; i < packages.length; i++) {
			if (name.equals(packages[i].getElementName()) && !packages[i].isReadOnly())
				v.add(packages[i]);
		}
		return (IPackageFragment[])v.toArray(new IPackageFragment[v.size()]);
	}

	IPackageFragment pickPackage(final Shell shell, IPackageFragment[] packages) {
		if (packages.length == 1)
			return packages[0];
		
		int flags= JavaElementLabelProvider.SHOW_BASICS | JavaElementLabelProvider.SHOW_CONTAINER
					| JavaElementLabelProvider.SHOW_ROOT;
		ILabelProvider renderer= new JavaElementLabelProvider(flags);
		
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, renderer);
		dialog.setTitle(ReorgMessages.getString("cuSavePolicy.save.title")); //$NON-NLS-1$
		dialog.setMessage(ReorgMessages.getString("cuSavePolicy.pickPkg.message")); //$NON-NLS-1$		 
		dialog.setElements(packages);
		dialog.open();

		Object[] selection= dialog.getSelectedElements();
		if (selection != null && selection.length == 1) {
			return (IPackageFragment)selection[0];
		}
		return null;
	}
	
	private boolean shouldMoveCU(final Shell shell, ICompilationUnit cu, final String newPackage) {
		String message= ReorgMessages.getString("cuSavePolicy.confirmMoveCU"); //$NON-NLS-1$
		String title= ReorgMessages.getString("cuSavePolicy.save.title"); //$NON-NLS-1$
		return confirm(shell, title, message, new Object[] { newPackage });
	}

	private IPackageFragment tryCreatePackage(final Shell shell, IPackageFragmentRoot pkgRoot, String packageName) throws JavaModelException {
		String title= ReorgMessages.getString("cuSavePolicy.save.title"); //$NON-NLS-1$
		String message= ReorgMessages.getString("cuSavePolicy.confirmCreatePkg"); //$NON-NLS-1$
		if (confirm(shell, title, message, new Object[] { packageName }))
			return pkgRoot.createPackageFragment(packageName, true, null);
		return null;
	}
	
	public ICompilationUnit postSave(ICompilationUnit original) {
	
		IPackageFragment oldPackage= (IPackageFragment) original.getParent();
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
	
		try {
			
			ICompilationUnit newCU= null;
			IPackageFragment newPackage= handlePackageChanged(shell, original);
			
			if (newPackage != null) {
				
				// handle the move, perhaps renaming
				if (fNewTypeName == null)
					fNewTypeName= original.getElementName();
					
				fNewTypeName= checkOverwriteCU(shell, original, newPackage, fNewTypeName);
				if (fNewTypeName == null)
					return null;
				
				original.move(newPackage, null, fNewTypeName, true, null);
				newCU= newPackage.getCompilationUnit(fNewTypeName);
				
			} else if (fNewTypeName != null) {
				
				fNewTypeName= checkOverwriteCU(shell, original, oldPackage, fNewTypeName);
				if (fNewTypeName == null)
					return null;
				
				original.rename(fNewTypeName, true, null);
				newCU= oldPackage.getCompilationUnit(fNewTypeName);
			}
			
			if (newCU != null) {
				
				fNewTypeName= newCU.getElementName();
				IType newType= getMainType(newCU);
				
				if (newType != null) {
					if (!makeMainTypeName(fNewTypeName).equals(newType.getElementName()))
						newType.rename(makeMainTypeName(fNewTypeName), false, null);
				}
			}
			
			return newCU;
			
		} catch (JavaModelException e) {
			showErrorDialog(shell, e);
			return null;
		}
	}

	private String getPackageDeclaration(ICompilationUnit cu) throws JavaModelException {
		IPackageDeclaration[] pkgs= cu.getPackageDeclarations();
		if (pkgs.length == 0)
			return ""; //$NON-NLS-1$
		else 
			return pkgs[0].getElementName();		
	}

	private void showErrorDialog(Shell shell, JavaModelException e) {
		String title= ReorgMessages.getString("cuSavePolicy.save.title"); //$NON-NLS-1$
		String message= ReorgMessages.getString("cuSavePolicy.error.exception"); //$NON-NLS-1$
		ErrorDialog.openError(shell, title, message, e.getStatus());
	}
	
	private static Object getResource(IPackageFragment fragment, String name) throws JavaModelException {
		Object[] children= fragment.getNonJavaResources();
		for (int i= 0; i < children.length; i++) {
			if (children[i] instanceof IResource) {
				IResource child= (IResource)children[i];
				if (child.getName().equals(name))
					return children[i];
			} else if (children[i] instanceof IStorage) {
				IStorage child= (IStorage)children[i];
				if (child.getName().equals(name))
					return children[i];
			}
		}
		return null;
	}
	
	/**
	 * Returns the actual destination for the given <code>dest</code> if the
	 * elements to be dropped are files or compilation units.
	 */
	private static IPackageFragment getDestinationAsPackageFragement(Object dest) throws JavaModelException {
		if (dest instanceof IPackageFragment)
			return (IPackageFragment)dest;
		
		if (dest instanceof IJavaProject) {
			dest= getDestinationAsPackageFragmentRoot((IJavaProject)dest);
		}
			
		if (dest instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)dest;
			return root.getPackageFragment(DEFAULT_PACKAGE);
		}
		
		return null;
	}	
	
	/**
	 * Returns the package fragment root to be used as a destination for the
	 * given project. If the project has more than one package fragment root
	 * that isn't an archive <code>null</code> is returned.
	 */
	public static IPackageFragmentRoot getDestinationAsPackageFragmentRoot(IJavaProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
		IPackageFragmentRoot result= null;
		for (int i= 0; i < roots.length; i++) {
			if (! roots[i].isArchive()) {
				if (result != null)
					return null;
				result= roots[i];
			}
		}
		return result;
	}
	
}