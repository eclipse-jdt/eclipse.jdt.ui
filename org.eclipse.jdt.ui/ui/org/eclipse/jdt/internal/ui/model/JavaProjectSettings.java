/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Model object for Java project settings.
 * 
 * @since 3.2
 */
public final class JavaProjectSettings extends PlatformObject {

	/** The settings folder */
	static final String NAME_SETTINGS_FOLDER= ".settings"; //$NON-NLS-1$

	/** The associated Java project */
	private final IJavaProject fProject;

	/**
	 * Creates a new java project settings object.
	 * 
	 * @param project
	 *            the associated java project
	 */
	public JavaProjectSettings(final IJavaProject project) {
		Assert.isNotNull(project);
		fProject= project;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(final Object object) {
		if (object instanceof JavaProjectSettings) {
			final JavaProjectSettings settings= (JavaProjectSettings) object;
			return settings.fProject.equals(fProject);
		}
		return false;
	}

	/**
	 * Returns the associated project.
	 * 
	 * @return the associated project
	 */
	public IJavaProject getProject() {
		return fProject;
	}

	/**
	 * Returns the associated resource.
	 * 
	 * @return the associated resource, or <code>null</code>
	 */
	public IFolder getResource() {
		return fProject.getProject().getFolder(NAME_SETTINGS_FOLDER);
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return fProject.hashCode();
	}
}