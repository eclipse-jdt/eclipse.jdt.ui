package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;import java.util.ResourceBundle;import org.eclipse.core.runtime.CoreException;import org.eclipse.jface.action.IMenuManager;import org.eclipse.jface.action.IStatusLineManager;import org.eclipse.jface.action.MenuManager;import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.SelectionChangedEvent;import org.eclipse.ui.IEditorActionBarContributor;import org.eclipse.ui.IEditorInput;import org.eclipse.ui.IPartService;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.part.EditorActionBarContributor;import org.eclipse.ui.texteditor.AbstractTextEditor;import org.eclipse.ui.texteditor.DefaultRangeIndicator;import org.eclipse.ui.texteditor.ITextEditorActionConstants;import org.eclipse.ui.views.contentoutline.IContentOutlinePage;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.ISourceRange;import org.eclipse.jdt.core.ISourceReference;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.IContextMenuConstants;import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;import org.eclipse.jdt.ui.text.JavaTextTools;import org.eclipse.jdt.internal.debug.ui.display.InspectAction;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.AddMethodEntryBreakpointAction;import org.eclipse.jdt.internal.ui.actions.OpenImportDeclarationAction;import org.eclipse.jdt.internal.ui.actions.ShowInPackageViewAction;import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;



/**
 * Java specific text editor.
 */
public abstract class JavaEditor extends AbstractTextEditor implements ISelectionChangedListener {
		
	/** The outline page */
	protected JavaOutlinePage fOutlinePage;
	
	/** Outliner context menu Id */
	protected String fOutlinerContextMenuId;
	
	/** The resource bundle */
	private ResourceBundle fResourceBundle;
		
	
	/**
	 * Returns the smallest ISourceReference also implementing IJavaElement
	 * containing the given position.
	 */
	abstract protected ISourceReference getJavaSourceReferenceAt(int position);
	
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
	 * Returns the editor's resource bundle.
	 *
	 * @return the editor's resource bundle
	 */
	protected ResourceBundle getResourceBundle() {
		return JavaPlugin.getDefault().getResourceBundle();
	}
	
	/**
	 * Convenience method for safely accessing resources.
	 */
	protected String getResourceString(String key) {
		return JavaPlugin.getDefault().getResourceString(key);
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
		
		MenuManager search= new JavaSearchGroup().getMenuManagerForGroup(isTextSelectionEmpty());
		menu.appendToGroup(ITextEditorActionConstants.GROUP_FIND, search);
		addAction(menu, "Inspect");
		addAction(menu, "Display");
		addAction(menu, "RunToLine");

	}			
	
	/**
	 * Creates the outline page used with this editor.
	 */
	protected JavaOutlinePage createOutlinePage() {
		
		JavaOutlinePage page= new JavaOutlinePage(fOutlinerContextMenuId, this);
		
		page.addSelectionChangedListener(this);
		setOutlinePageInput(page, getEditorInput());
		
		// page.setAction("ShowTypeHierarchy", new ShowTypeHierarchyAction(page));
		page.setAction("OpenImportDeclaration", new OpenImportDeclarationAction(page));
		page.setAction("ShowInPackageView", new ShowInPackageViewAction(getSite(), page));
		page.setAction("AddMethodEntryBreakpoint", new AddMethodEntryBreakpointAction(page));
	
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
				int offset= range.getOffset();
				int length= range.getLength();
				
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
				
				return;
				
			} catch (JavaModelException x) {
			} catch (IllegalArgumentException x) {
			}
		}
		
		if (moveCursor)
			resetHighlightRange();
	}
		
	public void setSelection(ISourceReference reference) {
		
		try {
			// find this source reference in this editor's working copy
			reference= getJavaSourceReferenceAt(reference.getSourceRange().getOffset());
		} catch (JavaModelException x) {
			// be tolerant, just go with what is there
		}
		
		// set hightlight range
		setSelection(reference, true);
		
		// set outliner selection
		if (fOutlinePage != null) {
			fOutlinePage.removeSelectionChangedListener(this);
			fOutlinePage.select(reference);
			fOutlinePage.addSelectionChangedListener(this);
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
			
			ISourceReference reference= getJavaSourceReferenceAt(offset);
			while (reference != null) {
				ISourceRange range= reference.getSourceRange();
				if (offset < range.getOffset() + range.getLength() && range.getOffset() < offset + length) {
					setHighlightRange(range.getOffset(), range.getLength(), true);
					if (fOutlinePage != null) {
						fOutlinePage.removeSelectionChangedListener(this);
						fOutlinePage.select(reference);
						fOutlinePage.addSelectionChangedListener(this);
					}
					return;
				}
				IJavaElement parent= ((IJavaElement) reference).getParent();
				if (parent instanceof ISourceReference)
					reference= (ISourceReference) parent;
				else
					reference= null;
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
		setAction("Display", new EditorDisplayAction(getResourceBundle(), "Editor.Display.", this));
		setAction("Inspect", new InspectAction(getResourceBundle(), "Editor.Inspect.", this));
		setAction("RunToLine", new RunToLineAction(getResourceBundle(), "Editor.RunToLine.", this));
	}
	
	private boolean isTextSelectionEmpty() {
		ISelection selection= getSelectionProvider().getSelection();
		if (!(selection instanceof ITextSelection))
			return true;
		return ((ITextSelection)selection).getLength() == 0;	
	}
}