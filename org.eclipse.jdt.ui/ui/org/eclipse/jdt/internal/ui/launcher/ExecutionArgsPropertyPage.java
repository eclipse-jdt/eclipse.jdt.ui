/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
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

	public static final String PREFIX= "executionArgs.";
	public static final String VMARGS_LABEL= PREFIX + "vmArgs.label";
	public static final String PROGARGS_LABEL= PREFIX + "programArgs.label";
	public static final String NO_ARGS_LABEL= PREFIX + "no_args.label";
	public static final String ERROR_PREFIX= PREFIX + "error.setArgs.";
	public static final String ERROR_MESSAGE= PREFIX + "error.setArgs.message";

	private Text fVMArgs;
	private Text fProgramArgs;

	protected Control createContents(Composite parent) {

		ResourceBundle bundle= JavaLaunchUtils.getResourceBundle();

		Composite contents= new Composite(parent, SWT.NULL);

		contents.setLayout(new GridLayout());

		ExecutionArguments args= getArguments();

		if (args == null) {
			Label l= new Label(contents, SWT.NULL);
			l.setText(bundle.getString(NO_ARGS_LABEL));
		} else {

			Label l= new Label(contents, SWT.NULL);
			l.setLayoutData(new GridData());
			l.setText(bundle.getString(PROGARGS_LABEL));

			fProgramArgs= new Text(contents, SWT.BORDER);
			fProgramArgs.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fProgramArgs.setText(args.getProgramArguments());

			l= new Label(contents, SWT.NULL);
			l.setLayoutData(new GridData());
			l.setText(bundle.getString(VMARGS_LABEL));

			fVMArgs= new Text(contents, SWT.BORDER);
			fVMArgs.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
			args= new ExecutionArguments("", "");
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
				JavaLaunchUtils.errorDialog(getControl().getShell(), ERROR_PREFIX, e.getStatus());
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
			if (name.endsWith(".java")) {
				name= name.substring(0, name.length() - 5);
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

