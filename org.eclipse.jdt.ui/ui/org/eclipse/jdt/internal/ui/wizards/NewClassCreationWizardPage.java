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
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class NewClassCreationWizardPage extends TypePage {
	
	private final static String PAGE_NAME= "NewClassCreationWizardPage";
	
	protected final static String METHODS= PAGE_NAME + ".methods";
	
	private SelectionButtonDialogFieldGroup fMethodStubsButtons;
	
	public NewClassCreationWizardPage(IWorkspaceRoot root) {
		super(true, PAGE_NAME, root);
		
		String[] buttonNames3= new String[] {
			getResourceString(METHODS + ".main"), getResourceString(METHODS + ".constructors"),
			getResourceString(METHODS + ".inherited")
		};		
		fMethodStubsButtons= new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames3, 1);
		fMethodStubsButtons.setLabelText(getResourceString(METHODS + ".label"));		
	}

	// -------- Initialization ---------

	/**
	 * @see ContainerPage#initFields
	 */		
	protected void initFields(IJavaElement selection) {
		super.initFields(selection);
		String superclass= "java.lang.Object";
		List superinterfaces= new ArrayList(5);
		if (selection instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit)selection;
			String typename= cu.getElementName();
			IPackageFragment pack= getPackageFragment();
			if (pack != null && !"".equals(pack.getElementName())) {
				superclass= pack.getElementName() + "." + typename.substring(0, typename.indexOf('.'));
			} else {
				superclass= typename.substring(0, typename.indexOf('.'));
			}
			IPackageFragmentRoot root= getPackageFragmentRoot();
			if (root != null) {
				try {
					IType type= JavaModelUtility.findType(root.getJavaProject(), superclass);
					if (type != null && type.isInterface()) {
						superinterfaces.add(superclass);
						superclass= "java.lang.Object";
					}
				} catch (JavaModelException e) {
					// ignore this exception now
				}
			}
		} else if (selection instanceof IType || selection instanceof IClassFile) {
			try {
				IType type;
				if (selection instanceof IType) {
					type= (IType)selection;
				} else {
					type= ((IClassFile)selection).getType();
				}
				if (type.isInterface()) {
					superinterfaces.add(JavaModelUtility.getFullyQualifiedName(type));
				} else {
					superclass= JavaModelUtility.getFullyQualifiedName(type);
				}
			} catch (JavaModelException e) {
				// ignore this exception now
			}			
		}
		
		setTypeName("", true);
		setSuperClass(superclass, true);
		setSuperInterfaces(superinterfaces, true);
		
		updateStatus(findMostSevereStatus());
	}		

	/**
	 * @see ContainerPage#setDefaultAttributes
	 */	
	protected void setDefaultAttributes() {
		setTypeName("", true);
		setSuperClass("java.lang.Object", true);
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
		res= res.getMoreSevere(getSuperClassStatus());
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
				
		createSuperClassControls(composite, nColumns);
		createSuperInterfacesControls(composite, nColumns);
				
		createSeparator(composite, nColumns);
		
		createMethodStubSelectionControls(composite, nColumns);
		
		setControl(composite);
		
		setFocus();
	}
	
	protected void createMethodStubSelectionControls(Composite composite, int nColumns) {
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getLabelControl(composite), nColumns);
		DialogField.createEmptySpace(composite);
		LayoutUtil.setHorizontalSpan(fMethodStubsButtons.getSelectionButtonsGroup(composite), nColumns - 1);	
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
	
	/**
	 * @see TypePage#evalMethods
	 */
	protected String[] evalMethods(IType type, int indent, ImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		List newMethods= new ArrayList();
		
		if (fMethodStubsButtons.isSelected(0)) {
			String main= "\tpublic static void main(String[] args) {\n\t}\n";
			newMethods.add(main);
		}
		if (fMethodStubsButtons.isSelected(1) || fMethodStubsButtons.isSelected(2)) {
			ITypeHierarchy hierarchy= type.newSupertypeHierarchy(monitor);
			if (fMethodStubsButtons.isSelected(1)) {
				IType superclass= hierarchy.getSuperclass(type);
				if (superclass != null) {
					StubUtility.evalConstructors(type, superclass, newMethods, imports);
				}
			}
			if (fMethodStubsButtons.isSelected(2)) {
				StubUtility.evalUnimplementedMethods(type, hierarchy, newMethods, imports);
			}
		}
		monitor.done();
		String[] res= new String[newMethods.size()];
		newMethods.toArray(res);
		return res;
	}	

}