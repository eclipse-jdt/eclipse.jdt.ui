/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class NewInterfaceCreationWizardPage extends TypePage {
	
	private final static String PAGE_NAME= "NewInterfaceCreationWizardPage";
		
	public NewInterfaceCreationWizardPage(IWorkspaceRoot root) {
		super(false, PAGE_NAME, root);		
	}

	// -------- Initialization ---------

	/**
	 * @see ContainerPage#initFields
	 */		
	protected void initFields(IJavaElement selection) {
		super.initFields(selection);
		
		String selName= null;
		List superinterfaces= new ArrayList(5);
		if (selection instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit)selection;
			String typename= cu.getElementName();
			IPackageFragment pack= getPackageFragment();
			if (pack != null && !"".equals(pack.getElementName())) {
				selName= pack.getElementName() + "." + typename.substring(0, typename.indexOf('.'));
			} else {
				selName= typename.substring(0, typename.indexOf('.'));
			}
			IPackageFragmentRoot root= getPackageFragmentRoot();
			if (root != null) {
				try {
					IType type= JavaModelUtility.findType(root.getJavaProject(), selName);
					if (type != null && type.isInterface()) {
						superinterfaces.add(selName);
					}
				} catch (JavaModelException e) {
					// ignore this exception now
				}
			}
		} else if (selection instanceof IType) {
			IType type= (IType)selection;
			try {
				if (type.isInterface()) {
					superinterfaces.add(JavaModelUtility.getFullyQualifiedName(type));
				}
			} catch (JavaModelException e) {
				// ignore this exception now
			}			
		} else if (selection instanceof IClassFile) {
			try {
				IType type= ((IClassFile)selection).getType();
				if (type.isInterface()) {
					superinterfaces.add(JavaModelUtility.getFullyQualifiedName(type));
				}
			} catch (JavaModelException e) {
				// ignore this exception now
			}			
		}
		
		setTypeName("", true);
		setSuperInterfaces(superinterfaces, true);
		updateStatus(findMostSevereStatus());	
	}		

	/**
	 * Called when default attributes have to be set.
	 * @see ContainerPage#setDefaultAttributes
	 */	
	protected void setDefaultAttributes() {
		setTypeName("", true);
		setSuperInterfaces(new ArrayList(5), true);
		super.setDefaultAttributes();
		updateStatus(findMostSevereStatus());
	}
	
	// ------ validation --------
	
	/**
	 * Finds the most severe error (if there is one)
	 */
	private StatusInfo findMostSevereStatus() {
		StatusInfo res= getContainerStatus();
		res= res.getMoreSevere(getPackageStatus());
		res= res.getMoreSevere(getEnclosingTypeStatus());
		res= res.getMoreSevere(getTypeNameStatus());
		res= res.getMoreSevere(getModifierStatus());
		return res.getMoreSevere(getSuperInterfacesStatus());
	}
	
	/**
	 * @see ContainerPage#fieldUpdated
	 */
	protected void fieldUpdated(String fieldName) {
		super.fieldUpdated(fieldName);
		
		updateStatus(findMostSevereStatus());
	}
	
	
	// ------ ui --------
	
	/**
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		int nColumns= 4;
		
		MGridLayout layout= new MGridLayout();
		layout.marginWidth= 4;
		layout.marginHeight= 4;	
		layout.minimumWidth= 400;
		layout.minimumHeight= 0;
		layout.numColumns= nColumns;		
		composite.setLayout(layout);
		
		createContainerControls(composite, nColumns);	
		createPackageControls(composite, nColumns);	
		createEnclosingTypeControls(composite, nColumns);
				
		createSeparator(composite, nColumns);
		
		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);

		createSeparator(composite, nColumns);
		
		createSuperInterfacesControls(composite, nColumns);
						
		setControl(composite);
		
		setFocus();
	}
	
	
	// ---- creation ----------------

	/**
	 * @see NewElementWizardPage#getRunnable
	 */		
	public IRunnableWithProgress getRunnable() {				
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					createType(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 				
			}
		};
	}
	

}