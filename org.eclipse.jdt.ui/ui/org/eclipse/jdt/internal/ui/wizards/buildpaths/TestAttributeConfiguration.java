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

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.ClasspathAttributeConfiguration;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class TestAttributeConfiguration extends ClasspathAttributeConfiguration {

	public static final String TRUE= "true"; //$NON-NLS-1$

	@Override
	public ImageDescriptor getImageDescriptor(ClasspathAttributeAccess attribute) {
		return JavaPluginImages.DESC_OBJS_TEST_ATTRIB;
	}

	@Override
	public String getNameLabel(ClasspathAttributeAccess attribute) {
		return attribute.getParentClasspassEntry().getEntryKind() == IClasspathEntry.CPE_SOURCE
				? NewWizardMessages.CPListLabelProvider_test_sources_label
				: NewWizardMessages.CPListLabelProvider_test_dependency_label;
	}

	@Override
	public String getValueLabel(ClasspathAttributeAccess access) {
		String value= access.getClasspathAttribute().getValue();
		return TRUE.equals(value) ? NewWizardMessages.CPListLabelProvider_test_yes : NewWizardMessages.CPListLabelProvider_test_no;
	}

	@Override
	public boolean canEdit(ClasspathAttributeAccess attribute) {
		return true;
	}

	@Override
	public boolean canRemove(ClasspathAttributeAccess attribute) {
		return false;
	}

	@Override
	public IClasspathAttribute performEdit(Shell shell, ClasspathAttributeAccess attribute) {
		String initialValue= attribute.getClasspathAttribute().getValue();
		String newValue= TRUE.equals(initialValue) ? null : TRUE;
		return JavaCore.newClasspathAttribute(CPListElement.TEST, newValue);
	}

	@Override
	public IClasspathAttribute performRemove(ClasspathAttributeAccess attribute) {
		return JavaCore.newClasspathAttribute(CPListElement.TEST, null);
	}
}
