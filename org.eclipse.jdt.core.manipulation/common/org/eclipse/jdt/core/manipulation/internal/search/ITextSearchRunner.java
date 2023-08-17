/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.search;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

/**
 * A {@link ITextSearchRunner} searches the content of a workspace file resources
 * for matches to a given search pattern.
 */
public interface ITextSearchRunner {
	public void search(IFile[] files, ITextSearchCollector collector, Pattern searchPattern, IProgressMonitor monitor);
	public void search(IResource[] rootResources, Pattern filePattern, boolean visitDerived, ITextSearchCollector collector, Pattern searchPattern, IProgressMonitor monitor);
}
