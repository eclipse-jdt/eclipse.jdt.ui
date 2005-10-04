package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferencePage;

public class SortMembersMessageDialog extends OptionalMessageDialog {
	
	/**
	 * Opens the dialog but only if the user hasn't choosen to hide it.
	 * Returns <code>NOT_SHOWN</code> if the dialog was not shown.
	 */
	public static int open(String id, Shell parent, String title, Image titleImage, String message, int dialogType, String[] buttonLabels, int defaultButtonIndex) {
		if (!OptionalMessageDialog.isDialogEnabled(id))
			return OptionalMessageDialog.NOT_SHOWN;
		
		MessageDialog dialog= new SortMembersMessageDialog(id, parent, title, titleImage, message, dialogType, buttonLabels, defaultButtonIndex);
		return dialog.open();
	}
	
	private String fMessage;

	private SortMembersMessageDialog(String id, Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
		super(id, parentShell, dialogTitle, dialogTitleImage, null, dialogImageType, dialogButtonLabels, defaultIndex);
		fMessage= dialogMessage;
	}
		
	private Control createLinkControl(Composite composite) {
		Link link= new Link(composite, SWT.WRAP | SWT.RIGHT);
		link.setText(fMessage); 
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openCodeTempatePage(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID);
			}
		});
		link.setToolTipText(JavaUIMessages.SortMembersMessageDialog_configure_preferences_tool_tip); 
		GridData gridData= new GridData(GridData.FILL, GridData.CENTER, true, false);
		gridData.widthHint= convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);//convertWidthInCharsToPixels(60);
		link.setLayoutData(gridData);
		link.setFont(composite.getFont());
		
		return link;
	}
	
	protected void openCodeTempatePage(String id) {
		PreferencesUtil.createPreferenceDialogOn(getShell(), MembersOrderPreferencePage.PREF_ID, null, null).open();
	}

	protected Control createMessageArea(Composite composite) {
		Composite messageComposite= new Composite(composite, SWT.NONE);
		messageComposite.setFont(composite.getFont());
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		messageComposite.setLayout(layout);
		messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		super.createMessageArea(messageComposite);
		
		createLinkControl(messageComposite);
		
		return messageComposite;
	}

}
