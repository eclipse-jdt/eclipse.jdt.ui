/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.saveparticipant;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * An instance of <code>IPostSaveListener</code> is informed when 
 * a compilation unit is saved. In oder to get notified the listener
 * must be registered through the {@link SaveParticipantRegistry}
 * and be enabled on the save participant preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @since 3.3
 */
public interface IPostSaveListener {
	
	/**
	 * A human readable name of this listener.
	 * 
	 * @return the name, not null
	 * @since 3.3
	 */
	String getName();
	
	/**
	 * The unique id of this listener.
	 * 
	 * @return the id, not null, not empty
	 * @since 3.3
	 */
	String getId();
	
	/**
	 * Informs this post save listener that the <code>unit</code> 
	 * was saved by the compilation unit document provider.<br><br>
	 * 
	 * To prevent loose of data an implementor of <code>saved</code>
	 * has to follow strict rules and has to take extra care that the execution
	 * of <code>saved</code> does not fail with an exception.<br><br>
	 * 
	 * The implementor must not touch any file other the <code>unit</code>. 
	 * To ensure this <code>saved</code> is executed with a lock on the units 
	 * resource. Changing the scheduling rule, or posting a new job is not allowed. 
	 * Furthermore the implementor is not allowed to save the <code>unit</code>. 
	 * A call to <code>saved</code> should be as fast as possible, as this is 
	 * executed whenever a compilation unit is saved. A post save listener
	 * must not make assumptions about the order of execution of the 
	 * registered post save listeners as the order is arbitrary.<br><br>
	 * 
	 * <code>saved</code> must be able to deal with unsaved resources and with
	 * compilation units which are not on the classpath.<br><br>
	 * 
	 * A listener can open a dialog while <code>saved</code> is executed but
	 * it is not guaranteed, that saved is executed in the UI thread.<br><br>  
	 *
	 * @param unit The unit which was saved, not null
	 * @param monitor A monitor to report progress to
	 * @since 3.3
	 */
	void saved(ICompilationUnit unit, IProgressMonitor monitor) throws CoreException;

}
