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
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
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
	public static final int SHOW_STATIC= MemberFilter.SHOW_STATIC;
	public static final int SHOW_FIELDS= MemberFilter.SHOW_FIELDS;
	
	/** deprecated */
	public static final int HIDE_STATIC= 8;
	/** deprecated */
	public static final int HIDE_FIELDS= 8;
	
	private static final String TAG_SHOWFIELDS= "showfields"; //$NON-NLS-1$
	private static final String TAG_SHOWSTATIC= "showstatic"; //$NON-NLS-1$
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
		boolean doShowFields= store.getBoolean(getPreferenceKey(SHOW_FIELDS));
		boolean doShowStatic= store.getBoolean(getPreferenceKey(SHOW_STATIC));
		boolean doHidePublic= store.getBoolean(getPreferenceKey(FILTER_NONPUBLIC));

		fFilter= new MemberFilter();
		if (doShowFields)
			fFilter.addProperty(SHOW_FIELDS);
		if (doShowStatic)
			fFilter.addProperty(SHOW_STATIC);			
		if (doHidePublic)
			fFilter.addProperty(FILTER_NONPUBLIC);		
	
		// fields
		String title= ActionMessages.getString("MemberFilterActionGroup.show_fields.label"); //$NON-NLS-1$
		String helpContext= IJavaHelpContextIds.FILTER_FIELDS_ACTION;
		MemberFilterAction hideFields= new MemberFilterAction(this, title, SHOW_FIELDS, helpContext, doShowFields);
		hideFields.setDescription(ActionMessages.getString("MemberFilterActionGroup.show_fields.description")); //$NON-NLS-1$
		hideFields.setToolTipText(ActionMessages.getString("MemberFilterActionGroup.show_fields.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideFields, "fields_co.gif"); //$NON-NLS-1$
		
		// static
		title= ActionMessages.getString("MemberFilterActionGroup.show_static.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_STATIC_ACTION;
		MemberFilterAction hideStatic= new MemberFilterAction(this, title, SHOW_STATIC, helpContext, doShowStatic);
		hideStatic.setDescription(ActionMessages.getString("MemberFilterActionGroup.show_static.description")); //$NON-NLS-1$
		hideStatic.setToolTipText(ActionMessages.getString("MemberFilterActionGroup.show_static.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideStatic, "static_co.gif"); //$NON-NLS-1$
		
		// non-public
		title= ActionMessages.getString("MemberFilterActionGroup.hide_nonpublic.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_PUBLIC_ACTION;
		MemberFilterAction hideNonPublic= new MemberFilterAction(this, title, FILTER_NONPUBLIC, helpContext, doHidePublic);
		hideNonPublic.setDescription(ActionMessages.getString("MemberFilterActionGroup.hide_nonpublic.description")); //$NON-NLS-1$
		hideNonPublic.setToolTipText(ActionMessages.getString("MemberFilterActionGroup.hide_nonpublic.tooltip")); //$NON-NLS-1$
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
	 * @param filterProperty Constants SHOW_FIELDS, FILTER_NONPUBLIC & SHOW_STATIC as defined by this
	 * action group
	 * @param set The new value. If true, the elements of the given types are filtered.
	 */	
	public void setMemberFilter(int filterProperty, boolean set) {
		if (set) {
			fFilter.addProperty(filterProperty);
		} else {
			fFilter.removeProperty(filterProperty);
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
	 * @param filterProperty Constants SHOW_FIELDS, FILTER_NONPUBLIC & SHOW_STATIC as defined by this
	 * action group
	 */	
	public boolean hasMemberFilter(int filterProperty) {
		return fFilter.hasProperty(filterProperty);
	}
	
	/**
	 * Saves the state of the filter actions in a memento.
	 */
	public void saveState(IMemento memento) {
		memento.putString(TAG_SHOWFIELDS, String.valueOf(hasMemberFilter(SHOW_FIELDS)));
		memento.putString(TAG_SHOWSTATIC, String.valueOf(hasMemberFilter(SHOW_STATIC)));
		memento.putString(TAG_HIDENONPUBLIC, String.valueOf(hasMemberFilter(FILTER_NONPUBLIC)));
	}
	
	/**
	 * Restores the state of the filter actions from a memento.
	 */	
	public void restoreState(IMemento memento) {
		boolean set= Boolean.valueOf(memento.getString(TAG_SHOWFIELDS)).booleanValue();
		setMemberFilter(SHOW_FIELDS, set);
		set= Boolean.valueOf(memento.getString(TAG_SHOWSTATIC)).booleanValue();
		setMemberFilter(SHOW_STATIC, set);
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
