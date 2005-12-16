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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Resource mapping for java project settings.
 * 
 * @since 3.2
 */
public final class JavaProjectSettingsResourceMapping extends ResourceMapping {

	/** The name of the settings folder */
	private static final String NAME_SETTINGS_FOLDER= ".settings"; //$NON-NLS-1$

	/** The project settings */
	private final JavaProjectSettings fProjectSettings;

	/** The resource traversals */
	private ResourceTraversal[] fResourceTraversals= null;

	/**
	 * Creates a new java project settings resource mapping.
	 * 
	 * @param settings
	 *            the project settings
	 */
	public JavaProjectSettingsResourceMapping(final JavaProjectSettings settings) {
		Assert.isNotNull(settings);
		fProjectSettings= settings;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(final Object object) {
		if (object instanceof JavaProjectSettingsResourceMapping) {
			final JavaProjectSettingsResourceMapping mapping= (JavaProjectSettingsResourceMapping) object;
			return mapping.fProjectSettings.equals(fProjectSettings);
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getModelObject() {
		return fProjectSettings;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getModelProviderId() {
		return JavaModelProvider.JAVA_MODEL_PROVIDER_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	public IProject[] getProjects() {
		return new IProject[] { fProjectSettings.getProject().getProject()};
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceTraversal[] getTraversals(final ResourceMappingContext context, final IProgressMonitor monitor) throws CoreException {
		if (fResourceTraversals == null) {
			final IProject[] projects= getProjects();
			fResourceTraversals= new ResourceTraversal[] { new ResourceTraversal(new IResource[] { projects[0].getFolder(NAME_SETTINGS_FOLDER)}, IResource.DEPTH_INFINITE, IResource.NONE)};
		}
		return fResourceTraversals;
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return fProjectSettings.hashCode();
	}
}