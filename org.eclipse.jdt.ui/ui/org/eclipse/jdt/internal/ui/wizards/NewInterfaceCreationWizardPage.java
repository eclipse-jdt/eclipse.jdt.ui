/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class NewInterfaceCreationWizardPage extends TypePage {
	
	private final static String PAGE_NAME= "NewInterfaceCreationWizardPage";
		
	public NewInterfaceCreationWizardPage(IWorkspaceRoot root) {
		super(false, PAGE_NAME, root);		
	}

	// -------- Initialization ---------

	/**
	 * Should be called from the wizard with the input element.
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= null;
		
		if (selection != null && !selection.isEmpty()) {
			Object selectedElement= selection.getFirstElement();
			if (selectedElement instanceof IAdaptable) {
				IAdaptable adaptable= (IAdaptable) selectedElement;			
				
				jelem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
				if (jelem == null) {
					IResource resource= (IResource) adaptable.getAdapter(IResource.class);
					if (resource != null) {
						IProject proj= resource.getProject();
						if (proj != null) {
							jelem= JavaCore.create(proj);
						}
					}
				}
			}
		}
		if (jelem == null) {
			jelem= EditorUtility.getActiveEditorJavaInput();
		}		
		initContainerPage(jelem);
		initTypePage(jelem);
		updateStatus(findMostSevereStatus());
	}		
	
	// ------ validation --------
	
	/**
	 * Finds the most severe error (if there is one)
	 */
	private IStatus findMostSevereStatus() {
		return StatusTool.getMostSevere(new IStatus[] {
			fContainerStatus,
			isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
			fTypeNameStatus,
			fModifierStatus,
			fSuperInterfacesStatus
		});		
	}
		
	/**
	 * @see ContainerPage#handleFieldChanged
	 */
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		
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
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					createType(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} 				
			}
		};
	}
	

}