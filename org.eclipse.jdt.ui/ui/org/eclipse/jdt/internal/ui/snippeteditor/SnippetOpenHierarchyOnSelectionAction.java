package org.eclipse.jdt.internal.ui.snippeteditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;

/**
 * This action opens a Java editor on the element represented by text selection of
 * the connected java source editor. In addition, if the element is a type, it also 
 * opens shows the element in the type hierarchy viewer.
 */
public class SnippetOpenHierarchyOnSelectionAction extends SnippetOpenOnSelectionAction {
	
	public SnippetOpenHierarchyOnSelectionAction(JavaSnippetEditor editor) {
		super(editor);
	}
	
	public SnippetOpenHierarchyOnSelectionAction() {
		this(null);
	}
	
	protected void setResources() {
		setText(SnippetMessages.getString("SnippetOpenHierarchyOnSelectionAction.label")); //$NON-NLS-1$
		setDescription(SnippetMessages.getString("SnippetOpenHierarchyOnSelectionAction.tooltip")); //$NON-NLS-1$
		setToolTipText(SnippetMessages.getString("SnippetOpenHierarchyOnSelectionAction.description")); //$NON-NLS-1$
		setDialogTitle(SnippetMessages.getString("SnippetOpenHierarchyOnSelectionDialog.title")); //$NON-NLS-1$
		setDialogMessage(SnippetMessages.getString("SnippetOpenHierarchyOnSelectionDialog.message")); //$NON-NLS-1$
	}

	/**
	 * @see AbstractOpenJavaElementAction#open
	 */
	protected void open(ISourceReference element) throws JavaModelException, PartInitException {
		if (element instanceof IMember) {
			OpenTypeHierarchyUtil.open(new IMember[] { (IMember) element }, fEditor.getSite().getWorkbenchWindow());
		} else {
			getShell().getDisplay().beep();
		}
	}
	
}

