/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.StringCharacterIterator;
import java.util.Properties;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Properties key hyperlink detector.
 * <p>
 * XXX:	This does not work for properties files coming from a JAR due to
 * 		missing J Core functionality. For details see:
 * 		https://bugs.eclipse.org/bugs/show_bug.cgi?id=22376
 * </p>
 *
 * @since 3.1
 */
public class PropertyKeyHyperlinkDetector extends AbstractHyperlinkDetector {

	/*
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		ITextEditor textEditor= (ITextEditor)getAdapter(ITextEditor.class);
		if (region == null || textEditor == null)
			return null;

		IEditorSite site= textEditor.getEditorSite();
		if (site == null)
			return null;

		int offset= region.getOffset();
		if (!checkEnabled(textEditor, offset))
			return null;

		ITypedRegion partition= null;
		try {
			IStorageEditorInput storageEditorInput= (IStorageEditorInput)textEditor.getEditorInput();
			IDocument document= textEditor.getDocumentProvider().getDocument(storageEditorInput);
			if (document instanceof IDocumentExtension3)
				partition= ((IDocumentExtension3)document).getPartition(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, offset, false);

			// Check whether it is the correct partition
			if (partition == null || !IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType())) {
				return null;
			}

			// Check whether the partition covers the selection
			if (offset + region.getLength() > partition.getOffset() + partition.getLength()) {
				return null;
			}

			// Extract the key from the partition (which contains key and assignment
			String key= document.get(partition.getOffset(), partition.getLength());

			String realKey= key.trim();
			int delta= key.indexOf(realKey);

			String unicodeKey= getUnicodeString(realKey);
			// Check whether the key is valid
			Properties properties= new Properties();
			properties.load(new ByteArrayInputStream(document.get().getBytes()));
			if (properties.getProperty(unicodeKey) == null) {
				return null;
			}

			return new PropertyKeyHyperlink[] {new PropertyKeyHyperlink(new Region(partition.getOffset() + delta, realKey.length()), realKey, textEditor)};

		} catch (BadLocationException ex) {
			return null;
		} catch (BadPartitioningException ex) {
			return null;
		} catch (IOException ex) {
			return null;
		}
	}

	private String getUnicodeString(String key) {
		StringCharacterIterator iter= new StringCharacterIterator(key);
		StringBuffer result= new StringBuffer();
		while (iter.getIndex() < iter.getEndIndex()) {
			char c= iter.current();
			if (c == '\\') {
				iter.next();
				c= iter.current();
				if (c == 'u') {
					StringBuffer unicode= new StringBuffer();
					unicode.append(iter.next());
					unicode.append(iter.next());
					unicode.append(iter.next());
					unicode.append(iter.next());
					c= (char)Integer.parseInt(unicode.toString(), 16);
				}
			}
			result.append(c);
			iter.next();
		}
		return result.toString();
	}

	private boolean checkEnabled(ITextEditor textEditor, int offset) {
		if (offset < 0)
			return false;

		 // XXX: Must be changed to IStorageEditorInput once support for JARs is available (see class Javadoc for details)
		return textEditor.getEditorInput() instanceof IFileEditorInput;
	}
}
