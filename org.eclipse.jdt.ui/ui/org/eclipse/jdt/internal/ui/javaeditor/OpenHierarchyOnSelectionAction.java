package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;



/**
 * This action opens a java editor on the element represented by text selection of
 * the connected java source editor. In addition, if the element is a type, it also 
 * opens shows the element in the type hierarchy viewer.
 */
public class OpenHierarchyOnSelectionAction extends OpenOnSelectionAction {
	
	public OpenHierarchyOnSelectionAction(ImageDescriptor image, ITextEditor editor) {
		this();
		setImageDescriptor(image);
		setContentEditor(editor);
	}
	
	public OpenHierarchyOnSelectionAction() {
		super();
		setText(JavaEditorMessages.getString("OpenHierarchy.label")); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("OpenHierarchy.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("OpenHierarchy.description")); //$NON-NLS-1$
		setDialogTitle(JavaEditorMessages.getString("OpenHierarchy.dialog.title")); //$NON-NLS-1$
		setDialogMessage(JavaEditorMessages.getString("OpenHierarchy.dialog.message")); //$NON-NLS-1$
	}
	
	/**
	 * Overwrites OpenOnSelectionAction#setContentEditor
	 * No installation of a selection listener -> empty selection ok
	 */
	public void setContentEditor(ITextEditor editor) {
		fEditor= editor;
		setEnabled(editor != null);
	}
	
	/**
	 * @see OpenJavaElementAction#open
	 */
	protected void open(ISourceReference sourceReference) throws JavaModelException, PartInitException {
		if (sourceReference instanceof IMember) {
			IMember member= (IMember) sourceReference;
			OpenTypeHierarchyUtil.open(new IJavaElement[] { member }, fEditor.getSite().getWorkbenchWindow());
		} else {
			getShell().getDisplay().beep();
		}
	}
		
	/**
	 * @see OpenOnSelectionAction#openOnEmptySelection(ITextSelection)
	 */
	protected void openOnEmptySelection(ITextSelection selection) throws JavaModelException, PartInitException {
		if (fEditor instanceof JavaEditor) {
			open(((JavaEditor)fEditor).getJavaSourceReferenceAt(selection.getOffset()));
		}
	}
}