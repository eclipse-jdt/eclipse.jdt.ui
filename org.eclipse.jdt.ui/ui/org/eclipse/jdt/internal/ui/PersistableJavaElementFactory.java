/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/** 
 * The JavaElementFactory is used to save and recreate an IJavaElement object.
 * As such, it implements the IPersistableElement interface for storage
 * and the IElementFactory interface for recreation.
 *
 * @see IMemento
 * @see IPersistableElement
 * @see IElementFactory
 */
public class PersistableJavaElementFactory implements IElementFactory, IPersistableElement {

	private static final String KEY= "elementID"; //$NON-NLS-1$
	private static final String FACTORY_ID= "org.eclipse.jdt.ui.PersistableJavaElementFactory"; //$NON-NLS-1$

	private IJavaElement fElement;
	
	/**
	 * Create a JavaElementFactory.  
	 */
	public PersistableJavaElementFactory() {
	}

	/**
	 * Create a JavaElementFactory.  This constructor is typically used
	 * for our IPersistableElement side.
	 */
	public PersistableJavaElementFactory(IJavaElement element) {
		fElement= element;
	}

	/*
	 * @see IElementFactory
	 */
	public IAdaptable createElement(IMemento memento) {
	
		String identifier= memento.getString(KEY);
		if (identifier != null) {
			return JavaCore.create(identifier);
		}
		return null;
	}
	
	/*
	 * @see IPersistableElement.
	 */
	public String getFactoryId() {
		return FACTORY_ID;
	}
	/*
	 * @see IPersistableElement
	 */
	public void saveState(IMemento memento) {
		memento.putString(KEY, fElement.getHandleIdentifier());
	}
}
