/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.widgets.Composite;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.codemanipulation.IImportsStructure;import org.eclipse.jdt.internal.ui.dialogs.StatusTool;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


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
	protected IStatus findMostSevereStatus() {
		return StatusTool.getMostSevere(new IStatus[] {
			fContainerStatus,
			isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
			fTypeNameStatus,
			fModifierStatus,
			fSuperClassStatus,
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

		// createSeparator(composite, nColumns);
				
		createSuperClassControls(composite, nColumns);
		createSuperInterfacesControls(composite, nColumns);
				
		// createSeparator(composite, nColumns);
		
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
	
	/**
	 * @see TypePage#evalMethods
	 */
	protected String[] evalMethods(IType type, IImportsStructure imports, IProgressMonitor monitor) throws CoreException {
		List newMethods= new ArrayList();
		
		boolean doConstr= fMethodStubsButtons.isSelected(1);
		boolean doInherited= fMethodStubsButtons.isSelected(2);
		String[] meth= constructInheritedMethods(type, doConstr, doInherited, imports, new SubProgressMonitor(monitor, 1));
		for (int i= 0; i < meth.length; i++) {
			newMethods.add(meth[i]);
		}
		if (monitor != null) {
			monitor.done();
		}
		
		if (fMethodStubsButtons.isSelected(0)) {
			String main= "public static void main(String[] args) {}";
			newMethods.add(main);
		}		
		
		return (String[]) newMethods.toArray(new String[newMethods.size()]);
	}
	
}