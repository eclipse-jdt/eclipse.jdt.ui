/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template;

import org.eclipse.core.runtime.CoreException;

/**
 * A template context. A template context is associated with a context type.
 */
public abstract class TemplateContext {

	/** The context type of this context */
	private final ContextType fContextType;
	/** A flag to indicate that the context should not be modified. */
	private boolean fReadOnly;

	/**
	 * Creates a template context of a particular context type.
	 */
	protected TemplateContext(ContextType contextType) {
		fContextType= contextType;
		fReadOnly= true;
	}

	/**
	 * Returns the context type of this context.
	 */
	public ContextType getContextType() {
	 	return fContextType;   
	}
	
	/**
	 * Sets or clears the read only flag.
	 */
	public void setReadOnly(boolean readOnly) {
		fReadOnly= readOnly;	
	}
	
	public boolean isReadOnly() {
		return fReadOnly;	
	}

	/**
	 * Evaluates the template and returns a template buffer.
	 */
	public abstract TemplateBuffer evaluate(Template template) throws CoreException;
	
	/**
	 * Tests if the specified template can be evaluated in this context.
	 */
	public abstract boolean canEvaluate(Template template);
	
}
