package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.tasklist.TaskList;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.internal.ui.javaeditor.structureselection.SelectionHistory;
import org.eclipse.jdt.internal.ui.javaeditor.structureselection.StructureSelectEnclosingAction;
import org.eclipse.jdt.internal.ui.javaeditor.structureselection.StructureSelectHistoryAction;
import org.eclipse.jdt.internal.ui.javaeditor.structureselection.StructureSelectNextAction;
import org.eclipse.jdt.internal.ui.javaeditor.structureselection.StructureSelectPreviousAction;
import org.eclipse.jdt.internal.ui.javaeditor.structureselection.StructureSelectionAction;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.actions.SurroundWithTryCatchAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgGroup;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionSourceViewer;
import org.eclipse.jdt.internal.ui.text.java.IProblemRequestorExtension;
import org.eclipse.jdt.internal.ui.text.java.IReconcilingParticipant;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.OpenActionGroup;
import org.eclipse.jdt.ui.actions.ShowActionGroup;
import org.eclipse.jdt.ui.text.JavaTextTools;


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
			if (fTextConverters != null) {
				for (Iterator e = fTextConverters.iterator(); e.hasNext();)
					((ITextConverter) e.next()).customizeDocumentCommand(getDocument(), command);
			}
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
	};
	
	static class TabConverter implements ITextConverter {
		
		private String fTabString= "";
		
		public void setNumberOfSpacesPerTab(int ratio) {
			StringBuffer buffer= new StringBuffer();
			for (int i= 0; i < ratio; i++)
				buffer.append(' ');
			fTabString= buffer.toString();
		}		
		
		public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
			String text= command.text;
			if (text != null) {
				int index= text.indexOf('\t');
				if (index > -1) {
					int length= text.length();
					StringBuffer buffer= new StringBuffer();
					buffer.append(text.substring(0, index));
					for (int i= index; i < length; i++) {
						char c= text.charAt(i);
						if (c == '\t')
							buffer.append(fTabString);
						else
							buffer.append(c);
					}
					command.text= buffer.toString();
				}
			}
		}
	};
	
	
	/* Preference key for code formatter tab size */
	private final static String CODE_FORMATTER_TAB_SIZE= "org.eclipse.jdt.core.formatter.tabulation.size";	
	/* Preference key for code formatter tab character */
	private final static String CODE_FORMATTER_TAB_CHAR= "org.eclipse.jdt.core.formatter.tabulation.char";	
	/** Preference key for matching brackets */
	public final static String MATCHING_BRACKETS=  "matchingBrackets";
	/** Preference key for matching brackets color */
	public final static String MATCHING_BRACKETS_COLOR=  "matchingBracketsColor";
	/** Preference key for highlighting current line */
	public final static String CURRENT_LINE= "currentLine";
	/** Preference key for highlight color of current line */
	public final static String CURRENT_LINE_COLOR= "currentLineColor";
	/** Preference key for showing print marging ruler */
	public final static String PRINT_MARGIN= "printMargin";
	/** Preference key for print margin ruler color */
	public final static String PRINT_MARGIN_COLOR= "printMarginColor";
	/** Preference key for print margin ruler column */
	public final static String PRINT_MARGIN_COLUMN= "printMarginColumn";
	/** Preference key for inserting spaces rather than tabs */
	public final static String SPACES_FOR_TABS= "spacesForTabs";
	/** Preference key for problem indication */
	public final static String PROBLEM_INDICATION= "problemIndication";
	/** Preference key for problem highlight color */
	public final static String PROBLEM_INDICATION_COLOR= "problemIndicationColor";
	/** Preference key for shwoing the overview ruler */
	public final static String OVERVIEW_RULER= "overviewRuler";
	
	
	
	/** The status line clearer */
	protected ISelectionChangedListener fStatusLineClearer;
	/** The editor's save policy */
	protected ISavePolicy fSavePolicy;
	/** Listener to annotation model changes that updates the error tick in the tab image */
	private JavaEditorErrorTickUpdater fJavaEditorErrorTickUpdater;
	/** The editor's paint manager */
	private PaintManager fPaintManager;
	/** The editor's bracket painter */
	private BracketPainter fBracketPainter;
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
	
	/** The standard action groups added to the menu */
	/* package */ ActionGroup fStandardActionGroups;
	
	/**
	 * Creates a new compilation unit editor.
	 */
	public CompilationUnitEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
		setEditorContextMenuId("#CompilationUnitEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#CompilationUnitRulerContext"); //$NON-NLS-1$
		setOutlinerContextMenuId("#CompilationUnitOutlinerContext"); //$NON-NLS-1$
		setHelpContextId(IJavaHelpContextIds.COMPILATION_UNIT_EDITOR);
		fSavePolicy= null;
			
		fJavaEditorErrorTickUpdater= new JavaEditorErrorTickUpdater(this);
	}
	
	/*
	 * @see AbstractTextEditor#createActions()
	 */
	protected void createActions() {
		
		super.createActions();

		setAction("CorrectionAssistProposal", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "CorrectionAssistProposal.", this, JavaCorrectionSourceViewer.CORRECTIONASSIST_PROPOSALS));			 //$NON-NLS-1$ //$NON-NLS-2$

		
		setAction("ContentAssistProposal", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS));			 //$NON-NLS-1$ //$NON-NLS-2$
		setAction("ContentAssistContextInformation", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistContextInformation.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION));			 //$NON-NLS-1$ //$NON-NLS-2$
		setAction("AddImportOnSelection", new AddImportOnSelectionAction(this));		 //$NON-NLS-1$
		setAction("OrganizeImports", new OrganizeImportsAction(this)); //$NON-NLS-1$
		
		setAction("Comment", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Comment.", this, ITextOperationTarget.PREFIX)); //$NON-NLS-1$ //$NON-NLS-2$
		setAction("Uncomment", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Uncomment.", this, ITextOperationTarget.STRIP_PREFIX)); //$NON-NLS-1$ //$NON-NLS-2$
		setAction("Format", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "Format.", this, ISourceViewer.FORMAT)); //$NON-NLS-1$ //$NON-NLS-2$
				
		setAction("SurroundWithTryCatch", new SurroundWithTryCatchAction(this)); //$NON-NLS-1$

		fSelectionHistory= new SelectionHistory(this);
		setAction(StructureSelectionAction.ENCLOSING, new StructureSelectEnclosingAction(this, fSelectionHistory));
		setAction(StructureSelectionAction.NEXT, new StructureSelectNextAction(this, fSelectionHistory));
		setAction(StructureSelectionAction.PREVIOUS, new StructureSelectPreviousAction(this, fSelectionHistory));
		StructureSelectHistoryAction historyAction= new StructureSelectHistoryAction(this, fSelectionHistory);
		setAction(StructureSelectionAction.HISTORY, historyAction);
		fSelectionHistory.setHistoryAction(historyAction);		

		fStandardActionGroups= new CompositeActionGroup(new ActionGroup[] {
			new OpenActionGroup(this), new ShowActionGroup(this), new GenerateActionGroup(this)});
	}
	
	/*
	 * @see JavaEditor#getElementAt(int)
	 */
	protected IJavaElement getElementAt(int offset) {
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
		
		if (unit != null) {
			synchronized (unit) {
				try {
					
					if (!unit.isConsistent()) {
						IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
						if (model instanceof IProblemRequestorExtension) {
							IProblemRequestorExtension requestor= (IProblemRequestorExtension) model;
							requestor.beginReporting();
							unit.reconcile(requestor);
							requestor.endReporting();
						}
					}
					
					return unit.getElementAt(offset);
				
				} catch (JavaModelException x) {
				}
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
			// nothing found, be tolerant and go on
		}
		return null;
	}
	
	/*
	 * @see AbstractTextEditor#editorContextMenuAboutToShow(IMenuManager)
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		
		/*
		 * http://dev.eclipse.org/bugs/show_bug.cgi?id=8735
		 * Removed duplicates of Edit menu entries to shorten context menu.
		 * Will be reworked for overal context menu reorganization.
		 */
//		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "ContentAssistProposal"); //$NON-NLS-1$
		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "AddImportOnSelection"); //$NON-NLS-1$
		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "OrganizeImports"); //$NON-NLS-1$

		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "CorrectionAssistProposal"); //$NON-NLS-1$

		
		addAction(menu, IContextMenuConstants.GROUP_GENERATE, "SurroundWithTryCatch"); //$NON-NLS-1$
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, "Comment"); //$NON-NLS-1$
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, "Uncomment"); //$NON-NLS-1$
		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, "Format"); //$NON-NLS-1$
	}
	
	/*
	 * @see JavaEditor#createOutlinePage()
	 */
	protected JavaOutlinePage createOutlinePage() {
		JavaOutlinePage page= super.createOutlinePage();
		
		page.setAction("OrganizeImports", new OrganizeImportsAction(this)); //$NON-NLS-1$
		//page.setAction("ReplaceWithEdition", new JavaReplaceWithEditionAction(page)); //$NON-NLS-1$
		//page.setAction("AddEdition", new JavaAddElementFromHistory(this, page)); //$NON-NLS-1$
		
		IAction deleteElement= ReorgGroup.createDeleteAction(page);
		page.setAction("DeleteElement", deleteElement); //$NON-NLS-1$
		
		return page;
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
	 * @see AbstractTextEditor#doSave(IProgressMonitor)
	 */
	public void doSave(IProgressMonitor progressMonitor) {
		
		IDocumentProvider p= getDocumentProvider();
		if (p == null)
			return;
			
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
	 * Jumps to the error next according to the given direction.
	 */
	public void gotoError(boolean forward) {
		
		ISelectionProvider provider= getSelectionProvider();
		
		if (fStatusLineClearer != null) {
			provider.removeSelectionChangedListener(fStatusLineClearer);
			fStatusLineClearer= null;
		}
		
		ITextSelection s= (ITextSelection) provider.getSelection();
		IMarker nextError= getNextError(s.getOffset(), forward);
		
		if (nextError != null) {
			
			gotoMarker(nextError);
			
			IWorkbenchPage page= getSite().getPage();
			
			IViewPart view= view= page.findView("org.eclipse.ui.views.TaskList"); //$NON-NLS-1$
			if (view instanceof TaskList) {
				StructuredSelection ss= new StructuredSelection(nextError);
				((TaskList) view).setSelection(ss, true);
			}
			
			setStatusLineErrorMessage(nextError.getAttribute(IMarker.MESSAGE, ""));
			
		} else {
			
			setStatusLineErrorMessage(null);
			
		}
	}
	
	/**
	 * Sets the given message as error message to this editor's status line.
	 * @param msg message to be set
	 */
	protected void setStatusLineErrorMessage(String msg) {
		// set error message
		getStatusLineManager().setErrorMessage(msg);
		// install message remover
		if (msg == null || msg.trim().length() == 0) {
			if (fStatusLineClearer != null) {
				getSelectionProvider().removeSelectionChangedListener(fStatusLineClearer);
				fStatusLineClearer= null;
			}
		} else if (fStatusLineClearer == null) {
			fStatusLineClearer= new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					getSelectionProvider().removeSelectionChangedListener(fStatusLineClearer);
					fStatusLineClearer= null;
					getStatusLineManager().setErrorMessage(null);
				}
			};
			getSelectionProvider().addSelectionChangedListener(fStatusLineClearer);
		}
	}
	
	private IMarker getNextError(int offset, boolean forward) {
		
		IMarker nextError= null;
		
		IDocument document= getDocumentProvider().getDocument(getEditorInput());
		int endOfDocument= document.getLength(); 
		int distance= 0;
		
		IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());
		Iterator e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Annotation a= (Annotation) e.next();
			if (a instanceof MarkerAnnotation) {
				MarkerAnnotation ma= (MarkerAnnotation) a;
				IMarker marker= ma.getMarker();
		
				if (MarkerUtilities.isMarkerType(marker, IMarker.PROBLEM)) {
					Position p= model.getPosition(a);
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
							nextError= marker;
						}
					}
				}
		
			}
		}
		
		return nextError;
	}
	
	/*
	 * @see AbstractTextEditor#isSaveAsAllowed() 
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	/*
	 * 1GF7WG9: ITPJUI:ALL - EXCEPTION: "Save As..." always fails
	 */
	protected IPackageFragment getPackage(IWorkspaceRoot root, IPath path) {
				
		if (path.segmentCount() == 1) {
			
			IProject project= root.getProject(path.toString());
			if (project != null) {
				IJavaProject jProject= JavaCore.create(project);
				if (jProject != null) {
					try {
						IJavaElement element= jProject.findElement(new Path("")); //$NON-NLS-1$
						if (element instanceof IPackageFragment) {
							IPackageFragment fragment= (IPackageFragment) element;
							IJavaElement parent= fragment.getParent();
							if (parent instanceof IPackageFragmentRoot) {
								IPackageFragmentRoot pRoot= (IPackageFragmentRoot) parent;
								if ( !pRoot.isArchive() && !pRoot.isExternal() && path.equals(pRoot.getPath()))
									return fragment;
							}
						}
					} catch (JavaModelException x) {
						// ignore
					}
				}
			}
			
			return null;
			
		} else if (path.segmentCount() > 1) {
		
			IFolder folder= root.getFolder(path);
			IJavaElement element= JavaCore.create(folder);
			if (element instanceof IPackageFragment)
				return (IPackageFragment) element;
		}
		
		return null;
	}
	
	/*
	 * 1GEUSSR: ITPUI:ALL - User should never loose changes made in the editors.
	 * Changed behavior to make sure that if called inside a regular save (because
	 * of deletion of input element) there is a way to report back to the caller.
	 */	
	protected void performSaveAs(IProgressMonitor progressMonitor) {
		
		Shell shell= getSite().getShell();
		
		SaveAsDialog dialog= new SaveAsDialog(shell);
		IEditorInput input = getEditorInput();
		
		IFile original= null;
		if (input instanceof IFileEditorInput)
			original= ((IFileEditorInput) input).getFile();
			
		if (original != null)
			dialog.setOriginalFile(original);
			
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
			
		filePath= filePath.removeTrailingSeparator();
		final String fileName= filePath.lastSegment();
		IPath folderPath= filePath.removeLastSegments(1);
		if (folderPath == null) {
			if (progressMonitor != null)
				progressMonitor.setCanceled(true);			
			return;
		}
		
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		
		/*
		 * 1GF7WG9: ITPJUI:ALL - EXCEPTION: "Save As..." always fails
		 */
		final IPackageFragment fragment= getPackage(root, folderPath);
		
		IFile file= root.getFile(filePath);
		
		/*
		 * Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=8873
		 * Problem caused by http://dev.eclipse.org/bugs/show_bug.cgi?id=9351
		 * Will be removed if #9351 is solved.
		 */
		if (original != null && original.equals(file)) {
			doSave(progressMonitor);
			return;
		}
		/* end of fix */
		
		final FileEditorInput newInput= new FileEditorInput(file);
		
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			public void execute(final IProgressMonitor monitor) throws CoreException {
				
				if (fragment != null) {
					try {	
						
						// copy to another package
						IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
						ICompilationUnit unit= manager.getWorkingCopy(getEditorInput());
						
						/*
						 * 1GJXY0L: ITPJUI:WINNT - NPE during save As in Java editor
						 * Introduced null check, just go on in the null case
						 */
						if (unit != null) {
							/* 
							 * 1GF5YOX: ITPJUI:ALL - Save of delete file claims it's still there
							 * Changed false to true.
							 */
							unit.copy(fragment, null, fileName, true, monitor);
							return;
						}
						
					} catch (JavaModelException x) {
					}
				}
				
				// if (fragment == null) then copy to a directory which is not a package
				// if (unit == null) copy the file that is not a compilation unit
				
				/* 
				 * 1GF5YOX: ITPJUI:ALL - Save of delete file claims it's still there
				 * Changed false to true.
				 */
				getDocumentProvider().saveDocument(monitor, newInput, getDocumentProvider().getDocument(getEditorInput()), true);
			}
		};
		
		boolean success= false;
		try {
			
			if (fragment == null)
				getDocumentProvider().aboutToChange(newInput);
			
			new ProgressMonitorDialog(shell).run(false, true, op);
			setInput(newInput);
			success= true;
			
		} catch (InterruptedException x) {
		} catch (InvocationTargetException x) {
			
			/* 
			 * 1GF5YOX: ITPJUI:ALL - Save of delete file claims it's still there
			 * Missing resources.
			 */						
			Throwable t= x.getTargetException();
			if (t instanceof CoreException) {
				CoreException cx= (CoreException) t;
				ErrorDialog.openError(shell, JavaEditorMessages.getString("CompilationUnitEditor.error.saving.title2"), JavaEditorMessages.getString("CompilationUnitEditor.error.saving.message2"), cx.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				MessageDialog.openError(shell, JavaEditorMessages.getString("CompilationUnitEditor.error.saving.title3"), JavaEditorMessages.getString("CompilationUnitEditor.error.saving.message3") + t.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		} finally {
			
			if (fragment == null)
				getDocumentProvider().changed(newInput);
				
			if (progressMonitor != null)
				progressMonitor.setCanceled(!success);
		}
	}
	
	/*
	 * @see AbstractTextEditor#doSetInput(IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		fJavaEditorErrorTickUpdater.setAnnotationModel(getDocumentProvider().getAnnotationModel(input));
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
	
	private void startShowingPrintMargin() {
		if (fPrintMarginPainter == null) {
			fPrintMarginPainter= new PrintMarginPainter(getSourceViewer());
			fPrintMarginPainter.setMarginRulerColor(getColor(PRINT_MARGIN_COLOR));
			fPrintMarginPainter.setMarginRulerColumn(getPreferenceStore().getInt(PRINT_MARGIN_COLUMN));
			fPaintManager.addPainter(fPrintMarginPainter);
		}
	}
	
	private void stopShowingPrintMargin() {
		if (fPrintMarginPainter != null) {
			fPaintManager.removePainter(fPrintMarginPainter);
			fPrintMarginPainter.deactivate(true);
			fPrintMarginPainter.dispose();
			fPrintMarginPainter= null;
		}
	}
	
	private boolean isShowingPrintMarginEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(PRINT_MARGIN);
	}
	
	private void startProblemIndication() {
		if (fProblemPainter == null) {
			fProblemPainter= new ProblemPainter(this, getSourceViewer());
			fProblemPainter.setHighlightColor(getColor(PROBLEM_INDICATION_COLOR));
			fPaintManager.addPainter(fProblemPainter);
		}
	}
	
	private void stopProblemIndication() {
		if (fProblemPainter != null) {
			fPaintManager.removePainter(fProblemPainter);
			fProblemPainter.deactivate(true);
			fProblemPainter.dispose();
			fProblemPainter= null;
		}
	}
	
	private boolean isProblemIndicationEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(PROBLEM_INDICATION);
	}
	
	private void startTabConversion() {
		if (fTabConverter == null) {
			fTabConverter= new TabConverter();
			fTabConverter.setNumberOfSpacesPerTab(getPreferenceStore().getInt(CODE_FORMATTER_TAB_SIZE));
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			asv.addTextConverter(fTabConverter);
		}
	}
	
	private void stopTabConversion() {
		if (fTabConverter != null) {
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			asv.removeTextConverter(fTabConverter);
			fTabConverter= null;
		}
	}
	
	private boolean isTabConversionEnabled() {
		IPreferenceStore store= getPreferenceStore();
		return store.getBoolean(SPACES_FOR_TABS);
	}
	
	private void startShowingOverviewRuler() {
		AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
		asv.showOverviewRuler();
	}
	
	private void stopShowingOverviewRuler() {
		AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
		asv.hideOverviewRuler();
	}
	
	private boolean isShowingOverviewRuler() {
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
	
	/*
	 * @see AbstractTextEditor#dispose()
	 */
	public void dispose() {
		if (fJavaEditorErrorTickUpdater != null) {
			fJavaEditorErrorTickUpdater.setAnnotationModel(null);
			fJavaEditorErrorTickUpdater= null;
		}
		
		if (fSelectionHistory != null)
			fSelectionHistory.dispose();
		
		stopBracketHighlighting();
		stopLineHighlighting();
		
		if (fPaintManager != null) {
			fPaintManager.dispose();
			fPaintManager= null;
		}
		
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
		if (isShowingPrintMarginEnabled())
			startShowingPrintMargin();
		if (isProblemIndicationEnabled())
			startProblemIndication();
		if (isTabConversionEnabled())
			startTabConversion();
		if (isShowingOverviewRuler())
			startShowingOverviewRuler();
	}
	
	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		
		try {
			
			AdaptedSourceViewer asv= (AdaptedSourceViewer) getSourceViewer();
			if (asv != null) {
					
				String p= event.getProperty();		
				if (CODE_FORMATTER_TAB_SIZE.equals(p) || CODE_FORMATTER_TAB_CHAR.equals(p)) {
				    SourceViewerConfiguration configuration= getSourceViewerConfiguration();
					String[] types= configuration.getConfiguredContentTypes(asv);					
					for (int i= 0; i < types.length; i++)
					    asv.setIndentPrefixes(configuration.getIndentPrefixes(asv, types[i]), types[i]);
					    
					if (fTabConverter != null)
						fTabConverter.setNumberOfSpacesPerTab(getPreferenceStore().getInt(CODE_FORMATTER_TAB_SIZE));
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
					if (fLinePainter != null)
						fLinePainter.setHighlightColor(getColor(CURRENT_LINE_COLOR));
					return;
				}
				
				if (PRINT_MARGIN.equals(p)) {
					if (isShowingPrintMarginEnabled())
						startShowingPrintMargin();
					else
						stopShowingPrintMargin();
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
				
				if (PROBLEM_INDICATION.equals(p)) {
					if (isProblemIndicationEnabled())
						startProblemIndication();
					else
						stopProblemIndication();
					return;
				}
				
				if (PROBLEM_INDICATION_COLOR.equals(p)) {
					if (fProblemPainter != null)
						fProblemPainter.setHighlightColor(getColor(PROBLEM_INDICATION_COLOR));
					return;
				}
				
				if (OVERVIEW_RULER.equals(p))  {
					if (isShowingOverviewRuler())
						startShowingOverviewRuler();
					else
						stopShowingOverviewRuler();
					return;
				}
				
				IContentAssistant c= asv.getContentAssistant();
				if (c instanceof ContentAssistant)
					ContentAssistPreference.changeConfiguration((ContentAssistant) c, getPreferenceStore(), event);
			}
				
		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}
	
	/*
	 * @see AbstractTextEditor#affectsTextPresentation(PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		String p= event.getProperty();
		
		boolean affects=MATCHING_BRACKETS_COLOR.equals(p) || 
									CURRENT_LINE_COLOR.equals(p) ||
									PROBLEM_INDICATION_COLOR.equals(p);
									
		return affects ? affects : super.affectsTextPresentation(event);
	}
	
	/*
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 */
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		return new AdaptedSourceViewer(parent, ruler, styles);
	}
		
	/*
	 * @see IReconcilingParticipant#reconciled()
	 */
	public void reconciled() {
		if (!WorkInProgressPreferencePage.synchronizeOutlineOnCursorMove()) {
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
}