/*
 * Copyright (c) 2000, 2002 IBM Corp. and others..
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.eclipse.jdt.internal.ui.filters;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.IJavaStatusConstants;

/**
 * Represents a custom filter which is provided by the
 * "org.eclipse.jdt.ui.javaElementFilters" extension point.
 * 
 * since 2.0
 */
public class FilterDescriptor implements Comparable {

	private static String PATTERN_FILTER_ID_PREFIX= "_patternFilterId_"; //$NON-NLS-1$


	private static final String EXTENSION_POINT_NAME= "javaElementFilters"; //$NON-NLS-1$

	private static final String FILTER_TAG= "filter"; //$NON-NLS-1$

	private static final String PATTERN_ATTRIBUTE= "pattern"; //$NON-NLS-1$	
	private static final String ID_ATTRIBUTE= "id"; //$NON-NLS-1$
	private static final String VIEW_ID_ATTRIBUTE= "viewId"; //$NON-NLS-1$
	private static final String CLASS_ATTRIBUTE= "class"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE= "name"; //$NON-NLS-1$
	private static final String ENABLED_ATTRIBUTE= "enabled"; //$NON-NLS-1$
	private static final String DESCRIPTION_ATTRIBUTE= "description"; //$NON-NLS-1$	
	/**
	 * @deprecated	use "enabled" instead
	 */
	private static final String SELECTED_ATTRIBUTE= "selected"; //$NON-NLS-1$

	private static FilterDescriptor[] fgFilterDescriptors;


	private IConfigurationElement fElement;

	/**
	 * Returns all contributed Java element filters.
	 */
	public static FilterDescriptor[] getFilterDescriptors() {
		if (fgFilterDescriptors == null) {
			IPluginRegistry registry= Platform.getPluginRegistry();
			IConfigurationElement[] elements= registry.getConfigurationElementsFor(JavaUI.ID_PLUGIN, EXTENSION_POINT_NAME);
			fgFilterDescriptors= createFilterDescriptors(elements);
		}	
		return fgFilterDescriptors;
	} 
	/**
	 * Returns all Java element filters which
	 * are contributed to the given view.
	 */
	public static FilterDescriptor[] getFilterDescriptors(String viewId) {
		FilterDescriptor[] filterDescs= FilterDescriptor.getFilterDescriptors();
		List result= new ArrayList(filterDescs.length);
		for (int i= 0; i < filterDescs.length; i++) {
			String vid= filterDescs[i].getViewId();
			if (vid == null || vid.equals(viewId))
				result.add(filterDescs[i]);
		}
		return (FilterDescriptor[])result.toArray(new FilterDescriptor[result.size()]);
	}
	
	/**
	 * Creates a new filter descriptor for the given configuration element.
	 */
	private FilterDescriptor(IConfigurationElement element) {
		fElement= element;
		// it is either a pattern filter or a custom filter
		Assert.isTrue(isPatternFilter() ^ isCustomFilter());
		Assert.isNotNull(getId());
		Assert.isNotNull(getName());
	}

	/**
	 * Creates a new <code>ViewerFilter</code>.
	 * This method is only valid for viewer filters.
	 * 
	 * @throws AssertionFailedException if this is a pattern filter
	 * 
	 */
	public ViewerFilter createViewerFilter() {
		Assert.isTrue(isCustomFilter());
		ViewerFilter result= null;
		try {
			result= (ViewerFilter)fElement.createExecutableExtension(CLASS_ATTRIBUTE);
		} catch (CoreException ex) {
			handleError(ex.getStatus());
		} catch (ClassCastException ex) {
			handleError(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IJavaStatusConstants.INTERNAL_ERROR, ex.getLocalizedMessage(), ex));
			return null;
		}
		return result;
	}

	private void handleError(IStatus status) {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();		
		if (shell != null) {
			String title= FilterMessages.getString("FilterDescriptor.filterCreationError.title"); //$NON-NLS-1$
			String message= FilterMessages.getFormattedString("FilterDescriptor.filterCreationError.message", getId()); //$NON-NLS-1$
			ErrorDialog.openError(shell, title, message, status);
		}
		JavaPlugin.log(status);
	}
	
	//---- XML Attribute accessors ---------------------------------------------
	
	/**
	 * Returns the filter's id.
	 * <p>
	 * This attribute is mandatory for custom filters.
	 * The ID for pattern filters is
	 * PATTERN_FILTER_ID_PREFIX plus the pattern itself.
	 * </p>
	 */
	public String getId() {
		if (isPatternFilter()) {
			String viewId= getViewId();
			if (viewId == null)
				return PATTERN_FILTER_ID_PREFIX + getPattern();
			else
				return viewId + PATTERN_FILTER_ID_PREFIX + getPattern();
		} else
			return fElement.getAttribute(ID_ATTRIBUTE);
	}
	
	/**
	 * Returns the filter's name.
	 * <p>
	 * If the name of a pattern filter is missing
	 * then the pattern is used as its name.
	 * </p>
	 */
	public String getName() {
		String name= fElement.getAttribute(NAME_ATTRIBUTE);
		if (name == null && isPatternFilter())
			name= getPattern();
		return name;
	}

	/**
	 * Returns the filter's pattern.
	 * 
	 * @return the pattern string or <code>null</code> if it's not a pattern filter
	 */
	public String getPattern() {
		return fElement.getAttribute(PATTERN_ATTRIBUTE);
	}

	/**
	 * Returns the filter's viewId.
	 * 
	 * @return the view ID or <code>null</code> if the filter is for all views
	 */
	public String getViewId() {
		return fElement.getAttribute(VIEW_ID_ATTRIBUTE);
	}

	/**
	 * Returns the filter's description.
	 * 
	 * @return the description or <code>null</code> if no description is provided
	 */
	public String getDescription() {
		String description= fElement.getAttribute(DESCRIPTION_ATTRIBUTE);
		if (description == null)
			description= ""; //$NON-NLS-1$
		return description;
	}

	/**
	 * @return <code>true</code> if this filter is a custom filter.
	 */
	public boolean isPatternFilter() {
		return getPattern() != null;
	}

	/**
	 * @return <code>true</code> if this filter is a pattern filter.
	 */
	public boolean isCustomFilter() {
		return fElement.getAttribute(CLASS_ATTRIBUTE) != null;
	}

	/**
	 * Returns <code>true</code> if the filter
	 * is initially enabled.
	 * 
	 * This attribute is optional and defaults to <code>true</code>.
	 */
	public boolean isEnabled() {
		String strVal= fElement.getAttribute(ENABLED_ATTRIBUTE);
		if (strVal == null)
			// backward compatibility
			strVal= fElement.getAttribute(SELECTED_ATTRIBUTE);
		return strVal == null || Boolean.valueOf(strVal).booleanValue();
	}

	/* 
	 * Implements a method from IComparable 
	 */ 
	public int compareTo(Object o) {
		if (o instanceof FilterDescriptor)
			return Collator.getInstance().compare(getName(), ((FilterDescriptor)o).getName());
		else
			return Integer.MIN_VALUE;
	}

	//---- initialization ---------------------------------------------------
	
	/**
	 * Creates the filter descriptors.
	 */
	private static FilterDescriptor[] createFilterDescriptors(IConfigurationElement[] elements) {
		List result= new ArrayList(5);
		Set descIds= new HashSet(5);
		for (int i= 0; i < elements.length; i++) {
			IConfigurationElement element= elements[i];
			if (FILTER_TAG.equals(element.getName())) {
				FilterDescriptor desc= new FilterDescriptor(element);
				if (!descIds.contains(desc.getId())) {
					result.add(desc);
					descIds.add(desc.getId());
				}
			}
		}
		Collections.sort(result);
		return (FilterDescriptor[])result.toArray(new FilterDescriptor[result.size()]);
	}
}