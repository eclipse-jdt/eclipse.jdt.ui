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
		String title= ActionMessages.getString("MemberFilterActionGroup.hide_fields.label"); //$NON-NLS-1$
		String helpContext= IJavaHelpContextIds.FILTER_FIELDS_ACTION;
		MemberFilterAction hideFields= new MemberFilterAction(this, title, FILTER_FIELDS, helpContext, doHideFields);
		hideFields.setDescription(ActionMessages.getString("MemberFilterActionGroup.hide_fields.description")); //$NON-NLS-1$
		hideFields.setToolTipText(ActionMessages.getString("MemberFilterActionGroup.hide_fields.tooltip")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hideFields, "fields_co.gif"); //$NON-NLS-1$
		
		// static
		title= ActionMessages.getString("MemberFilterActionGroup.hide_static.label"); //$NON-NLS-1$
		helpContext= IJavaHelpContextIds.FILTER_STATIC_ACTION;
		MemberFilterAction hideStatic= new MemberFilterAction(this, title, FILTER_STATIC, helpContext, doHideStatic);
		hideStatic.setDescription(ActionMessages.getString("MemberFilterActionGroup.hide_static.description")); //$NON-NLS-1$
		hideStatic.setToolTipText(ActionMessages.getString("MemberFilterActionGroup.hide_static.tooltip")); //$NON-NLS-1$
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
	 * @param filterProperty Constants FILTER_FIELDS, FILTER_PUBLIC & FILTER_PRIVATE as defined by this
	 * action group
	 * @param set The new value. If true, the elements of the given types are filtered.
	 * @deprecated use setMemberFilter(int, boolean, boolean)
	 */	
	public void setMemberFilter(int filterProperty, boolean set) {
		setMemberFilter(filterProperty, set, true);
	}

	/**
	 * Set the current filter.
	 * @param filterProperty Constants FILTER_FIELDS, FILTER_PUBLIC & FILTER_PRIVATE as defined by this
	 * action group
	 * @param set The new value. If true, the elements of the given types are filtered.
	 * @param refreshViewer Indicates if the viewer should be refreshed.
	 */	
	public void setMemberFilter(int filterProperty, boolean set, boolean refreshViewer) {
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
		if (refreshViewer)
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
	 * @param memento
	 * @param refreshViewer Indicates if the viewer should be refreshed.
	 */	
	public void restoreState(IMemento memento, boolean refreshViewer) {
		boolean set= Boolean.valueOf(memento.getString(TAG_HIDEFIELDS)).booleanValue();
		setMemberFilter(FILTER_FIELDS, set, refreshViewer);
		set= Boolean.valueOf(memento.getString(TAG_HIDESTATIC)).booleanValue();
		setMemberFilter(FILTER_STATIC, set, refreshViewer);
		set= Boolean.valueOf(memento.getString(TAG_HIDENONPUBLIC)).booleanValue();
		setMemberFilter(FILTER_NONPUBLIC, set, refreshViewer);		
	}
	
	/**
	 * Restores the state of the filter actions from a memento.
	 * @deprecated use restoreState(IMemento, boolean)
	 */	
	public void restoreState(IMemento memento) {
		restoreState(memento, true);
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
