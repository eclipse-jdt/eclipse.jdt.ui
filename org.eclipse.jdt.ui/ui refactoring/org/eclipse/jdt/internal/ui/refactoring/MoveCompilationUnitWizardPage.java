/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;
import java.util.ArrayList;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Text;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.util.RowLayouter;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.core.refactoring.cus.MoveCompilationUnitRefactoring;
class MoveCompilationUnitWizardPage extends TextInputWizardPage{
	
	private Button fButton;
	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 */
	public MoveCompilationUnitWizardPage(boolean isLastUserPage) {
		super(isLastUserPage);
	}
	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 * @param initialSetting the initialSetting.
	 */
	public MoveCompilationUnitWizardPage(boolean isLastUserPage, String initialSetting) {
		super(isLastUserPage, initialSetting);
	}
	
	/**
	 * @see TextInputWizardPage#getLabelText
	 */
	protected String getLabelText(){
		return RefactoringResources.getResourceString("Refactoring.MoveCompilationUnit.wizardpage.labelmessage");
	}
		
	/* (non-JavaDoc)
	 * Method defined in IWizardPage
	 */
	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		result.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		
		Label label= new Label(result, SWT.NONE);
		label.setText(getLabelText());
		
		Text text= createTextInputField(result);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fButton= createButton(result);
		layouter.perform(label, text, 1);
	}
	
	/* (non-Javadoc)
	 * Method declared in IDialogPage
	 */
	public void dispose() {
		fButton= null;
	}
	
	private Button createButton(Composite composite){
		Button button= new Button(composite, SWT.PUSH);
		button.setLayoutData(new GridData());
		button.setText(RefactoringResources.getResourceString("Refactoring.MoveCompilationUnit.wizardpage.buttonlabel"));
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				handleButtonSelection(e);
			}	
		});
		return button;
	}
	
	private void handleButtonSelection(SelectionEvent e){
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), getLabelProvider(), false, false);
		dialog.setTitle(RefactoringResources.getResourceString("Refactoring.MoveCompilationUnit.wizardpage.dialog.title"));
		if (dialog.open(getValidPackages(), getText()) == ElementListSelectionDialog.OK){
			IPackageFragment selectedPackage= (IPackageFragment)dialog.getSelectedElement();
			setText(selectedPackage.getElementName());						
		}
	}	
	
	private IPackageFragmentRoot getPackageFragmentRoot(ICompilationUnit cu){
		return (IPackageFragmentRoot)cu.getParent().getParent();
	}
	
	private boolean isOkToConsider(IPackageFragment pack){
		return (!pack.isDefaultPackage()) && (!pack.equals(getCu().getParent()));
	}
	
	private ICompilationUnit getCu(){
		return ((MoveCompilationUnitRefactoring)getRefactoring()).getCompilationUnit();
	}
	
	private List getValidPackages() {
		IPackageFragmentRoot root= getPackageFragmentRoot(getCu());
		IJavaElement[] children= null;
		try {
			children= root.getChildren();
		} catch (JavaModelException e){
			return new ArrayList(0);
		}	
		List l= new ArrayList(children.length);
		for (int i= 0; i < children.length; i++){
			if (children[i] instanceof IPackageFragment){
				if (isOkToConsider((IPackageFragment)children[i]))
					l.add(children[i]);
			}
		}
		return l;
	}
	
	private ILabelProvider getLabelProvider(){
		return new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
	}
}
