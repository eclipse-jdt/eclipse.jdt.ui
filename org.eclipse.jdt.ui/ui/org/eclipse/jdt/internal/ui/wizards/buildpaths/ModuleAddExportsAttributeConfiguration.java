/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;
import org.eclipse.jdt.ui.wizards.ClasspathAttributeConfiguration;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class ModuleAddExportsAttributeConfiguration extends ClasspathAttributeConfiguration {

	public ModuleAddExportsAttributeConfiguration() {
	}

	@Override
	public ImageDescriptor getImageDescriptor(ClasspathAttributeAccess attribute) {
		return JavaPluginImages.DESC_OBJS_ADD_EXPORTS_ATTRIB;
	}

	@Override
	public String getNameLabel(ClasspathAttributeAccess attribute) {
		return NewWizardMessages.CPListLabelProvider_add_exports_full_label;
	}

	@Override
	public String getValueLabel(ClasspathAttributeAccess access) {
		return access.getClasspathAttribute().getValue();
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
		IPath entryPath= attribute.getParentClasspassEntry().getPath();
		IJavaElement[] sourceElements= ModuleAddExport.getTargetJavaElements(attribute.getJavaProject(), entryPath);
		String newValue= BuildPathDialogAccess.configureAddExports(shell, sourceElements, initialValue);
		if(null == newValue)	// Was the dialog cancelled?
			return null;
		return JavaCore.newClasspathAttribute(IClasspathAttribute.ADD_READS, newValue);
	}

	@Override
	public IClasspathAttribute performRemove(ClasspathAttributeAccess attribute) {
		return JavaCore.newClasspathAttribute(IClasspathAttribute.ADD_READS, null);
	}
}
