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
	private static final ITypeExtender[] EMPTY_PROPERTY_TESTER_ARRAY= new ITypeExtender[0];
	private static final TypeExtension[] EMPTY_PROPERTY_INTERFACE_ARRAY= new TypeExtension[0];
	
	public static final Object NOT_LOADED= new Object();
	private static final Object CONTINUE= new Object();
		
	private static final Map fInterfaceMap= new HashMap();
	
	/* a special property interface that marks the end of an evaluation chain */
	private static final TypeExtension END_POINT= new TypeExtension(null) {
		/* package */ Object internalPerform(Object o, String name, Object[] args) {
			return CONTINUE;
		}	
	};
	
	private Class fType;
	private ITypeExtender[] fExtenders;
	
	private TypeExtension fExtends;
	private TypeExtension[] fImplements;
	
	private TypeExtension(Class type) {
		fType= type;
		synchronized (fInterfaceMap) {
			fInterfaceMap.put(fType, this);
		}
	}
	
	public static Object perform(Object o, String method, Object[] args) throws CoreException {
		TypeExtension pi= get(o.getClass());
		Object result= pi.internalPerform(o, method, args);
		if (result != CONTINUE)
			return result;
		throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
			IStatus.ERROR, "Unknown method: " + method, null));
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
	
	/* package */ Object internalPerform(Object element, String method, Object[] args) throws CoreException {
		synchronized (this) {
			if (fExtenders == null)
				initialize();
		}
		Object result;
		
		// handle testers associated with this interface
		for (int i= 0; i < fExtenders.length; i++) {
			ITypeExtender extender= fExtenders[i];
			if (extender == null || !extender.handles(method))
				continue;
			if (extender.isLoaded()) {
				return extender.perform(element, method, args);
			} else {
				if (extender.canLoad()) {
					try {
						TypeExtenderDescriptor descriptor= (TypeExtenderDescriptor)extender;
						TypeExtender temp= descriptor.create();
						temp.initialize(descriptor.getProperties());
						synchronized (fExtenders) {
							fExtenders[i]= extender= temp;
						}
						return extender.perform(element, method, args);
					} catch (CoreException e) {
						JavaPlugin.getDefault().getLog().log(e.getStatus());
						// disable tester
						fExtenders[i]= null;
					}
				} else {
					return NOT_LOADED;
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
		result= fExtends.internalPerform(element, method, args);
		if (result != CONTINUE)
			return result;
		
		// handle implements chain
		synchronized (this) {
			if (fImplements == null) {
				Class[] interfaces= fType.getInterfaces();
				if (interfaces.length == 0) {
					fImplements= EMPTY_PROPERTY_INTERFACE_ARRAY;
				} else {
					fImplements= new TypeExtension[interfaces.length];
					for (int i= 0; i < interfaces.length; i++) {
						fImplements[i]= TypeExtension.get(interfaces[i]);
					}				
				}
			}
		}
		for (int i= 0; i < fImplements.length; i++) {
			result= fImplements[i].internalPerform(element, method, args);
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
			fExtenders= EMPTY_PROPERTY_TESTER_ARRAY;
		else
			fExtenders= (ITypeExtender[])result.toArray(new ITypeExtender[result.size()]);
	}
}
