package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.BreakpointPropertySource.BooleanLabelProvider;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class JavaWatchpointPropertySource extends JavaBreakpointPropertySource {

	/**
	 * Holds all property descriptors defined in this class (the property
	 * descriptors unique to IJavaLineBreakpoints)
	 */
	private static IPropertyDescriptor[] fLocalJavaWatchpointDescriptors;
	
	/**
	 * Holds all property descriptors defined in this class's hierarchy (all
	 * property descriptors for IJavaLineBreakpoints)
	 */
	private static IPropertyDescriptor[] fAllJavaWatchpointDescriptors;

	// Property Values
	protected static final String P_ID_ACCESS = "access";
	protected static final String P_ID_MODIFICATION = "modification";
	
	private static final String P_ACCESS = "access";
	private static final String P_MODIFICATION = "modification";
	
	static {
		fLocalJavaWatchpointDescriptors = new IPropertyDescriptor[2];
		PropertyDescriptor propertyDescriptor;
				
		propertyDescriptor = new ComboBoxPropertyDescriptor(P_ID_ACCESS, P_ACCESS, BOOLEAN_LABEL_ARRAY);
		propertyDescriptor.setLabelProvider(new BooleanLabelProvider());
		fLocalJavaWatchpointDescriptors[0] = propertyDescriptor;	

		propertyDescriptor = new ComboBoxPropertyDescriptor(P_ID_MODIFICATION, P_MODIFICATION, BOOLEAN_LABEL_ARRAY);
		propertyDescriptor.setLabelProvider(new BooleanLabelProvider());
		fLocalJavaWatchpointDescriptors[1] = propertyDescriptor;	
	}

	/**
	 * @see IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (fAllJavaWatchpointDescriptors == null) {
			IPropertyDescriptor[] superDescriptors = super.getPropertyDescriptors();
			fAllJavaWatchpointDescriptors = new IPropertyDescriptor[superDescriptors.length + fLocalJavaWatchpointDescriptors.length];
			System.arraycopy(superDescriptors, 0, fAllJavaWatchpointDescriptors, 0, superDescriptors.length);
			System.arraycopy(fLocalJavaWatchpointDescriptors, 0, fAllJavaWatchpointDescriptors, superDescriptors.length, fLocalJavaWatchpointDescriptors.length);
		}
		return fAllJavaWatchpointDescriptors;
	}

	/**
	 * @see IPropertySource#getPropertyValue(Object)
	 */
	public Object getPropertyValue(Object id) {
		if (id.equals(P_ID_ACCESS)) {
			try {
				IJavaWatchpoint wp = (IJavaWatchpoint) fBreakpoint;
				return wp.isAccess() ? P_VALUE_TRUE : P_VALUE_FALSE;
			} catch (CoreException ce) {
				return null;
			}
		} else if (id.equals(P_ID_MODIFICATION)) {
			try {
				IJavaWatchpoint wp = (IJavaWatchpoint) fBreakpoint;
				return wp.isModification() ? P_VALUE_TRUE : P_VALUE_FALSE;
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
		if (id.equals(P_ID_ACCESS)) {
			try {
				IJavaWatchpoint wp = (IJavaWatchpoint) fBreakpoint;
				wp.setAccess(((Integer)value).equals(P_VALUE_TRUE) ? true : false);
			} catch (CoreException ce) {
			}		
		} else if (id.equals(P_ID_MODIFICATION)) {
			try {
				IJavaWatchpoint wp = (IJavaWatchpoint) fBreakpoint;
				wp.setModification(((Integer)value).equals(P_VALUE_TRUE) ? true : false);
			} catch (CoreException ce) {
			}		
		} else {
			super.setPropertyValue(id, value);
		}
	}

}

