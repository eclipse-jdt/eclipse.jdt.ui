/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

/**
 * A frame source is the source of frames which appear in a frame list.
 * The frame list asks for the current frame whenever it switches
 * to another frame, so that the context can be restored when the
 * frame becomes current again.
 *
 * @see FrameList
 */
/*
 * COPIED unmodified from org.eclipse.ui.views.navigator
 */
/* package */ interface IFrameSource {
/**
 * Returns a new frame describing the current state of the source.
 */
Frame getCurrentFrame();
}
