/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IMemento;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyMessages;

/**
 * @since 2.0
 */
public class MemberFilterActionGroup {

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
		return "MemberFilterActionGroup." + fViewerId + '.' + String.valueOf(filterProperty);
	}
	
	/**
	 * Filters the method list
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
	 */	
	public boolean hasMemberFilter(int filterProperty) {
		return fFilter.hasFilter(filterProperty);
	}
	
	/**
	 * Saves the state of the filter actions
	 */
	public void saveState(IMemento memento) {
		memento.putString(TAG_HIDEFIELDS, String.valueOf(hasMemberFilter(FILTER_FIELDS)));
		memento.putString(TAG_HIDESTATIC, String.valueOf(hasMemberFilter(FILTER_STATIC)));
		memento.putString(TAG_HIDENONPUBLIC, String.valueOf(hasMemberFilter(FILTER_NONPUBLIC)));
	}
	
	/**
	 * Restores the state of the filter actions
	 */	
	public void restoreState(IMemento memento) {
		boolean set= Boolean.valueOf(memento.getString(TAG_HIDEFIELDS)).booleanValue();
		setMemberFilter(FILTER_FIELDS, set);
		set= Boolean.valueOf(memento.getString(TAG_HIDESTATIC)).booleanValue();
		setMemberFilter(FILTER_STATIC, set);
		set= Boolean.valueOf(memento.getString(TAG_HIDENONPUBLIC)).booleanValue();
		setMemberFilter(FILTER_NONPUBLIC, set);		
	}
	
	/**
	 * Adds the filter actions to the tool bar
	 */
	public void contributeToToolBar(IToolBarManager tbm) {
		tbm.add(fFilterActions[0]); // fields
		tbm.add(fFilterActions[1]); // static
		tbm.add(fFilterActions[2]); // public
	}
}
