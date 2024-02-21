/*  SolidEditor.java  */

package armadesignstudio.plugin;

/*
 * Copyright (C) 2005 by Nik Trevallyn-Jones
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

import armadesignstudio.*;
import armadesignstudio.ui.*;
import armadesignstudio.math.*;
import armadesignstudio.object.*;
import armadesignstudio.animation.*;

import buoy.event.*;
import buoy.widget.*;
import java.text.*;
import java.io.*;
import java.util.*;


/**
 * Editor for Solids
 */

public class SolidTool implements ModellingTool, Plugin
{
    protected Scene theScene;
    protected BFrame parent;
    protected ArrayList selList;

    CharArrayWriter message;
    BTextArea messageArea = new BTextArea(10, 45);
    BScrollPane scroll = new BScrollPane(BScrollPane.SCROLLBAR_AS_NEEDED,
					 BScrollPane.SCROLLBAR_AS_NEEDED);

    protected BButton validateB = Translate.button("validate", this,
						   "validate");
    protected BButton editB = Translate.button("edit", this, "edit");


    /**
     *  get the translator name (used as description)
     */
    public String getName()
    { return "Solid Editor..."; }

    /**
     * This is called when the menu item for this tool is selected.
     *
     * The single argument is the LayoutWindow in which the command was chosen.
     * The Scene object can then be obtained from the LayoutWindow's
     * getScene() method.
     */
    public void commandSelected(LayoutWindow window)
    {
	theScene = window.getScene();
	parent = window;

	int[] sel = theScene.getSelection();

	if (sel.length != 1
	    || !(theScene.getObject(sel[0]).object instanceof TriangleMesh)) {

	    new BStandardDialog("selectionError", new String [] {
		Translate.text("solidEditor"),
		Translate.text("please select a single triangle mesh object")
	    }, BStandardDialog.ERROR).showMessageDialog(parent);

	    return;
	}

	if (message == null) message = new CharArrayWriter(1024*16);
	else message.reset();

	final Object3D obj = theScene.getObject(sel[0]).object;
	final LayoutWindow win = window;

	Runnable onClose = new Runnable() {
		public void run() {
		    theScene.objectModified(obj);
		    win.updateImage();
		}
	    };

	ObjectInfo info = theScene.getObject(sel[0]);
	SolidEditorWindow ed = new SolidEditorWindow(window, "Solid", info,
						     onClose, true, message);

	ed.setVisible(true);

	System.out.println(message.toString());
    }

    public void processMessage(int message, Object args[])
    {
	// our own private message
	if (message == -1) {

	    SolidEditorWindow ed = new
		SolidEditorWindow((EditingWindow) args[0], "Solid",
				  (ObjectInfo) args[1],
				  (Runnable) args[2], true,
				  (Writer) args[3]);

	    ed.setVisible(true);
	}
    }

    /**
     *  validate the selected objects
     */
    protected void validate(WidgetEvent ev)
    {
    }

    /**
     *  validate the selected objects
     */
    protected void edit(WidgetEvent ev)
    {
    }

}
