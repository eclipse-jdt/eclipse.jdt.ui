package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.BreakpointPropertySource;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.IJavaBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

/**
 * Handle properties for instances of IJavaBreakpoint & IJavaLineBreakpoint
 */
public class JavaBreakpointPropertySource extends BreakpointPropertySource {

	/**
	 * Holds all property descriptors defined in this class (the property
	 * descriptors unique to IJavaBreakpoints)
	 */
	private static IPropertyDescriptor[] fLocalJavaDescriptors;
	
	/**
	 * Holds all property descriptors defined in this class's hierarchy (all
	 * property descriptors for IJavaBreakpoints)
	 */
	private static IPropertyDescriptor[] fAllJavaDescriptors;

	protected static JavaElementLabelProvider fJavaLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);

	// Property Values
	protected static final String P_ID_HIT_COUNT = "hitCount";
	protected static final String P_ID_LOCATION = "location";
	
	private static final String P_HIT_COUNT = "hit count";
	private static final String P_LOCATION = "location";
	
	private static final int DEFAULT_HIT_COUNT_VALUE = 0;
	
	public static class PositiveNumericCellValidator implements ICellEditorValidator {
		public String isValid(Object value) {
			if ((value != null) && (value instanceof String)) {
				try {
					int num = Integer.parseInt((String)value);
					if (num >= 0) {
						return null;
					}
				} catch (NumberFormatException nfe) {
				}
			}
			return "Invalid value";
		}
	}
	
	static {
		fLocalJavaDescriptors = new IPropertyDescriptor[2];
		PropertyDescriptor propertyDescriptor;
				
		propertyDescriptor = new TextPropertyDescriptor(P_ID_HIT_COUNT, P_HIT_COUNT);
		propertyDescriptor.setValidator(new PositiveNumericCellValidator());
		fLocalJavaDescriptors[0] = propertyDescriptor;	

		propertyDescriptor = new PropertyDescriptor(P_ID_LOCATION, P_LOCATION);
		fLocalJavaDescriptors[1] = propertyDescriptor;	
	}

	/**
	 * @see IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (fAllJavaDescriptors == null) {
			IPropertyDescriptor[] superDescriptors = super.getPropertyDescriptors();
			fAllJavaDescriptors = new IPropertyDescriptor[superDescriptors.length + fLocalJavaDescriptors.length];
			System.arraycopy(superDescriptors, 0, fAllJavaDescriptors, 0, superDescriptors.length);
			System.arraycopy(fLocalJavaDescriptors, 0, fAllJavaDescriptors, superDescriptors.length, fLocalJavaDescriptors.length);
		}
		return fAllJavaDescriptors;
	}

	/**
	 * @see IPropertySource#getPropertyValue(Object)
	 */
	public Object getPropertyValue(Object id) {
		if (id.equals(P_ID_LOCATION)) {
			try {
				IJavaBreakpoint bp = (IJavaBreakpoint)fBreakpoint;
				StringBuffer buffer = getFullyQualifiedTypeName(bp);
				if (bp instanceof IJavaLineBreakpoint) {
					IJavaLineBreakpoint jlbp = (IJavaLineBreakpoint) bp;
					int lineNumber = jlbp.getLineNumber();
					if (lineNumber > 0) {
						buffer.append(" [");
						buffer.append(DebugUIUtils.getResourceString("jdi_model_presentation.line"));
						buffer.append(' ');
						buffer.append(lineNumber);
						buffer.append(']');
					}
					IMember member = jlbp.getMember();
					if (member != null) {
						buffer.append(" - ");
						buffer.append(fJavaLabelProvider.getText(member));							
					}
				}
				return buffer.toString();
			} catch (CoreException ce) {
				return null;
			}
		} else if (id.equals(P_ID_HIT_COUNT)) {
			try {
				IJavaBreakpoint bp = (IJavaBreakpoint)fBreakpoint;
				int hitCount = bp.getHitCount();
				if (hitCount < 0) {
					hitCount = DEFAULT_HIT_COUNT_VALUE;
				}
				return String.valueOf(hitCount);
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
		if (id.equals(P_ID_HIT_COUNT)) {
			try {
				IJavaBreakpoint bp = (IJavaBreakpoint)fBreakpoint;
				int hitCount = Integer.parseInt((String)value);
				bp.setHitCount(hitCount);
			} catch (NumberFormatException nfe) {
				return;
			} catch (CoreException ce) {
				return;				
			}
		} else {
			super.setPropertyValue(id, value);
		}
	}
	
	/**
	 * Helper method
	 */
	protected StringBuffer getFullyQualifiedTypeName(IJavaBreakpoint bp) {
		StringBuffer buffer = new StringBuffer(20);
		try {
			IType type = bp.getType();
			buffer.append(type.getFullyQualifiedName());
		} catch (CoreException ce) {			
		}
		return buffer;
	}

}

