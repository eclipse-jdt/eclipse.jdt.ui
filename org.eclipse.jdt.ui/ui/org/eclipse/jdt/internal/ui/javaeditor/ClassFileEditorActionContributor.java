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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.ui.IEditorPart;


public class ClassFileEditorActionContributor extends BasicJavaEditorActionContributor {

	public ClassFileEditorActionContributor() {
		super();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.BasicJavaEditorActionContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
	 * @since 3.4
	 */
	public void setActiveEditor(IEditorPart part) {
		if (part instanceof JavaEditor) {
			if (((JavaEditor) part).isBreadcrumbActive())
				return;
		}
		super.setActiveEditor(part);
	}
}
