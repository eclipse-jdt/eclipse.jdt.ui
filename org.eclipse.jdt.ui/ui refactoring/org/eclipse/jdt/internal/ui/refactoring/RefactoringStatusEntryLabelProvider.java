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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

public class RefactoringStatusEntryLabelProvider extends LabelProvider{
		public String getText(Object element){
			return Strings.removeNewLine(((RefactoringStatusEntry)element).getMessage());
		}
		public Image getImage(Object element){
			RefactoringStatusEntry entry= (RefactoringStatusEntry)element;
			if (entry.isFatalError())
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_FATAL);
			else if (entry.isError())
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_ERROR);
			else if (entry.isWarning())	
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_WARNING);
			else 
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_INFO);
		}
}
