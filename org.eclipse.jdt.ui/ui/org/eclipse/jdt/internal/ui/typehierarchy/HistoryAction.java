/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

/**
 * Action used for the type hierarchy forward / backward buttons
 */
public class HistoryAction extends Action {

	private TypeHierarchyViewPart fViewPart;
	private int fIndex;
	
	public HistoryAction(TypeHierarchyViewPart viewPart, int index, IType type) {
		super();
		fViewPart= viewPart;
		fIndex= index;		
		
		String fullTypeName= JavaModelUtil.getFullyQualifiedName(type);
		setText(fullTypeName);
		try {
			if (type.isClass()) {
				setImageDescriptor(JavaPluginImages.DESC_OBJS_CLASS);
			} else {
				setImageDescriptor(JavaPluginImages.DESC_OBJS_INTERFACE);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		
		setDescription(TypeHierarchyMessages.getFormattedString("HistoryAction.description", fullTypeName)); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getFormattedString("HistoryAction.tooltip", fullTypeName)); //$NON-NLS-1$
	}	
	
	/*
	 * @see Action#run()
	 */
	public void run() {
		fViewPart.gotoHistoryEntry(fIndex);
	}
	
}
