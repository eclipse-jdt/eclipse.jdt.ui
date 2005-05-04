/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.internal.resources.mapping.ResourceMappingContext;

public interface IInternalRefactoringProcessorIds {
	
	/**
	 * Processor ID of the copy processor (value <code>"org.eclipse.jdt.ui.CopyProcessor"</code>).
	 * 
	 * The copy processor is used when copying elements via drag and drop or when pasting
	 * elements from the clipboard. The copy processor loads the following participants,
	 * depending on the type of the element that gets copied:
	 * <ul>
	 *   <li><code>IJavaProject</code>: no participants are loaded.</li>
	 *   <li><code>IPackageFragmentRoot</code>: participants registered for copying 
	 *       <code>IPackageFragmentRoot</code> and <code>ResourceMapping</code>.</li>
	 *   <li><code>IPackageFragment</code>: participants registered for copying 
	 *       <code>IPackageFragment</code> and <code>ResourceMapping</code>.</li>
	 *   <li><code>ICompilationUnit</code>: participants registered for copying 
	 *       <code>ICompilationUnit</code> and <code>ResourceMapping</code>.</li>
	 *   <li><code>IType</code>: like ICompilationUnit if the primary top level type is copied.
	 *       Otherwise no participants are loaded.</li>
	 *   <li><code>IMember</code>: no participants are loaded.</li>
	 *   <li><code>IFolder</code>: participants registered for copying folders.</li>
	 *   <li><code>IFile</code>: participants registered for copying files.</li>
	 * </ul>
	 * <p>
	 * Use the method {@link ResourceMapping#accept(ResourceMappingContext context, IResourceVisitor visitor, IProgressMonitor monitor)} 
	 * to enumerate the resources which form the Java element. <code>ResourceMappingContext.LOCAL_CONTEXT</code> 
	 * should be use as the <code>ResourceMappingContext</code> passed to the accept methdod.
	 * </p>
	 * @see org.eclipse.core.internal.resources.mapping.ResourceMapping
	 * @since 3.1
	 */
	public static String COPY_PROCESSOR= "org.eclipse.jdt.ui.CopyProcessor";  //$NON-NLS-1$
	
}
