/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyMessages;
import org.eclipse.jdt.internal.ui.viewsupport.MemberFilter;
import org.eclipse.jdt.internal.ui.viewsupport.MemberFilterAction;

/**
 * Action Group that contributes filter buttons for a views showing methods and fields:
 * Filter public / static & fields.
 * The action group installs a filter on a structured viewer. The filter is connected to the actions and is updated
 * when the state of the buttons changes.
 *  
 * @since 2.0
 */
public class MemberFilterActionGroup extends ActionGroup {

	public static final int FILTER_NONPUBLIC= MemberFilter.FILTER_NONPUBLIC;
	public static final int FILTER_STATIC= MemberFilter.FILTER_STATIC;
	public static final int FILTER_FIELDS= MemberFilter.FILTER_FIELDS;
	
	private static final String TAG_HIDEFIELDS= "hidefields"; //$NON-NLS-1$
	private static final String TAG_HIDESTATIC= "hidestatic"; //$NON-NLS-1$
	private static final String TAG_HIDENONPUBLIC= "hidenonpublic"; //$NON-NLS-1$
	
	private MemberFilterAction[] fFilterActions;
	private MemberFilter fFilter;
	
	private StructuredViewer fViewer;
	private String fViewerId;
	
	
	/**
	 * Creates the action group for a viewer. A filter is added to the viewer that is connected to the
	 * toolbar buttons that can be added to the toolbar with <code>fillActionBars</code>.
	 * @param viewer The viewer to be filtered.
	 * @param viewerId A unique id of the viewer. Used as a key to to store the last used filter
	 * settings in the preference store.
	 */
	public MemberFilterActionGroup(StructuredViewer viewer, String viewerId) {
		fViewer= viewer;
		fViewerId= viewerId;
		
		// get initial values
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		boolean doHideFields= store.getBoolean(getPreferenceKey(FILTER_FIELDS));
		boolean doHideStatic= store.getBoolean(getPreferenceKey(FILTER_STATIC));
		boolean doHidePublic= store.getBoolean(getPreferenceKey(FILTER_NONPUBLIC));

		fFilter= new MemberFilter();
		if (doHideFields)
			fFilter.addFilter(FILTER_FIELDS);
		if (doHideStatic)
			fFilter.addFilter(FILTER_STATIC);			
		if (doHidePublic)
			fFilter.addFilter(FILTER_NONPUBLIC);		
	
		// fields
		String title= TypeHierarchyMessages.getString("MethodsViewer.hide_fields.label"); //$NON-NLS-1$
		String helpContext= IJavaHelpContextIds.FILTER_FIELDS_ACTION;
		MemberFilterAction hideFields= new MemberFilterAction(this, title, FILTER_FIELDS, helpContext, doHideFields);
		hideFields.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.description")); //$NON-NLS-1$
		hideFields.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.tooltip.checked")); //$NON-NLS-1$
		hideFields.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_fields.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideFields, "fields_co.gif"); //$NON-NLS-1$
		
		// static
		title= TypeHierarchyMessages.getString("MethodsViewer.hide_static.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_STATIC_ACTION;
		MemberFilterAction hideStatic= new MemberFilterAction(this, title, FILTER_STATIC, helpContext, doHideStatic);
		hideStatic.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_static.description")); //$NON-NLS-1$
		hideStatic.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_static.tooltip.checked")); //$NON-NLS-1$
		hideStatic.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_static.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideStatic, "static_co.gif"); //$NON-NLS-1$
		
		// non-public
		title= TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_PUBLIC_ACTION;
		MemberFilterAction hideNonPublic= new MemberFilterAction(this, title, FILTER_NONPUBLIC, helpContext, doHidePublic);
		hideNonPublic.setDescription(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.description")); //$NON-NLS-1$
		hideNonPublic.setToolTipChecked(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.tooltip.checked")); //$NON-NLS-1$
		hideNonPublic.setToolTipUnchecked(TypeHierarchyMessages.getString("MethodsViewer.hide_nonpublic.tooltip.unchecked")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideNonPublic, "public_co.gif"); //$NON-NLS-1$
	
		// order corresponds to order in toolbar
		fFilterActions= new MemberFilterAction[] { hideFields, hideStatic, hideNonPublic };
		
		fViewer.addFilter(fFilter);
	}
	
	private String getPreferenceKey(int filterProperty) {
		return "MemberFilterActionGroup." + fViewerId + '.' + String.valueOf(filterProperty); //$NON-NLS-1$
	}
	
	/**
	 * Set the current filter.
	 * @param filterProperty Constants FILTER_FIELDS, FILTER_PUBLIC & FILTER_PRIVATE as defined by this
	 * action group
	 * @param set The new value. If true, the elements of the given types are filtered.
	 */	
	public void setMemberFilter(int filterProperty, boolean set) {
		if (set) {
			fFilter.addFilter(filterProperty);
		} else {
			fFilter.removeFilter(filterProperty);
		}
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		
		for (int i= 0; i < fFilterActions.length; i++) {
			int currProperty= fFilterActions[i].getFilterProperty();
			if (currProperty == filterProperty) {
				fFilterActions[i].setChecked(set);
			}
			store.setValue(getPreferenceKey(currProperty), hasMemberFilter(currProperty));
		}
		fViewer.refresh();
	}

	/**
	 * Returns <code>true</code> if the given filter is set.
	 * @param filterProperty Constants FILTER_FIELDS, FILTER_PUBLIC & FILTER_PRIVATE as defined by this
	 * action group
	 */	
	public boolean hasMemberFilter(int filterProperty) {
		return fFilter.hasFilter(filterProperty);
	}
	
	/**
	 * Saves the state of the filter actions in a memento.
	 */
	public void saveState(IMemento memento) {
		memento.putString(TAG_HIDEFIELDS, String.valueOf(hasMemberFilter(FILTER_FIELDS)));
		memento.putString(TAG_HIDESTATIC, String.valueOf(hasMemberFilter(FILTER_STATIC)));
		memento.putString(TAG_HIDENONPUBLIC, String.valueOf(hasMemberFilter(FILTER_NONPUBLIC)));
	}
	
	/**
	 * Restores the state of the filter actions from a memento.
	 */	
	public void restoreState(IMemento memento) {
		boolean set= Boolean.valueOf(memento.getString(TAG_HIDEFIELDS)).booleanValue();
		setMemberFilter(FILTER_FIELDS, set);
		set= Boolean.valueOf(memento.getString(TAG_HIDESTATIC)).booleanValue();
		setMemberFilter(FILTER_STATIC, set);
		set= Boolean.valueOf(memento.getString(TAG_HIDENONPUBLIC)).booleanValue();
		setMemberFilter(FILTER_NONPUBLIC, set);		
	}


	/* (non-Javadoc)
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		contributeToToolBar(actionBars.getToolBarManager());
	};
	
	/**
	 * Adds the filter actions to the tool bar
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(fFilterActions[0]); // fields
		tbm.add(fFilterActions[1]); // static
		tbm.add(fFilterActions[2]); // public
	}
	
	/* (non-Javadoc)
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		fViewer.removeFilter(fFilter);
		super.dispose();
	}

}
