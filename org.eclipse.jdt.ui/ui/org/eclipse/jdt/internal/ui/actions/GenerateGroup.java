/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.actions;


import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.ui.IContextMenuConstants;

public class GenerateGroup extends ContextMenuGroup {

	public static final String GROUP_NAME= IContextMenuConstants.GROUP_GENERATE;
	
	private AddUnimplementedMethodsAction fAddUnimplementedMethods;
	private AddMethodStubAction fAddMethodStub;
	private AddGetterSetterAction fAddGetterSetter;
	
	public void fill(IMenuManager manager, GroupContext context) {

		createActions(context.getSelectionProvider());

		if (fAddUnimplementedMethods.canActionBeAdded()) {
			manager.appendToGroup(GROUP_NAME, fAddUnimplementedMethods);
		}
		
		if (fAddGetterSetter.canActionBeAdded()) {
			manager.appendToGroup(GROUP_NAME, fAddGetterSetter);
		}		
	}
	
	private void createActions(ISelectionProvider provider) {
		if (fAddUnimplementedMethods != null)
			return;
		
		fAddUnimplementedMethods= new AddUnimplementedMethodsAction(provider);
		fAddGetterSetter= new AddGetterSetterAction(provider);
	}
}