/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.jdt.internal.core.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.actions.StructuredSelectionProvider;

public class CopyAction extends ReorgDestinationAction {
	
	public CopyAction(StructuredSelectionProvider provider) {
		this(ReorgMessages.getString("copyAction.label"), provider); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("copyAction.description")); //$NON-NLS-1$
	}

	public CopyAction(String name, StructuredSelectionProvider provider) {
		super(name, provider);
	}
	
	ReorgRefactoring createRefactoring(List elements){
		return new CopyRefactoring(elements);
	}
	
	String getActionName() {
		return ReorgMessages.getString("copyAction.name"); //$NON-NLS-1$
	}
	
	String getDestinationDialogMessage() {
		return ReorgMessages.getString("copyAction.destination.label"); //$NON-NLS-1$
	}
}
