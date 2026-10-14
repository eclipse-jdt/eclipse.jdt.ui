/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.text.IJavaPartitionerManager;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public class JavaPartitionerManager implements IJavaPartitionerManager {

	private final static String[] LEGAL_CONTENT_TYPES= new String[] {
			IJavaPartitions.JAVA_DOC,
			IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
			IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
			IJavaPartitions.JAVA_STRING,
			IJavaPartitions.JAVA_CHARACTER,
			IJavaPartitions.JAVA_MULTI_LINE_STRING,
			IJavaPartitions.JAVA_MARKDOWN_COMMENT
	};

	private static ITextEditor fEditor;

	@Override
	public IPartitionTokenScanner getPartitionScanner() {
		return new FastJavaPartitionScanner(EditorUtility.getJavaProject(fEditor));
	}

	/**
	 * Factory method for creating a Java-specific document partitioner using this object's
	 * partitions scanner. This method is a convenience method.
	 *
	 * @return a newly created Java document partitioner
	 */
	@Override
	public IDocumentPartitioner createDocumentPartitioner() {
		return createDocumentPartitioner(EditorUtility.getJavaProject(fEditor));
	}

	/**
	 * Creates a Java document partitioner bound to the given project, independent of the active
	 * editor. The project determines, for example, whether text blocks are recognized.
	 * <p>
	 * May be called from any thread.
	 * </p>
	 *
	 * @param javaProject the project of the document, or <code>null</code> to use the workspace
	 *            settings
	 * @return a newly created Java document partitioner, not yet connected to a document
	 */
	public static IDocumentPartitioner createDocumentPartitioner(IJavaProject javaProject) {
		return new FastJavaPartitioner(new FastJavaPartitionScanner(javaProject), LEGAL_CONTENT_TYPES);
	}

	public void clearEditorInfo(ITextEditor editor) {
		if (editor == fEditor) {
			fEditor= null;
		}
	}

	public void dispose() {
		fEditor= null;
	}

	public void setEditorInfo(ITextEditor editor) {
		fEditor= editor;
	}
}
