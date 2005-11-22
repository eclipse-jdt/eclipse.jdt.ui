/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.merge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IStreamMerger;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * Stream merger for Java files with identical content.
 * 
 * @since 3.2
 */
public final class JavaFileStreamMerger implements IStreamMerger {

	/** Range comparator for Java files */
	private static final class JavaLineComparator implements IRangeComparator {

		/** The source lines */
		private final String[] fSourceLines;

		/**
		 * Creates a new java line comparator.
		 * 
		 * @param stream
		 *            the input stream
		 * @param encoding
		 *            the encoding of the input stream
		 * @throws IOException
		 *             if an input/output error occurs
		 */
		public JavaLineComparator(final InputStream stream, final String encoding) throws IOException {
			Assert.isNotNull(stream);
			Assert.isNotNull(encoding);
			final BufferedReader reader= new BufferedReader(new InputStreamReader(stream, encoding));
			String line= null;
			final List list= new ArrayList();
			while ((line= reader.readLine()) != null)
				list.add(line);
			fSourceLines= (String[]) list.toArray(new String[list.size()]);
		}

		/**
		 * Returns the line with the specified index.
		 * 
		 * @param index
		 *            the index
		 * @return the line at the specified index
		 */
		public String getLine(final int index) {
			return fSourceLines[index];
		}

		/**
		 * {@inheritDoc}
		 */
		public int getRangeCount() {
			return fSourceLines.length;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean rangesEqual(final int left, final IRangeComparator comparator, final int right) {
			return fSourceLines[left].equals(((JavaLineComparator) comparator).fSourceLines[right]);
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean skipRangeComparison(final int length, final int max, final IRangeComparator comparator) {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public IStatus merge(final OutputStream output, final String outputEncoding, final InputStream ancestor, final String ancestorEncoding, final InputStream target, final String targetEncoding, final InputStream source, final String sourceEncoding, final IProgressMonitor monitor) {
		JavaLineComparator ancestorComparator= null;
		JavaLineComparator targetComparator= null;
		JavaLineComparator sourceComparator= null;
		try {
			ancestorComparator= new JavaLineComparator(ancestor, ancestorEncoding);
			targetComparator= new JavaLineComparator(target, targetEncoding);
			sourceComparator= new JavaLineComparator(source, sourceEncoding);
		} catch (UnsupportedEncodingException exception) {
			return new Status(IStatus.ERROR, CompareUI.PLUGIN_ID, 1, MergeMessages.JavaStreamMerger_unsupported_encoding, exception);
		} catch (IOException exception) {
			return new Status(IStatus.ERROR, CompareUI.PLUGIN_ID, 1, MergeMessages.JavaStreamMerger_error_io, exception);
		}

		final RangeDifference[] differences= RangeDifferencer.findRanges(monitor, ancestorComparator, targetComparator, sourceComparator);
		if (differences.length == 0)
			return Status.OK_STATUS;

		return new Status(IStatus.ERROR, CompareUI.PLUGIN_ID, CONFLICT, MergeMessages.JavaStreamMerger_auto_merge_failed, null);
	}
}
