/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.util.ListenerList;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A converting selection provider is a special selection provider which converts
 * a selection before notifying any listeners. Additional it converts the selection
 * on <code>getSelection</code> and <code>setSelection</code>. The strategy used to
 * convert the selection is defined by a given {@link ISelectionConverter}.
 *   
 * @since 3.2
 */
public final class ConvertingSelectionProvider implements ISelectionProvider {

	/**
	 * Creates a new default Java selection converter. The converter converts the 
	 * selection using the following algorithm:
	 * <ul>
	 *   <li><code>convertFrom</code>: resource and Java elements are left as is. For 
	 *       all other elements the converter first tries to adapt the element to 
	 *       <code>IJavaElement</code>. Then it tries to adapt to <code>IResource</code>. 
	 *       If both adaption fail the original element is used.</li>
	 *   <li><code>convertTo</code>: no conversion is taking place. The original elements 
	 *       are used.</li>       
	 * </ul>
	 * 
	 * @return a new selection converter
	 */
	public ISelectionConverter createDefaultConverter() {
		return new DefaultJavaConverter();
	}
	
	/**
	 * Creates a new converting selection provider using the default converter.
	 *  
	 * @param provider the original selection provider to fetch the selection from
	 * 
	 * @return a new converting selection provider
	 */
	public static ISelectionProvider create(ISelectionProvider provider) {
		return new ConvertingSelectionProvider(provider, new DefaultJavaConverter());
	}

	/**
	 * Creates a new converting selection provider.
	 *  
	 * @param provider the original selection provider to fetch the selection from
	 * @param converter the actual converter strategy to use
	 * 
	 * @return a new converting selection provider
	 */
	public static ISelectionProvider create(ISelectionProvider provider, ISelectionConverter converter) {
		return new ConvertingSelectionProvider(provider, converter);
	}
	
	private ISelectionProvider fProvider;
	private ISelectionConverter fConverter;
	private SelectionChangedListener fListener;
	
	private class SelectionChangedListener implements ISelectionChangedListener {
		
		ListenerList fListeners= new ListenerList();
		
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection= fConverter.convertFrom(event.getSelection());
			SelectionChangedEvent newEvent= new SelectionChangedEvent(ConvertingSelectionProvider.this, selection);
			Object[] listeners= fListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				((ISelectionChangedListener)listeners[i]).selectionChanged(newEvent);
			}
		}
		public void addListener(ISelectionChangedListener listener) {
			fListeners.add(listener);
		}
		public void removeListener(ISelectionChangedListener listener) {
			fListeners.remove(listener);
		}
		public boolean isEmpty() {
			return fListeners.isEmpty();
		}
	}
	
	private static class DefaultJavaConverter implements ISelectionConverter {
		public ISelection convertFrom(ISelection viewerSelection) {
			if (! (viewerSelection instanceof IStructuredSelection))
				return viewerSelection;
			IStructuredSelection selection= (IStructuredSelection)viewerSelection;
			List result= new ArrayList(selection.size());
			for (Iterator iter= selection.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof IResource || element instanceof IJavaElement) {
					result.add(element);
				} else if (element instanceof IAdaptable) {
					IAdaptable adaptable= (IAdaptable)element;
					IJavaElement jElement= (IJavaElement)adaptable.getAdapter(IJavaElement.class);
					if (jElement != null) {
						result.add(jElement);
					} else {
						IResource resource= (IResource)adaptable.getAdapter(IResource.class);
						if (resource != null) {
							result.add(resource);
						} else {
							result.add(element);
						}
					}
				} else {
					result.add(element);
				}
			}
			return new StructuredSelection(result);
		}

		public ISelection convertTo(ISelection selection) {
			return selection;
		}
	}
	
	private ConvertingSelectionProvider(ISelectionProvider provider, ISelectionConverter converter) {
		Assert.isNotNull(provider);
		Assert.isNotNull(converter);
		fProvider= provider;
		fConverter= converter;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ISelection getSelection() {
		return fConverter.convertFrom(fProvider.getSelection());
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSelection(ISelection selection) {
		fProvider.setSelection(fConverter.convertTo(selection));
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (fListener == null) {
			fListener= new SelectionChangedListener();
			fProvider.addSelectionChangedListener(fListener);
		}
		fListener.addListener(listener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		if (fListener == null)
			return;
		fListener.removeListener(listener);
		if (fListener.isEmpty()) {
			fProvider.removeSelectionChangedListener(fListener);
			fListener= null;
		}
	}
}