package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class MoveInnerToToplnputPage extends UserInputWizardPage{

	public static final String PAGE_NAME= "MoveInnerToToplnputPage"; //$NON-NLS-1$

	public MoveInnerToToplnputPage() {
		super(PAGE_NAME, true);
	}

	public void createControl(Composite parent) {
		Composite newControl= new Composite(parent, SWT.BORDER);
		setControl(newControl);
		WorkbenchHelp.setHelp(newControl, IJavaHelpContextIds.MOVE_INNER_TO_TOP_WIZARD_PAGE);
	}
}
