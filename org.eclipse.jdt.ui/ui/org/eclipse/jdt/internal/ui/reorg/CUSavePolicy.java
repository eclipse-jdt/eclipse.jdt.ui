/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.ISavePolicy;
import org.eclipse.jdt.internal.ui.util.JdtHackFinder;


public class CUSavePolicy implements ISavePolicy {
	
	private static final String PREFIX= "SmartSave.";
	private static final String RENAME_CU= "rename_cu.";
	private static final String MOVE_CU= "move_cu.";
	private static final String CREATE_PKG= "create_pkg.";
	private static final String PICK_PKG= "pick_pkg.";
	private static final String EXCEPTION= "exception.";

	private String fOldTypeName;

	/**
	 * 
	 * @return com.oti.leapfrog.javamodel.ILFType
	 * @param cu com.oti.leapfrog.javamodel.LFCompilationUnit
	 */
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
		if (cuName.endsWith(".java"))
			return cuName.substring(0, cuName.length()-5);
		return cuName;
	}
	/**
	 * 
	 * @param cu com.oti.leapfrog.javamodel.LFCompilationUnit
	 * @param info CompilationUnitInfo
	 */
	protected String handleTypeNameChanged(final Shell shell, ICompilationUnit cu, String oldName) throws JavaModelException {
		IType publicType= getMainType(cu);
		if (publicType != null) {
			String typeName= publicType.getElementName();
			if (typeName.equals(oldName))
				return null;
			renameConstructors(publicType, oldName, typeName);
			if (!typeName.equals(makeMainTypeName(cu.getElementName()))) {
				if (shouldRenameCU(shell, typeName)) {
					return typeName+".java";
				}
			}
		}
		return null;
	}
	private boolean shouldRenameCU(final Shell shell, String typeName) {
		final String cuName= typeName+".java";
		String message= JavaPlugin.getResourceString(PREFIX+RENAME_CU+"message");
		String title= JavaPlugin.getResourceString(PREFIX+RENAME_CU+"title");
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

/**
 * 
 * @param t com.oti.leapfrog.javamodel.ILFType
 */
protected void renameConstructors(IType t, String oldName, String newName) throws JavaModelException {
	if (oldName == null)
		return;
	IMethod[] p= t.getMethods();
	for (int i= 0; i < p.length; i++) {
		if (oldName.equals(p[i].getElementName()))
			p[i].rename(newName, true, null);
	}
}
/**
 * 
 * @param cu com.oti.leapfrog.javamodel.LFCompilationUnit
 * @param typeName java.lang.String
 */
protected String checkOverwriteCU(final Shell shell, final ICompilationUnit cu, final IPackageFragment newPackage, final String newName) {
	final INamingPolicy namingPolicy= ReorgSupportFactory.createNamingPolicy(cu);
	class Runner implements Runnable {
		public String fNewName= null;
		
		public void run() {
				NameClashDialog dialog= new NameClashDialog(shell, new IInputValidator() {
					public String isValid(String newText) {
						return namingPolicy.isValidNewName(cu, newPackage, newText);
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
	if (namingPolicy.isValidNewName(cu, newPackage, newName) != null) {
		Runner r= new Runner();
		shell.getDisplay().syncExec(r);
		return r.fNewName;
	}
	return newName;
}
/**
 * collectInfo method comment.
 */
public void preSave(ICompilationUnit element) {
	ICompilationUnit cu= (ICompilationUnit)element;
	fOldTypeName= null;
	try {
		IType t= getMainType(cu);
		if (t != null)
			fOldTypeName= t.getElementName();
	} catch (JavaModelException e) {
	}
}
	/**
	 * 
	 * @param cu com.oti.leapfrog.javamodel.LFCompilationUnit
	 * @param oldPackageName java.lang.String
	 */
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
		String title= JavaPlugin.getResourceString(PREFIX+PICK_PKG+"title");
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, title, null, renderer, true, false);
		dialog.setMessage(JavaPlugin.getResourceString(PREFIX+PICK_PKG+"message"));
		 
		dialog.open(packages);
		Object[] selection= dialog.getSelectedElements();
		if (selection != null && selection.length == 1) {
			return (IPackageFragment)selection[0];
		}
		return null;
	}
	/**
	 * 
	 * @param cu com.oti.leapfrog.javamodel.LFCompilationUnit
	 * @param typeName java.lang.String
	 */
	private boolean shouldMoveCU(final Shell shell, ICompilationUnit cu, final String newPackage) {
		String message= JavaPlugin.getResourceString(PREFIX+MOVE_CU+"message");
		String title= JavaPlugin.getResourceString(PREFIX+MOVE_CU+"title");
		return confirm(shell, title, message, new Object[] { newPackage });
	}

	private IPackageFragment tryCreatePackage(final Shell shell, IPackageFragmentRoot pkgRoot, String packageName) throws JavaModelException {
		String title= JavaPlugin.getResourceString(PREFIX+CREATE_PKG+"title");
		String message= JavaPlugin.getResourceString(PREFIX+CREATE_PKG+"message");
		if (confirm(shell, title, message, new Object[] { packageName }))
			return pkgRoot.createPackageFragment(packageName, true, null);
		return null;
	}
	/**
	 * update method comment.
	 */
	public void postSave(ICompilationUnit cu) {
	
		IPackageFragment oldPackage= (IPackageFragment)cu.getParent();
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
	
		try {
			ICompilationUnit newCU= null;
			String newName= handleTypeNameChanged(shell, cu, fOldTypeName);
			IPackageFragment newPackage= handlePackageChanged(shell, cu);
			if (newPackage != null) {
				// handle the move, perhaps renaming
				if (newName == null) {
					newName= cu.getElementName();
				}
				if ((newName= checkOverwriteCU(shell, cu, newPackage, newName)) == null)
					return;
				cu.move(newPackage, null, newName, true, null);
				newCU= newPackage.getCompilationUnit(newName);
			} else if (newName != null) {
				if ((newName= checkOverwriteCU(shell, cu, oldPackage, newName)) == null)
					return;
				cu.rename(newName, true, null);
				newCU= oldPackage.getCompilationUnit(newName);
			}
			if (newCU != null) {
				newName= newCU.getElementName();
				IType newType= getMainType(newCU);
				if (newType != null) {
					if (!makeMainTypeName(newName).equals(newType.getElementName())) {
						newType.rename(makeMainTypeName(newName), false, null);
					}
				}
			}
		} catch (JavaModelException e) {
			showErrorDialog(shell, e);
		}
	}


	private String getPackageDeclaration(ICompilationUnit cu) throws JavaModelException {
		IPackageDeclaration[] pkgs= cu.getPackageDeclarations();
		if (pkgs.length == 0)
			return "";
		else 
			return pkgs[0].getElementName();		
	}

	private void showErrorDialog(Shell shell, JavaModelException e) {
		String title= JavaPlugin.getResourceString(PREFIX+EXCEPTION+"title");
		String message= JavaPlugin.getResourceString(PREFIX+EXCEPTION+"message");
		ErrorDialog.openError(shell, title, message, e.getStatus());
	}
	
}