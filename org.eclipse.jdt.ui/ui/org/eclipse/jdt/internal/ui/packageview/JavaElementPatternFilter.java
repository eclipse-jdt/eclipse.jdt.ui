/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.packageview;import java.util.List;import java.util.StringTokenizer;import java.util.Vector;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.StringMatcher;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerFilter;


/**
 * The JavaElementPatternFilter is the filter used to determine whether 
 * a JavaElement is shown.
 */
class JavaElementPatternFilter extends ViewerFilter {
	private String[] fPatterns;
	private StringMatcher[] fMatchers;
	
	static String COMMA_SEPARATOR = ",";
	static String FILTERS_TAG= "javaElementFilters";
	
	/**
	 * Creates a new resource pattern filter.
	 */
	public JavaElementPatternFilter() {
		super();
	}
	
	/**
	 * Return the currently configured StringMatchers. If there aren't any look
	 * them up.
	 */
	private StringMatcher[] getMatchers() {
		if (fMatchers == null)
			initializeFromPreferences();
		return fMatchers;
	}
	
	/**
	 * Gets the patterns for the receiver. Returns the cached values if there
	 * are any - if not look it up.
	 */
	public String[] getPatterns() {
		if (fPatterns == null)
			initializeFromPreferences();
		return fPatterns;
	}
	
	/**
	 * Initialize the settings from the workbench preferences.
	 */
	private void initializeFromPreferences() {
		JavaPlugin plugin= JavaPlugin.getDefault();
		String storedPatterns= plugin.getPreferenceStore().getString(FILTERS_TAG);
	
		if (storedPatterns.length() == 0) {
			List defaultFilters= FiltersContentProvider.getDefaultFilters();
			String[] patterns= new String[defaultFilters.size()];
			defaultFilters.toArray(patterns);
			setPatterns(patterns);
			return;
		}
	
		//Get the strings separated by a comma and filter them from the currently
		//defined ones
	
		List definedFilters = FiltersContentProvider.getDefinedFilters();
	
		StringTokenizer entries = new StringTokenizer(storedPatterns, COMMA_SEPARATOR);
		Vector patterns = new Vector();
	
		while (entries.hasMoreElements()) {
			String nextToken = entries.nextToken();
			if (definedFilters.indexOf(nextToken) > -1)
				patterns.addElement(nextToken);
		}
	
		//Convert to an array of Strings
		String[] patternArray = new String[patterns.size()];
		patterns.toArray(patternArray);
		setPatterns(patternArray);
	
	}
	
	/* (non-Javadoc)
	 * Method declared on ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		String matchName= null;
		if (element instanceof IJavaElement) {
			matchName= ((IJavaElement) element).getElementName();
		} else if (element instanceof IAdaptable) {
			IAdaptable adaptable= (IAdaptable) element;
			IResource resource= (IResource) adaptable.getAdapter(IResource.class);
			if (resource != null)
				matchName= resource.getName();
		}
		if (matchName != null) {
			StringMatcher[] testMatchers= getMatchers();
			for (int i = 0; i < testMatchers.length; i++) {
				if (testMatchers[i].match(matchName))
					return false;
			}
			return true;
		}
		return true;
	}
	
	/**
	 * Sets the patterns to filter out for the receiver.
	 */
	public void setPatterns(String[] newPatterns) {
		fPatterns = newPatterns;
		fMatchers = new StringMatcher[newPatterns.length];
		for (int i = 0; i < newPatterns.length; i++) {
			//Reset the matchers to prevent constructor overhead
			fMatchers[i]= new StringMatcher(newPatterns[i], true, false);
		}
	}
}
