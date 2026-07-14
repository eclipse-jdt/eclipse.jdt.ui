/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.manipulation.CodeGeneration;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

/**
 * Wizard page to  create a new compact source file.
 * <p>
 * Note: This class is not intended to be subclassed, but clients can instantiate.
 * To implement a different kind of a new compact wizard page, extend <code>NewTypeWizardPage</code>.
 * </p>
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.39
 */
public class NewCompactWizardPage extends NewTypeWizardPage {

	private final static String PAGE_NAME= "NewCompactWizardPage"; //$NON-NLS-1$

	  private final static int TYPE = NewTypeWizardPage.COMPACT_TYPE;
	/**
	 * Creates a new <code>NewCompactWizardPage</code>
	 */
	public NewCompactWizardPage() {
		super(TYPE, PAGE_NAME);
		setTitle(NewWizardMessages.NewCompactCreationWizard_title);
		setDescription(NewWizardMessages.NewCompactWizardPage_description);
	}

	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);
		initContainerPage(jelem);
		initTypePage(jelem);
		doStatusUpdate();
	}

	@Override
	protected void initTypePage(IJavaElement elem) {
		IJavaProject project= null;
		IPackageFragment pack= null;

		if (elem != null) {
			project= elem.getJavaProject();
		}

		String typeName= ""; //$NON-NLS-1$

		ITextSelection selection= getCurrentTextSelection();
		if (selection != null) {
			String text= selection.getText();
			if (text != null ) {
				typeName= getUniqueJavaTypeName (pack, text);
			}
		}

		setTypeName(typeName, true);

		setAddComments(StubUtility.doAddComments(project), true);
	}

	private void doStatusUpdate() {
		// status of all used components
		IStatus[] status= new IStatus[] {
			fTypeNameStatus,
			fModifierStatus,
			fContainerStatus
		};
		updateStatus(status);
	}

	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		doStatusUpdate();
	}


	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= createScrollableContainer(parent);
		composite.setFont(parent.getFont());

		int nColumns= 4;

		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
		composite.setLayout(layout);


		createContainerControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);


		createSeparator(composite, nColumns);

		createCommentControls(composite, nColumns);
		enableCommentControl(true);
		ScrolledComposite sc= (ScrolledComposite) composite.getParent();
		setControl(sc);

		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_COMPACT_WIZARD_PAGE);
		sc.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	/*
	 * @see WizardPage#becomesVisible
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
			doStatusUpdate();
		}
	}

	@Override
	protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		createMainMethod(type, imports);
	}

	@Override
	protected IMethod createMainMethod(IType type, ImportsManager imports) throws CoreException{
		if (type != null ) {
			StringBuilder buf= new StringBuilder();
			int modifiers= getModifiers();
			buf.append(Flags.toString(modifiers));
			if (modifiers != 0) {
				buf.append(' ');
			}
			final String lineDelim= "\n"; //$NON-NLS-1$
			if (isAddComments()) {
				String comment= CodeGeneration.getMethodComment(type.getCompilationUnit(), type.getTypeQualifiedName('.'), "main", new String[] { }, new String[0], Signature.createTypeSignature("void", true), null, lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
				if (comment != null) {
					buf.append(comment);
					buf.append(lineDelim);
				}
			}
			buf.append("void main() {"); //$NON-NLS-1$
			buf.append(lineDelim);
			final String content= CodeGeneration.getMethodBodyContent(type.getCompilationUnit(), type.getTypeQualifiedName('.'), "main", false, "", lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
			if (content != null && content.length() != 0)
				buf.append(content);
			buf.append(lineDelim);
			buf.append("}"); //$NON-NLS-1$
			ICompilationUnit cu= type.getCompilationUnit();
			cu.getBuffer().setContents(buf.toString());
			return type.getMethod("main", new String[0]); //$NON-NLS-1$
		}
		return null;
	}
}
