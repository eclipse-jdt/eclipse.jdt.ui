/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * Drop down action for toolbars containing <code>ClasspathModifierAction</code>s.
 * The drop down action manages a list of actions that are displayed when invocing 
 * the drop down. If there is at least one valid action, then the drop down action 
 * itself will also be valid and invocing run will delegate the call to the 
 * first valid action in the list.
 */
//TODO: Fix (action is changed but not description/image/...)
public class ClasspathModifierDropDownAction extends Action implements IClasspathModifierAction , IMenuCreator, ISelectionChangedListener {
    
    /** The menu to be populated with items*/
    private Menu fMenu;
    /** A list of actions that will be used as 
     * drop down items*/
    protected List fActions;
    /** Index of the action that can be executed when clicking directly on the dropdown button.*/
    private int fIndex;
    
    /**
     * Create a drop down action using the same descriptors as the provided action, but it's on 
     * tool tip text. The action will automatically be put in the list of actions that are 
     * managed by this drop down menu.
     * 
     * @param action an action to be added to the dropdown menu
     * @param text a label text for the action
     * @param toolTipText the tooltip text for the drop down menu
     */
    public ClasspathModifierDropDownAction(IClasspathModifierAction action, String text, String toolTipText) {
        super(text, IAction.AS_DROP_DOWN_MENU);
        
        fActions= new ArrayList();
        fActions.add(action);
        fIndex= 0;
        
        setImageDescriptor(action.getImageDescriptor());
        setDisabledImageDescriptor(action.getDisabledImageDescriptor());
        setToolTipText(toolTipText);
    }
    
    /**
     * Runs the first action of the list of managed actions that is valid.
     */
    public void run() {
        IClasspathModifierAction action= (IClasspathModifierAction)fActions.get(fIndex);
        action.run();
    }

    public IMenuCreator getMenuCreator() {
        return this;
    }

    public Menu getMenu(Control parent) {
        if (fMenu != null) {
            fMenu.dispose();
        }
        fMenu = new Menu(parent);
        createEntries(fMenu);
        return fMenu;

    }

    public Menu getMenu(Menu parent) {
        return fMenu;
    }
    
    /**
     * Add dynamically an action to the drop down menu.
     * 
     * @param action the action to be added
     */
    public void addAction(IClasspathModifierAction action) {
        fActions.add(action);
    }
    
    /**
     * Add dynamically an array of actions to the 
     * drop down menu.
     * 
     * @param actions an array of actions to be added
     */
    public void addActions(IClasspathModifierAction[] actions) {
        for(int i= 0; i < actions.length; i++) {
            addAction(actions[i]);
        }
    }
    
    /**
     * Remove an action from the drop down menu
     *  
     * @param action the action to be removed
     */
    public void removeAction(IClasspathModifierAction action) {
        fActions.remove(action);
    }
    
    /**
     * Get all actions within this drop down menu.
     * 
     * @return an array of actions
     */
    public IClasspathModifierAction[] getActions() {
        return (IClasspathModifierAction[])fActions.toArray(new IClasspathModifierAction[fActions.size()]);
    }
    
    /**
     * Populate the menu with the given action item
     *  
     * @param parent the menu to add an action for
     * @param action the action to be added
     */
    private void addActionToMenu(Menu parent, IAction action) {
        ActionContributionItem item = new ActionContributionItem(action);
        item.fill(parent, -1);
    }
    
    /**
     * Fill the menu with all actions
     * 
     * @param menu the menu to be populated
     */
    private void createEntries(Menu menu) {
        for(int i= 0; i < fActions.size(); i++) {
            IAction action= (IAction)fActions.get(i);
            addActionToMenu(menu, action);
        }
    }
    
    public void dispose() {
        if (fMenu != null) {
            fMenu.dispose();
            fMenu = null;
        }
    }

	/**
     * {@inheritDoc}
     */
    public int getTypeId() {
	    return getFirstAction().getTypeId();
    }
    
    private IClasspathModifierAction getFirstAction() {
	    return (IClasspathModifierAction)fActions.get(0);
    }

	/**
     * {@inheritDoc}
     */
    public void selectionChanged(SelectionChangedEvent event) {
    	for (Iterator iterator= fActions.iterator(); iterator.hasNext();) {
	        IClasspathModifierAction action= (IClasspathModifierAction)iterator.next();
	        if (action instanceof ISelectionChangedListener) {
	        	((ISelectionChangedListener)action).selectionChanged(event);
	        }
        }
    	int i= 0;
    	for (Iterator iterator= fActions.iterator(); iterator.hasNext();) {
	        IClasspathModifierAction action= (IClasspathModifierAction)iterator.next();
	        if (action.isEnabled()) {
	        	setEnabled(true);
	        	fIndex= i;
	        	return;
	        }
	        i++;
        }
    	setEnabled(false);
    }
}
