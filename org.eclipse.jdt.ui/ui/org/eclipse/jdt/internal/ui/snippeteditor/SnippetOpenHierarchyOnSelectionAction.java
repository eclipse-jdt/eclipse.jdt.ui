package org.eclipse.jdt.internal.ui.snippeteditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyHelper;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PartInitException;

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
	 * @see OpenJavaElementAction#open
	 */
	protected void open(ISourceReference sourceReference) throws JavaModelException, PartInitException {
		IType type= getType(sourceReference);
		if (type != null) {
			OpenTypeHierarchyHelper helper= new OpenTypeHierarchyHelper();
			helper.open(new IType[] { type }, fEditor.getSite().getWorkbenchWindow());
			helper.selectMember(getMember(sourceReference));
		} else {
			getShell().getDisplay().beep();
		}
	}
	
	protected IType getType(ISourceReference sourceReference) {
		if ( !(sourceReference instanceof IJavaElement))
			return null;
			
		IJavaElement e= (IJavaElement) sourceReference;
		
		while (e != null) {
			if (e instanceof IType)
				return (IType) e;
			e= e.getParent();
		}
		
		return null;
	}
	
	protected IMember getMember(ISourceReference sourceReference) {
		if (sourceReference instanceof IMember)
			return (IMember) sourceReference;
		return null;
	}
}

