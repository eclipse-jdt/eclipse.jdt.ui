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
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.AnnotationRulerColumn;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerColumn;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartService;
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
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ResourceAction;
import org.eclipse.ui.texteditor.StatusTextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

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

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.jdt.ui.actions.OpenViewActionGroup;
import org.eclipse.jdt.ui.actions.ShowActionGroup;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.preferences.JavaEditorHoverConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.JavaEditorPreferencePage;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;
import org.eclipse.jdt.internal.ui.util.JavaUIHelp;
import org.eclipse.jdt.internal.ui.viewsupport.IViewPartInputProvider;


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
	
	/**
	 * Link mode.  
	 */
	class MouseClickListener implements KeyListener, MouseListener, MouseMoveListener, MouseTrackListener,
		FocusListener, PaintListener, IPropertyChangeListener, IDocumentListener, ITextInputListener {		

		/** The session is active. */
		private boolean fActive;

		/** The currently active style range. */
		private IRegion fActiveRegion;
		
		/** The hand cursor. */
		private Cursor fCursor;
		
		/** The default cursor. */
		private Cursor fDefaultCursor;
		
		/** The link color. */
		private Color fColor;

		public void deactivate() {
			if (!fActive)
				return;

			repairRepresentation();			
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

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;
				
			StyledText text= sourceViewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;

			sourceViewer.removeTextInputListener(this);

			IDocument document= sourceViewer.getDocument();
			if (document != null)
				document.removeDocumentListener(this);
				
			text.removeKeyListener(this);
			text.removeMouseListener(this);
			text.removeMouseMoveListener(this);
			text.removeFocusListener(this);
			text.removePaintListener(this);
			
			IPreferenceStore preferenceStore= getPreferenceStore();
			preferenceStore.removePropertyChangeListener(this);
			
			if (fColor != null) {
				fColor.dispose();
				fColor= null;
			}
				
			if (fCursor != null) {
				fCursor.dispose();
				fCursor= null;
			}				
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

			if (fActiveRegion == null)
				return;
			
			ISourceViewer viewer= getSourceViewer();
			if (viewer != null) {
				resetCursor(viewer);

				int offset= fActiveRegion.getOffset();
				int length= fActiveRegion.getLength();

				// remove style
				if (viewer instanceof ITextViewerExtension2)
					((ITextViewerExtension2) viewer).invalidateTextPresentation(offset, length);
				else
					viewer.invalidateTextPresentation();

				// remove underline				
				offset -= viewer.getVisibleRegion().getOffset();
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
				
				return text.getOffsetAtLocation(relativePosition) + viewer.getVisibleRegion().getOffset();

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
			int offset= region.getOffset() - viewer.getVisibleRegion().getOffset();
			int length= region.getLength();
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
			if (text == null || text.isDisposed())
				return;
			Display display= text.getDisplay();			
			if (fDefaultCursor == null)
				fDefaultCursor= new Cursor(display, SWT.CURSOR_IBEAM);			
			text.setCursor(fDefaultCursor);
			
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

			if (event.keyCode != SWT.CTRL) {
				deactivate();
				return;
			}
			
			fActive= true;

			ISourceViewer viewer= getSourceViewer();
			if (viewer == null)
				return;
			
			IRegion region= getCurrentTextRegion(viewer);
			if (region == null)
				return;
			
			highlightRegion(viewer, region);
			activateCursor(viewer);												
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
				
			if (event.stateMask != SWT.CTRL) {
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
			
			if (!fActive) {
				if (event.stateMask != SWT.CTRL)
					return;
				// Ctrl was already pressed
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
			if (region == null) {
				repairRepresentation();
				return;
			}
			
			highlightRegion(viewer, region);	
			activateCursor(viewer);												
		}

		/*
		 * @see org.eclipse.swt.events.MouseTrackListener#mouseEnter(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseEnter(MouseEvent e) {}

		/*
		 * @see org.eclipse.swt.events.MouseTrackListener#mouseExit(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseExit(MouseEvent e) {
			repairRepresentation();			
		}

		/*
		 * @see org.eclipse.swt.events.MouseTrackListener#mouseHover(org.eclipse.swt.events.MouseEvent)
		 */
		public void mouseHover(MouseEvent e) {}
		
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
			deactivate();
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
			if (oldInput == null)
				return;
			deactivate();
			uninstall();
		}

		/*
		 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
		 */
		public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
			if (newInput == null)
				return;
			install();
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
				
			IRegion region= viewer.getVisibleRegion();			
			if (!includes(region, fActiveRegion))
			 	return;		    

			StyledText text= viewer.getTextWidget();
			if (text == null || text.isDisposed())
				return;
			
			int offset= fActiveRegion.getOffset() -  region.getOffset();
			int length= fActiveRegion.getLength();
				
			// support for bidi
			Point minLocation= getMinimumLocation(text, offset, length);
			Point maxLocation= getMaximumLocation(text, offset, length);
	
			int x1= minLocation.x;
			int x2= minLocation.x + maxLocation.x - minLocation.x - 1;
			int y= minLocation.y + text.getLineHeight() - 1;
			
			GC gc= event.gc;
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

	}	
	
	/** Preference key for showing the line number ruler */
	public final static String LINE_NUMBER_RULER= "lineNumberRuler"; //$NON-NLS-1$
	/** Preference key for the foreground color of the line numbers */
	public final static String LINE_NUMBER_COLOR= "lineNumberColor"; //$NON-NLS-1$
	/** Preference key for the link color */
	public final static String LINK_COLOR= "linkColor"; //$NON-NLS-1$
	
	/** The outline page */
	protected JavaOutlinePage fOutlinePage;
	/** Outliner context menu Id */
	protected String fOutlinerContextMenuId;
	/** The selection changed listener */
	protected ISelectionChangedListener fSelectionChangedListener= new SelectionChangedListener();
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
		
		if (JavaEditorPreferencePage.synchronizeOutlineOnCursorMove())
			fUpdater= new OutlinePageSelectionUpdater();
	}
	
	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected final ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		ISourceViewer viewer= createJavaSourceViewer(parent, ruler, styles);
		StyledText text= viewer.getTextWidget();
		text.addBidiSegmentListener(new  BidiSegmentListener() {
			public void lineGetSegments(BidiSegmentEvent event) {
				event.segments= getBidiLineSegments(event.lineOffset, event.lineText);
			}
		});
		JavaUIHelp.setHelp(this, text, IJavaHelpContextIds.JAVA_EDITOR);
		return viewer;
	}
	
	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
			return super.createSourceViewer(parent, ruler, styles);
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
		
		int offset= sourceViewer.getVisibleRegion().getOffset();
		int caret= offset + styledText.getCaretOffset();
		
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
		super.dispose();
	}
	
	protected void createActions() {
		super.createActions();
		
		ResourceAction action= new AddTaskAction(JavaEditorMessages.getResourceBundle(), "AddTask.", this); //$NON-NLS-1$
		action.setHelpContextId(IAbstractTextEditorHelpContextIds.ADD_TASK_ACTION);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.ADD_TASK);
		setAction(ITextEditorActionConstants.ADD_TASK, action);

		ActionGroup oeg, ovg, sg, jsg;
		fActionGroups= new CompositeActionGroup(new ActionGroup[] {
			oeg= new OpenEditorActionGroup(this),
			ovg= new OpenViewActionGroup(this),
			sg= new ShowActionGroup(this),
			jsg= new JavaSearchActionGroup(this)
		});
		fContextMenuGroup= new CompositeActionGroup(new ActionGroup[] {oeg, ovg, sg, jsg});
		
		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc.", this, ISourceViewer.INFORMATION, true); //$NON-NLS-1$				
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_JAVADOC);
		setAction("ShowJavaDoc", action); //$NON-NLS-1$
	
		fEncodingSupport= new DefaultEncodingSupport();
		fEncodingSupport.initialize(this);
		
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
			
			if (JavaSourceViewerConfiguration.PREFERENCE_TAB_WIDTH.equals(property)) {
				Object value= event.getNewValue();
				if (value instanceof Integer) {
					sourceViewer.getTextWidget().setTabs(((Integer) value).intValue());
				} else if (value instanceof String) {
					sourceViewer.getTextWidget().setTabs(Integer.parseInt((String) value));
				}
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

			if (PreferenceConstants.EDITOR_SHOW_HOVER.equals(property)) {
				updateHoverBehavior();
			}

			if (JavaEditorHoverConfigurationBlock.isAffectedBy(property)) {
				updateHoverBehavior();
			}				
			
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
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
	protected int[] getBidiLineSegments(int lineOffset, String line) {
		IDocumentProvider provider= getDocumentProvider();
		if (provider != null) {
			IDocument document= provider.getDocument(getEditorInput());
			if (document != null)
				try {
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
				
			int[] stateMasks= configuration.getConfiguredTextHoverStateMasks(getSourceViewer(), t);

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer instanceof ITextViewerExtension2) {
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

}