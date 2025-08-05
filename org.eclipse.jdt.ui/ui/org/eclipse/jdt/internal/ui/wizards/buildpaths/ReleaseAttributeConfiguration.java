/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.ClasspathAttributeConfiguration;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class ReleaseAttributeConfiguration extends ClasspathAttributeConfiguration {

	@Override
	public ImageDescriptor getImageDescriptor(ClasspathAttributeAccess attribute) {
		return JavaPluginImages.DESC_OBJ_RELEASE;
	}

	@Override
	public String getNameLabel(ClasspathAttributeAccess attribute) {
		return NewWizardMessages.ReleaseAttributeConfiguration_nameLabel;
	}

	@Override
	public String getValueLabel(ClasspathAttributeAccess access) {
		String value= access.getClasspathAttribute().getValue();
		if (value == null || value.isBlank()) {
			return NewWizardMessages.ReleaseAttributeConfiguration_defaultReleaseName;
		}
		return value;
	}

	@Override
	public boolean canEdit(ClasspathAttributeAccess attribute) {
		return true;
	}

	@Override
	public boolean canRemove(ClasspathAttributeAccess attribute) {
		return attribute.getClasspathAttribute().getValue() != null;
	}

	@Override
	public IClasspathAttribute performEdit(Shell shell, ClasspathAttributeAccess attribute) {
		String initialValue= attribute.getClasspathAttribute().getValue();
		IPath path= attribute.getParentClasspassEntry().getPath();

		ReleaseSelectionDialog dialog= new ReleaseSelectionDialog(shell, initialValue, path);
		if (dialog.open() == Window.OK) {
			String value= dialog.getValue();
			if (value == null) {
				return performRemove(attribute);
			}
			return JavaCore.newClasspathAttribute(CPListElement.RELEASE, value);
		}
		return JavaCore.newClasspathAttribute(CPListElement.RELEASE, initialValue);
	}

	@Override
	public IClasspathAttribute performRemove(ClasspathAttributeAccess attribute) {
		return JavaCore.newClasspathAttribute(CPListElement.RELEASE, null);
	}

	private static final class ReleaseSelectionDialog extends Dialog {

		private IPath fPath;

		private String fValue;

		private List<String> fVersionsList;

		private ReleaseSelectionDialog(Shell parentShell, String initialValue, IPath path) {
			super(parentShell);
			fValue= initialValue;
			fPath= path;
			fVersionsList= Stream.concat(Stream.of(NewWizardMessages.ReleaseAttributeConfiguration_defaultReleaseName),
					JavaCore.getAllJavaSourceVersionsSupportedByCompiler().stream().filter(s -> !s.startsWith("1."))).toList(); //$NON-NLS-1$
		}

		public String getValue() {
			if (isValidRelease()) {
				return fValue;
			}
			return null;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(NewWizardMessages.ReleaseAttributeConfiguration_dialogTitle);
			newShell.setSize(600, 350);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			GridLayout layout= (GridLayout) composite.getLayout();
			layout.numColumns= 2;
			Label nameLabel= new Label(composite, SWT.NONE);
			nameLabel.setText(NewWizardMessages.ReleaseAttributeConfiguration_nameLabel);
			ComboViewer viewer= new ComboViewer(composite);
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.setInput(fVersionsList);
			if (isValidRelease()) {
				viewer.setSelection(new StructuredSelection(fValue));
			} else {
				viewer.setSelection(new StructuredSelection(NewWizardMessages.ReleaseAttributeConfiguration_defaultReleaseName));
			}
			viewer.addSelectionChangedListener(event -> {
				fValue= (String) viewer.getStructuredSelection().getFirstElement();
			});
			Label pathLabel= new Label(composite, SWT.NONE);
			pathLabel.setText(NewWizardMessages.ReleaseAttributeConfiguration_path);
			Label pathValueLabel= new Label(composite, SWT.NONE);
			pathValueLabel.setText(String.valueOf(fPath));
			new Label(composite, SWT.NONE);
			Link infoLabel= new Link(composite, SWT.NONE);
			GridData gdInfo= new GridData();
			gdInfo.grabExcessHorizontalSpace= true;
			gdInfo.horizontalAlignment= SWT.FILL;
			gdInfo.grabExcessVerticalSpace= true;
			gdInfo.verticalAlignment= SWT.FILL;
			infoLabel.setLayoutData(gdInfo);
			infoLabel.setText(NewWizardMessages.ReleaseAttributeConfiguration_warning);
			applyDialogFont(composite);
			infoLabel.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Program.launch("https://github.com/eclipse-jdt/eclipse.jdt.core/issues"); //$NON-NLS-1$
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			return composite;
		}

		private boolean isValidRelease() {
			return fVersionsList.contains(fValue) && !NewWizardMessages.ReleaseAttributeConfiguration_defaultReleaseName.equals(fValue);
		}
	}

}
