package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.debug.core.IJavaStackFrame;

import org.eclipse.jdt.launching.ProjectSourceLocator;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class JavaUISourceLocator implements ISourceLocator {

	private IJavaProject fJavaProject; 
	private ProjectSourceLocator fProjectSourceLocator;
	private boolean fAllowedToAsk;
	
	public JavaUISourceLocator(IJavaProject project) {
		fJavaProject= project;
		fProjectSourceLocator= new ProjectSourceLocator(project);
		fAllowedToAsk= true;
	}

	/*
	 * @see ISourceLocator#getSourceElement(IStackFrame)
	 */
	public Object getSourceElement(IStackFrame stackFrame) {
		Object res= fProjectSourceLocator.getSourceElement(stackFrame);
		if (res == null && fAllowedToAsk) {
			IJavaStackFrame frame= (IJavaStackFrame)stackFrame.getAdapter(IJavaStackFrame.class);
			if (frame != null) {
				try {
					showDebugSourcePage(frame.getDeclaringTypeName());
					res= fProjectSourceLocator.getSourceElement(stackFrame);
				} catch (DebugException e) {
					JavaPlugin.log(e); 											
				}
			}
		}
		return res;
	}
	
	private void showDebugSourcePage(String typeName) {
		SourceLookupDialog dialog= new SourceLookupDialog(JavaPlugin.getActiveWorkbenchShell(), fJavaProject, typeName);
		dialog.open();
		fAllowedToAsk= !dialog.isNotAskAgain();
	}
	
	private static class SourceLookupDialog extends Dialog {
		
		private SourceLookupBlock fSourceLookupBlock;
		private String fTypeName;
		private boolean fNotAskAgain;
		private Button fAskAgainCheckBox;
		
		public SourceLookupDialog(Shell shell, IJavaProject project, String typeName) {
			super(shell);
			fSourceLookupBlock= new SourceLookupBlock(project);
			fTypeName= typeName;
			fNotAskAgain= false;
			fAskAgainCheckBox= null;
		}
		
		public boolean isNotAskAgain() {
			return fNotAskAgain;
		}
				
				
		protected Control createDialogArea(Composite parent) {
			getShell().setText(LauncherMessages.getString("JavaUISourceLocator.selectprojects.title"));
			
			Composite composite= (Composite) super.createDialogArea(parent);
			composite.setLayout(new GridLayout());
			
			Label message= new Label(composite, SWT.LEFT + SWT.WRAP);
			message.setText(LauncherMessages.getFormattedString("JavaUISourceLocator.selectprojects.message", fTypeName));
			GridData data= new GridData();
			data.widthHint= SWTUtil.convertWidthInCharsToPixels(70, message);
			message.setLayoutData(data);

			Control inner= fSourceLookupBlock.createControl(composite);
			inner.setLayoutData(new GridData(GridData.FILL_BOTH));
			fAskAgainCheckBox= new Button(composite, SWT.CHECK);
			fAskAgainCheckBox.setText(LauncherMessages.getString("JavaUISourceLocator.askagain.message"));
			Label askmessage= new Label(composite, SWT.LEFT + SWT.WRAP);
			askmessage.setText(LauncherMessages.getString("JavaUISourceLocator.askagain.description"));
			data= new GridData();
			data.widthHint= SWTUtil.convertWidthInCharsToPixels(70, message);
			askmessage.setLayoutData(data);

			return composite;
		}
		
		protected void okPressed() {
			try {
				if (fAskAgainCheckBox != null) {
					fNotAskAgain= fAskAgainCheckBox.getSelection();
				}
				fSourceLookupBlock.applyChanges();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			super.okPressed();
		}
	}

}

