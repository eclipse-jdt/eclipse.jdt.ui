/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


public abstract class JDTQuickMenuAction extends QuickMenuAction { 
	
	private CompilationUnitEditor fEditor;
	
	public JDTQuickMenuAction(String commandId) {
		super(commandId);
	}

	public JDTQuickMenuAction(CompilationUnitEditor editor, String commandId) {
		super(commandId);
		fEditor= editor;
	}

	protected Point getMenuLocation(StyledText text) {
		if (fEditor == null || text != fEditor.getViewer().getTextWidget())
			return null;
		return fEditor.getViewer().getTextWidget().toDisplay(computeWordStart());
	}
	
	private Point computeWordStart() {
		ITextSelection selection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
		IRegion textRegion= JavaWordFinder.findWord(fEditor.getViewer().getDocument(), selection.getOffset());
				
		IRegion widgetRegion= modelRange2WidgetRange(textRegion);
		int start= widgetRegion.getOffset();
				
		StyledText styledText= fEditor.getViewer().getTextWidget();
		Point result= styledText.getLocationAtOffset(start);
		result.y+= styledText.getLineHeight();
		
		return result;
	}
	
	private IRegion modelRange2WidgetRange(IRegion region) {
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension= (ITextViewerExtension5)viewer;
			return extension.modelRange2WidgetRange(region);
		}
		
		IRegion visibleRegion= viewer.getVisibleRegion();
		int start= region.getOffset() - visibleRegion.getOffset();
		int end= start + region.getLength();
		if (end > visibleRegion.getLength())
			end= visibleRegion.getLength();
			
		return new Region(start, end - start);
	}	
}
