package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;

/**
 * Java color manager.
 */
public class JavaColorManager implements IColorManager {
	
	protected Map fKeyTable= new HashMap(10);
	protected Map fDisplayTable= new HashMap(2);
	
	protected String[] fPredefinedColors= {
		IJavaColorConstants.JAVA_MULTI_LINE_COMMENT,
		IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT,
		IJavaColorConstants.JAVA_KEYWORD,
		IJavaColorConstants.JAVA_TYPE,
		IJavaColorConstants.JAVA_STRING,
		IJavaColorConstants.JAVA_DEFAULT,
		IJavaColorConstants.JAVADOC_KEYWORD,
		IJavaColorConstants.JAVADOC_TAG,
		IJavaColorConstants.JAVADOC_LINK,
		IJavaColorConstants.JAVADOC_DEFAULT
	};	
	
	
	public JavaColorManager(IPreferenceStore store) {
		for (int i= 0; i < fPredefinedColors.length; i++) {
			String key= fPredefinedColors[i];
			fKeyTable.put(key, PreferenceConverter.getColor(store, key));
		}
	}
	
	public boolean affectsBehavior(PropertyChangeEvent event) {
		String property= event.getProperty();
		return property.startsWith(IJavaColorConstants.PREFIX);
	}
	
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		RGB rgb= null;
		
		Object value= event.getNewValue();
		if (value instanceof RGB)
			rgb= (RGB) value;
		else if (value instanceof String)
			rgb= StringConverter.asRGB((String) value);
			
		if (rgb != null)
			fKeyTable.put(event.getProperty(), rgb);
	}
	
	private void dispose(Display display) {		
		Map colorTable= (Map) fDisplayTable.get(display);
		if (colorTable != null) {
			Iterator e= colorTable.values().iterator();
			while (e.hasNext())
				((Color) e.next()).dispose();
		}
	}
	
	/**
	 * @see IColorManager#getColor(RGB)
	 */
	public Color getColor(RGB rgb) {
		
		if (rgb == null)
			return null;
		
		final Display display= Display.getCurrent();
		Map colorTable= (Map) fDisplayTable.get(display);
		if (colorTable == null) {
			colorTable= new HashMap(10);
			fDisplayTable.put(display, colorTable);
			display.disposeExec(new Runnable() {
				public void run() {
					dispose(display);
				}
			});
		}
		
		Color color= (Color) colorTable.get(rgb);
		if (color == null) {
			color= new Color(Display.getCurrent(), rgb);
			colorTable.put(rgb, color);
		}
		
		return color;
	}
	
	/**
	 * @see IColorManager#dispose
	 */
	public void dispose() {
		/* 
		 * unfortunately the displays are already gone at this point
		 * see getColor's dispose runnable for disposing colors
		 */
	}
	
	/**
	 * @see IColorManager#getColor(String)
	 */
	public Color getColor(String key) {
		
		if (key == null)
			return null;
			
		RGB rgb= (RGB) fKeyTable.get(key);
		return getColor(rgb);
	}
}
