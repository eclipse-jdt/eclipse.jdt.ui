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

import org.eclipse.osgi.util.NLS;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.ClasspathAttributeConfiguration;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class ReleaseAttributeConfiguration extends ClasspathAttributeConfiguration {

	@Override
	public ImageDescriptor getImageDescriptor(ClasspathAttributeAccess attribute) {
		return JavaPluginImages.DESC_OBJS_CLASS;
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
		InputDialog dialog= new InputDialog(shell, NewWizardMessages.ReleaseAttributeConfiguration_dialogTitle, NLS.bind(NewWizardMessages.ReleaseAttributeConfiguration_dialogMessage, path), initialValue, new IInputValidator() {

			@Override
			public String isValid(String newText) {
				if (!newText.isBlank()) {
					try {
						int r= Integer.parseInt(newText);
						if (r < 9) {
							return NewWizardMessages.ReleaseAttributeConfiguration_errorInvalidRelease;
						}
					} catch (RuntimeException e) {
						return NewWizardMessages.ReleaseAttributeConfiguration_errorInvalidNumber;
					}
				}
				return null;
			}
		});
		if (dialog.open() == Window.OK) {
			String value= dialog.getValue();
			if (value.isBlank()) {
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

}
