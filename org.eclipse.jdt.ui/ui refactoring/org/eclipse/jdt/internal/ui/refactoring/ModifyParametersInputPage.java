package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class ModifyParametersInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ModifyParametersInputPage"; //$NON-NLS-1$
	private Label fSignaturePreview;
	private List fParameterInfoList;
	
	public ModifyParametersInputPage() {
		super(PAGE_NAME, true);
		setMessage(RefactoringMessages.getString("ModifyParametersInputPage.new_order")); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout((new GridLayout()));
		
		createParameterTableComposite(composite);
		
		Label label= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
		
		fSignaturePreview= new Label(composite, SWT.WRAP);
		GridData gl= new GridData(GridData.FILL_BOTH);
		gl.widthHint= convertWidthInCharsToPixels(50);
		fSignaturePreview.setLayoutData(gl);
		updateSignaturePreview();
		
		setControl(composite);
		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.MODIFY_PARAMETERS_WIZARD_PAGE);
	}

	private void createParameterTableComposite(Composite composite) {
		String labelText= RefactoringMessages.getString("ModifyParametersInputPage.parameters");
		ChangeParametersControl cp= new ChangeParametersControl(composite, SWT.NONE, labelText, new ParameterChangeListener() {
			public void parameterChanged(ParameterInfo parameter) {
				getModifyParametersRefactoring().setNewNames(getParameterInfoRenamings());
				updateSignaturePreview();
			}
			public void parameterReordered() {
				getModifyParametersRefactoring().setNewParameterOrder(getNewParameterOrder());
				updateSignaturePreview();
			}
		}, true);
		cp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
		fParameterInfoList= createParameterInfoList();
		cp.setInput(fParameterInfoList);
	}

	private Map getParameterInfoRenamings() {
		Map map= new HashMap(fParameterInfoList.size());
		for (Iterator iter = fParameterInfoList.iterator(); iter.hasNext();) {
			ParameterInfo info = (ParameterInfo) iter.next();
			map.put(info.getOldName(), info.getNewName());
		}
		return map;
	}

	private String[] getNewParameterOrder() {
		String[] newOrder= new String[fParameterInfoList.size()];
		int i= 0;
		for (Iterator iter = fParameterInfoList.iterator(); iter.hasNext(); i++){
			ParameterInfo info = (ParameterInfo) iter.next();
			newOrder[i]= info.getOldName();
		}
		return newOrder;
	}

	private List createParameterInfoList() {
		try {
			String[] typeNames= getModifyParametersRefactoring().getMethod().getParameterTypes();
			String[] oldNames= getModifyParametersRefactoring().getMethod().getParameterNames();
			List result= new ArrayList(typeNames.length);
			for (int i= 0; i < oldNames.length; i++){
				result.add(new ParameterInfo(Signature.toString(typeNames[i]), oldNames[i], i));
			}
			return result;
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("ModifyParamatersInputPage.modify_Parameters"), RefactoringMessages.getString("ModifyParametersInputPage.exception")); //$NON-NLS-2$ //$NON-NLS-1$
			return new ArrayList(0);
		}		
	}

	private void setNewParameterOrder(String[] newNames){
		getModifyParametersRefactoring().setNewParameterOrder(newNames);
	}
	
	private ModifyParametersRefactoring getModifyParametersRefactoring(){
		return	(ModifyParametersRefactoring)getRefactoring();
	}

	private void updateSignaturePreview() {
		try{
			fSignaturePreview.setText(RefactoringMessages.getString("ModifyParametersInputPage.method_Signature_Preview") + getModifyParametersRefactoring().getMethodSignaturePreview()); //$NON-NLS-1$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("ModifyParamatersInputPage.modify_Parameters"), RefactoringMessages.getString("ModifyParametersInputPage.exception")); //$NON-NLS-2$ //$NON-NLS-1$
		}	
	}	
}	