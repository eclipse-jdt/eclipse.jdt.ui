package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AddMethodEntryBreakpointAction;
import org.eclipse.jdt.internal.ui.actions.AddWatchpointAction;
import org.eclipse.jdt.internal.ui.actions.OpenImportDeclarationAction;
import org.eclipse.jdt.internal.ui.actions.OpenSuperImplementationAction;
import org.eclipse.jdt.internal.ui.actions.ShowInPackageViewAction;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;



/**
 * Java specific text editor.
 */
public abstract class JavaEditor extends AbstractTextEditor implements ISelectionChangedListener {
		
	/** The outline page */
	protected JavaOutlinePage fOutlinePage;
	
	/** Outliner context menu Id */
	protected String fOutlinerContextMenuId;	
		
	/**
	 * Returns the most narrow java element including the given offset
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
	}
	
	/**
	 * @see AbstractTextEditor#createSourceViewer(Composite, IVerticalRuler, int)
	 * 
	 * This is the code that can be found in 1.0 fixing the bidi rendering of Java code.
	 * Looking for something less vulernable in this stream.
	 *
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		ISourceViewer viewer= super.createSourceViewer(parent, ruler, styles);
		StyledText text= viewer.getTextWidget();
		text.setBidiColoring(true);
		return viewer;
	}
	 */
	
	/**
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
	 * @see AbstractTextEditor#editorContextMenuAboutToShow
	 */
	public void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		
		addGroup(menu, ITextEditorActionConstants.GROUP_EDIT, IContextMenuConstants.GROUP_REORGANIZE);
		addGroup(menu, ITextEditorActionConstants.GROUP_EDIT, IContextMenuConstants.GROUP_GENERATE);
		addGroup(menu, ITextEditorActionConstants.GROUP_EDIT, IContextMenuConstants.GROUP_NEW);
		
		new JavaSearchGroup(false).fill(menu, ITextEditorActionConstants.GROUP_FIND, isTextSelectionEmpty());
		addAction(menu, ITextEditorActionConstants.GROUP_FIND, "ShowJavaDoc");
		addAction(menu, ITextEditorActionConstants.GROUP_FIND, "OpenSuperImplementation");
		menu.appendToGroup(ITextEditorActionConstants.GROUP_FIND, new ShowInPackageViewAction());
	}			
	
	/**
	 * Creates the outline page used with this editor.
	 */
	protected JavaOutlinePage createOutlinePage() {
		
		JavaOutlinePage page= new JavaOutlinePage(fOutlinerContextMenuId, this);
		
		page.addSelectionChangedListener(this);
		setOutlinePageInput(page, getEditorInput());
		
		// page.setAction("ShowTypeHierarchy", new ShowTypeHierarchyAction(page));	//$NON-NLS-1$
		page.setAction("OpenImportDeclaration", new OpenImportDeclarationAction(page)); //$NON-NLS-1$
		page.setAction("ShowInPackageView", new ShowInPackageViewAction()); //$NON-NLS-1$
		page.setAction("AddMethodEntryBreakpoint", new AddMethodEntryBreakpointAction(page)); //$NON-NLS-1$
		page.setAction("AddWatchpoint", new AddWatchpointAction(page)); // $NON-NLS-1$
		StructuredSelectionProvider selectionProvider= StructuredSelectionProvider.createFrom(page);
		page.setAction("OpenSuperImplementation", new OpenSuperImplementationAction(selectionProvider)); // $NON-NLS-1$
		return page;
	}
	
	/**
	 * Informs the editor that its outliner has been closed.
	 */
	public void outlinePageClosed() {
		if (fOutlinePage != null) {
			fOutlinePage.removeSelectionChangedListener(this);
			fOutlinePage= null;
			resetHighlightRange();
		}
	}
	
	/*
	 * Get the dektop's StatusLineManager
	 */
	protected IStatusLineManager getStatusLineManager() {
		IEditorActionBarContributor contributor= getEditorSite().getActionBarContributor();
		if (contributor instanceof EditorActionBarContributor) {
			return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
		}
		return null;
	}	
	
	/**
	 * @see AbstractTextEditor#getAdapter(Class)
	 */
	public Object getAdapter(Class required) {
		
		if (IContentOutlinePage.class.equals(required)) {
			if (fOutlinePage == null)
				fOutlinePage= createOutlinePage();
			return fOutlinePage;
		}
		return super.getAdapter(required);
	}
	
	protected void setSelection(ISourceReference reference, boolean moveCursor) {
		
		if (reference != null) {
			
			try {
				
				ISourceRange range= reference.getSourceRange();
				if (range == null)
					return;
				
				int offset= range.getOffset();
				int length= range.getLength();
				
				if (offset > -1 && length >= 0) {
				
					setHighlightRange(offset, length, moveCursor);
					
					if (moveCursor && (reference instanceof IMember)) {
						range= ((IMember) reference).getNameRange();
						offset= range.getOffset();
						length= range.getLength();
						if (range != null && offset > -1 && length > 0) {
							if (getSourceViewer() != null) {
								getSourceViewer().revealRange(offset, length);
								getSourceViewer().setSelectedRange(offset, length);
							}
						}
					}
				}
				
				return;
				
			} catch (JavaModelException x) {
			} catch (IllegalArgumentException x) {
			}
		}
		
		if (moveCursor)
			resetHighlightRange();
	}
		
	public void setSelection(IJavaElement element) {
		
		if (element == null || element instanceof ICompilationUnit) {
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
				fOutlinePage.removeSelectionChangedListener(this);
				fOutlinePage.select(reference);
				fOutlinePage.addSelectionChangedListener(this);
			}
		}
	}
	
	public void selectionChanged(SelectionChangedEvent event) {
				
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
		setSelection(reference, !isActivePart());
	}
	
	/**
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
						fOutlinePage.removeSelectionChangedListener(this);
						fOutlinePage.select((ISourceReference) element);
						fOutlinePage.addSelectionChangedListener(this);
					}
					return;
				}
				element= element.getParent();
			}
			
		} catch (JavaModelException x) {
		}
		
		resetHighlightRange();
	}
			
	protected boolean isActivePart() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IPartService service= window.getPartService();
		return (this == service.getActivePart());
	}
	
	/**
	 * @see AbstractTextEditor#doSetInput
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		setOutlinePageInput(fOutlinePage, input);
	}
	
	protected void createActions() {
		super.createActions();
		
		setAction("ShowJavaDoc", new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ShowJavaDoc.", this, ISourceViewer.INFORMATION));
		
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(getSite().getWorkbenchWindow().getSelectionService());
		setAction("OpenSuperImplementation", new OpenSuperImplementationAction(provider)); //$NON-NLS-1$
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
	
	/**
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		super.handlePreferenceStoreChanged(event);
		
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
		}
	}
}