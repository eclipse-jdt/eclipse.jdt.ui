/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.launching.ExecutionArguments;

public class ExecutionArgsPropertyPage extends PropertyPage {

	private Text fVMArgs;
	private Text fProgramArgs;

	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();

		Composite contents= new Composite(parent, SWT.NULL);

		contents.setLayout(new GridLayout());

		ExecutionArguments args= getArguments();

		if (args == null) {
			Label l= new Label(contents, SWT.NULL);
			l.setText(LauncherMessages.getString("executionArgsPropertyPage.notAvailable")); //$NON-NLS-1$
		} else {

			GridData gd;
			Label l= new Label(contents, SWT.NULL);
			l.setLayoutData(new GridData());
			l.setText(LauncherMessages.getString("executionArgsPropertyPage.programArgs")); //$NON-NLS-1$

			fProgramArgs= new Text(contents, SWT.BORDER);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint= convertWidthInCharsToPixels(60);
			fProgramArgs.setLayoutData(gd);
			fProgramArgs.setText(args.getProgramArguments());

			l= new Label(contents, SWT.NULL);
			l.setLayoutData(new GridData());
			l.setText(LauncherMessages.getString("executionArgsPropertyPage.vmArgs")); //$NON-NLS-1$

			fVMArgs= new Text(contents, SWT.BORDER);
			gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint= convertWidthInCharsToPixels(60);
			fVMArgs.setLayoutData(gd);
			fVMArgs.setText(args.getVMArguments());

		}

		return contents;
	}

	private ExecutionArguments getArguments() {
		IType type= getType();
		if (type == null)
			return null;
		ExecutionArguments args= null;
		if (!JavaModelUtility.hasMainMethod(type))
			return null;
		try {
			args= ExecutionArguments.getArguments(type);
		} catch (CoreException e) {
		}
		if (args == null) {
			args= new ExecutionArguments("", ""); //$NON-NLS-2$ //$NON-NLS-1$
		}
		return args;
	}

	public boolean performOk() {
		setArgs();
		return true;
	}

	private void setArgs() {
		if (fVMArgs != null) {
			IType type= getType();
			try {
				ExecutionArguments.setArguments(type, new ExecutionArguments(fVMArgs.getText(), fProgramArgs.getText()));
			} catch (CoreException e) {
				JavaLaunchUtils.errorDialog(getControl().getShell(), LauncherMessages.getString("executionArgsPropertyPage.error.title"), LauncherMessages.getString("executionArgsPropertyPage.error.setArgs"), e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
			}
		}
	}

	private IType getType() {
		Object element= getElement();
		if (element instanceof IResource) {
			element= JavaCore.create((IResource) element);
		}
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit) element;
			String name= cu.getElementName();
			if (name.endsWith(".java")) { //$NON-NLS-1$
				//fix for 1GF5ZBA: ITPJUI:WINNT - assertion failed after rightclick on a compilation unit with strange name
				name= name.substring(0, name.indexOf(".")); //$NON-NLS-1$
				IType t= cu.getType(name);
				if (t.exists())
					element= t;
				else {
					try {
						IType[] types= cu.getTypes();
						for (int i= 0; i < types.length; i++) {
							if (Flags.isPublic(types[i].getFlags()))
								return types[i];
						}
					} catch (JavaModelException e) {
						// ignore, can't set args.
					}
				}
			}
		}
		if (element instanceof IClassFile) {
			try {
				element= ((IClassFile) element).getType();
			} catch (JavaModelException e) {
			}
		}
		if (element instanceof IType) {
			return (IType) element;
		}
		return null;
	}

}

