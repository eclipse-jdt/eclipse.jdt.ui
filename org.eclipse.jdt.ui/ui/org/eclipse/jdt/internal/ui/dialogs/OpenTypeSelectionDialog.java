/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;import org.eclipse.swt.SWT;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.IDialogSettings;import org.eclipse.jface.operation.IRunnableContext;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

public class OpenTypeSelectionDialog extends TypeSelectionDialog {

	private boolean fShowInTypeHierarchy;
	private static final String SECTION_NAME= "OpenTypeSelectionDialog"; //$NON-NLS-1$
	private static final String SHOW_IN_TYPE_HIERARCHY= "showInTypeHierarchy"; //$NON-NLS-1$

	public OpenTypeSelectionDialog(Shell parent, IRunnableContext context, IJavaSearchScope scope, int style, boolean ignoreCase, boolean matchEmtpyString) {
		super(parent, context, scope, style, ignoreCase, matchEmtpyString);
		fShowInTypeHierarchy= getDialogSetting().getBoolean(SHOW_IN_TYPE_HIERARCHY);
	}
	
	/**
	 * @see Windows#configureShell
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, new Object[] { IJavaHelpContextIds.OPEN_TYPE_DIALOG });
	}	

	public boolean showInTypeHierarchy() {
		return fShowInTypeHierarchy;
	}
	
	private IDialogSettings getDialogSetting() {
		IDialogSettings mainStore= JavaPlugin.getDefault().getDialogSettings(); 
		IDialogSettings result= mainStore.getSection(SECTION_NAME);
		if (result == null) {
			result= mainStore.addNewSection(SECTION_NAME);
			result.put(SHOW_IN_TYPE_HIERARCHY, true);
		}
		return result;
	}
	
	public Control createDialogArea(Composite parent) {
		Composite contents= (Composite)super.createDialogArea(parent);
		
		final Button check= new Button(contents, SWT.CHECK);
		check.setText(JavaUIMessages.getString("OpenTypeSelectionDialog.checkboxtext")); //$NON-NLS-1$
		check.setSelection(fShowInTypeHierarchy);
		check.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				fShowInTypeHierarchy= check.getSelection();
			}
		});
		
		return contents;
	}


	public boolean close() {
		if (getReturnCode() != CANCEL)
			getDialogSetting().put(SHOW_IN_TYPE_HIERARCHY, fShowInTypeHierarchy);
		return super.close();
	}
}
