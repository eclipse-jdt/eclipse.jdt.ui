/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

class ComparePreviewer extends CompareViewerSwitchingPane implements IPreviewViewer {
	
	/**
	 * An input element for the <code>ComparePreviewer</code> class. It manages the left
	 * and right hand side input stream for the actual compare viewer as well as value indicating
	 * the type of the input streams. Example type values are: <code>"java"</code> for input
	 * stream containing Java source code, or "gif" for input stream containing gif files.
	 */
	public static class CompareInput {
		/** The left hand side */
		public InputStream left;
		/** The right hand side */
		public InputStream right;
		/** The input streams' type */
		public String type;
		/** The change element */
		public ChangeElement element;
		public CompareInput(ChangeElement e, String l, String r, String t) {
			this(e, createInputStream(l), createInputStream(r), t);
		}
		public CompareInput(ChangeElement e, InputStream l, InputStream r, String t) {
			Assert.isNotNull(e);
			Assert.isNotNull(l);
			Assert.isNotNull(r);
			Assert.isNotNull(t);
			element= e;
			left= l;
			right= r;
			type= t;
		}
		private static InputStream createInputStream(String s) {
			// Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=19319
			try {
				return new ByteArrayInputStream(s.getBytes(ResourcesPlugin.getEncoding()));
			} catch (UnsupportedEncodingException e) {
				return new ByteArrayInputStream(s.getBytes());
			}
		}
	}
	
	/** A flag indicating that the input elements of a compare viewer a of type Java */
	public static final String JAVA_TYPE= "java"; //$NON-NLS-1$
	/** A flag indicating that the input elements of a compare viewer a of type text */
	public static final String TEXT_TYPE= "txt"; //$NON-NLS-1$

	private static class CompareElement implements ITypedElement, IStreamContentAccessor {
		private InputStream fContent;
		private String fType;
		public CompareElement(InputStream content, String type) {
			fContent= content;
			fType= type;
		}
		public String getName() {
			return RefactoringMessages.getString("ComparePreviewer.element_name"); //$NON-NLS-1$
		}
		public Image getImage() {
			return null;
		}
		public String getType() {
			return fType;
		}
		public InputStream getContents() throws CoreException {
			return fContent;
		}
	}
		
	private CompareConfiguration fCompareConfiguration;
	private ChangeElementLabelProvider fLabelProvider;
	private CompareInput fCompareInput;
	
	public ComparePreviewer(Composite parent) {
		super(parent, SWT.BORDER | SWT.FLAT, true);
		fCompareConfiguration= new CompareConfiguration();
		fCompareConfiguration.setLeftEditable(false);
		fCompareConfiguration.setLeftLabel(RefactoringMessages.getString("ComparePreviewer.original_source")); //$NON-NLS-1$
		fCompareConfiguration.setRightEditable(false);
		fCompareConfiguration.setRightLabel(RefactoringMessages.getString("ComparePreviewer.refactored_source")); //$NON-NLS-1$
		fLabelProvider= new ChangeElementLabelProvider(
			JavaElementLabelProvider.SHOW_POST_QUALIFIED| JavaElementLabelProvider.SHOW_SMALL_ICONS);
	}
	
	public Control getControl() {
		return this;
	}
	
	public void refresh() {
		getViewer().refresh();
	}
	
	protected Viewer getViewer(Viewer oldViewer, Object input) {
		return CompareUI.findContentViewer(oldViewer, (ICompareInput)input, this, fCompareConfiguration);
	}
	
	public void setInput(Object input) {
		if (input instanceof CompareInput) {
			fCompareInput= (CompareInput)input;
			super.setInput(new DiffNode(
				new CompareElement(fCompareInput.left, fCompareInput.type),
				new CompareElement(fCompareInput.right, fCompareInput.type)));
		} else {
			fCompareInput= null;
			super.setInput(input);
		}
	}
	
	public void setText(String text) {
		if (fCompareInput == null) {
			super.setText(text);
			setImage(null);
			return;
		}
		setImage(fLabelProvider.getImage(fCompareInput.element));
		super.setText(fLabelProvider.getText(fCompareInput.element));
	}
}
