/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.text;


import java.util.List;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jdt.ui.text.IColorManager;


/**
 * 
 */
public final class SingleTokenJavaScanner extends AbstractJavaScanner{
	
	
	private String[] fProperty;
	
	public SingleTokenJavaScanner(IColorManager manager, IPreferenceStore store, String property) {
		super(manager, store);
		fProperty= new String[] { property };
		initialize();
	}

	/*
	 * @see AbstractJavaScanner#getTokenProperties()
	 */
	protected String[] getTokenProperties() {
		return fProperty;
	}

	/*
	 * @see AbstractJavaScanner#createRules()
	 */
	protected List createRules() {
		setDefaultReturnToken(getToken(fProperty[0]));
		return null;
	}
}

