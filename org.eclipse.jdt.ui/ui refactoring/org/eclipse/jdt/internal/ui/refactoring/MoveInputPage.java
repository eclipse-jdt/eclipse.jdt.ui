package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.changes.MoveRefactoring;
import org.eclipse.jdt.internal.core.refactoring.changes.ReorgRefactoring;

public class MoveInputPage extends ReorgInputPage {

	public MoveInputPage() {
		super("MovePage");
		setDescription("description");
	}

	/**
	 * @see ReorgInputPage#getValidationMessage(Object)
	 */
	String getValidationMessage(Object element) {
		if (getMoveRefactoring().canMoveTo(element))
			return null;
		else
			return "Invalid Selection";	
	}

	/**
	 * @see ReorgInputPage#updateMessage(Object)
	 */
	void updateMessage(Object selectedElement) {
		setMessage("Move selected elements to " + ReorgRefactoring.getElementName(selectedElement));
	}

	private MoveRefactoring getMoveRefactoring(){
		return (MoveRefactoring)getRefactoring();
	}
}

