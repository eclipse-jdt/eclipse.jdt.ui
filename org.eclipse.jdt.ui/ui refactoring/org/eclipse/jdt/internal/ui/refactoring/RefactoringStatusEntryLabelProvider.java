/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.swt.graphics.Image;

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
