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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


public abstract class QuickAccessAction2 extends Action {
	
	private CompilationUnitEditor fEditor;
	private IWorkbenchPartSite fSite;
	
	public QuickAccessAction2(CompilationUnitEditor editor) {
		fEditor= editor;
		fSite= editor.getSite();
	}

	public void run() {
		MenuManager menu= new MenuManager();
		addItems(menu);
		Shell parent= fEditor.getViewer().getTextWidget().getShell();
		Menu widget= menu.createContextMenu(parent);
		if (widget.getItemCount() > 0) {
			widget.setDefaultItem(widget.getItem(0));
		}
		
		Point cursorLocation= fEditor.getViewer().getTextWidget().toDisplay(computeWordStart());
		widget.setLocation(cursorLocation);
		widget.setVisible(true);
	}
	
	protected abstract void addItems(IMenuManager menu);
	
	/**
	 * Determines graphical area covered by the given text region.
	 *
	 * @param region the region whose graphical extend must be computed
	 * @return the graphical extend of the given region
	 */
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
	
	/**
	 * Translates a given region of the text viewer's document into
	 * the corresponding region of the viewer's widget.
	 * 
	 * @param region the document region
	 * @return the corresponding widget region
	 * @since 2.1
	 */
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
