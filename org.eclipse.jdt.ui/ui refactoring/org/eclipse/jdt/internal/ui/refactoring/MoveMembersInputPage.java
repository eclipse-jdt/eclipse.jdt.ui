package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveMembersRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class MoveMembersInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "MoveMembersInputPage"; //$NON-NLS-1$
	
	private Text fTextField;
	
	public MoveMembersInputPage() {
		super(PAGE_NAME, true);
	}
	
	public void setVisible(boolean visible){
		if (visible)
			setDescription(getMoveRefactoring().getMovedMembers().length + " member(s) from \'" + getMoveRefactoring().getDeclaringType().getFullyQualifiedName() + "\'.");
		super.setVisible(visible);	
	}
	
	public void createControl(Composite parent) {		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.numColumns= 3;
		gl.makeColumnsEqualWidth= false;
		composite.setLayout(gl);
		
		Label label= new Label(composite, SWT.NONE);
		label.setText("&Select destination type:");
		label.setLayoutData(new GridData());
		
		fTextField= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button button= new Button(composite, SWT.PUSH);
		button.setText("&Browse...");
		button.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				openTypeSelectionDialog();
			}
		});
		
		setControl(composite);
	}
	
	protected boolean performFinish() {
		initializeRefactoring();
		return super.performFinish();
	}
	
	public IWizardPage getNextPage() {
		initializeRefactoring();
		return super.getNextPage();
	}

	private void initializeRefactoring() {
		try {
			getMoveRefactoring().setDestinationTypeFullyQualifiedName(fTextField.getText());
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, "Move Member", "Unexpected exception. See log for details.");
		}
	}
	
	private static IJavaSearchScope createWorkspaceSourceScope(){
		JavaCore javaCore = JavaCore.getJavaCore();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List elems= new ArrayList();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (project.isAccessible()) {
				IJavaProject jp= javaCore.create(project);
				try {
					IPackageFragmentRoot[] roots= jp.getPackageFragmentRoots();
					for (int j= 0; j < roots.length; j++) {
						if (canMoveToTypesDeclaredIn(roots[j]))
							elems.add(roots[j]);	
					}
				} catch(JavaModelException e) {
					//ignore
				}
			}
		}
		IPackageFragmentRoot[] roots= (IPackageFragmentRoot[]) elems.toArray(new IPackageFragmentRoot[elems.size()]);
		return SearchEngine.createJavaSearchScope(roots);
	}
	
	private static boolean canMoveToTypesDeclaredIn(IPackageFragmentRoot root) throws JavaModelException{
		if (root.isArchive())	
			return false;
		if (! root.isConsistent())	
			return false;
		if (root.isExternal())	
			return false;
		if (root.isReadOnly())	
			return false;
		return true;
	}
	
	private void openTypeSelectionDialog(){
		int elementKinds= IJavaSearchConstants.CLASS | IJavaSearchConstants.INTERFACE;
		IJavaSearchScope scope= createWorkspaceSourceScope();
		TypeSelectionDialog dialog= new TypeSelectionDialog(getShell(), getWizard().getContainer(), elementKinds, scope);
		dialog.setTitle("Choose Type");
		dialog.setMessage("&Choose a type (? = any character, * = any string):");
		dialog.setUpperListLabel("&Matching types:");
		dialog.setLowerListLabel("&Qualifier:");
		dialog.setMatchEmptyString(false);
		dialog.setFilter(createInitialFilter());
		if (dialog.open() == Dialog.CANCEL)
			return;
		IType firstResult= (IType)dialog.getFirstResult();		
		fTextField.setText(JavaModelUtil.getFullyQualifiedName(firstResult));	
	}

	private String createInitialFilter() {
		if (! fTextField.getText().trim().equals(""))
			return fTextField.getText();
		else
			return "A";	
	}
	
	private MoveMembersRefactoring getMoveRefactoring(){
		return (MoveMembersRefactoring)getRefactoring();
	}
}
