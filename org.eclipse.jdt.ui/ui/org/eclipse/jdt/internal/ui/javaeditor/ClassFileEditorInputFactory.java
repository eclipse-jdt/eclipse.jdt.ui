package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * The factory which is capable of recreating class file editor
 * inputs stored in a memento.
 */
public class ClassFileEditorInputFactory implements IElementFactory {
	
	public final static String ID=  "org.eclipse.jdt.ui.ClassFileEditorInputFactory";
	public final static String KEY= "org.eclipse.jdt.ui.ClassFileIdentifier";
	
	public ClassFileEditorInputFactory() {
	}
	
	/**
	 * @see IElementFactory#createElement
	 */
	public IAdaptable createElement(IMemento memento) {
		String identifier= memento.getString(KEY);
		if (identifier != null) {
			IJavaElement element= JavaCore.create(identifier);
			try {
				return EditorUtility.getEditorInput(element);
			} catch (JavaModelException x) {
			}
		}
		return null;
	}
	
	public static void saveState(IMemento memento, ClassFileEditorInput input) {
		IClassFile c= input.getClassFile();
		memento.putString(KEY, c.getHandleIdentifier());
	}
}