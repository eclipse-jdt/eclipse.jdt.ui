/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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


import java.util.Map;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.source.IVerticalRuler;

import org.eclipse.ui.IEditorInput;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerRulerAction;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IResourceLocator;



class ClassFileMarkerRulerAction extends MarkerRulerAction {


	public ClassFileMarkerRulerAction(String prefix, IVerticalRuler ruler, ITextEditor editor, String markerType, boolean askForLabel) {
		super(JavaEditorMessages.getBundleForConstructedKeys(), prefix, editor, ruler, markerType, askForLabel);
	}

	/**
	 * @see MarkerRulerAction#getResource()
	 */
	@Override
	protected IResource getResource() {

		IResource resource= null;

		IEditorInput input= getTextEditor().getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			IClassFile c= ((IClassFileEditorInput) input).getClassFile();
			IResourceLocator locator= c.getAdapter(IResourceLocator.class);
			if (locator != null) {
				try {
					resource= locator.getContainingResource(c);
				} catch (JavaModelException x) {
					// ignore but should inform
				}
			}
		}

		return resource;
	}

	/**
	 * @see MarkerRulerAction#getInitialAttributes()
	 */
	@Override
	protected Map<String, Object> getInitialAttributes() {

		Map<String, Object> attributes= super.getInitialAttributes();

		IEditorInput input= getTextEditor().getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			IClassFile classFile= ((IClassFileEditorInput) input).getClassFile();
			JavaCore.addJavaElementMarkerAttributes(attributes, classFile);
		}

		return attributes;
	}
}
