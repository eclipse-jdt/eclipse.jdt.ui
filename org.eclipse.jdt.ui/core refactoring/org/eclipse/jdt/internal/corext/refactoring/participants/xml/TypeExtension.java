/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * A property interface adds additional test methods to an already
 * existing class. It is comparable to <code>IAdaptable<code> in the
 * sense that it extends existing class. However the major differences
 * are:
 * <ul>
 *   <li>more than one property tester can be added to a class.</li>
 *   <li>the contribution is done using the normal XML based
 *       extension mechanism.</li>
 *   <li>if the plug-in declaring a property isn't loaded yet during
 *       property testing this is singnal to the caller using the special
 *       return value <code>TestResult.NOT_LOADED</code>.
 * </ul>
 */
public class TypeExtension {
	
	private static final String EXT_POINT= "typeExtenders"; //$NON-NLS-1$
	private static final String TYPE= "type"; //$NON-NLS-1$
	private static final ITypeExtender[] EMPTY_TYPE_EXTENDER_ARRAY= new ITypeExtender[0];
	private static final TypeExtension[] EMPTY_TYPE_EXTENSION_ARRAY= new TypeExtension[0];

	/* a special type extender instance that used to signal that method searching has to continue */
	private static final ITypeExtender CONTINUE= new ITypeExtender() {
		public boolean handles(String method) {
			return false;
		}
		public boolean isLoaded() {
			return false;
		}
		public boolean canLoad() {
			return false;
		}
		public Object invoke(Object receiver, String method, Object[] args) throws CoreException {
			return null;
		}
	};
		
	/* a special type extension instance that marks the end of an evaluation chain */
	private static final TypeExtension END_POINT= new TypeExtension() {
		/* package */ ITypeExtender find(Object o, String name) {
			return CONTINUE;
		}	
	};
	
	/*
	 * Map containing all already instanciated type extension object. Key is
	 * of type <code>Class</code>, value is of type <code>TypeExtension</code>. 
	 */
	private static final Map fInterfaceMap= new HashMap();
	
	/*
	 * A cache to give fast access to that 100 method invocations.
	 */
	private static final LRUCache fMethodCache= new LRUCache(100);
	
	/* debugging flag to enable tracing */
	private static final boolean TRACING;
	static {
		String value= Platform.getDebugOption("org.eclipse.jdt.ui/typeExtension/tracing"); //$NON-NLS-1$
		TRACING= value != null && value.equalsIgnoreCase("true"); //$NON-NLS-1$
	}
	
	/* the type this extension is extending */
	private Class fType;
	/* the list of associated extenders */
	private ITypeExtender[] fExtenders;
	
	/* the extension associated with <code>fType</code>'s super class */
	private TypeExtension fExtends;
	/* the extensions associated with <code>fTypes</code>'s interfaces */ 
	private TypeExtension[] fImplements;
	
	private TypeExtension() {
		// special constructor to create the CONTINUE instance
	}
	
	private TypeExtension(Class type) {
		Assert.isNotNull(type);
		fType= type;
		synchronized (fInterfaceMap) {
			fInterfaceMap.put(fType, this);
		}
	}
	
	public static Method getMethod(Object receiver, String method) throws CoreException {
		// TODO must synchronize access to method cache.
		long start= 0;
		if (TRACING)
			start= System.currentTimeMillis();
		
		Class clazz= receiver.getClass();
		Method result= new Method(clazz, method);
		Object cached= fMethodCache.get(result);
		if (cached != null) {
			if (TRACING) {
				System.out.println("[Type Extension] - method " +
					clazz.getName() + "#" + method +
					" found in cache: " + 
					(System.currentTimeMillis() - start) + " ms.");
				return (Method)cached;
			}
		}
		TypeExtension extension= get(clazz);
		ITypeExtender extender= extension.find(receiver, method);
		if (extender == CONTINUE) {
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
				IStatus.ERROR, "Unknown method: " + method, null));
		}
		result.setExtender(extender);
		fMethodCache.put(result, result);
		if (TRACING) {
			System.out.println("[Type Extension] - method " +
				clazz.getName() + "#" + method +
				" not found in cache: " + 
				(System.currentTimeMillis() - start) + " ms.");
		}
		return result;
	}
	
	private static TypeExtension get(Class clazz) {
		synchronized(fInterfaceMap) {
			TypeExtension result= (TypeExtension)fInterfaceMap.get(clazz);
			if (result == null) {
				result= new TypeExtension(clazz);
			}
			return result;
		}
	}
	
	/* package */ ITypeExtender find(Object element, String method) throws CoreException {
		synchronized (this) {
			if (fExtenders == null)
				initialize();
		}
		ITypeExtender result;
		
		// handle testers associated with this interface
		for (int i= 0; i < fExtenders.length; i++) {
			ITypeExtender extender= fExtenders[i];
			if (extender == null || !extender.handles(method))
				continue;
			if (extender.isLoaded()) {
				return extender;
			} else {
				if (extender.canLoad()) {
					try {
						TypeExtenderDescriptor descriptor= (TypeExtenderDescriptor)extender;
						TypeExtender temp= descriptor.create();
						temp.initialize(descriptor.getProperties());
						synchronized (fExtenders) {
							fExtenders[i]= extender= temp;
						}
						return extender;
					} catch (CoreException e) {
						JavaPlugin.getDefault().getLog().log(e.getStatus());
						// disable tester
						fExtenders[i]= null;
					}
				} else {
					return extender;
				}
			}
		}
		
		// handle extends chain
		synchronized (this) {
			if (fExtends == null) {
				Class superClass= fType.getSuperclass();
				if (superClass != null) {
					fExtends= TypeExtension.get(superClass);
				} else {
					fExtends= END_POINT;
				}
			}
		}
		result= fExtends.find(element, method);
		if (result != CONTINUE)
			return result;
		
		// handle implements chain
		synchronized (this) {
			if (fImplements == null) {
				Class[] interfaces= fType.getInterfaces();
				if (interfaces.length == 0) {
					fImplements= EMPTY_TYPE_EXTENSION_ARRAY;
				} else {
					fImplements= new TypeExtension[interfaces.length];
					for (int i= 0; i < interfaces.length; i++) {
						fImplements[i]= TypeExtension.get(interfaces[i]);
					}				
				}
			}
		}
		for (int i= 0; i < fImplements.length; i++) {
			result= fImplements[i].find(element, method);
			if (result != CONTINUE)
				return result;
		}
		return CONTINUE;
	}
	
	private void initialize() {
		IPluginRegistry registry= Platform.getPluginRegistry();
		IConfigurationElement[] ces= registry.getConfigurationElementsFor(
			JavaPlugin.getPluginId(), 
			EXT_POINT); 
		String fTypeName= fType.getName();
		List result= new ArrayList(2);
		for (int i= 0; i < ces.length; i++) {
			IConfigurationElement config= ces[i];
			if (fTypeName.equals(config.getAttribute(TYPE)))
				result.add(new TypeExtenderDescriptor(config));
		}
		if (result.size() == 0)
			fExtenders= EMPTY_TYPE_EXTENDER_ARRAY;
		else
			fExtenders= (ITypeExtender[])result.toArray(new ITypeExtender[result.size()]);
	}
}
