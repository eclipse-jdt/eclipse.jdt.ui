package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.util.ResourceBundle;import org.eclipse.jface.text.ITextSelection;import org.eclipse.ui.PartInitException;import org.eclipse.ui.texteditor.ITextEditor;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.ISourceReference;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyHelper;

/**
 * This action opens a java editor on the element represented by text selection of
 * the connected java source editor. In addition, if the element is a type, it also 
 * opens shows the element in the type hierarchy viewer.
 */
public class OpenHierarchyOnSelectionAction extends OpenOnSelectionAction {
	
	public OpenHierarchyOnSelectionAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		setEnabled(editor != null);
	}
	
	public OpenHierarchyOnSelectionAction(ResourceBundle bundle, String prefix) {
		this(bundle, prefix, null);
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
	
	/**
	 * @see OpenOnSelectionAction#openOnEmptySelection(ITextSelection)
	 */
	protected void openOnEmptySelection(ITextSelection selection) throws JavaModelException, PartInitException {
		if (fEditor instanceof JavaEditor) {
			open(((JavaEditor)fEditor).getJavaSourceReferenceAt(selection.getOffset()));
		}
	}
}