/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;
import java.util.ArrayList;import java.util.List;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.cus.MoveCompilationUnitRefactoring;import org.eclipse.jdt.internal.core.refactoring.DebugUtils;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jface.viewers.ILabelProvider;
class MoveCompilationUnitWizardPage extends TextInputWizardPage implements IStringButtonAdapter{
	
	/*
	 * uses the knowledge that StringButtonDialogField is a subclass of StringDialogField
	 */

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
	
	/**
	 * Adds a text field.
	 * On each keystroke in this field <code>checkState</code> is called.
	 * @see #checkState
	 */
	protected StringDialogField createStringDialogField(){
		StringButtonDialogField stringInput= new StringButtonDialogField(this);
		stringInput.setButtonLabel(RefactoringResources.getResourceString("Refactoring.MoveCompilationUnit.wizardpage.buttonlabel"));
		stringInput.setLabelText(getLabelText());
		stringInput.setText(getInitialValue());
		stringInput.setDialogFieldListener(new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				checkState();
			};
		});
		setStringInput(stringInput);
		return stringInput;
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
	
	/**
	 * @see IStringButtonAdapter#changeControlPressed(DialogField)
	 */
	public void changeControlPressed(DialogField field) {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), getLabelProvider(), false, false);
		dialog.setTitle(RefactoringResources.getResourceString("Refactoring.MoveCompilationUnit.wizardpage.dialog.title"));
		if (dialog.open(getValidPackages(), getStringInput().getText()) == ElementListSelectionDialog.OK){
			IPackageFragment selectedPackage= (IPackageFragment)dialog.getSelectedElement();
			getStringInput().setText(selectedPackage.getElementName());
		}	
	}
}
