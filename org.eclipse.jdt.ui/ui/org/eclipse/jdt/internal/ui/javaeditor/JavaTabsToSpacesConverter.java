/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TabsToSpacesConverter;

import org.eclipse.jdt.ui.text.IJavaPartitions;

public class JavaTabsToSpacesConverter extends TabsToSpacesConverter {

	@Override
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		IDocumentExtension3 extension= (IDocumentExtension3) document;
		IDocumentPartitioner partitioner= extension.getDocumentPartitioner("___java_partitioning"); //$NON-NLS-1$
		if (partitioner != null) {
			String contentType= partitioner.getContentType(command.offset);
			if (IJavaPartitions.JAVA_STRING.equals(contentType) || IJavaPartitions.JAVA_MULTI_LINE_STRING.equals(contentType)) {
				// do not convert tabs to spaces when inserting within a string or text block
				return;
			}
		}
		super.customizeDocumentCommand(document, command);
	}

}
