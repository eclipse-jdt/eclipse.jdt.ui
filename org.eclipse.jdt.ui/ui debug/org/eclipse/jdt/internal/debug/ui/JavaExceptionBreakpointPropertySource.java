package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.BreakpointPropertySource.BooleanLabelProvider;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * Handle properties for instances of IJavaExceptionBreakpoint
 */
public class JavaExceptionBreakpointPropertySource extends JavaBreakpointPropertySource {
	
	/**
	 * Holds all property descriptors defined in this class (the property
	 * descriptors unique to IJavaExceptionBreakpoints)
	 */
	private static IPropertyDescriptor[] fLocalJavaExceptionDescriptors;
	
	/**
	 * Holds all property descriptors defined in this class's hierarchy (all
	 * property descriptors for IJavaExceptionBreakpoints)
	 */
	private static IPropertyDescriptor[] fAllJavaExceptionDescriptors;

	// Property Values
	protected static final String P_ID_CAUGHT = "caught";
	protected static final String P_ID_UNCAUGHT = "uncaught";
	
	private static final String P_CAUGHT = "caught";
	private static final String P_UNCAUGHT = "uncaught";
	
	static {
		fLocalJavaExceptionDescriptors = new IPropertyDescriptor[2];
		PropertyDescriptor propertyDescriptor;
				
		propertyDescriptor = new ComboBoxPropertyDescriptor(P_ID_CAUGHT, P_CAUGHT, BOOLEAN_LABEL_ARRAY);
		propertyDescriptor.setLabelProvider(new BooleanLabelProvider());
		fLocalJavaExceptionDescriptors[0] = propertyDescriptor;	

		propertyDescriptor = new ComboBoxPropertyDescriptor(P_ID_UNCAUGHT, P_UNCAUGHT, BOOLEAN_LABEL_ARRAY);
		propertyDescriptor.setLabelProvider(new BooleanLabelProvider());
		fLocalJavaExceptionDescriptors[1] = propertyDescriptor;	
	}

	/**
	 * @see IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (fAllJavaExceptionDescriptors == null) {
			IPropertyDescriptor[] superDescriptors = super.getPropertyDescriptors();
			fAllJavaExceptionDescriptors = new IPropertyDescriptor[superDescriptors.length + fLocalJavaExceptionDescriptors.length];
			System.arraycopy(superDescriptors, 0, fAllJavaExceptionDescriptors, 0, superDescriptors.length);
			System.arraycopy(fLocalJavaExceptionDescriptors, 0, fAllJavaExceptionDescriptors, superDescriptors.length, fLocalJavaExceptionDescriptors.length);
		}
		return fAllJavaExceptionDescriptors;
	}

	/**
	 * @see IPropertySource#getPropertyValue(Object)
	 */
	public Object getPropertyValue(Object id) {
		if (id.equals(P_ID_CAUGHT)) {
			try {
				IJavaExceptionBreakpoint bp = (IJavaExceptionBreakpoint) fBreakpoint;
				return bp.isCaught() ? P_VALUE_TRUE : P_VALUE_FALSE;
			} catch (CoreException ce) {
				return null;
			}
		} else if (id.equals(P_ID_UNCAUGHT)) {
			try {
				IJavaExceptionBreakpoint bp = (IJavaExceptionBreakpoint) fBreakpoint;
				return bp.isUncaught() ? P_VALUE_TRUE : P_VALUE_FALSE;
			} catch (CoreException ce) {
				return null;
			}
		} else {
			return super.getPropertyValue(id);
		}
	}

	/**
	 * @see IPropertySource#setPropertyValue(Object, Object)
	 */
	public void setPropertyValue(Object id, Object value) {
		if (id.equals(P_ID_CAUGHT)) {
			try {
				IJavaExceptionBreakpoint bp = (IJavaExceptionBreakpoint) fBreakpoint;
				bp.setCaught(((Integer)value).equals(P_VALUE_TRUE) ? true : false);
			} catch (CoreException ce) {
			}		
		} else if (id.equals(P_ID_UNCAUGHT)) {
			try {
				IJavaExceptionBreakpoint bp = (IJavaExceptionBreakpoint) fBreakpoint;
				bp.setUncaught(((Integer)value).equals(P_VALUE_TRUE) ? true : false);
			} catch (CoreException ce) {
			}		
		} else {
			super.setPropertyValue(id, value);
		}
	}

}

