/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.IPropertiesFilePartitions;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFilePartitionScanner;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileSourceViewerConfiguration;

/**
 * Properties file merge viewer.
 *
 * @since 3.1
 */
public class PropertiesFileMergeViewer extends TextMergeViewer {

	/**
	 * Creates a properties file merge viewer under the given parent control.
	 *
	 * @param parent the parent control
	 * @param configuration the configuration object
	 */
	public PropertiesFileMergeViewer(Composite parent, CompareConfiguration configuration) {
		super(parent, SWT.LEFT_TO_RIGHT, configuration);
	}

	/*
	 * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#configureTextViewer(org.eclipse.jface.text.TextViewer)
	 */
	protected void configureTextViewer(TextViewer textViewer) {
		if (textViewer instanceof SourceViewer) {
			JavaTextTools tools= JavaCompareUtilities.getJavaTextTools();
			if (tools != null)
				((SourceViewer)textViewer).configure(getSourceViewerConfiguration(tools));
		}
	}

	private SourceViewerConfiguration getSourceViewerConfiguration(JavaTextTools textTools) {
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		return new PropertiesFileSourceViewerConfiguration(textTools.getColorManager(), store, null, getDocumentPartitioning());
	}

	/*
	 * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#getDocumentPartitioner()
	 */
	protected IDocumentPartitioner getDocumentPartitioner() {
		return new FastPartitioner(new PropertiesFilePartitionScanner(), IPropertiesFilePartitions.PARTITIONS);
	}

	/*
	 * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#getDocumentPartitioning()
	 * @since 3.3
	 */
	protected String getDocumentPartitioning() {
		return IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING;
	}

	/*
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#getTitle()
	 */
	public String getTitle() {
		return CompareMessages.PropertiesFileMergeViewer_title;
	}
}
