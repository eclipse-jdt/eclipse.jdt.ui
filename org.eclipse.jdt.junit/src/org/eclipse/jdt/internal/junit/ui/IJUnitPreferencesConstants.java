package org.eclipse.jdt.internal.junit.ui;

/**
 * Defines constants which are used to refer to values in the plugin's preference store.
 */
public interface IJUnitPreferencesConstants {
	/**
	 * Boolean preference controlling whether the failure stack should be
	 * filtered.
	 */	
	public static String DO_FILTER_STACK= JUnitPlugin.PLUGIN_ID + ".do_filter_stack"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the JUnit view should be shown on
	 * errors only.
	 */	
	public static String SHOW_ON_ERROR_ONLY= JUnitPlugin.PLUGIN_ID + ".show_on_error"; //$NON-NLS-1$
	
	/**
	 * List of active stack filters. A String containing a comma separated list
	 * of fully qualified type names/patterns.
	 */			
	public static final String PREF_ACTIVE_FILTERS_LIST = JUnitPlugin.PLUGIN_ID + ".active_filters"; //$NON-NLS-1$
	
	/**
	 * List of inactive stack filters. A String containing a comma separated
	 * list of fully qualified type names/patterns.
	 */				
	public static final String PREF_INACTIVE_FILTERS_LIST = JUnitPlugin.PLUGIN_ID + ".inactive_filters"; //$NON-NLS-1$	

}
