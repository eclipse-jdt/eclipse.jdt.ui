package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.IMenuManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;

import org.eclipse.jdt.ui.actions.OpenActionGroup;
import org.eclipse.jdt.ui.actions.ShowActionGroup;

/**
 * Java specific text editor.
 */
public class ClassFileEditor extends JavaEditor {
	
	/** The standard action groups added to the menu */
	/* package */ ActionGroup fStandardActionGroups;
	
	/**
	 * Default constructor.
	 */
	public ClassFileEditor() {
		super();
		setDocumentProvider(JavaPlugin.getDefault().getClassFileDocumentProvider());
		setEditorContextMenuId("#ClassFileEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#ClassFileRulerContext"); //$NON-NLS-1$
		setOutlinerContextMenuId("#ClassFileOutlinerContext"); //$NON-NLS-1$
		setHelpContextId(IJavaHelpContextIds.CLASS_FILE_EDITOR);
	}
	
	/**
	 * @see AbstractTextEditor#createActions()
	 */
	protected void createActions() {
		super.createActions();
		
		setAction(ITextEditorActionConstants.SAVE, null);
		setAction(ITextEditorActionConstants.REVERT_TO_SAVED, null);
		
		/*
		 * 1GF82PL: ITPJUI:ALL - Need to be able to add bookmark to classfile
		 *
		 *  // replace default action with class file specific ones
		 *
		 *	setAction(ITextEditorActionConstants.BOOKMARK, new AddClassFileMarkerAction("AddBookmark.", this, IMarker.BOOKMARK, true)); //$NON-NLS-1$
		 *	setAction(ITextEditorActionConstants.ADD_TASK, new AddClassFileMarkerAction("AddTask.", this, IMarker.TASK, false)); //$NON-NLS-1$
		 *	setAction(ITextEditorActionConstants.RULER_MANAGE_BOOKMARKS, new ClassFileMarkerRulerAction("ManageBookmarks.", getVerticalRuler(), this, IMarker.BOOKMARK, true)); //$NON-NLS-1$
		 *	setAction(ITextEditorActionConstants.RULER_MANAGE_TASKS, new ClassFileMarkerRulerAction("ManageTasks.", getVerticalRuler(), this, IMarker.TASK, true)); //$NON-NLS-1$
		 */
		setAction(ITextEditorActionConstants.BOOKMARK, null);
		setAction(ITextEditorActionConstants.ADD_TASK, null);
		
		fStandardActionGroups= new CompositeActionGroup(
			new ActionGroup[] {new OpenActionGroup(this), new ShowActionGroup(this) });
	}
	
	/**
	 * @see JavaEditor#getElementAt(int)
	 */
	protected IJavaElement getElementAt(int offset) {
		if (getEditorInput() instanceof IClassFileEditorInput) {
			try {
				IClassFileEditorInput input= (IClassFileEditorInput) getEditorInput();
				return input.getClassFile().getElementAt(offset);
			} catch (JavaModelException x) {
			}
		}
		return null;
	}
	
	/**
	 * @see JavaEditor#getCorrespondingElement(IJavaElement)
	 */
	protected IJavaElement getCorrespondingElement(IJavaElement element) {
		if (getEditorInput() instanceof IClassFileEditorInput) {
			IClassFileEditorInput input= (IClassFileEditorInput) getEditorInput();
			IJavaElement parent= JavaModelUtil.findParentOfKind(element, IJavaElement.CLASS_FILE);
			if (input.getClassFile().equals(parent))
				return element;
		}
		return null;
	}
	
	/**
	 * @see IEditorPart#saveState(IMemento)
	 */
	public void saveState(IMemento memento) {
	}
	
	/**
	 * @see JavaEditor#setOutlinePageInput(JavaOutlinePage, IEditorInput)
	 */
	protected void setOutlinePageInput(JavaOutlinePage page, IEditorInput input) {
		if (page != null && input instanceof IClassFileEditorInput) {
			IClassFileEditorInput cfi= (IClassFileEditorInput) input;
			page.setInput(cfi.getClassFile());
		}
	}
	
	/*
	 * 1GEPKT5: ITPJUI:Linux - Source in editor for external classes is editable
	 * Removed methods isSaveOnClosedNeeded and isDirty.
	 * Added method isEditable.
	 */
	/**
	 * @see AbstractTextEditor#isEditable()
	 */
	public boolean isEditable() {
		return false;
	}
	
	/**
	 * Translates the given editor input into an <code>ExternalClassFileEditorInput</code>
	 * if it is a file editor input representing an external class file.
	 * 
	 * @param input the editor input to be transformed if necessary
	 * @return the transformed editor input
	 */
	protected IEditorInput transformEditorInput(IEditorInput input) {
		
		if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) input).getFile();
			IClassFileEditorInput classFileInput= new ExternalClassFileEditorInput(file);
			if (classFileInput.getClassFile() != null)
				input= classFileInput;
		}
		
		return input;
	}
	
	/**
	 * @see AbstractTextEditor#doSetInput(IEditorInput)
	 */
	protected void doSetInput(IEditorInput input) throws CoreException {
		input= transformEditorInput(input);
		if (!(input instanceof IClassFileEditorInput))
			throw new CoreException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_RESOURCE_TYPE, JavaEditorMessages.getString("ClassFileEditor.error.invalid_input_message"))); //$NON-NLS-1$
			
		super.doSetInput(input);
	}
}