package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
import java.util.ResourceBundle;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;

/**
 * This action opens a java editor on the element represented by text selection of
 * the connected java source editor. In addition, if the element is a type, it also 
 * opens shows the element in the type hierarchy viewer.
 */
public class OpenHierarchyOnSelectionAction extends OpenOnSelectionAction {
	
	public OpenHierarchyOnSelectionAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	
	public OpenHierarchyOnSelectionAction(ResourceBundle bundle, String prefix) {
		super(bundle, prefix);
	}
	
	/**
	 * @see OpenJavaElementAction#open
	 */
	protected void open(ISourceReference sourceReference) throws JavaModelException, PartInitException {
		super.open(sourceReference);
		openClassHierarchy(getType(sourceReference), getMember(sourceReference));
	}
	
	protected void openClassHierarchy(IType type, IMember member) {
		if (type == null) {
			getShell().getDisplay().beep();
		} else {
			IWorkbenchPage page= JavaPlugin.getDefault().getActivePage();
			try {
				
				IViewPart view= view= page.showView(JavaUI.ID_TYPE_HIERARCHY);
				if (view instanceof TypeHierarchyViewPart) {
					TypeHierarchyViewPart part= (TypeHierarchyViewPart) view;
					
					if (type != null)
						part.setInput(type);
					
					if (member != null)
						part.selectMember(member);
				}
				
			} catch (PartInitException e) {
				MessageDialog.openError(getShell(), "Error in OpenHierarchyOnSelectionAction", e.getMessage());
			}
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