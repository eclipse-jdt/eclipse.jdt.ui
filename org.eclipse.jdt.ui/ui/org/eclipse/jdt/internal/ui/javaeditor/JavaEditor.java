/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BidiSegmentEvent;
import org.eclipse.swt.custom.BidiSegmentListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.editors.text.DefaultEncodingSupport;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.AddTaskAction;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceAction;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.StatusTextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.tasklist.TaskList;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectEnclosingAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectNextAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectPreviousAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;
import org.eclipse.jdt.internal.ui.search.SearchUsagesInFileAction;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.internal.ui.util.JavaUIHelp;
import org.eclipse.jdt.internal.ui.viewsupport.IViewPartInputProvider;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.jdt.ui.actions.OpenViewActionGroup;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;






/**
 * Java specific text editor.
 */
public abstract class JavaEditor extends StatusTextEditor implements IViewPartInputProvider {
		
	/**
	 * "Smart" runnable for updating the outline page's selection.
	 */
	class OutlinePageSelectionUpdater implements Runnable {
		
		/** Has the runnable already been posted? */
		private boolean fPosted= false;
		
		public OutlinePageSelectionUpdater() {
		}
		
		/*
		 * @see Runnable#run()
		 */
		public void run() {
			synchronizeOutlinePageSelection();
			fPosted= false;
		}
		
		/**
		 * Posts this runnable into the event queue.
		 */
		public void post() {
			if (fPosted)
				return;
				
			Shell shell= getSite().getShell();
			if (shell != null & !shell.isDisposed()) {
				fPosted= true;
				shell.getDisplay().asyncExec(this);
			}
		}
	};
	
	
	class SelectionChangedListener  implements ISelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}
	};
	
		/*
	 * Link mode.  
	 */
	class MouseClickListener implements KeyListener, MouseListener, MouseMoveListener,
		FocusListener, PaintListener, IPropertyChangeListener, IDocumentListener, ITextInputListener {

		/** The session is active. */
		private boolean fActive;

		/** The currently active style range. */
		private IRegion fActiveRegion;
		/** The currently active style range as position. */
		private Position fRememberedPosition;
		/** The hand cursor. */
		private Cursor fCursor;
		
		/** The link color. */
		private Color fColor;

		public void deactivate() {
			deactivate(false);
		}

		public void deactivate(boolean redrawAll) {
			if (!fActive)
				return;

			repairRepresentation(redrawAll);			
			fActive= false;
		}

		public void install() {

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			StyledText text= sourceViewer.getTextWidget();			
			if (text == null || text.isDisposed())
				return;
				
			updateColor(sourceViewer);

			sourceViewer.addTextInputListener(this);
			
			IDocument document= sourceViewer.getDocument();
			if (document != null)
				document.addDocumentListener(this);			

			text.addKeyListener(this);
			text.addMouseListener(this);
			text.addMouseMoveListener(this);
			text.addFocusListener(this);
			text.addPaintListener(this);
			
			IPreferenceStore preferenceStore= getPreferenceStore();
			preferenceStore.addPropertyChangeListener(this);			
		}
		
		public void uninstall() {

			if (fColor != null) {
				fColor.dispose();
				fColor= null;
			}
			
			if (fCursor != null) {
				fCursor.dispose();
				fCursor= null;
			}
			
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			sourceViewer.removeTextInputListener(this);

			IDocument document= sourceViewer.getDocument();
			if (document != null)
				document.removeDocumentListener(this);
				
			IPreferenceStore preferenceStore= getPreferenceStore();
			if (preferenceStore != null)
				preferenceStore.removePropertyChangeListener(this);
			
			StyledText text= sourceViewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
				
			text.removeKeyListener(this);
			text.removeMouseListener(this);
			text.removeMouseMoveListener(this);
			text.removeFocusListener(this);
			text.removePaintListener(this);
			}
				
		/*
		 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(JavaEditor.LINK_COLOR)) {
				ISourceViewer viewer= getSourceViewer();
				if (viewer != null)	
					updateColor(viewer);
			}
		}

		private void updateColor(ISourceViewer viewer) {
			if (fColor != null)
				fColor.dispose();
	
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;

			Display display= text.getDisplay();
			fColor= createColor(getPreferenceStore(), JavaEditor.LINK_COLOR, display);
		}

		/**
		 * Creates a color from the information stored in the given preference store.
		 * Returns <code>null</code> if there is no such information available.
		 */
		private Color createColor(IPreferenceStore store, String key, Display display) {
		
			RGB rgb= null;		
			
			if (store.contains(key)) {
				
				if (store.isDefault(key))
					rgb= PreferenceConverter.getDefaultColor(store, key);
				else
					rgb= PreferenceConverter.getColor(store, key);
			
				if (rgb != null)
					return new Color(display, rgb);
			}
			
			return null;
		}		
	
		private void repairRepresentation() {			
			repairRepresentation(false);
		}

		private void repairRepresentation(boolean redrawAll) {			

			if (fActiveRegion == null)
				return;
			
			ISourceViewer viewer= getSourceViewer();
			if (viewer != null) {
				resetCursor(viewer);

				int offset= fActiveRegion.getOffset();
				int length= fActiveRegion.getLength();

				// remove style
				if (!redrawAll && viewer instanceof ITextViewerExtension2)
					((ITextViewerExtension2) viewer).invalidateTextPresentation(offset, length);
				else
					viewer.invalidateTextPresentation();

				// remove underline				
				if (viewer instanceof ITextViewerExtension3) {
					ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
					offset= extension.modelOffset2WidgetOffset(offset);
				} else {
				offset -= viewer.getVisibleRegion().getOffset();
				}
				
				StyledText text= viewer.getTextWidget();
				text.redrawRange(offset, length, true);					
			}

			fActiveRegion= null;
		}

		private IJavaElement getInput(JavaEditor editor) {
			if (editor == null)
				return null;
			IEditorInput input= editor.getEditorInput();
			if (input instanceof IClassFileEditorInput)
				return ((IClassFileEditorInput)input).getClassFile();
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
			return manager.getWorkingCopy(input);			
		}


		// will eventually be replaced by a method provided by jdt.core		
		private IRegion selectWord(IDocument document, int anchor) {
		
			try {		
				int offset= anchor;
				char c;
	
				while (offset >= 0) {
					c= document.getChar(offset);
					if (!Character.isJavaIdentifierPart(c))
						break;
					--offset;
				}
	
				int start= offset;
	
				offset= anchor;
				int length= document.getLength();
	
				while (offset < length) {
					c= document.getChar(offset);
					if (!Character.isJavaIdentifierPart(c))
						break;
					++offset;
				}
				
				int end= offset;
				
				if (start == end)
					return new Region(start, 0);
				else
					return new Region(start + 1, end - start - 1);
				
			} catch (BadLocationException x) {
				return null;
			}
		}

		IRegion getCurrentTextRegion(ISourceViewer viewer) {

			int offset= getCurrentTextOffset(viewer);				
			if (offset == -1)
				return null;

			IJavaElement input= SelectionConverter.getInput(JavaEditor.this);
			if (input == null)
				return null;

			try {
				IJavaElement[] elements= ((ICodeAssist) input).codeSelect(offset, 0);
				if (elements == null || elements.length == 0)
					return null;
					
				return selectWord(viewer.getDocument(), offset);
					
			} catch (JavaModelException e) {
				return null;	
			}
		}

		private int getCurrentTextOffset(ISourceViewer viewer) {

			try {					
				StyledText text= viewer.getTextWidget();			
				if (text == null || text.isDisposed())
					return -1;

				Display display= text.getDisplay();				
				Point absolutePosition= display.getCursorLocation();
				Point relativePosition= text.toControl(absolutePosition);
				
				int widgetOffset= text.getOffsetAtLocation(relativePosition);
				if (viewer instanceof ITextViewerExtension3) {
					ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
					return extension.widgetOffset2ModelOffset(widgetOffset);
				} else {
					return widgetOffset + viewer.getVisibleRegion().getOffset();
				}

			} catch (IllegalArgumentException e) {
				return -1;
			}			
		}

		private void highlightRegion(ISourceViewer viewer, IRegion region) {

			if (region.equals(fActiveRegion))
				return;

			repairRepresentation();

			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;

			// highlight region
			int offset= 0;
			int length= 0;
			
			if (viewer instanceof ITextViewerExtension3) {
				ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
				IRegion widgetRange= extension.modelRange2WidgetRange(region);
				if (widgetRange == null)
					return;
					
				offset= widgetRange.getOffset();
				length= widgetRange.getLength();
				
			} else {
				offset= region.getOffset() - viewer.getVisibleRegion().getOffset();
				length= region.getLength();
			}
			
			StyleRange oldStyleRange= text.getStyleRangeAtOffset(offset);
			Color foregroundColor= fColor;
			Color backgroundColor= oldStyleRange == null ? text.getBackground() : oldStyleRange.background;
			StyleRange styleRange= new StyleRange(offset, length, foregroundColor, backgroundColor);
			text.setStyleRange(styleRange);

			// underline
			text.redrawRange(offset, length, true);

			fActiveRegion= region;
		}

		private void activateCursor(ISourceViewer viewer) {
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
			Display display= text.getDisplay();
			if (fCursor == null)
				fCursor= new Cursor(display, SWT.CURSOR_HAND);
			text.setCursor(fCursor);
		}
		
		private void resetCursor(ISourceViewer viewer) {
			StyledText text= viewer.getTextWidget();
			if (text != null && !text.isDisposed())
				text.setCursor(null);
						
			if (fCursor != null) {
				fCursor.dispose();
				fCursor= null;
			}
		}

		/*
		 * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
		 */
		public void keyPressed(KeyEvent event) {

			if (fActive) {
				deactivate();
				return;	
			}

			if (event.keyCode != SWT.MOD1) {
				deactivate();
				return;
			}
			
			fActive= true;

//			removed for #25871			
//
//			ISourceViewer viewer= getSourceViewer();
//			if (viewer == null)
//				return;
//			
//			IRegion region= getCurrentTextRegion(viewer);
//			if (region == null)
//				return;
//			
//			highlightRegion(viewer, region);
//			activateCursor(viewer);												
		}

		/*
		 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
		 */
		public void keyReleased(KeyEvent event) {
			
			if (!fActive)
				return;

			deactivate();				
		}

		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseDoubleClick(MouseEvent e) {}
		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseDown(MouseEvent event) {
			
			if (!fActive)
				return;
				
			if (event.stateMask != SWT.MOD1) {
				deactivate();
				return;	
			}
			
			if (event.button != 1) {
				deactivate();
				return;	
			}			
		}

		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseUp(MouseEvent e) {

			if (!fActive)
				return;
				
			if (e.button != 1) {
				deactivate();
				return;
			}
				
			deactivate();
			
			IAction action= getAction("OpenEditor");  //$NON-NLS-1$
			if (action == null)
				return;
				
			action.run();			
		}

		/*
		 * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseMove(MouseEvent event) {
			
			if (event.widget instanceof Control && !((Control) event.widget).isFocusControl()) {
				deactivate();
				return;
			}
			
			if (!fActive) {
				if (event.stateMask != SWT.MOD1)
					return;
				// MOD1 was already pressed
				fActive= true;
			}
	
			ISourceViewer viewer= getSourceViewer();
			if (viewer == null) {
				deactivate();
				return;
			}
				
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed()) {
				deactivate();
				return;
			}
				
			if ((event.stateMask & SWT.BUTTON1) != 0 && text.getSelectionCount() != 0) {
				deactivate();
				return;
			}
		
			IRegion region= getCurrentTextRegion(viewer);
			if (region == null || region.getLength() == 0) {
				repairRepresentation();
				return;
			}
			
			highlightRegion(viewer, region);	
			activateCursor(viewer);												
		}

		/*
		 * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
		 */
		public void focusGained(FocusEvent e) {}

		/*
		 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
		 */
		public void focusLost(FocusEvent event) {
			deactivate();
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
			if (fActive && fActiveRegion != null) {
				fRememberedPosition= new Position(fActiveRegion.getOffset(), fActiveRegion.getLength());
				try {
					event.getDocument().addPosition(fRememberedPosition);
				} catch (BadLocationException x) {
					fRememberedPosition= null;
		}
			}
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
			if (fRememberedPosition != null && !fRememberedPosition.isDeleted()) {
				event.getDocument().removePosition(fRememberedPosition);
				fActiveRegion= new Region(fRememberedPosition.getOffset(), fRememberedPosition.getLength());
		}
			fRememberedPosition= null;

			ISourceViewer viewer= getSourceViewer();
			if (viewer != null) {
				StyledText widget= viewer.getTextWidget();
				if (widget != null && !widget.isDisposed()) {
					widget.getDisplay().asyncExec(new Runnable() {
						public void run() {
							deactivate();
						}
					});
				}
			}
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
			if (oldInput == null)
				return;
			deactivate();
			oldInput.removeDocumentListener(this);
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
			if (newInput == null)
				return;
			newInput.addDocumentListener(this);
		}

		/*
		 * @see PaintListener#paintControl(PaintEvent)
		 */
		public void paintControl(PaintEvent event) {	
			if (fActiveRegion == null)
				return;
	
			ISourceViewer viewer= getSourceViewer();
			if (viewer == null)
				return;
				
			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
				
				
			int offset= 0;
			int length= 0;

			if (viewer instanceof ITextViewerExtension3) {
				
				ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
				IRegion widgetRange= extension.modelRange2WidgetRange(new Region(offset, length));
				if (widgetRange == null)
					return;
					
				offset= widgetRange.getOffset();
				length= widgetRange.getLength();
				
			} else {
				
			IRegion region= viewer.getVisibleRegion();			
			if (!includes(region, fActiveRegion))
			 	return;		    

				offset= fActiveRegion.getOffset() - region.getOffset();
				length= fActiveRegion.getLength();
			}
			
			// support for bidi
			Point minLocation= getMinimumLocation(text, offset, length);
			Point maxLocation= getMaximumLocation(text, offset, length);
	
			int x1= minLocation.x;
			int x2= minLocation.x + maxLocation.x - minLocation.x - 1;
			int y= minLocation.y + text.getLineHeight() - 1;
			
			GC gc= event.gc;
			if (fColor != null && !fColor.isDisposed())
			gc.setForeground(fColor);
			gc.drawLine(x1, y, x2, y);
		}

		private boolean includes(IRegion region, IRegion position) {
			return
				position.getOffset() >= region.getOffset() &&
				position.getOffset() + position.getLength() <= region.getOffset() + region.getLength();
		}

		private Point getMinimumLocation(StyledText text, int offset, int length) {
			Point minLocation= new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
	
			for (int i= 0; i <= length; i++) {
				Point location= text.getLocationAtOffset(offset + i);
				
				if (location.x < minLocation.x)
					minLocation.x= location.x;			
				if (location.y < minLocation.y)
					minLocation.y= location.y;			
			}	
			
			return minLocation;
		}
	
		private Point getMaximumLocation(StyledText text, int offset, int length) {
			Point maxLocation= new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
	
			for (int i= 0; i <= length; i++) {
				Point location= text.getLocationAtOffset(offset + i);
				
				if (location.x > maxLocation.x)
					maxLocation.x= location.x;			
				if (location.y > maxLocation.y)
					maxLocation.y= location.y;			
			}	
			
			return maxLocation;
		}
	};
	
	/**
	 * This action dispatches into two behaviours: If there is no current text
	 * hover, the javadoc is displayed using information presenter. If there is
	 * a current text hover, it is converted into a information presenter in
	 * order to make it sticky.
	 */
	class InformationDispatchAction extends TextEditorAction {
		
		/** The wrapped text operation action. */
		private final TextOperationAction fTextOperationAction;
		
		/**
		 * Creates a dispatch action.
		 */
		public InformationDispatchAction(ResourceBundle resourceBundle, String prefix, final TextOperationAction textOperationAction) {
			super(resourceBundle, prefix, JavaEditor.this);
			if (textOperationAction == null)
				throw new IllegalArgumentException();
			fTextOperationAction= textOperationAction;
		}
		
		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null) {	
				fTextOperationAction.run();
				return;
			}
				
			if (! (sourceViewer instanceof ITextViewerExtension2)) {
				fTextOperationAction.run();
				return;
			}
				
			ITextViewerExtension2 textViewerExtension2= (ITextViewerExtension2) sourceViewer;

			// does a text hover exist?
			ITextHover textHover= textViewerExtension2.getCurrentTextHover();
			if (textHover == null) {
				fTextOperationAction.run();
				return;				
			}

			Point hoverEventLocation= textViewerExtension2.getHoverEventLocation();
			int offset= computeOffsetAtLocation(sourceViewer, hoverEventLocation.x, hoverEventLocation.y);
			if (offset == -1) {
				fTextOperationAction.run();
				return;				
			}				

			try {
				// get the text hover content
				IDocument document= sourceViewer.getDocument();
				String contentType= document.getContentType(offset);

				final IRegion hoverRegion= textHover.getHoverRegion(sourceViewer, offset);						
				if (hoverRegion == null)
					return;
				
				final String hoverInfo= textHover.getHoverInfo(sourceViewer, hoverRegion);
	
				// with information provider
				IInformationProvider informationProvider= new IInformationProvider() {
					/*
					 * @see org.eclipse.jface.text.information.IInformationProvider#getSubject(org.eclipse.jface.text.ITextViewer, int)
					 */
					public IRegion getSubject(ITextViewer textViewer, int offset) {					
						return hoverRegion;
					}
					/*
					 * @see org.eclipse.jface.text.information.IInformationProvider#getInformation(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
					 */
					public String getInformation(ITextViewer textViewer, IRegion subject) {
						return hoverInfo;
					}
				};

				fInformationPresenter.setOffset(offset);	
				fInformationPresenter.setInformationProvider(informationProvider, contentType);
				fInformationPresenter.showInformation();

			} catch (BadLocationException e) {				
			}
		}

		// modified version from TextViewer
		private int computeOffsetAtLocation(ITextViewer textViewer, int x, int y) {
			
			StyledText styledText= textViewer.getTextWidget();
			IDocument document= textViewer.getDocument();
			
			if (document == null)
				return -1;		

			try {
				int widgetLocation= styledText.getOffsetAtLocation(new Point(x, y));
				if (textViewer instanceof ITextViewerExtension3) {
					ITextViewerExtension3 extension= (ITextViewerExtension3) textViewer;
					return extension.widgetOffset2ModelOffset(widgetLocation);
				} else {
					IRegion visibleRegion= textViewer.getVisibleRegion();
					return widgetLocation + visibleRegion.getOffset();
				}
			} catch (IllegalArgumentException e) {
				return -1;	
			}

		}
	};
	
	static protected class AnnotationAccess implements IAnnotationAccess {

		/*
		 * @see org.eclipse.jface.text.source.IAnnotationAccess#getType(org.eclipse.jface.text.source.Annotation)
		 */
		public Object getType(Annotation annotation) {
			if (annotation instanceof IJavaAnnotation) {
				IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
				if (javaAnnotation.isRelevant())
					return javaAnnotation.getAnnotationType();
			}
			return null;
		}

		/*
		 * @see org.eclipse.jface.text.source.IAnnotationAccess#isMultiLine(org.eclipse.jface.text.source.Annotation)
		 */
		public boolean isMultiLine(Annotation annotation) {
			return true;
		}

		/*
		 * @see org.eclipse.jface.text.source.IAnnotationAccess#isTemporary(org.eclipse.jface.text.source.Annotation)
		 */
		public boolean isTemporary(Annotation annotation) {
			if (annotation instanceof IJavaAnnotation) {
				IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
				if (javaAnnotation.isRelevant())
					return javaAnnotation.isTemporary();
			}
			return false;
		}
	};

	
	/** Preference key for showing the line number ruler */
	protected final static String LINE_NUMBER_RULER= PreferenceConstants.EDITOR_LINE_NUMBER_RULER;
	/** Preference key for the foreground color of the line numbers */
	protected final static String LINE_NUMBER_COLOR= PreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR;
	/** Preference key for the link color */
	protected final static String LINK_COLOR= PreferenceConstants.EDITOR_LINK_COLOR;
	/** Preference key for matching brackets */
	protected final static String MATCHING_BRACKETS=  PreferenceConstants.EDITOR_MATCHING_BRACKETS;
	/** Preference key for matching brackets color */
	protected final static String MATCHING_BRACKETS_COLOR=  PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;
	/** Preference key for highlighting current line */
	protected final static String CURRENT_LINE= PreferenceConstants.EDITOR_CURRENT_LINE;
	/** Preference key for highlight color of current line */
	protected final static String CURRENT_LINE_COLOR= PreferenceConstants.EDITOR_CURRENT_LINE_COLOR;
	/** Preference key for showing print marging ruler */
	protected final static String PRINT_MARGIN= PreferenceConstants.EDITOR_PRINT_MARGIN;
	/** Preference key for print margin ruler color */
	protected final static String PRINT_MARGIN_COLOR= PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR;
	/** Preference key for print margin ruler column */
	protected final static String PRINT_MARGIN_COLUMN= PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN;
	/** Preference key for error indication */
	protected final static String ERROR_INDICATION= PreferenceConstants.EDITOR_PROBLEM_INDICATION;
	/** Preference key for error color */
	protected final static String ERROR_INDICATION_COLOR= PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR;
	/** Preference key for warning indication */
	protected final static String WARNING_INDICATION= PreferenceConstants.EDITOR_WARNING_INDICATION;
	/** Preference key for warning color */
	protected final static String WARNING_INDICATION_COLOR= PreferenceConstants.EDITOR_WARNING_INDICATION_COLOR;
	/** Preference key for task indication */
	protected final static String TASK_INDICATION= PreferenceConstants.EDITOR_TASK_INDICATION;
	/** Preference key for task color */
	protected final static String TASK_INDICATION_COLOR= PreferenceConstants.EDITOR_TASK_INDICATION_COLOR;
	/** Preference key for bookmark indication */
	protected final static String BOOKMARK_INDICATION= PreferenceConstants.EDITOR_BOOKMARK_INDICATION;
	/** Preference key for bookmark color */
	protected final static String BOOKMARK_INDICATION_COLOR= PreferenceConstants.EDITOR_BOOKMARK_INDICATION_COLOR;
	/** Preference key for search result indication */
	protected final static String SEARCH_RESULT_INDICATION= PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION;
	/** Preference key for search result color */
	protected final static String SEARCH_RESULT_INDICATION_COLOR= PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_COLOR;
	/** Preference key for unknown annotation indication */
	protected final static String UNKNOWN_INDICATION= PreferenceConstants.EDITOR_UNKNOWN_INDICATION;
	/** Preference key for unknown annotation color */
	protected final static String UNKNOWN_INDICATION_COLOR= PreferenceConstants.EDITOR_UNKNOWN_INDICATION_COLOR;
	/** Preference key for shwoing the overview ruler */
	protected final static String OVERVIEW_RULER= PreferenceConstants.EDITOR_OVERVIEW_RULER;
	/** Preference key for error indication in overview ruler */
	protected final static String ERROR_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for warning indication in overview ruler */
	protected final static String WARNING_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for task indication in overview ruler */
	protected final static String TASK_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for bookmark indication in overview ruler */
	protected final static String BOOKMARK_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for search result indication in overview ruler */
	protected final static String SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for unknown annotation indication in overview ruler */
	protected final static String UNKNOWN_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER;
	
	protected final static char[] BRACKETS= { '{', '}', '(', ')', '[', ']' };

	/** The outline page */
	protected JavaOutlinePage fOutlinePage;
	/** Outliner context menu Id */
	protected String fOutlinerContextMenuId;
	/** The selection changed listener */
	protected ISelectionChangedListener fSelectionChangedListener= new SelectionChangedListener();
	/** The editor's bracket matcher */
	protected JavaPairMatcher fBracketMatcher= new JavaPairMatcher(BRACKETS);
	/** The outline page selection updater */
	private OutlinePageSelectionUpdater fUpdater;
	/** Indicates whether this editor should react on outline page selection changes */
	private int fIgnoreOutlinePageSelection;
	/** The line number ruler column */
	private LineNumberRulerColumn fLineNumberRulerColumn;
	/** This editor's encoding support */
	private DefaultEncodingSupport fEncodingSupport;
	/** The mouse listener */
	private MouseClickListener fMouseListener;
	/** The information presenter. */
	private InformationPresenter fInformationPresenter;
	/** The annotation access */
	protected IAnnotationAccess fAnnotationAccess= new AnnotationAccess();
	/** The overview ruler */
	protected OverviewRuler isOverviewRulerVisible;
	/** The source viewer decoration support */
	protected SourceViewerDecorationSupport fSourceViewerDecorationSupport;
	/** The overview ruler */
	protected OverviewRuler fOverviewRuler;
	/** History for structure select action */
	private SelectionHistory fSelectionHistory;
	
	protected CompositeActionGroup fActionGroups;
	private CompositeActionGroup fContextMenuGroup;

		
	/**
	 * Returns the most narrow java element including the given offset
	 * 
	 * @param offset the offset inside of the requested element
	 */
	abstract protected IJavaElement getElementAt(int offset);
	
	/**
	 * Returns the java element of this editor's input corresponding to the given IJavaElement
	 */
	abstract protected IJavaElement getCorrespondingElement(IJavaElement element);
	
	/**
	 * Sets the input of the editor's outline page.
	 */
	abstract protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input);
	
	
	/**
	 * Default constructor.
	 */
	public JavaEditor() {
		super();
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		setSourceViewerConfiguration(new JavaSourceViewerConfiguration(textTools, this));
		setRangeIndicator(new DefaultRangeIndicator());
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		
		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE))
			fUpdater= new OutlinePageSelectionUpdater();
	}
	
	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected final ISourceViewer createSourceViewer(Composite parent, IVerticalRuler verticalRuler, int styles) {
		
		ISharedTextColors sharedColors= JavaPlugin.getDefault().getJavaTextTools().getColorManager();
		fOverviewRuler= new OverviewRuler(fAnnotationAccess, VERTICAL_RULER_WIDTH, sharedColors);		
		ISourceViewer viewer= createJavaSourceViewer(parent, verticalRuler, fOverviewRuler, isOverviewRulerVisible(), styles);
		
		StyledText text= viewer.getTextWidget();
		text.addBidiSegmentListener(new  BidiSegmentListener() {
			public void lineGetSegments(BidiSegmentEvent event) {
				event.segments= getBidiLineSegments(event.lineOffset, event.lineText);
			}
		});
		
		JavaUIHelp.setHelp(this, text, IJavaHelpContextIds.JAVA_EDITOR);
				
		fSourceViewerDecorationSupport= new SourceViewerDecorationSupport(viewer, fOverviewRuler, fAnnotationAccess, sharedColors);
		configureSourceViewerDecorationSupport();
		return viewer;
	}
	
	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean isOverviewRulerVisible, int styles) {
			return new JavaSourceViewer(parent, verticalRuler, fOverviewRuler, isOverviewRulerVisible(), styles);
	}
	
	/*
	 * @see AbstractTextEditor#affectsTextPresentation(PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		return textTools.affectsBehavior(event);
	}
		
	/**
	 * Sets the outliner's context menu ID.
	 */
	protected void setOutlinerContextMenuId(String menuId) {
		fOutlinerContextMenuId= menuId;
	}
			
	/**
	 *  Returns the standard action group of this editor.
	 */
	protected ActionGroup getActionGroup() {
		return fActionGroups;
	} 
	
	/*
	 * @see AbstractTextEditor#editorContextMenuAboutToShow
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
			
		super.editorContextMenuAboutToShow(menu);
		menu.appendToGroup(ITextEditorActionConstants.GROUP_UNDO, new Separator(IContextMenuConstants.GROUP_OPEN));	
		menu.insertAfter(IContextMenuConstants.GROUP_OPEN, new GroupMarker(IContextMenuConstants.GROUP_SHOW));	
		
		ActionContext context= new ActionContext(getSelectionProvider().getSelection());
		fContextMenuGroup.setContext(context);
		fContextMenuGroup.fillContextMenu(menu);
		fContextMenuGroup.setContext(null);
	}			
	
	/**
	 * Creates the outline page used with this editor.
	 */
	protected JavaOutlinePage createOutlinePage() {
		
		JavaOutlinePage page= new JavaOutlinePage(fOutlinerContextMenuId, this);
		
		page.addSelectionChangedListener(fSelectionChangedListener);
		setOutlinePageInput(page, getEditorInput());
		
		return page;
	}
	
	/**
	 * Informs the editor that its outliner has been closed.
	 */
	public void outlinePageClosed() {
		if (fOutlinePage != null) {
			fOutlinePage.removeSelectionChangedListener(fSelectionChangedListener);
			fOutlinePage= null;
			resetHighlightRange();
		}
	}
	
	/**
	 * Synchronizes the outliner selection with the actual cursor
	 * position in the editor.
	 */
	public void synchronizeOutlinePageSelection() {
		
		if (isEditingScriptRunning())
			return;
		
		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer == null || fOutlinePage == null)
			return;
			
		StyledText styledText= sourceViewer.getTextWidget();
		if (styledText == null)
			return;
		
		int caret= 0;
		if (sourceViewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) sourceViewer;
			caret= extension.widgetOffset2ModelOffset(styledText.getCaretOffset());
		} else {
		int offset= sourceViewer.getVisibleRegion().getOffset();
			caret= offset + styledText.getCaretOffset();
		}
		
		IJavaElement element= getElementAt(caret);
		if (element instanceof ISourceReference) {
			fOutlinePage.removeSelectionChangedListener(fSelectionChangedListener);
			fOutlinePage.select((ISourceReference) element);
			fOutlinePage.addSelectionChangedListener(fSelectionChangedListener);
		}
	}
	
	
	/*
	 * Get the desktop's StatusLineManager
	 */
	protected IStatusLineManager getStatusLineManager() {
		IEditorActionBarContributor contributor= getEditorSite().getActionBarContributor();
		if (contributor instanceof EditorActionBarContributor) {
			return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
		}
		return null;
	}	
	
	/*
	 * @see AbstractTextEditor#getAdapter(Class)
	 */
	public Object getAdapter(Class required) {
		
		if (IContentOutlinePage.class.equals(required)) {
			if (fOutlinePage == null)
				fOutlinePage= createOutlinePage();
			return fOutlinePage;
		}
		
		if (IEncodingSupport.class.equals(required))
			return fEncodingSupport;
			
		return super.getAdapter(required);
	}
	
	protected void setSelection(ISourceReference reference, boolean moveCursor) {
		
		ISelection selection= getSelectionProvider().getSelection();
		if (selection instanceof TextSelection) {
			TextSelection textSelection= (TextSelection) selection;
			if (textSelection.getOffset() != 0 || textSelection.getLength() != 0)
				markInNavigationHistory();
		}
		
		if (reference != null) {
			
			StyledText  textWidget= null;
			
			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer != null)
				textWidget= sourceViewer.getTextWidget();
			
			if (textWidget == null)
				return;
				
			try {
				
				ISourceRange range= reference.getSourceRange();
				if (range == null)
					return;
				
				int offset= range.getOffset();
				int length= range.getLength();
				
				if (offset < 0 || length < 0)
					return;
					
				textWidget.setRedraw(false);
				
				setHighlightRange(offset, length, moveCursor);

				if (!moveCursor)
					return;
											
				offset= -1;
				length= -1;
				
				if (reference instanceof IMember) {
					range= ((IMember) reference).getNameRange();
					if (range != null) {
						offset= range.getOffset();
						length= range.getLength();
					}
				} else if (reference instanceof IImportDeclaration) {
					String name= ((IImportDeclaration) reference).getElementName();
					if (name != null && name.length() > 0) {
						String content= reference.getSource();
						offset= range.getOffset() + content.indexOf(name);
						length= name.length();
					}
				} else if (reference instanceof IPackageDeclaration) {
					String name= ((IPackageDeclaration) reference).getElementName();
					if (name != null && name.length() > 0) {
						String content= reference.getSource();
						offset= range.getOffset() + content.indexOf(name);
						length= name.length();
					}
				}
				
				if (offset > -1 && length > 0) {
					sourceViewer.revealRange(offset, length);
					sourceViewer.setSelectedRange(offset, length);
				}
				
			} catch (JavaModelException x) {
			} catch (IllegalArgumentException x) {
			} finally {
				if (textWidget != null)
					textWidget.setRedraw(true);
			}
			
		} else if (moveCursor) {
			resetHighlightRange();
		}
		
		markInNavigationHistory();
	}
		
	public void setSelection(IJavaElement element) {
		
		if (element == null || element instanceof ICompilationUnit || element instanceof IClassFile) {
			/*
			 * If the element is an ICompilationUnit this unit is either the input
			 * of this editor or not being displayed. In both cases, nothing should
			 * happened. (http://dev.eclipse.org/bugs/show_bug.cgi?id=5128)
			 */
			return;
		}
		
		IJavaElement corresponding= getCorrespondingElement(element);
		if (corresponding instanceof ISourceReference) {
			ISourceReference reference= (ISourceReference) corresponding;
			// set hightlight range
			setSelection(reference, true);
			// set outliner selection
			if (fOutlinePage != null) {
				fOutlinePage.removeSelectionChangedListener(fSelectionChangedListener);
				fOutlinePage.select(reference);
				fOutlinePage.addSelectionChangedListener(fSelectionChangedListener);
			}
		}
	}
	
	public synchronized void editingScriptStarted() {
		++ fIgnoreOutlinePageSelection;
	}
	
	public synchronized void editingScriptEnded() {
		-- fIgnoreOutlinePageSelection;
	}
	
	public synchronized boolean isEditingScriptRunning() {
		return (fIgnoreOutlinePageSelection > 0);
	}
	
	protected void doSelectionChanged(SelectionChangedEvent event) {
				
		ISourceReference reference= null;
		
		ISelection selection= event.getSelection();
		Iterator iter= ((IStructuredSelection) selection).iterator();
		while (iter.hasNext()) {
			Object o= iter.next();
			if (o instanceof ISourceReference) {
				reference= (ISourceReference) o;
				break;
			}
		}
		if (!isActivePart() && JavaPlugin.getActivePage() != null)
			JavaPlugin.getActivePage().bringToTop(this);
			
		try {
			editingScriptStarted();
			setSelection(reference, !isActivePart());
		} finally {
			editingScriptEnded();
		}
	}
	
	/*
	 * @see AbstractTextEditor#adjustHighlightRange(int, int)
	 */
	protected void adjustHighlightRange(int offset, int length) {
		
		try {
			
			IJavaElement element= getElementAt(offset);
			while (element instanceof ISourceReference) {
				ISourceRange range= ((ISourceReference) element).getSourceRange();
				if (offset < range.getOffset() + range.getLength() && range.getOffset() < offset + length) {
					setHighlightRange(range.getOffset(), range.getLength(), true);
					if (fOutlinePage != null) {
						fOutlinePage.removeSelectionChangedListener(fSelectionChangedListener);
						fOutlinePage.select((ISourceReference) element);
						fOutlinePage.addSelectionChangedListener(fSelectionChangedListener);
					}
					return;
				}
				element= element.getParent();
			}
			
		} catch (JavaModelException x) {
			JavaPlugin.log(x.getStatus());
		}
		
		resetHighlightRange();
	}
			
	protected boolean isActivePart() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IPartService service= window.getPartService();
		IWorkbenchPart part= service.getActivePart();
		return part != null && part.equals(this);
	}
	
	/*
	 * @see StatusTextEditor#getStatusHeader(IStatus)
	 */
	protected String getStatusHeader(IStatus status) {
		if (fEncodingSupport != null) {
			String message= fEncodingSupport.getStatusHeader(status);
			if (message != null)
				return message;
		}
		return super.getStatusHeader(status);
	}
	
	/*
	 * @see StatusTextEditor#getStatusBanner(IStatus)
	 */
	protected String getStatusBanner(IStatus status) {
		if (fEncodingSupport != null) {
			String message= fEncodingSupport.getStatusBanner(status);
			if (message != null)
				return message;
		}
		return super.getStatusBanner(status);
	}
	
	/*
	 * @see StatusTextEditor#getStatusMessage(IStatus)
	 */
	protected String getStatusMessage(IStatus status) {
		if (fEncodingSupport != null) {
			String message= fEncodingSupport.getStatusMessage(status);
			if (message != null)
				return message;
		}
		return super.getStatusMessage(status);
	}
	
	/*
	 * @see AbstractTextEditor#doSetInput
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (fEncodingSupport != null)
			fEncodingSupport.reset();
		setOutlinePageInput(fOutlinePage, input);
	}
	
	/*
	 * @see IWorkbenchPart#dispose()
	 */
	public void dispose() {

		if (fMouseListener != null) {
			fMouseListener.uninstall();
				fMouseListener= null;
			}
			
		if (fEncodingSupport != null) {
				fEncodingSupport.dispose();
				fEncodingSupport= null;
		}
		
		if (fSourceViewerDecorationSupport != null) {
			fSourceViewerDecorationSupport.dispose();
			fSourceViewerDecorationSupport= null;
		}
		
		if (fBracketMatcher != null) {
			fBracketMatcher.dispose();
			fBracketMatcher= null;
		}
		
		if (fSelectionHistory != null) {
			fSelectionHistory.dispose();
			fSelectionHistory= null;
		}
				
		super.dispose();
	}
	
	protected void createActions() {
		super.createActions();
		
		ResourceAction resAction= new AddTaskAction(JavaEditorMessages.getResourceBundle(), "AddTask.", this); //$NON-NLS-1$
		resAction.setHelpContextId(IAbstractTextEditorHelpContextIds.ADD_TASK_ACTION);
		resAction.setActionDefinitionId(ITextEditorActionDefinitionIds.ADD_TASK);
		setAction(ITextEditorActionConstants.ADD_TASK, resAction);

		ActionGroup oeg, ovg, jsg;
		fActionGroups= new CompositeActionGroup(new ActionGroup[] {
			oeg= new OpenEditorActionGroup(this),
			ovg= new OpenViewActionGroup(this),
			jsg= new JavaSearchActionGroup(this)
		});
		fContextMenuGroup= new CompositeActionGroup(new ActionGroup[] {oeg, ovg, jsg});
		
		resAction= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc.", this, ISourceViewer.INFORMATION, true); //$NON-NLS-1$
		resAction= new InformationDispatchAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc.", (TextOperationAction) resAction); //$NON-NLS-1$
		resAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		setAction("ShowJavaDoc", resAction); //$NON-NLS-1$
		
		Action action= new GotoMatchingBracketAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);				
		setAction(GotoMatchingBracketAction.GOTO_MATCHING_BRACKET, action);
			
		action= new SearchUsagesInFileAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_REFERENCES);
		setAction(SearchUsagesInFileAction.SHOWREFERENCES, action);
		
		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(),"ShowOutline.", this, JavaSourceViewer.SHOW_OUTLINE, true); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		setAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE, action);

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(),"OpenStructure.", this, JavaSourceViewer.OPEN_STRUCTURE, true); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE);
		setAction(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE, action);
		
		fEncodingSupport= new DefaultEncodingSupport();
		fEncodingSupport.initialize(this);
		
		fSelectionHistory= new SelectionHistory(this);

		action= new StructureSelectEnclosingAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_ENCLOSING);				
		setAction(StructureSelectionAction.ENCLOSING, action);

		action= new StructureSelectNextAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_NEXT);
		setAction(StructureSelectionAction.NEXT, action);

		action= new StructureSelectPreviousAction(this, fSelectionHistory);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_PREVIOUS);
		setAction(StructureSelectionAction.PREVIOUS, action);

		StructureSelectHistoryAction historyAction= new StructureSelectHistoryAction(this, fSelectionHistory);
		historyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST);		
		setAction(StructureSelectionAction.HISTORY, historyAction);
		fSelectionHistory.setHistoryAction(historyAction);
				
		action= GoToNextPreviousMemberAction.newGoToNextMemberAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_NEXT_MEMBER);				
		setAction(GoToNextPreviousMemberAction.NEXT_MEMBER, action);

		action= GoToNextPreviousMemberAction.newGoToPreviousMemberAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);				
		setAction(GoToNextPreviousMemberAction.PREVIOUS_MEMBER, action);
		
		if (fMouseListener == null) {
			fMouseListener= new MouseClickListener();
			fMouseListener.install();	
		}
	}
	
	private boolean isTextSelectionEmpty() {
		ISelection selection= getSelectionProvider().getSelection();
		if (!(selection instanceof ITextSelection))
			return true;
		return ((ITextSelection)selection).getLength() == 0;	
	}
	
	public void updatedTitleImage(Image image) {
		setTitleImage(image);
	}
	
	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		try {			

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			String property= event.getProperty();	
			
			if (PreferenceConstants.EDITOR_TAB_WIDTH.equals(property)) {
				Object value= event.getNewValue();
				if (value instanceof Integer) {
					sourceViewer.getTextWidget().setTabs(((Integer) value).intValue());
				} else if (value instanceof String) {
					sourceViewer.getTextWidget().setTabs(Integer.parseInt((String) value));
				}
				return;
			}
			
			if (OVERVIEW_RULER.equals(property))  {
				if (isOverviewRulerVisible())
					showOverviewRuler();
				else
					hideOverviewRuler();
				return;
			}
			
			if (LINE_NUMBER_RULER.equals(property)) {
				if (isLineNumberRulerVisible())
					showLineNumberRuler();
				else
					hideLineNumberRuler();
				return;
			}
			
			if (fLineNumberRulerColumn != null &&
						(LINE_NUMBER_COLOR.equals(property) || 
						PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(property)  ||
						PREFERENCE_COLOR_BACKGROUND.equals(property))) {
					
					initializeLineNumberRulerColumn(fLineNumberRulerColumn);
			}
				
			if (isJavaEditorHoverProperty(property))
				updateHoverBehavior();
			
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}
	
	private boolean isJavaEditorHoverProperty(String property) {
		return	PreferenceConstants.EDITOR_TEXT_HOVER_MODIFIERS.equals(property);
	}
	
	/**
	 * Shows the line number ruler column.
	 */
	private void showLineNumberRuler() {
		IVerticalRuler v= getVerticalRuler();
		if (v instanceof CompositeRuler) {
			CompositeRuler c= (CompositeRuler) v;
			c.addDecorator(1, createLineNumberRulerColumn());
		}
	}
	
	/**
	 * Hides the line number ruler column.
	 */
	private void hideLineNumberRuler() {
		IVerticalRuler v= getVerticalRuler();
		if (v instanceof CompositeRuler) {
			CompositeRuler c= (CompositeRuler) v;
			c.removeDecorator(1);
		}
	}
	
	/**
	 * Return whether the line number ruler column should be 
	 * visible according to the preference store settings.
	 * @return <code>true</code> if the line numbers should be visible
	 */
	private boolean isLineNumberRulerVisible() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(LINE_NUMBER_RULER);
	}
	
	/**
	 * Returns a segmentation of the line of the given document appropriate for bidi rendering.
	 * The default implementation returns only the string literals of a java code line as segments.
	 * 
	 * @param document the document
	 * @param lineOffset the offset of the line
	 * @return the line's bidi segmentation
	 * @throws BadLocationException in case lineOffset is not valid in document
	 */
	public static int[] getBidiLineSegments(IDocument document, int lineOffset) throws BadLocationException {
			
		IRegion line= document.getLineInformationOfOffset(lineOffset);
		ITypedRegion[] linePartitioning= document.computePartitioning(lineOffset, line.getLength());
		
		List segmentation= new ArrayList();
		for (int i= 0; i < linePartitioning.length; i++) {
			if (JavaPartitionScanner.JAVA_STRING.equals(linePartitioning[i].getType()))
				segmentation.add(linePartitioning[i]);
		}
		
		
		if (segmentation.size() == 0) 
			return null;
			
		int size= segmentation.size();
		int[] segments= new int[size * 2 + 1];
		
		int j= 0;
		for (int i= 0; i < size; i++) {
			ITypedRegion segment= (ITypedRegion) segmentation.get(i);
			
			if (i == 0)
				segments[j++]= 0;
				
			int offset= segment.getOffset() - lineOffset;
			if (offset > segments[j - 1])
				segments[j++]= offset;
				
			if (offset + segment.getLength() >= line.getLength())
				break;
				
			segments[j++]= offset + segment.getLength();
		}
		
		if (j < segments.length) {
			int[] result= new int[j];
			System.arraycopy(segments, 0, result, 0, j);
			segments= result;
		}
		
		return segments;
	}
		
	/**
	 * Returns a segmentation of the given line appropriate for bidi rendering. The default
	 * implementation returns only the string literals of a java code line as segments.
	 * 
	 * @param lineOffset the offset of the line
	 * @param line the content of the line
	 * @return the line's bidi segmentation
	 */
	protected int[] getBidiLineSegments(int widgetLineOffset, String line) {
		IDocumentProvider provider= getDocumentProvider();
		if (provider != null && line != null && line.length() > 0) {
			IDocument document= provider.getDocument(getEditorInput());
			if (document != null)
				try {
					
					int lineOffset;
					
					ISourceViewer sourceViewer= getSourceViewer();
					if (sourceViewer instanceof ITextViewerExtension3) {
						ITextViewerExtension3 extension= (ITextViewerExtension3) sourceViewer;
						lineOffset= extension.widgetOffset2ModelOffset(widgetLineOffset);
					} else {
						IRegion visible= sourceViewer.getVisibleRegion();
						lineOffset= visible.getOffset() + widgetLineOffset;
					}
					
					return getBidiLineSegments(document, lineOffset);
					
				} catch (BadLocationException x) {
					// ignore
				}
		}
		return null;
	}
	
	/*
	 * @see AbstractTextEditor#handleCursorPositionChanged()
	 */
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		if (!isEditingScriptRunning() && fUpdater != null)
			fUpdater.post();
	}
 
	/**
	 * Initializes the given line number ruler column from the preference store.
	 * @param rulerColumn the ruler column to be initialized
	 */
	protected void initializeLineNumberRulerColumn(LineNumberRulerColumn rulerColumn) {
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		IColorManager manager= textTools.getColorManager();	
		
		IPreferenceStore store= getPreferenceStore();
		if (store != null) {
		
			RGB rgb=  null;
			// foreground color
			if (store.contains(LINE_NUMBER_COLOR)) {
				if (store.isDefault(LINE_NUMBER_COLOR))
					rgb= PreferenceConverter.getDefaultColor(store, LINE_NUMBER_COLOR);
				else
					rgb= PreferenceConverter.getColor(store, LINE_NUMBER_COLOR);
			}
			rulerColumn.setForeground(manager.getColor(rgb));
			
			
			rgb= null;
			// background color
			if (!store.getBoolean(PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {
				if (store.contains(PREFERENCE_COLOR_BACKGROUND)) {
					if (store.isDefault(PREFERENCE_COLOR_BACKGROUND))
						rgb= PreferenceConverter.getDefaultColor(store, PREFERENCE_COLOR_BACKGROUND);
					else
						rgb= PreferenceConverter.getColor(store, PREFERENCE_COLOR_BACKGROUND);
				}
			}
			rulerColumn.setBackground(manager.getColor(rgb));
			
			rulerColumn.redraw();
		}
	}
	
	/**
	 * Creates a new line number ruler column that is appropriately initialized.
	 */
	protected IVerticalRulerColumn createLineNumberRulerColumn() {
		fLineNumberRulerColumn= new LineNumberRulerColumn();
		initializeLineNumberRulerColumn(fLineNumberRulerColumn);
		return fLineNumberRulerColumn;
	}
	
	/*
	 * @see AbstractTextEditor#createVerticalRuler()
	 */
	protected IVerticalRuler createVerticalRuler() {
		CompositeRuler ruler= new CompositeRuler();
		ruler.addDecorator(0, new AnnotationRulerColumn(VERTICAL_RULER_WIDTH));
		if (isLineNumberRulerVisible())
			ruler.addDecorator(1, createLineNumberRulerColumn());
		return ruler;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#updatePropertyDependentActions()
	 */
	protected void updatePropertyDependentActions() {
		super.updatePropertyDependentActions();
		if (fEncodingSupport != null)
			fEncodingSupport.reset();
	}

	/*
	 * Update the hovering behavior depending on the preferences.
	 */
	private void updateHoverBehavior() {
		SourceViewerConfiguration configuration= getSourceViewerConfiguration();
		String[] types= configuration.getConfiguredContentTypes(getSourceViewer());

		for (int i= 0; i < types.length; i++) {
			
			String t= types[i];

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer instanceof ITextViewerExtension2) {
				// Remove existing hovers			
				((ITextViewerExtension2)sourceViewer).removeTextHovers(t);
				
				int[] stateMasks= configuration.getConfiguredTextHoverStateMasks(getSourceViewer(), t);

				if (stateMasks != null) {
					for (int j= 0; j < stateMasks.length; j++)	{
						int stateMask= stateMasks[j];
						ITextHover textHover= configuration.getTextHover(sourceViewer, t, stateMask);
						((ITextViewerExtension2)sourceViewer).setTextHover(textHover, t, stateMask);
					}
				} else {
					ITextHover textHover= configuration.getTextHover(sourceViewer, t);
					((ITextViewerExtension2)sourceViewer).setTextHover(textHover, t, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
				}
			} else
				sourceViewer.setTextHover(configuration.getTextHover(sourceViewer, t), t);
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.viewsupport.IViewPartInputProvider#getViewPartInput()
	 */
	public Object getViewPartInput() {
		return getEditorInput().getAdapter(IJavaElement.class);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#doSetSelection(ISelection)
	 */
	protected void doSetSelection(ISelection selection) {
		super.doSetSelection(selection);
		synchronizeOutlinePageSelection();
	}

	/*
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.
	 * widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		fSourceViewerDecorationSupport.install(getPreferenceStore());

		IInformationControlCreator informationControlCreator= new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				boolean cutDown= false;
				int style= cutDown ? SWT.NONE : (SWT.V_SCROLL | SWT.H_SCROLL);
				return new DefaultInformationControl(parent, SWT.RESIZE, style, new HTMLTextPresenter(cutDown));
			}
		};

		fInformationPresenter= new InformationPresenter(informationControlCreator);
		fInformationPresenter.setSizeConstraints(60, 10, true, true);		
		fInformationPresenter.install(getSourceViewer());
	}
	
	protected void showOverviewRuler() {
		if (fOverviewRuler != null) {
			if (getSourceViewer() instanceof ISourceViewerExtension) {
				((ISourceViewerExtension) getSourceViewer()).showAnnotationsOverview(true);
				fSourceViewerDecorationSupport.updateOverviewDecorations();
			}
		}
	}

	protected void hideOverviewRuler() {
		if (getSourceViewer() instanceof ISourceViewerExtension) {
			fSourceViewerDecorationSupport.hideAnnotationOverview();
			((ISourceViewerExtension) getSourceViewer()).showAnnotationsOverview(false);
		}
	}

	protected boolean isOverviewRulerVisible() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(OVERVIEW_RULER);
	}

	protected void configureSourceViewerDecorationSupport() {
	
		fSourceViewerDecorationSupport.setCharacterPairMatcher(fBracketMatcher);
				
		fSourceViewerDecorationSupport.setAnnotationPainterPreferenceKeys(AnnotationType.UNKNOWN, UNKNOWN_INDICATION_COLOR, UNKNOWN_INDICATION, UNKNOWN_INDICATION_IN_OVERVIEW_RULER, 0);
		fSourceViewerDecorationSupport.setAnnotationPainterPreferenceKeys(AnnotationType.BOOKMARK, BOOKMARK_INDICATION_COLOR, BOOKMARK_INDICATION, BOOKMARK_INDICATION_IN_OVERVIEW_RULER, 1);
		fSourceViewerDecorationSupport.setAnnotationPainterPreferenceKeys(AnnotationType.TASK, TASK_INDICATION_COLOR, TASK_INDICATION, TASK_INDICATION_IN_OVERVIEW_RULER, 2);
		fSourceViewerDecorationSupport.setAnnotationPainterPreferenceKeys(AnnotationType.SEARCH, SEARCH_RESULT_INDICATION_COLOR, SEARCH_RESULT_INDICATION, SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER, 3);
		fSourceViewerDecorationSupport.setAnnotationPainterPreferenceKeys(AnnotationType.WARNING, WARNING_INDICATION_COLOR, WARNING_INDICATION, WARNING_INDICATION_IN_OVERVIEW_RULER, 4);
		fSourceViewerDecorationSupport.setAnnotationPainterPreferenceKeys(AnnotationType.ERROR, ERROR_INDICATION_COLOR, ERROR_INDICATION, ERROR_INDICATION_IN_OVERVIEW_RULER, 5);
				
		
		fSourceViewerDecorationSupport.setCursorLinePainterPreferenceKeys(CURRENT_LINE, CURRENT_LINE_COLOR);
		fSourceViewerDecorationSupport.setMarginPainterPreferenceKeys(PRINT_MARGIN, PRINT_MARGIN_COLOR, PRINT_MARGIN_COLUMN);
		fSourceViewerDecorationSupport.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR);		
	}
	
	/**
	 * Jumps to the matching bracket.
	 */
	public void gotoMatchingBracket() {
		
		ISourceViewer sourceViewer= getSourceViewer();
		IDocument document= sourceViewer.getDocument();
		if (document == null)
			return;
		
		IRegion selection= getSignedSelection(sourceViewer);

		int selectionLength= Math.abs(selection.getLength());
		if (selectionLength > 1) {
			setStatusLineErrorMessage(JavaEditorMessages.getString("GotoMatchingBracket.error.invalidSelection"));	//$NON-NLS-1$		
			sourceViewer.getTextWidget().getDisplay().beep();
			return;
		}

		// #26314
		int sourceCaretOffset= selection.getOffset() + selection.getLength();
		if (isSurroundedByBrackets(document, sourceCaretOffset))
			sourceCaretOffset -= selection.getLength();

		IRegion region= fBracketMatcher.match(document, sourceCaretOffset);
		if (region == null) {
			setStatusLineErrorMessage(JavaEditorMessages.getString("GotoMatchingBracket.error.noMatchingBracket"));	//$NON-NLS-1$		
			sourceViewer.getTextWidget().getDisplay().beep();
			return;		
		}
		
		int offset= region.getOffset();
		int length= region.getLength();
		
		if (length < 1)
			return;
			
		int anchor= fBracketMatcher.getAnchor();
		int targetOffset= (JavaPairMatcher.RIGHT == anchor) ? offset : offset + length - 1;
		
		boolean visible= false;
		if (sourceViewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) sourceViewer;
			visible= (extension.modelOffset2WidgetOffset(targetOffset) > -1);
		} else {
			IRegion visibleRegion= sourceViewer.getVisibleRegion();
			visible= (targetOffset >= visibleRegion.getOffset() && targetOffset < visibleRegion.getOffset() + visibleRegion.getLength());
		}
		
		if (!visible) {
			setStatusLineErrorMessage(JavaEditorMessages.getString("GotoMatchingBracket.error.bracketOutsideSelectedElement"));	//$NON-NLS-1$		
			sourceViewer.getTextWidget().getDisplay().beep();
			return;
		}
		
		if (selection.getLength() < 0)
			targetOffset -= selection.getLength();
			
		sourceViewer.setSelectedRange(targetOffset, selection.getLength());
		sourceViewer.revealRange(targetOffset, selection.getLength());
	}

	/**
	 * Ses the given message as error message to this editor's status line.
	 * @param msg message to be set
	 */
	protected void setStatusLineErrorMessage(String msg) {
		IEditorStatusLine statusLine= (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
		if (statusLine != null)
			statusLine.setMessage(true, msg, null);	
	}
	
	private static IRegion getSignedSelection(ITextViewer viewer) {

		StyledText text= viewer.getTextWidget();
		int caretOffset= text.getCaretOffset();
		Point selection= text.getSelection();
		
		// caret left
		int offset, length;
		if (caretOffset == selection.x) {
			offset= selection.y;
			length= selection.x - selection.y;			
			
		// caret right
		} else {
			offset= selection.x;
			length= selection.y - selection.x;			
		}
		
		return new Region(offset, length);
	}
	
	private static boolean isBracket(char character) {
		for (int i= 0; i != BRACKETS.length; ++i)
			if (character == BRACKETS[i])
				return true;
		return false;
	}

	private static boolean isSurroundedByBrackets(IDocument document, int offset) {
		if (offset == 0 || offset == document.getLength())
			return false;

		try {
			return
				isBracket(document.getChar(offset - 1)) &&
				isBracket(document.getChar(offset));
			
		} catch (BadLocationException e) {
			return false;	
		}
	}

	/**
	 * Jumps to the error next according to the given direction.
	 */
	public void gotoError(boolean forward) {
		
		ISelectionProvider provider= getSelectionProvider();
		
		ITextSelection s= (ITextSelection) provider.getSelection();
		Position errorPosition= new Position(0, 0);
		IJavaAnnotation nextError= getNextError(s.getOffset(), forward, errorPosition);
		
		if (nextError != null) {
			
			IMarker marker= null;
			if (nextError instanceof MarkerAnnotation)
				marker= ((MarkerAnnotation) nextError).getMarker();
			else {
				Iterator e= nextError.getOverlaidIterator();
				if (e != null) {
					while (e.hasNext()) {
						Object o= e.next();
						if (o instanceof MarkerAnnotation) {
							marker= ((MarkerAnnotation) o).getMarker();
							break;
						}
					}
				}
			}
			
			if (marker != null) {
				IWorkbenchPage page= getSite().getPage();
				IViewPart view= view= page.findView("org.eclipse.ui.views.TaskList"); //$NON-NLS-1$
				if (view instanceof TaskList) {
					StructuredSelection ss= new StructuredSelection(marker);
					((TaskList) view).setSelection(ss, true);
				}
			}
			
			selectAndReveal(errorPosition.getOffset(), errorPosition.getLength());
			setStatusLineErrorMessage(nextError.getMessage());
			
		} else {
			
			setStatusLineErrorMessage(null);
			
		}
	}

	private IJavaAnnotation getNextError(int offset, boolean forward, Position errorPosition) {
		
		IJavaAnnotation nextError= null;
		Position nextErrorPosition= null;
		
		IDocument document= getDocumentProvider().getDocument(getEditorInput());
		int endOfDocument= document.getLength(); 
		int distance= 0;
		
		IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
		Iterator e= new JavaAnnotationIterator(model, false);
		while (e.hasNext()) {
			
			IJavaAnnotation a= (IJavaAnnotation) e.next();
			if (a.hasOverlay() || !a.isProblem())
				continue;
				
			Position p= model.getPosition((Annotation) a);
			if (!p.includes(offset)) {
				
				int currentDistance= 0;
				
				if (forward) {
					currentDistance= p.getOffset() - offset;
					if (currentDistance < 0)
						currentDistance= endOfDocument - offset + p.getOffset();
				} else {
					currentDistance= offset - p.getOffset();
					if (currentDistance < 0)
						currentDistance= offset + endOfDocument - p.getOffset();
				}						
										
				if (nextError == null || currentDistance < distance) {
					distance= currentDistance;
					nextError= a;
					nextErrorPosition= p;
				}
			}
		}
		
		if (nextErrorPosition != null) {
			errorPosition.setOffset(nextErrorPosition.getOffset());
			errorPosition.setLength(nextErrorPosition.getLength());
		}
		
		return nextError;
	}
}