package org.eclipse.jdt.internal.ui.viewsupport;


import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * A <code>JavaOverlayDescriptor</code> implements predicates on Java elements and defines 
 * classes of Java element properties. It assigns a unique name to each of
 * these classes. 
 */
public final class JavaOverlayDescriptor implements IOverlayDescriptor {
	
	public final static int ABSTRACT= 		0x001;
	public final static int FINAL=		0x002;
	public final static int SYNCHRONIZED=	0x004;
	public final static int STATIC=		0x008;
	public final static int RUNNABLE= 		0x010;
	public final static int WARNING=		0x020;
	public final static int ERROR=		0x040;

	static final int[]  FLAGS= { ABSTRACT, FINAL, SYNCHRONIZED, STATIC, RUNNABLE, WARNING, ERROR  };
	static final String[] TAGS= { "A", "F", "Sy", "St", "R", "W", "E" };
	
	private String fBase;
	private int fFlags;
	private String fName;
	
	public JavaOverlayDescriptor(String base, int flags) {
		fBase= base;
		fFlags= flags;
	}
		
	public String getBaseName() {
		return fBase;
	}
		
	public ImageDescriptor[][] getOverlays() {
		ImageDescriptor[][] overlays= new ImageDescriptor[3][];
		overlays[0]= new ImageDescriptor[5];
		overlays[1]= new ImageDescriptor[1];
		overlays[2]= new ImageDescriptor[2];
		int n= 0;
		if (isStatic())
			overlays[0][n++]= JavaPluginImages.DESC_OVR_STATIC;
		if (isFinal())
			overlays[0][n++]= JavaPluginImages.DESC_OVR_FINAL;
		if (isAbstract())
			overlays[0][n++]= JavaPluginImages.DESC_OVR_ABSTRACT;
		if (isSynchronized())
			overlays[0][n++]= JavaPluginImages.DESC_OVR_SYNCH;
		
		n= 0;
		if (isRunnable())
			overlays[1][n++]= JavaPluginImages.DESC_OVR_RUN;	
		
		n= 0;
		if (isWarning())
			overlays[2][n++]= JavaPluginImages.DESC_OVR_WARNING;	
		if (isError())
			overlays[2][n++]= JavaPluginImages.DESC_OVR_ERROR;	
		return overlays;		
	}	
	/**
	 * @see Object#equals
	 */
	public boolean equals(Object obj) {
		if ( !(obj instanceof JavaOverlayDescriptor))
			return false;
			
		JavaOverlayDescriptor k= (JavaOverlayDescriptor) obj;
		return (fBase.equals(k.fBase) && fFlags == k.fFlags);
	}
	
	/**
	 * @see Object#hashCode
	 */
	public int hashCode() {
		return fBase.hashCode() | fFlags;
	}
	
	public boolean isAbstract() {
		return (fFlags & ABSTRACT) != 0;
	}
	
	public boolean isFinal() {
		return (fFlags & FINAL) != 0;
	}
	
	public boolean isSynchronized() {
		return (fFlags & SYNCHRONIZED) != 0;
	}
	
	public boolean isStatic() {
		return (fFlags & STATIC) != 0;
	}
	
	public boolean isRunnable() {
		return (fFlags & RUNNABLE) != 0;			
	}
	
	public boolean isWarning() {
		return (fFlags & WARNING) != 0;
	}
	
	public boolean isError() {
		return (fFlags & ERROR) != 0;
	}
	/**
	 * @see IOverlayDescriptor#getKey()
	 */
	public String getKey() {
		if (fName == null)
			fName= createName();
		return fName;
	}
	
	private String createName() {	
		StringBuffer b= new StringBuffer();
		b.append(fBase);
		b.append("_OVR");
		for (int i= 0; i < FLAGS.length; i++) {
			if ((fFlags & FLAGS[i]) != 0) {
				b.append('_');
				b.append(TAGS[i]);
			}
		}
		return b.toString();
	}
};
