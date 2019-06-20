/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;

import org.eclipse.ltk.ui.refactoring.model.AbstractResourceMappingMerger;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Java-aware refactoring model merger.
 *
 * @since 3.2
 */
public final class JavaModelMerger extends AbstractResourceMappingMerger {

	/**
	 * Creates a new java model merger.
	 *
	 * @param provider
	 *            the model provider
	 */
	public JavaModelMerger(final ModelProvider provider) {
		super(provider);
	}

	@Override
	protected IProject[] getDependencies(final IProject[] projects) {
		Assert.isNotNull(projects);
		final Set<IProject> set= new HashSet<>();
		for (IProject project : projects) {
			getDependentProjects(set, project);
		}
		final IProject[] result= new IProject[set.size()];
		set.toArray(result);
		return result;
	}

	/**
	 * Returns the dependent projects of the specified project.
	 *
	 * @param set
	 *            the project set
	 * @param project
	 *            the project to get its dependent projects
	 */
	private void getDependentProjects(final Set<IProject> set, final IProject project) {
		Assert.isNotNull(set);
		Assert.isNotNull(project);
		final IJavaModel model= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		if (model != null) {
			try {
				final String name= project.getName();
				for (IJavaProject p : model.getJavaProjects()) {
					for (String n : p.getRequiredProjectNames()) {
						if (name.equals(n)) {
							set.add(p.getProject());
						}
					}
				}
			} catch (JavaModelException exception) {
				JavaPlugin.log(exception);
			}
		}
	}
}
