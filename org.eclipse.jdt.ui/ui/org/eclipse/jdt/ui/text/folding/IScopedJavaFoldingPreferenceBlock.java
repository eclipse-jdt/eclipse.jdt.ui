package org.eclipse.jdt.ui.text.folding;

import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * Extends {@link IJavaFoldingPreferenceBlock} for supporting preferences in a given scope (e.g. projects).
 * @since 3.34
 */
public interface IScopedJavaFoldingPreferenceBlock extends IJavaFoldingPreferenceBlock {
	/**
	 * Marks this preference block to be configured in an {@link IScopeContext}.
	 * @param context The scope context the preferences apply to.
	 */
	default void setScopeContext(IScopeContext context) {

	}
}
