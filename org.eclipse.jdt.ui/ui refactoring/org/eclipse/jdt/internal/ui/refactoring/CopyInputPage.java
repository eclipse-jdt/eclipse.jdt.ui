package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.changes.CopyRefactoring;
import org.eclipse.jdt.internal.core.refactoring.changes.ReorgRefactoring;

public class CopyInputPage extends ReorgInputPage {
	
	public CopyInputPage() {
		super("CopyPage");
		setDescription("description");
	}
		
	/**
	 * @see ReorgInputPage#getValidationMessage(Object)
	 */	
	String getValidationMessage(Object element){
		if (getCopyRefactoring().canCopyTo(element))
			return null;
		else
			return "Invalid Selection";	
	}
	
	/**
	 * @see ReorgInputPage#updateMessage(Object)
	 */
	void updateMessage(Object element){
		setMessage("Copy selected elements to " + ReorgRefactoring.getElementName(element));
	}
	
	CopyRefactoring getCopyRefactoring(){
		return (CopyRefactoring)getRefactoring();
	}
}

