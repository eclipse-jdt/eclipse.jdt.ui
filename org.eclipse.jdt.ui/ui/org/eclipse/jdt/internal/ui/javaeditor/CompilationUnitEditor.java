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


import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.tasklist.TaskList;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.compare.LocalHistoryActionGroup;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectEnclosingAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectNextAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectPreviousAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;
import org.eclipse.jdt.internal.ui.preferences.JavaEditorPreferencePage;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.JavaPairMatcher;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionSourceViewer;
import org.eclipse.jdt.internal.ui.text.java.IReconcilingParticipant;
import org.eclipse.jdt.internal.ui.text.java.SmartBracesAutoEditStrategy;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitFlags;



/**
 * Java specific text editor.
 */
public class CompilationUnitEditor extends JavaEditor implements IReconcilingParticipant {
	
	
	interface ITextConverter {
		void customizeDocumentCommand(IDocument document, DocumentCommand command);
	};
	
	
	class AdaptedRulerLayout extends Layout {
		
		protected int fGap;
		protected AdaptedSourceViewer fAdaptedSourceViewer;
		
		
		protected AdaptedRulerLayout(int gap, AdaptedSourceViewer asv) {
			fGap= gap;
			fAdaptedSourceViewer= asv;
		}
		
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			Control[] children= composite.getChildren();
			Point s= children[children.length - 1].computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
			if (fAdaptedSourceViewer.isVerticalRulerVisible())
				s.x += fAdaptedSourceViewer.getVerticalRuler().getWidth() + fGap;
			return s;
		}
		
		protected void layout(Composite composite, boolean flushCache) {
			Rectangle clArea= composite.getClientArea();
			if (fAdaptedSourceViewer.isVerticalRulerVisible()) {
				
				StyledText textWidget= fAdaptedSourceViewer.getTextWidget();
				Rectangle trim= textWidget.computeTrim(0, 0, 0, 0);
				int scrollbarHeight= trim.height;
				
				IVerticalRuler vr= fAdaptedSourceViewer.getVerticalRuler();
				int vrWidth=vr.getWidth();
				
				int orWidth= 0;
				if (fAdaptedSourceViewer.isOverviewRulerVisible()) {
					OverviewRuler or= fAdaptedSourceViewer.getOverviewRuler();
					orWidth= or.getWidth();
					or.getControl().setBounds(clArea.width - orWidth, scrollbarHeight, orWidth, clArea.height - 3*scrollbarHeight);
				}
				
				textWidget.setBounds(vrWidth + fGap, 0, clArea.width - vrWidth - orWidth - 2*fGap, clArea.height);
				vr.getControl().setBounds(0, 0, vrWidth, clArea.height - scrollbarHeight);
				
			} else {
				StyledText textWidget= fAdaptedSourceViewer.getTextWidget();
				textWidget.setBounds(0, 0, clArea.width, clArea.height);
			}
		}
	};

	
	class AdaptedSourceViewer extends JavaCorrectionSourceViewer  {
		
		private List fTextConverters;
		private OverviewRuler fOverviewRuler;
		private boolean fIsOverviewRulerVisible;
		private boolean fIgnoreTextConverters= false;
		
		private IVerticalRuler fCachedVerticalRuler;
		private boolean fCachedIsVerticalRulerVisible;
		
		
		public AdaptedSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
			super(parent, ruler, styles, CompilationUnitEditor.this);
			
			fCachedVerticalRuler= ruler;
			fCachedIsVerticalRulerVisible= (ruler != null);
			fOverviewRuler= new OverviewRuler(VERTICAL_RULER_WIDTH);
			
			delayedCreateControl(parent, styles);
		}
		
		/*
		 * @see ISourceViewer#showAnnotations(boolean)
		 */
		public void showAnnotations(boolean show) {
			fCachedIsVerticalRulerVisible= (show && fCachedVerticalRuler != null);
			super.showAnnotations(show);
		}
		
		public IContentAssistant getContentAssistant() {
			return fContentAssistant;
		}
		
		/*
		 * @see ITextOperationTarget#doOperation(int)
		 */
		public void doOperation(int operation) {
		
			if (getTextWidget() == null)
				return;
			
			switch (operation) {
				case CONTENTASSIST_PROPOSALS:
					String msg= fContentAssistant.showPossibleCompletions();
					setStatusLineErrorMessage(msg);
					return;
				case UNDO:
					fIgnoreTextConverters= true;
					break;
				case REDO:
					fIgnoreTextConverters= true;
					break;
			}
			
			super.doOperation(operation);
		}
		
		public void insertTextConverter(ITextConverter textConverter, int index) {
			throw new UnsupportedOperationException();
		}
		
		public void addTextConverter(ITextConverter textConverter) {
			if (fTextConverters == null) {
				fTextConverters= new ArrayList(1);
				fTextConverters.add(textConverter);
			} else if (!fTextConverters.contains(textConverter))
				fTextConverters.add(textConverter);
		}
		
		public void removeTextConverter(ITextConverter textConverter) {
			if (fTextConverters != null) {
				fTextConverters.remove(textConverter);
				if (fTextConverters.size() == 0)
					fTextConverters= null;
			}
		}
		
		/*
		 * @see TextViewer#customizeDocumentCommand(DocumentCommand)
		 */
		protected void customizeDocumentCommand(DocumentCommand command) {
			super.customizeDocumentCommand(command);
			if (!fIgnoreTextConverters && fTextConverters != null) {
				for (Iterator e = fTextConverters.iterator(); e.hasNext();)
					((ITextConverter) e.next()).customizeDocumentCommand(getDocument(), command);
			}
			fIgnoreTextConverters= false;
		}
		
		public IVerticalRuler getVerticalRuler() {
			return fCachedVerticalRuler;
		}
		
		public boolean isVerticalRulerVisible() {
			return fCachedIsVerticalRulerVisible;
		}
		
		public OverviewRuler getOverviewRuler() {
			return fOverviewRuler;
		}
		
		/*
		 * @see TextViewer#createControl(Composite, int)
		 */
		protected void createControl(Composite parent, int styles) {
			// do nothing here
		}
		
		protected void delayedCreateControl(Composite parent, int styles) {
			//create the viewer
			super.createControl(parent, styles);
			
			Control control= getControl();
			if (control instanceof Composite) {
				Composite composite= (Composite) control;
				composite.setLayout(new AdaptedRulerLayout(GAP_SIZE, this));
				fOverviewRuler.createControl(composite, this);
			}
		}
		
		public void hideOverviewRuler() {
			fIsOverviewRulerVisible= false;
			Control control= getControl();
			if (control instanceof Composite) {
				Composite composite= (Composite) control;
				composite.layout();
			}
		}
		
		public void showOverviewRuler() {
			fIsOverviewRulerVisible= true;
			Control control= getControl();
			if (control instanceof Composite) {
				Composite composite= (Composite) control;
				composite.layout();
			}
		}
		
		public boolean isOverviewRulerVisible() {
			return fIsOverviewRulerVisible;
		}
		
		/*
		 * @see ISourceViewer#setDocument(IDocument, IAnnotationModel, int, int)
		 */
		public void setDocument(IDocument document, IAnnotationModel annotationModel, int visibleRegionOffset, int visibleRegionLength) {
			super.setDocument(document, annotationModel, visibleRegionOffset, visibleRegionLength);
			fOverviewRuler.setModel(annotationModel);
		}
		
		// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
		public void updateIndentationPrefixes() {
			SourceViewerConfiguration configuration= getSourceViewerConfiguration();
			String[] types= configuration.getConfiguredContentTypes(this);
			for (int i= 0; i < types.length; i++) {
				String[] prefixes= configuration.getIndentPrefixes(this, types[i]);
				if (prefixes != null && prefixes.length > 0)
					setIndentPrefixes(prefixes, types[i]);
			}
		}
		
		/*
		 * @see IWidgetTokenOwner#requestWidgetToken(IWidgetTokenKeeper)
		 */
		public boolean requestWidgetToken(IWidgetTokenKeeper requester) {
			if (WorkbenchHelp.isContextHelpDisplayed())
				return false;
			return super.requestWidgetToken(requester);
		}

		/*
		 * @see org.eclipse.jface.text.source.ISourceViewer#configure(org.eclipse.jface.text.source.SourceViewerConfiguration)
		 */
		public void configure(SourceViewerConfiguration configuration) {
			super.configure(configuration);
			prependAutoEditStrategy(new SmartBracesAutoEditStrategy(this), IDocument.DEFAULT_CONTENT_TYPE);
		}

	};
	
	static class TabConverter implements ITextConverter {
		
		private int fTabRatio;
		private ILineTracker fLineTracker;
		
		public TabConverter() {
		} 
		
		public void setNumberOfSpacesPerTab(int ratio) {
			fTabRatio= ratio;
		}
		
		public void setLineTracker(ILineTracker lineTracker) {
			fLineTracker= lineTracker;
		}
		
		private int insertTabString(StringBuffer buffer, int offsetInLine) {
			
			if (fTabRatio == 0)
				return 0;
				
			int remainder= offsetInLine % fTabRatio;
			remainder= fTabRatio - remainder;
			for (int i= 0; i < remainder; i++)
				buffer.append(' ');
			return remainder;
		}
		
		public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
			String text= command.text;
			if (text == null)
				return;
				
			int index= text.indexOf('\t');
			if (index > -1) {
				
				StringBuffer buffer= new StringBuffer();
				
				fLineTracker.set(command.text);
				int lines= fLineTracker.getNumberOfLines();
				
				try {
						
						for (int i= 0; i < lines; i++) {
							
							int offset= fLineTracker.getLineOffset(i);
							int endOffset= offset + fLineTracker.getLineLength(i);
							String line= text.substring(offset, endOffset);
							
							int position= 0;
							if (i == 0) {
								IRegion firstLine= document.getLineInformationOfOffset(command.offset);
								position= command.offset - firstLine.getOffset();	
							}
							
							int length= line.length();
							for (int j= 0; j < length; j++) {
								char c= line.charAt(j);
								if (c == '\t') {
									position += insertTabString(buffer, position);
								} else {
									buffer.append(c);
									++ position;
								}
							}
							
						}
						
						command.text= buffer.toString();
						
				} catch (BadLocationException x) {
				}
			}
		}
	};
	
	private class PropertyChangeListener implements IPropertyChangeListener {		
		/*
		 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
		 */
		public void propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent event) {
			handlePreferencePropertyChanged(event);
		}
	}
	
	/* Preference key for code formatter tab size */
	private final static String CODE_FORMATTER_TAB_SIZE= JavaCore.FORMATTER_TAB_SIZE;
	/** Preference key for matching brackets */
	private final static String MATCHING_BRACKETS=  PreferenceConstants.EDITOR_MATCHING_BRACKETS;
	/** Preference key for matching brackets color */
	private final static String MATCHING_BRACKETS_COLOR=  PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;
	/** Preference key for highlighting current line */
	private final static String CURRENT_LINE= PreferenceConstants.EDITOR_CURRENT_LINE;
	/** Preference key for highlight color of current line */
	private final static String CURRENT_LINE_COLOR= PreferenceConstants.EDITOR_CURRENT_LINE_COLOR;
	/** Preference key for showing print marging ruler */
	private final static String PRINT_MARGIN= PreferenceConstants.EDITOR_PRINT_MARGIN;
	/** Preference key for print margin ruler color */
	private final static String PRINT_MARGIN_COLOR= PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR;
	/** Preference key for print margin ruler column */
	private final static String PRINT_MARGIN_COLUMN= PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN;
	/** Preference key for inserting spaces rather than tabs */
	private final static String SPACES_FOR_TABS= PreferenceConstants.EDITOR_SPACES_FOR_TABS;
	/** Preference key for error indication */
	private final static String ERROR_INDICATION= PreferenceConstants.EDITOR_PROBLEM_INDICATION;
	/** Preference key for error color */
	private final static String ERROR_INDICATION_COLOR= PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR;
	/** Preference key for warning indication */
	private final static String WARNING_INDICATION= PreferenceConstants.EDITOR_WARNING_INDICATION;
	/** Preference key for warning color */
	private final static String WARNING_INDICATION_COLOR= PreferenceConstants.EDITOR_WARNING_INDICATION_COLOR;
	/** Preference key for task indication */
	private final static String TASK_INDICATION= PreferenceConstants.EDITOR_TASK_INDICATION;
	/** Preference key for task color */
	private final static String TASK_INDICATION_COLOR= PreferenceConstants.EDITOR_TASK_INDICATION_COLOR;
	/** Preference key for bookmark indication */
	private final static String BOOKMARK_INDICATION= PreferenceConstants.EDITOR_BOOKMARK_INDICATION;
	/** Preference key for bookmark color */
	private final static String BOOKMARK_INDICATION_COLOR= PreferenceConstants.EDITOR_BOOKMARK_INDICATION_COLOR;
	/** Preference key for search result indication */
	private final static String SEARCH_RESULT_INDICATION= PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION;
	/** Preference key for search result color */
	private final static String SEARCH_RESULT_INDICATION_COLOR= PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_COLOR;
	/** Preference key for unknown annotation indication */
	private final static String UNKNOWN_INDICATION= PreferenceConstants.EDITOR_UNKNOWN_INDICATION;
	/** Preference key for unknown annotation color */
	private final static String UNKNOWN_INDICATION_COLOR= PreferenceConstants.EDITOR_UNKNOWN_INDICATION_COLOR;
	/** Preference key for linked position color */
	private final static String LINKED_POSITION_COLOR= PreferenceConstants.EDITOR_LINKED_POSITION_COLOR;
	/** Preference key for shwoing the overview ruler */
	private final static String OVERVIEW_RULER= PreferenceConstants.EDITOR_OVERVIEW_RULER;
	/** Preference key for error indication in overview ruler */
	private final static String ERROR_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for warning indication in overview ruler */
	private final static String WARNING_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for task indication in overview ruler */
	private final static String TASK_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for bookmark indication in overview ruler */
	private final static String BOOKMARK_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for search result indication in overview ruler */
	private final static String SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for unknown annotation indication in overview ruler */
	private final static String UNKNOWN_INDICATION_IN_OVERVIEW_RULER= PreferenceConstants.EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER;
	/** Preference key for automatically closing strings */
	private final static String CLOSE_STRINGS= PreferenceConstants.EDITOR_CLOSE_STRINGS;
	/** Preference key for automatically wrapping Java strings */
	private final static String WRAP_STRINGS= PreferenceConstants.EDITOR_WRAP_STRINGS;
	/** Preference key for automatically closing brackets and parenthesis */
	private final static String CLOSE_BRACKETS= PreferenceConstants.EDITOR_CLOSE_BRACKETS;
	/** Preference key for automatically closing javadocs and comments */
	private final static String CLOSE_JAVADOCS= PreferenceConstants.EDITOR_CLOSE_JAVADOCS;
	/** Preference key for automatically adding javadoc tags */
	private final static String ADD_JAVADOC_TAGS= PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS;
	/** Preference key for automatically formatting javadocs */
	private final static String FORMAT_JAVADOCS= PreferenceConstants.EDITOR_FORMAT_JAVADOCS;
	/** Preference key for smart paste */
	private final static String SMART_PASTE= PreferenceConstants.EDITOR_SMART_PASTE;
	
	
	private final static class AnnotationInfo {
		public String fColorPreference;
		public String fOverviewRulerPreference;
		public String fEditorPreference;
	};
	
	private final static Map ANNOTATION_MAP;
	static {
		
		AnnotationInfo info;
		ANNOTATION_MAP= new HashMap();

		info= new AnnotationInfo();
		info.fColorPreference= TASK_INDICATION_COLOR;
		info.fOverviewRulerPreference= TASK_INDICATION_IN_OVERVIEW_RULER;
		info.fEditorPreference= TASK_INDICATION;
		ANNOTATION_MAP.put(AnnotationType.TASK, info);
		
		info= new AnnotationInfo();
		info.fColorPreference= ERROR_INDICATION_COLOR;
		info.fOverviewRulerPreference= ERROR_INDICATION_IN_OVERVIEW_RULER;
		info.fEditorPreference= ERROR_INDICATION;
		ANNOTATION_MAP.put(AnnotationType.ERROR, info);
		
		info= new AnnotationInfo();
		info.fColorPreference= WARNING_INDICATION_COLOR;
		info.fOverviewRulerPreference= WARNING_INDICATION_IN_OVERVIEW_RULER;
		info.fEditorPreference= WARNING_INDICATION;
		ANNOTATION_MAP.put(AnnotationType.WARNING, info);
		
		info= new AnnotationInfo();
		info.fColorPreference= BOOKMARK_INDICATION_COLOR;
		info.fOverviewRulerPreference=  BOOKMARK_INDICATION_IN_OVERVIEW_RULER;
		info.fEditorPreference=  BOOKMARK_INDICATION;
		ANNOTATION_MAP.put(AnnotationType.BOOKMARK, info);
		
		info= new AnnotationInfo();
		info.fColorPreference= SEARCH_RESULT_INDICATION_COLOR;
		info.fOverviewRulerPreference=  SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER;
		info.fEditorPreference=  SEARCH_RESULT_INDICATION;
		ANNOTATION_MAP.put(AnnotationType.SEARCH_RESULT, info);
		
		info= new AnnotationInfo();
		info.fColorPreference= UNKNOWN_INDICATION_COLOR;
		info.fOverviewRulerPreference=  UNKNOWN_INDICATION_IN_OVERVIEW_RULER;
		info.fEditorPreference=  UNKNOWN_INDICATION;
		ANNOTATION_MAP.put(AnnotationType.UNKNOWN, info);
	};
	
	private final static AnnotationType[] ANNOTATION_LAYERS= new AnnotationType[] {
		AnnotationType.UNKNOWN,
		AnnotationType.BOOKMARK,
		AnnotationType.TASK,
		AnnotationType.SEARCH_RESULT,
		AnnotationType.WARNING,
		AnnotationType.ERROR
	};
		
	
	/** The editor's save policy */
	protected ISavePolicy fSavePolicy;
	/** Listener to annotation model changes that updates the error tick in the tab image */
	private JavaEditorErrorTickUpdater fJavaEditorErrorTickUpdater;
	/** The editor's paint manager */
	private PaintManager fPaintManager;
	/** The editor's bracket painter */
	private BracketPainter fBracketPainter;
	/** The editor's bracket matcher */
	private JavaPairMatcher fBracketMatcher;
	/** The editor's line painter */
	private LinePainter fLinePainter;
	/** The editor's print margin ruler painter */
	private PrintMarginPainter fPrintMarginPainter;
	/** The editor's problem painter */
	private ProblemPainter fProblemPainter;
	/** The editor's tab converter */
	private TabConverter fTabConverter;
	/** History for structure select action */
	private SelectionHistory fSelectionHistory;
	/** The preference property change listener for java core. */
	private IPropertyChangeListener fPropertyChangeListener= new PropertyChangeListener();
	/** The remembered java element */
	private IJavaElement fRememberedElement;
	/** The remembered selection */
	private ITextSelection fRememberedSelection;
	/** The remembered java element offset */
	private int fRememberedElementOffset;
	/** The bracket inserter. */
	private BracketInserter fBracketInserter= new BracketInserter();
	
	/** The standard action groups added to the menu */
	private GenerateActionGroup fGenerateActionGroup;
	private CompositeActionGroup fContextMenuGroup;
	
	/**
	 * Creates a new compilation unit editor.
	 */
	public CompilationUnitEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
		setEditorContextMenuId("#CompilationUnitEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#CompilationUnitRulerContext"); //$NON-NLS-1$
		setOutlinerContextMenuId("#CompilationUnitOutlinerContext"); //$NON-NLS-1$
		// don't set help contextId, we install our own help context
		fSavePolicy= null;
			
		fJavaEditorErrorTickUpdater= new JavaEditorErrorTickUpdater(this);
	}
	
	/*
	 * @see AbstractTextEditor#createActions()
	 */
	protected void createActions() {
		
		super.createActions();

		Action action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "CorrectionAssistProposal.", this, JavaCorrectionSourceViewer.CORRECTIONASSIST_PROPOSALS); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.CORRECTION_ASSIST_PROPOSALS);		
		setAction("CorrectionAssistProposal", action); //$NON-NLS-1$

		action= new ContentAssistAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal.", this); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);		
		setAction("ContentAssistProposal", action); //$NON-NLS-1$

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistContextInformation.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);	//$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);		
		setAction("ContentAssistContextInformation", action); //$NON-NLS-1$

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Comment.", this, ITextOperationTarget.PREFIX); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.COMMENT);		
		setAction("Comment", action); //$NON-NLS-1$

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Uncomment.", this, ITextOperationTarget.STRIP_PREFIX); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.UNCOMMENT);		
		setAction("Uncomment", action); //$NON-NLS-1$

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Format.", this, ISourceViewer.FORMAT); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);		
		setAction("Format", action); //$NON-NLS-1$

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(),"ShowOutline.", this, JavaCorrectionSourceViewer.SHOW_OUTLINE); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_OUTLINE);
		setAction(IJavaEditorActionDefinitionIds.SHOW_OUTLINE, action);

		action= new TextOperationAction(JavaEditorMessages.getResourceBundle(),"OpenStructure.", this, JavaCorrectionSourceViewer.OPEN_STRUCTURE); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE);
		setAction(IJavaEditorActionDefinitionIds.OPEN_STRUCTURE, action);

		markAsStateDependentAction("CorrectionAssistProposal", true); //$NON-NLS-1$
		markAsStateDependentAction("ContentAssistProposal", true); //$NON-NLS-1$
		markAsStateDependentAction("ContentAssistContextInformation", true); //$NON-NLS-1$
		markAsStateDependentAction("Comment", true); //$NON-NLS-1$
		markAsStateDependentAction("Uncomment", true); //$NON-NLS-1$
		markAsStateDependentAction("Format", true); //$NON-NLS-1$

		action= new GotoMatchingBracketAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_MATCHING_BRACKET);				
		setAction(GotoMatchingBracketAction.GOTO_MATCHING_BRACKET, action);

		action= GoToNextPreviousMemberAction.newGoToNextMemberAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_NEXT_MEMBER);				
		setAction(GoToNextPreviousMemberAction.NEXT_MEMBER, action);

		action= GoToNextPreviousMemberAction.newGoToPreviousMemberAction(this);
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.GOTO_PREVIOUS_MEMBER);				
		setAction(GoToNextPreviousMemberAction.PREVIOUS_MEMBER, action);
		
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

		fGenerateActionGroup= new GenerateActionGroup(this, ITextEditorActionConstants.GROUP_EDIT);
		ActionGroup rg= new RefactorActionGroup(this, ITextEditorActionConstants.GROUP_EDIT);
		
		fActionGroups.addGroup(rg);
		fActionGroups.addGroup(fGenerateActionGroup);
		
		// We have to keep the context menu group separate to have better control over positioning
		fContextMenuGroup= new CompositeActionGroup(new ActionGroup[] {
			fGenerateActionGroup, 
			rg, 
			new LocalHistoryActionGroup(this, ITextEditorActionConstants.GROUP_EDIT)});
	}

	/*
	 * @see JavaEditor#getElementAt(int)
	 */
	protected IJavaElement getElementAt(int offset) {
		return getElementAt(offset, true);
	}
	
	/**
	 * Returns the most narrow element including the given offset.  If <code>reconcile</code>
	 * is <code>true</code> the editor's input element is reconciled in advance. If it is 
	 * <code>false</code> this method only returns a result if the editor's input element
	 * does not need to be reconciled.
	 * 
	 * @param offset the offset included by the retrieved element
	 * @param reconcile <code>true</code> if working copy should be reconciled
	 */
	protected IJavaElement getElementAt(int offset, boolean reconcile) {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
		
		if (unit != null) {
			try {
				if (reconcile) {
					synchronized (unit) {
						unit.reconcile();
					}
					return unit.getElementAt(offset);
				} else if (unit.isConsistent())
					return unit.getElementAt(offset);
					
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
				// nothing found, be tolerant and go on
			}
		}
		
		return null;
	}
	
	/*
	 * @see JavaEditor#getCorrespondingElement(IJavaElement)
	 */
	protected IJavaElement getCorrespondingElement(IJavaElement element) {
		try {
			return EditorUtility.getWorkingCopy(element, true);
		} catch (JavaModelException x) {
			JavaPlugin.log(x.getStatus());
			// nothing found, be tolerant and go on
		}
		return null;
	}
	
	/*
	 * @see AbstractTextEditor#editorContextMenuAboutToShow(IMenuManager)
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);		
		
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, "Format"); //$NON-NLS-1$
				
		ActionContext context= new ActionContext(getSelectionProvider().getSelection());
		fContextMenuGroup.setContext(context);
		fContextMenuGroup.fillContextMenu(menu);
		fContextMenuGroup.setContext(null);
	}
	
	/*
	 * @see JavaEditor#setOutlinePageInput(JavaOutlinePage, IEditorInput)
	 */
	protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input) {
		if (page != null) {
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
			page.setInput(manager.getWorkingCopy(input));
		}
	}
	
	/*
	 * @see AbstractTextEditor#performSaveOperation(WorkspaceModifyOperation, IProgressMonitor)
	 */
	protected void performSaveOperation(WorkspaceModifyOperation operation, IProgressMonitor progressMonitor) {
		IDocumentProvider p= getDocumentProvider();
		if (p instanceof CompilationUnitDocumentProvider) {
			CompilationUnitDocumentProvider cp= (CompilationUnitDocumentProvider) p;
			cp.setSavePolicy(fSavePolicy);
		}
		
		try {
			super.performSaveOperation(operation, progressMonitor);
		} finally {
			if (p instanceof CompilationUnitDocumentProvider) {
				CompilationUnitDocumentProvider cp= (CompilationUnitDocumentProvider) p;
				cp.setSavePolicy(null);
			}
		}
	}
	
	/*
	 * @see AbstractTextEditor#doSaveAs
	 */
	public void doSaveAs() {
		if (askIfNonWorkbenchEncodingIsOk()) {
			super.doSaveAs();
		}
	}

	/*
	 * @see AbstractTextEditor#doSave(IProgressMonitor)
	 */
	public void doSave(IProgressMonitor progressMonitor) {
		
		IDocumentProvider p= getDocumentProvider();
		if (p == null) {
			// editor has been closed
			return;
		}
			
		if (!askIfNonWorkbenchEncodingIsOk()) {
			progressMonitor.setCanceled(true);
			return;
		}
		
		if (p.isDeleted(getEditorInput())) {
			
			if (isSaveAsAllowed()) {
				
				/*
				 * 1GEUSSR: ITPUI:ALL - User should never loose changes made in the editors.
				 * Changed Behavior to make sure that if called inside a regular save (because
				 * of deletion of input element) there is a way to report back to the caller.
				 */
				 performSaveAs(progressMonitor);
			
			} else {
				
				/* 
				 * 1GF5YOX: ITPJUI:ALL - Save of delete file claims it's still there
				 * Missing resources.
				 */
				Shell shell= getSite().getShell();
				MessageDialog.openError(shell, JavaEditorMessages.getString("CompilationUnitEditor.error.saving.title1"), JavaEditorMessages.getString("CompilationUnitEditor.error.saving.message1")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		} else {	
			
			setStatusLineErrorMessage(null);
			
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
			ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
			
			if (unit != null) {
				synchronized (unit) { 
					performSaveOperation(createSaveOperation(false), progressMonitor); 
				}
			} else 
				performSaveOperation(createSaveOperation(false), progressMonitor);
		}
	}
	
	/**
	 * Asks the user if it is ok to store in non-workbench encoding.
	 * @return <true> if the user wants to continue
	 */
	private boolean askIfNonWorkbenchEncodingIsOk() {
		IDocumentProvider provider= getDocumentProvider();
		if (provider instanceof IStorageDocumentProvider) {
			IEditorInput input= getEditorInput();
			IStorageDocumentProvider storageProvider= (IStorageDocumentProvider)provider;
			String encoding= storageProvider.getEncoding(input);
			String defaultEncoding= storageProvider.getDefaultEncoding();
			if (encoding != null && !encoding.equals(defaultEncoding)) {
				Shell shell= getSite().getShell();
				String title= JavaEditorMessages.getString("CompilationUnitEditor.warning.save.nonWorkbenchEncoding.title"); //$NON-NLS-1$
				String msg;
				if (input != null)
					msg= MessageFormat.format(JavaEditorMessages.getString("CompilationUnitEditor.warning.save.nonWorkbenchEncoding.message1"), new String[] {input.getName(), encoding});//$NON-NLS-1$
				else
					msg= MessageFormat.format(JavaEditorMessages.getString("CompilationUnitEditor.warning.save.nonWorkbenchEncoding.message2"), new String[] {encoding});//$NON-NLS-1$
				return MessageDialog.openQuestion(shell, title, msg);
			}
		}
		return true;
	}
	
	/**
	 * Jumps to the error next according to the given direction.
	 */
	public void gotoError(boolean forward) {
		
		ISelectionProvider provider= getSelectionProvider();
		
		ITextSelection s= (ITextSelection) provider.getSelection();
		Position errorPosition= new Position(0, 0);
		IProblemAnnotation nextError= getNextError(s.getOffset(), forward, errorPosition);
		
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

	private final static char[] BRACKETS= { '{', '}', '(', ')', '[', ']' };

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
	 * Jumps to the matching bracket.
	 */
	public void gotoMatchingBracket() {

		if (fBracketMatcher == null)
			fBracketMatcher= new JavaPairMatcher(BRACKETS);

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
	
	private IProblemAnnotation getNextError(int offset, boolean forward, Position errorPosition) {
		
		IProblemAnnotation nextError= null;
		Position nextErrorPosition= null;
		
		IDocument document= getDocumentProvider().getDocument(getEditorInput());
		int endOfDocument= document.getLength(); 
		int distance= 0;
		
		IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
		Iterator e= new ProblemAnnotationIterator(model, false);
		while (e.hasNext()) {
			
			IProblemAnnotation a= (IProblemAnnotation) e.next();
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
	
	/*
	 * @see AbstractTextEditor#isSaveAsAllowed() 
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	/**
	 * The compilation unit editor implementation of this  <code>AbstractTextEditor</code>
	 * method asks the user for the workspace path of a file resource and saves the document
	 * there. See http://dev.eclipse.org/bugs/show_bug.cgi?id=6295
	 */
	protected void performSaveAs(IProgressMonitor progressMonitor) {
		
		Shell shell= getSite().getShell();
		IEditorInput input = getEditorInput();
		
		SaveAsDialog dialog= new SaveAsDialog(shell);
		
		IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		if (original != null)
			dialog.setOriginalFile(original);
			
		dialog.create();
		
			
		IDocumentProvider provider= getDocumentProvider();
		if (provider == null) {
			// editor has been programmatically closed while the dialog was open
			return;
		}
		
		if (provider.isDeleted(input) && original != null) {
			String message= JavaEditorMessages.getFormattedString("CompilationUnitEditor.warning.save.delete", new Object[] { original.getName() }); //$NON-NLS-1$
			dialog.setErrorMessage(null);
			dialog.setMessage(message, IMessageProvider.WARNING);
		}
			
		if (dialog.open() == Dialog.CANCEL) {
			if (progressMonitor != null)
				progressMonitor.setCanceled(true);
			return;
		}
			
		IPath filePath= dialog.getResult();
		if (filePath == null) {
			if (progressMonitor != null)
				progressMonitor.setCanceled(true);
			return;
		}
			
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IFile file= workspace.getRoot().getFile(filePath);
		final IEditorInput newInput= new FileEditorInput(file);
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(final IProgressMonitor monitor) throws CoreException {
				getDocumentProvider().saveDocument(monitor, newInput, getDocumentProvider().getDocument(getEditorInput()), true);
			}
		};
		
		boolean success= false;
		try {
			
			provider.aboutToChange(newInput);
			new ProgressMonitorDialog(shell).run(false, true, op);
			success= true;
			
		} catch (InterruptedException x) {
		} catch (InvocationTargetException x) {
			
			Throwable t= x.getTargetException();
			if (t instanceof CoreException) {
				CoreException cx= (CoreException) t;
				ErrorDialog.openError(shell, JavaEditorMessages.getString("CompilationUnitEditor.error.saving.title2"), JavaEditorMessages.getString("CompilationUnitEditor.error.saving.message2"), cx.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				MessageDialog.openError(shell, JavaEditorMessages.getString("CompilationUnitEditor.error.saving.title3"), JavaEditorMessages.getString("CompilationUnitEditor.error.saving.message3") + t.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			}
						
		} finally {
			provider.changed(newInput);
			if (success)
				setInput(newInput);
		}
		
		if (progressMonitor != null)
			progressMonitor.setCanceled(!success);
	}
	
	/*
	 * @see AbstractTextEditor#doSetInput(IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		configureTabConverter();
	}
	
	private void startBracketHighlighting() {
		if (fBracketPainter == null) {
			ISourceViewer sourceViewer= getSourceViewer();
			fBracketPainter= new BracketPainter(sourceViewer);
			fBracketPainter.setHighlightColor(getColor(MATCHING_BRACKETS_COLOR));
			fPaintManager.addPainter(fBracketPainter);
		}
	}
	
	private void stopBracketHighlighting() {
		if (fBracketPainter != null) {
			fPaintManager.removePainter(fBracketPainter);
			fBracketPainter.deactivate(true);
			fBracketPainter.dispose();
			fBracketPainter= null;
		}
	}
	
	private boolean isBracketHighlightingEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(MATCHING_BRACKETS);
	}
	
	private void startLineHighlighting() {
		if (fLinePainter == null) {
			ISourceViewer sourceViewer= getSourceViewer();
			fLinePainter= new LinePainter(sourceViewer);
			fLinePainter.setHighlightColor(getColor(CURRENT_LINE_COLOR));
			fPaintManager.addPainter(fLinePainter);
		}
	}
	
	private void stopLineHighlighting() {
		if (fLinePainter != null) {
			fPaintManager.removePainter(fLinePainter);
			fLinePainter.deactivate(true);
			fLinePainter.dispose();
			fLinePainter= null;
		}
	}
	
	private boolean isLineHighlightingEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(CURRENT_LINE);
	}
	
	private void showPrintMargin() {
		if (fPrintMarginPainter == null) {
			fPrintMarginPainter= new PrintMarginPainter(getSourceViewer());
			fPrintMarginPainter.setMarginRulerColor(getColor(PRINT_MARGIN_COLOR));
			fPrintMarginPainter.setMarginRulerColumn(getPreferenceStore().getInt(PRINT_MARGIN_COLUMN));
			fPaintManager.addPainter(fPrintMarginPainter);
		}
	}
	
	private void hidePrintMargin() {
		if (fPrintMarginPainter != null) {
			fPaintManager.removePainter(fPrintMarginPainter);
			fPrintMarginPainter.deactivate(true);
			fPrintMarginPainter.dispose();
			fPrintMarginPainter= null;
		}
	}
	
	private boolean isPrintMarginVisible() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(PRINT_MARGIN);
	}
	
	private void startAnnotationIndication(AnnotationType annotationType) {
		if (fProblemPainter == null) {
			fProblemPainter= new ProblemPainter(this, getSourceViewer());
			fPaintManager.addPainter(fProblemPainter);
		}
		fProblemPainter.setColor(annotationType, getColor(annotationType));
		fProblemPainter.paintAnnotations(annotationType, true);
		fProblemPainter.paint(IPainter.CONFIGURATION);
	}
	
	private void shutdownAnnotationIndication() {
		if (fProblemPainter != null) {
			
			if (!fProblemPainter.isPaintingAnnotations()) {
				fPaintManager.removePainter(fProblemPainter);
				fProblemPainter.deactivate(true);
				fProblemPainter.dispose();
				fProblemPainter= null;
			} else {
				fProblemPainter.paint(IPainter.CONFIGURATION);
			}
		}
	}

	private void stopAnnotationIndication(AnnotationType annotationType) {
		if (fProblemPainter != null) {
			fProblemPainter.paintAnnotations(annotationType, false);
			shutdownAnnotationIndication();
		}
	}
	
	private boolean isAnnotationIndicationEnabled(AnnotationType annotationType) {
		IPreferenceStore store= getPreferenceStore();
		AnnotationInfo info= (AnnotationInfo) ANNOTATION_MAP.get(annotationType);
		if (info != null)
			return store.getBoolean(info.fEditorPreference);
		return false;
	}
	
	private boolean isAnnotationIndicationInOverviewRulerEnabled(AnnotationType annotationType) {
		IPreferenceStore store= getPreferenceStore();
		AnnotationInfo info= (AnnotationInfo) ANNOTATION_MAP.get(annotationType);
		if (info != null)
			return store.getBoolean(info.fOverviewRulerPreference);
		return false;
	}
	
	private void showAnnotationIndicationInOverviewRuler(AnnotationType annotationType, boolean show) {
		AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
		OverviewRuler ruler= asv.getOverviewRuler();
		if (ruler != null) {
			ruler.setColor(annotationType, getColor(annotationType));
			ruler.showAnnotation(annotationType, show);
			ruler.update();
		}
	}

	private void setColorInOverviewRuler(AnnotationType annotationType, Color color) {
		AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
		OverviewRuler ruler= asv.getOverviewRuler();
		if (ruler != null) {
			ruler.setColor(annotationType, color);
			ruler.update();
		}
	}

	private void configureTabConverter() {
		if (fTabConverter != null) {
			IDocumentProvider provider= getDocumentProvider();
			if (provider instanceof CompilationUnitDocumentProvider) {
				CompilationUnitDocumentProvider cup= (CompilationUnitDocumentProvider) provider;
				fTabConverter.setLineTracker(cup.createLineTracker(getEditorInput()));
			}
		}
	}
	
	private int getTabSize() {
		Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
		return preferences.getInt(CODE_FORMATTER_TAB_SIZE);	
	}
	
	private void startTabConversion() {
		if (fTabConverter == null) {
			fTabConverter= new TabConverter();
			configureTabConverter();
			fTabConverter.setNumberOfSpacesPerTab(getTabSize());
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			asv.addTextConverter(fTabConverter);
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
			asv.updateIndentationPrefixes();
		}
	}
	
	private void stopTabConversion() {
		if (fTabConverter != null) {
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			asv.removeTextConverter(fTabConverter);
			// http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
			asv.updateIndentationPrefixes();
			fTabConverter= null;
		}
	}
	
	private boolean isTabConversionEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(SPACES_FOR_TABS);
	}
	
	private void showOverviewRuler() {
		AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
		asv.showOverviewRuler();
		
		OverviewRuler overviewRuler= asv.getOverviewRuler();
		if (overviewRuler != null) {
			for (int i= 0; i < ANNOTATION_LAYERS.length; i++) {
				AnnotationType type= ANNOTATION_LAYERS[i];
				overviewRuler.setLayer(type, i);	
				if (isAnnotationIndicationInOverviewRulerEnabled(type))
					showAnnotationIndicationInOverviewRuler(type, true);
			}
		}
	}
	
	private void hideOverviewRuler() {
		AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
		asv.hideOverviewRuler();
	}
	
	private boolean isOverviewRulerVisible() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(OVERVIEW_RULER);
	}
	
	private Color getColor(String key) {
		RGB rgb= PreferenceConverter.getColor(getPreferenceStore(), key);
		return getColor(rgb);
	}
	
	private Color getColor(RGB rgb) {
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		return textTools.getColorManager().getColor(rgb);
	}
	
	private Color getColor(AnnotationType annotationType) {
		AnnotationInfo info= (AnnotationInfo) ANNOTATION_MAP.get(annotationType);
		if (info != null)
			return getColor(info.fColorPreference);
		return null;
	}
	
	/*
	 * @see AbstractTextEditor#dispose()
	 */
	public void dispose() {

		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer instanceof ITextViewerExtension)
			((ITextViewerExtension) sourceViewer).removeVerifyKeyListener(fBracketInserter);

		if (fPropertyChangeListener != null) {
			Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
			preferences.removePropertyChangeListener(fPropertyChangeListener);
			fPropertyChangeListener= null;
		}
		
		if (fJavaEditorErrorTickUpdater != null) {
			fJavaEditorErrorTickUpdater.dispose();
			fJavaEditorErrorTickUpdater= null;
		}
		
		if (fSelectionHistory != null)
			fSelectionHistory.dispose();
		
		if (fPaintManager != null) {
			fPaintManager.dispose();
			fPaintManager= null;
		}
		
		if (fActionGroups != null)
			fActionGroups.dispose();		
		
		super.dispose();
	}
	
	/*
	 * @see AbstractTextEditor#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		
		super.createPartControl(parent);
		
		fPaintManager= new PaintManager(getSourceViewer());
		
		if (isBracketHighlightingEnabled())
			startBracketHighlighting();
		if (isLineHighlightingEnabled())
			startLineHighlighting();
		if (isPrintMarginVisible())
			showPrintMargin();
		
		
		Iterator e= ANNOTATION_MAP.keySet().iterator();
		while (e.hasNext()) {
			AnnotationType type= (AnnotationType) e.next();
			if (isAnnotationIndicationEnabled(type))
				startAnnotationIndication(type);
		}
			
		if (isTabConversionEnabled())
			startTabConversion();
		if (isOverviewRulerVisible())
			showOverviewRuler();
			
			
		Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
		preferences.addPropertyChangeListener(fPropertyChangeListener);			
		
		IPreferenceStore preferenceStore= getPreferenceStore();
		boolean closeBrackets= preferenceStore.getBoolean(CLOSE_BRACKETS);
		boolean closeStrings= preferenceStore.getBoolean(CLOSE_STRINGS);
		
		fBracketInserter.setCloseBracketsEnabled(closeBrackets);
		fBracketInserter.setCloseStringsEnabled(closeStrings);
		
		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer instanceof ITextViewerExtension)
			((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(fBracketInserter);
	}
	
	private static char getPeerCharacter(char character) {
		switch (character) {
			case '(':
				return ')';
				
			case ')':
				return '(';
				
			case '[':
				return ']';

			case ']':
				return '[';
				
			case '"':
				return character;
			
			default:
				throw new IllegalArgumentException();
		}					
	}
	
	private static class ExitPolicy implements LinkedPositionUI.ExitPolicy {
		
		final char fExitCharacter;
		
		public ExitPolicy(char exitCharacter) {
			fExitCharacter= exitCharacter;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitPolicy#doExit(org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager, org.eclipse.swt.events.VerifyEvent, int, int)
		 */
		public ExitFlags doExit(LinkedPositionManager manager, VerifyEvent event, int offset, int length) {
			
			if (event.character == fExitCharacter) {
				if (manager.anyPositionIncludes(offset, length))
					return new ExitFlags(LinkedPositionUI.COMMIT| LinkedPositionUI.UPDATE_CARET, false);
				else
					return new ExitFlags(LinkedPositionUI.COMMIT, true);
			}	
			
			switch (event.character) {			
			case '\b':
				if (manager.getFirstPosition().length == 0)
					return new ExitFlags(0, false);
				else
					return null;
				
			case '\n':
			case '\r':
				return new ExitFlags(LinkedPositionUI.COMMIT, true);
				
			default:
				return null;
			}						
		}

	}
	
	private class BracketInserter implements VerifyKeyListener, LinkedPositionUI.ExitListener {
		
		private boolean fCloseBrackets= true;
		private boolean fCloseStrings= true;
		
		private int fOffset;
		private int fLength;

		public void setCloseBracketsEnabled(boolean enabled) {
			fCloseBrackets= enabled;
		}

		public void setCloseStringsEnabled(boolean enabled) {
			fCloseStrings= enabled;
		}

		private boolean hasIdentifierToTheRight(IDocument document, int offset) {
			try {
				int end= offset;
				IRegion endLine= document.getLineInformationOfOffset(end);
				int maxEnd= endLine.getOffset() + endLine.getLength();
				while (end != maxEnd && Character.isWhitespace(document.getChar(end)))
					++end;

				return end != maxEnd && Character.isJavaIdentifierPart(document.getChar(end));

			} catch (BadLocationException e) {
				// be conservative
				return true;
			}
		}

		private boolean hasIdentifierToTheLeft(IDocument document, int offset) {
			try {
				int start= offset;
				IRegion startLine= document.getLineInformationOfOffset(start);
				int minStart= startLine.getOffset();
				while (start != minStart && Character.isWhitespace(document.getChar(start - 1)))
					--start;
				
				return start != minStart && Character.isJavaIdentifierPart(document.getChar(start - 1));

			} catch (BadLocationException e) {
				return true;
			}			
		}

		private boolean hasCharacterToTheRight(IDocument document, int offset, char character) {
			try {
				int end= offset;
				IRegion endLine= document.getLineInformationOfOffset(end);
				int maxEnd= endLine.getOffset() + endLine.getLength();
				while (end != maxEnd && Character.isWhitespace(document.getChar(end)))
					++end;
				
				return end != maxEnd && document.getChar(end) == character;


			} catch (BadLocationException e) {
				// be conservative
				return true;
			}			
		}
		
		/*
		 * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
		 */
		public void verifyKey(VerifyEvent event) {			

			if (!event.doit)
				return;

			final ISourceViewer sourceViewer= getSourceViewer();
			IDocument document= sourceViewer.getDocument();

			final Point selection= sourceViewer.getSelectedRange();
			final int offset= selection.x;
			final int length= selection.y;

			switch (event.character) {
			case '(':
				if (hasCharacterToTheRight(document, offset + length, '('))
					return;

				// fall through

			case '[':
					if (!fCloseBrackets)
						return;
 					if (hasIdentifierToTheRight(document, offset + length))
 						return;
			
				// fall through
			
			case '"':
				if (event.character == '"') {
					if (!fCloseStrings)
						return;
 					if (hasIdentifierToTheLeft(document, offset) || hasIdentifierToTheRight(document, offset + length))
 						return;
				}
				
				try {		
					ITypedRegion partition= document.getPartition(offset);
					if (! IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType()) && partition.getOffset() != offset)
						return;

					final char character= event.character;
					final char closingCharacter= getPeerCharacter(character);		
					final StringBuffer buffer= new StringBuffer();
					buffer.append(character);
					buffer.append(closingCharacter);

					document.replace(offset, length, buffer.toString());

					LinkedPositionManager manager= new LinkedPositionManager(document);
					manager.addPosition(offset + 1, 0);

					fOffset= offset;
					fLength= 2;
			
					LinkedPositionUI editor= new LinkedPositionUI(sourceViewer, manager);
					editor.setCancelListener(this);
					editor.setExitPolicy(new ExitPolicy(closingCharacter));
					editor.setFinalCaretOffset(offset + 2);
					editor.enter();

					IRegion newSelection= editor.getSelectedRegion();
					sourceViewer.setSelectedRange(newSelection.getOffset(), newSelection.getLength());
	
					event.doit= false;

				} catch (BadLocationException e) {
				}
				break;	
			}
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitListener#exit(boolean)
		 */
		public void exit(boolean accept) {
			if (accept)
				return;

			// remove brackets
			try {
				final ISourceViewer sourceViewer= getSourceViewer();
				IDocument document= sourceViewer.getDocument();
				document.replace(fOffset, fLength, null);
			} catch (BadLocationException e) {
			}
		}

	}
	
	protected AnnotationType getAnnotationType(String preferenceKey) {
		Iterator e= ANNOTATION_MAP.keySet().iterator();
		while (e.hasNext()) {
			AnnotationType type= (AnnotationType) e.next();
			AnnotationInfo info= (AnnotationInfo) ANNOTATION_MAP.get(type);
			if (info != null) {
				if (preferenceKey.equals(info.fColorPreference) || preferenceKey.equals(info.fEditorPreference) || preferenceKey.equals(info.fOverviewRulerPreference)) 
					return type;
			}
		}
		return null;
	}
	
	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		try {
			
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			if (asv != null) {
					
				String p= event.getProperty();		
				
				if (CLOSE_BRACKETS.equals(p)) {
					fBracketInserter.setCloseBracketsEnabled(getPreferenceStore().getBoolean(p));
					return;	
				}

				if (CLOSE_STRINGS.equals(p)) {
					fBracketInserter.setCloseStringsEnabled(getPreferenceStore().getBoolean(p));
					return;
				}
								
				if (SPACES_FOR_TABS.equals(p)) {
					if (isTabConversionEnabled())
						startTabConversion();
					else
						stopTabConversion();
					return;
				}
				
				if (MATCHING_BRACKETS.equals(p)) {
					if (isBracketHighlightingEnabled())
						startBracketHighlighting();
					else
						stopBracketHighlighting();
					return;
				}
				
				if (MATCHING_BRACKETS_COLOR.equals(p)) {
					if (fBracketPainter != null)
						fBracketPainter.setHighlightColor(getColor(MATCHING_BRACKETS_COLOR));
					return;
				}
				
				if (CURRENT_LINE.equals(p)) {
					if (isLineHighlightingEnabled())
						startLineHighlighting();
					else
						stopLineHighlighting();
					return;
				}
				
				if (CURRENT_LINE_COLOR.equals(p)) {
					if (fLinePainter != null) {
						stopLineHighlighting();
						startLineHighlighting();
					}					
					return;
				}
				
				if (PRINT_MARGIN.equals(p)) {
					if (isPrintMarginVisible())
						showPrintMargin();
					else
						hidePrintMargin();
					return;
				}
				
				if (PRINT_MARGIN_COLOR.equals(p)) {
					if (fPrintMarginPainter != null)
						fPrintMarginPainter.setMarginRulerColor(getColor(PRINT_MARGIN_COLOR));
					return;
				}
				
				if (PRINT_MARGIN_COLUMN.equals(p)) {
					if (fPrintMarginPainter != null)
						fPrintMarginPainter.setMarginRulerColumn(getPreferenceStore().getInt(PRINT_MARGIN_COLUMN));
					return;
				}
				
				if (OVERVIEW_RULER.equals(p))  {
					if (isOverviewRulerVisible())
						showOverviewRuler();
					else
						hideOverviewRuler();
					return;
				}
				
				AnnotationType type= getAnnotationType(p);
				if (type != null) {
					
					AnnotationInfo info= (AnnotationInfo) ANNOTATION_MAP.get(type);
					if (info.fColorPreference.equals(p)) {
						Color color= getColor(type);
						if (fProblemPainter != null) {
							fProblemPainter.setColor(type, color);
							fProblemPainter.paint(IPainter.CONFIGURATION);
						}
						setColorInOverviewRuler(type, color);
						return;
					}
					
					if (info.fEditorPreference.equals(p)) {
						if (isAnnotationIndicationEnabled(type))
							startAnnotationIndication(type);
						else
							stopAnnotationIndication(type);
						return;
					}
					
					if (info.fOverviewRulerPreference.equals(p)) {
						if (isAnnotationIndicationInOverviewRulerEnabled(type))
							showAnnotationIndicationInOverviewRuler(type, true);
						else
							showAnnotationIndicationInOverviewRuler(type, false);
						return;
					}
				}

				IContentAssistant c= asv.getContentAssistant();
				if (c instanceof ContentAssistant)
					ContentAssistPreference.changeConfiguration((ContentAssistant) c, getPreferenceStore(), event);
			}
				
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}

	/**
	 * Handles a property change event describing a change
	 * of the java core's preferences and updates the preference
	 * related editor properties.
	 * 
	 * @param event the property change event
	 */
	protected void handlePreferencePropertyChanged(org.eclipse.core.runtime.Preferences.PropertyChangeEvent event) {
		AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
		if (asv != null) {
			String p= event.getProperty();					
			if (CODE_FORMATTER_TAB_SIZE.equals(p)) {
				asv.updateIndentationPrefixes();
				if (fTabConverter != null)
					fTabConverter.setNumberOfSpacesPerTab(getTabSize());
			}
		}
	}
	
	/*
	 * @see AbstractTextEditor#affectsTextPresentation(PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		String p= event.getProperty();
		boolean affects=MATCHING_BRACKETS_COLOR.equals(p) || CURRENT_LINE_COLOR.equals(p) || 
									ERROR_INDICATION_COLOR.equals(p) || WARNING_INDICATION_COLOR.equals(p) || TASK_INDICATION_COLOR.equals(p);
		return affects ? affects : super.affectsTextPresentation(event);
	}
	
	/*
	 * @see JavaEditor#createJavaSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		return new AdaptedSourceViewer(parent, ruler, styles);
	}
	
	/*
	 * @see JavaEditor#synchronizeOutlinePageSelection()
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
		
		int modelCaret= 0;
		if (sourceViewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) sourceViewer;
			modelCaret= extension.widgetOffset2ModelOffset(styledText.getCaretOffset());
		} else {
			int offset= sourceViewer.getVisibleRegion().getOffset();
			modelCaret= offset + styledText.getCaretOffset();
		}
				
		IJavaElement element= getElementAt(modelCaret, false);
		ISourceReference reference= getSourceReference(element, modelCaret);
		if (reference != null) {
			fOutlinePage.removeSelectionChangedListener(fSelectionChangedListener);
			fOutlinePage.select(reference);
			fOutlinePage.addSelectionChangedListener(fSelectionChangedListener);
		}
	}
	
	private ISourceReference getSourceReference(IJavaElement element, int offset) {
		
		if ( !(element instanceof ISourceReference))
			return null;
		
		if (element.getElementType() == IJavaElement.IMPORT_DECLARATION) {
			
			IImportDeclaration declaration= (IImportDeclaration) element;
			IImportContainer container= (IImportContainer) declaration.getParent();
			ISourceRange srcRange= null;
			
			try {
				srcRange= container.getSourceRange();
			} catch (JavaModelException e) {
			}
			
			if (srcRange != null && srcRange.getOffset() == offset)
				return container;
		}
		
		return (ISourceReference) element;
	}
		
	/*
	 * @see IReconcilingParticipant#reconciled()
	 */
	public void reconciled() {
		if (!JavaEditorPreferencePage.synchronizeOutlineOnCursorMove()) {
			Shell shell= getSite().getShell();
			if (shell != null && !shell.isDisposed()) {
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						synchronizeOutlinePageSelection();
					}
				});
			}
		}
	}
	
	protected void updateStateDependentActions() {
		super.updateStateDependentActions();
		fGenerateActionGroup.editorStateChanged();
	}
	
	/**
	 * Returns the updated java element for the old java element.
	 */
	private IJavaElement findElement(IJavaElement element) {
		
		if (element == null)
			return null;
		
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
		
		if (unit != null) {
			try {
				
				synchronized (unit) {
					unit.reconcile();
				}
				IJavaElement[] findings= unit.findElements(element);
				if (findings != null && findings.length > 0)
					return findings[0];
			
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
				// nothing found, be tolerant and go on
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the offset of the given Java element.
	 */
	private int getOffset(IJavaElement element) {
		if (element instanceof ISourceReference) {
			ISourceReference sr= (ISourceReference) element;
			try {
				ISourceRange srcRange= sr.getSourceRange();
				if (srcRange != null)
					return srcRange.getOffset();
			} catch (JavaModelException e) {
			}
		}
		return -1;	
	}
	
	/*
	 * @see AbstractTextEditor#rememberSelection()
	 */
	protected void rememberSelection() {
		ISelectionProvider sp= getSelectionProvider();
		fRememberedSelection= (sp == null ? null : (ITextSelection) sp.getSelection());
		if (fRememberedSelection != null) {
			fRememberedElement= getElementAt(fRememberedSelection.getOffset(), true);
			fRememberedElementOffset= getOffset(fRememberedElement); 
		}
	}
	
	/*
	 * @see AbstractTextEditor#restoreSelection()
	 */
	protected void restoreSelection() {
		
		try {
			
			if (getSourceViewer() == null || fRememberedSelection == null)
				return;
				
			IJavaElement newElement= findElement(fRememberedElement);
			int newOffset= getOffset(newElement);
			int delta= (newOffset > -1 && fRememberedElementOffset > -1) ? newOffset - fRememberedElementOffset : 0;
			if (isValidSelection(delta + fRememberedSelection.getOffset(), fRememberedSelection.getLength()))
				selectAndReveal(delta + fRememberedSelection.getOffset(), fRememberedSelection.getLength());			
			
		} finally {
			fRememberedSelection= null;
			fRememberedElement= null;
			fRememberedElementOffset= -1;
		}
	}
	
	private boolean isValidSelection(int offset, int length) {
		IDocumentProvider provider= getDocumentProvider();
		if (provider != null) {
			IDocument document= provider.getDocument(getEditorInput());
			if (document != null) {
				int end= offset + length;
				int documentLength= document.getLength();
				return 0 <= offset  && offset <= documentLength && 0 <= end && end <= documentLength;
			}
		}
		return false;
	}
	
	/*
	 * @see AbstractTextEditor#canHandleMove(IEditorInput, IEditorInput)
	 */
	protected boolean canHandleMove(IEditorInput originalElement, IEditorInput movedElement) {
		
		String oldExtension= ""; //$NON-NLS-1$
		if (originalElement instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) originalElement).getFile();
			if (file != null) {
				String ext= file.getFileExtension();
				if (ext != null)
					oldExtension= ext;
			}
		}
		
		String newExtension= ""; //$NON-NLS-1$
		if (movedElement instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) movedElement).getFile();
			if (file != null)
				newExtension= file.getFileExtension();
		}
		
		return oldExtension.equals(newExtension);
	}
}