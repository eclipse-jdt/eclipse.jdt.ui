/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;

import org.eclipse.core.resources.IFile;

import org.eclipse.ui.editors.text.ForwardingDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;


/**
 * Shared properties file document provider specialized for Java properties files.
 * 
 * @since 3.1
 */
public class PropertiesFileDocumentProvider extends TextFileDocumentProvider {

	
	private static final IContentType JAVA_PROPERTIES_FILE_CONTENT_TYPE= Platform.getContentTypeManager().getContentType("org.eclipse.jdt.core.javaProperties"); //$NON-NLS-1$

	
	/**
	 * Creates a new properties file document provider and
	 * sets up the parent chain.
	 */
	public PropertiesFileDocumentProvider() {
		IDocumentProvider provider= new TextFileDocumentProvider();
		provider= new ForwardingDocumentProvider(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, new PropertiesFileDocumentSetupParticipant(), provider);
		setParentDocumentProvider(provider);
	}
	
	/*
	 * @see org.eclipse.ui.editors.text.TextFileDocumentProvider#createFileInfo(java.lang.Object)
	 */
	protected FileInfo createFileInfo(Object element) throws CoreException {
		if (JAVA_PROPERTIES_FILE_CONTENT_TYPE == null || !(element instanceof IFileEditorInput))
			return null;
			
		IFileEditorInput input= (IFileEditorInput)element;
		
		IFile file= input.getFile();
		if (file == null)
			return null;
		
		IContentDescription description= file.getContentDescription();
		if (description == null || !JAVA_PROPERTIES_FILE_CONTENT_TYPE.equals(description.getContentType()))
			return null;

		return super.createFileInfo(element);
	}
}
