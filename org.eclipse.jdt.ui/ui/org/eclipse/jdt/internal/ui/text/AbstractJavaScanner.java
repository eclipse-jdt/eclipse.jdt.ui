/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IColorManagerExtension;


/**
 * Initialized with a color manager and a preference store, its subclasses are
 * only responsible for providing a list of preference keys based on which tokens
 * are generated and to use this tokens to define the rules controlling this scanner.
 */
public abstract class AbstractJavaScanner extends BufferedRuleBasedScanner {
			
	
	private IColorManager fColorManager;
	private IPreferenceStore fPreferenceStore;
	
	private Map fTokenMap= new HashMap();
	private String[] fPropertyNamesColor;
	private String[] fPropertyNamesStyle;
	
	
	private boolean fNeedsLazyColorLoading;

	/** 
	 * Returns the list of preference keys which define the tokens
	 * used in the rules of this scanner.
	 */
	abstract protected String[] getTokenProperties();
		
	/**
	 * Creates the list of rules controlling this scanner.
	 */
	abstract protected List createRules();
		
	
	/**
	 * Creates an abstract Java scanner.
	 */
	public AbstractJavaScanner(IColorManager manager, IPreferenceStore store) {
		super();
		fColorManager= manager;
		fPreferenceStore= store;
	}
	
	/**
	 * Must be called after the constructor has been called.
	 */
	public final void initialize() {
		
		fPropertyNamesColor= getTokenProperties();
		int length= fPropertyNamesColor.length;
		fPropertyNamesStyle= new String[length];
		
		fNeedsLazyColorLoading= Display.getCurrent() == null; 

		for (int i= 0; i < length; i++) {
			fPropertyNamesStyle[i]= fPropertyNamesColor[i] + "_bold"; //$NON-NLS-1$
			if (fNeedsLazyColorLoading)
				addTokenWithProxyAttribute(fPropertyNamesColor[i], fPropertyNamesStyle[i]);
			else
				addToken(fPropertyNamesColor[i], fPropertyNamesStyle[i]);
		}
		
		initializeRules();
	}
		
	public IToken nextToken() {
		if (fNeedsLazyColorLoading)
			resolveProxyAttributes();
		return super.nextToken();
	}

	private void resolveProxyAttributes() {
		if (fNeedsLazyColorLoading && Display.getCurrent() != null) {
			for (int i= 0; i < fPropertyNamesColor.length; i++) {
				addToken(fPropertyNamesColor[i], fPropertyNamesStyle[i]);
			}
			fNeedsLazyColorLoading= true;
		}
	}

	private void addTokenWithProxyAttribute(String colorKey, String styleKey) {
		boolean bold= fPreferenceStore.getBoolean(styleKey);
		fTokenMap.put(colorKey, new Token(new TextAttribute(null, null, bold ? SWT.BOLD : SWT.NORMAL)));
	}

	private void addToken(String colorKey, String styleKey) {
		RGB rgb= PreferenceConverter.getColor(fPreferenceStore, colorKey);
		if (fColorManager instanceof IColorManagerExtension) {
			IColorManagerExtension ext= (IColorManagerExtension) fColorManager;
			ext.unbindColor(colorKey);
			ext.bindColor(colorKey, rgb);
		}
		
		boolean bold= fPreferenceStore.getBoolean(styleKey);
		
		if (!fNeedsLazyColorLoading)
			fTokenMap.put(colorKey, new Token(new TextAttribute(fColorManager.getColor(colorKey), null, bold ? SWT.BOLD : SWT.NORMAL)));
		else {
			Token token= ((Token)fTokenMap.get(colorKey));
			if (token != null)
				token.setData(new TextAttribute(fColorManager.getColor(colorKey), null, bold ? SWT.BOLD : SWT.NORMAL));
		}
	}
	
	protected Token getToken(String key) {
		if (fNeedsLazyColorLoading)
			resolveProxyAttributes();
		return (Token) fTokenMap.get(key);
	}
		
	private void initializeRules() {
		List rules= createRules();
		if (rules != null) {
			IRule[] result= new IRule[rules.size()];
			rules.toArray(result);
			setRules(result);
		}
	}
	
	private int indexOf(String property) {
		if (property != null) {
			int length= fPropertyNamesColor.length;
			for (int i= 0; i < length; i++) {
				if (property.equals(fPropertyNamesColor[i]) || property.equals(fPropertyNamesStyle[i]))
					return i;
			}
		}
		return -1;
	}
	
	public boolean affectsBehavior(PropertyChangeEvent event) {
		return indexOf(event.getProperty()) >= 0;
	}
	
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		String p= event.getProperty();
		int index= indexOf(p);
		Token token= getToken(fPropertyNamesColor[index]);
		if (fPropertyNamesColor[index].equals(p))
			adaptToColorChange(token, event);
		else
			adaptToStyleChange(token, event);
	}
	
	private void adaptToColorChange(Token token, PropertyChangeEvent event) {
		RGB rgb= null;
		
		Object value= event.getNewValue();
		if (value instanceof RGB)
			rgb= (RGB) value;
		else if (value instanceof String)
			rgb= StringConverter.asRGB((String) value);
			
		if (rgb != null) {
			
			String property= event.getProperty();
			
			if (fColorManager instanceof IColorManagerExtension) {
				IColorManagerExtension ext= (IColorManagerExtension) fColorManager;
				ext.unbindColor(property);
				ext.bindColor(property, rgb);
			}
			
			Object data= token.getData();
			if (data instanceof TextAttribute) {
				TextAttribute oldAttr= (TextAttribute) data;
				token.setData(new TextAttribute(fColorManager.getColor(property), oldAttr.getBackground(), oldAttr.getStyle()));
			}
		}
	}
	
	private void adaptToStyleChange(Token token, PropertyChangeEvent event) {
		boolean bold= false;
		Object value= event.getNewValue();
		if (value instanceof Boolean)
			bold= ((Boolean) value).booleanValue();
		else if (value instanceof String) {
			String s= (String) value;
			if (IPreferenceStore.TRUE.equals(s))
				bold= true;
			else if (IPreferenceStore.FALSE.equals(s))
				bold= false;
		}
		
		Object data= token.getData();
		if (data instanceof TextAttribute) {
			TextAttribute oldAttr= (TextAttribute) data;
			boolean isBold= (oldAttr.getStyle() == SWT.BOLD);
			if (isBold != bold) 
				token.setData(new TextAttribute(oldAttr.getForeground(), oldAttr.getBackground(), bold ? SWT.BOLD : SWT.NORMAL));
		}
	}
	/**
	 * Returns the preference store.
	 * 
	 * @return the preference store.
	 *
	 * @since 3.0
	 */
	protected IPreferenceStore getPreferenceStore() {
		return fPreferenceStore;
	}
}
