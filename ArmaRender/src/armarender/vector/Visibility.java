/*  Visibility.java  */

package armarender.vector;

/*
 * Visibility: handles visibility processing for renderers
 *
 * Author: Nik Trevallyn-Jones, nik777@users.sourceforge.net
 * $Id: Exp $
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * with this program. If not, the license is available from the
 * GNU project, at http://www.gnu.org.
 */


import armarender.*;
import armarender.object.*;
import armarender.math.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;


/**
 *  Visibility calculates whether a specific point is currently visible.
 */

public class Visibility
{
    public static final int SHOW		= 0;
    public static final int HIDE		= 1;

    public static final int ALL			= 0;
    public static final int FRONT		= 1;

    protected static final String[] typeNames = new String[] {
	Translate.text("all"),
	Translate.text("front"),
    };

    BComboBox visModeChoice, visTypeChoice;
    ValueField visPctField, dirXField, dirYField;
    Widget pluginConfig;
    RowContainer visRow, dfltPluginConfig;
    ColumnContainer configPanel;

    ObjectInfo theObj = null;
    int visMode, visType;
    double visPct, dirX, dirY;
    Vec3 viewDir, visDir;
    ArrayList plugins;

    public Visibility()
    {}

    public Widget getConfigPanel()
    {
	if (configPanel == null) {
	    configPanel = new ColumnContainer();

	    visRow = new RowContainer();

	    configPanel.add(visRow);

	    visModeChoice = new BComboBox(new String[] {
		Translate.text("show"),
		Translate.text("hide")
	    });

	    visTypeChoice = new BComboBox(typeNames);

	    if (plugins != null) {
		for (int x = plugins.size()-1; x >= 0; x--)
		    System.out.println("cannot add plugins yet");
	    }

	    dirXField = new ValueField(50, 0, 3);
	    dirYField = new ValueField(50, 0, 3);

	    visPctField = new ValueField(50, ValueField.NONNEGATIVE
					 | ValueField.INTEGER, 3);

	    dfltPluginConfig = new RowContainer();
	    dfltPluginConfig.add(visPctField);
	    dfltPluginConfig.add(new BLabel(Translate.text("%")));

	    visTypeChoice.addEventLink(ValueChangedEvent.class, new Object() {
		    void processEvent()
		    {
			int pos = visTypeChoice.getSelectedIndex();

			configPanel.remove(pluginConfig);

			dirXField.setEnabled(pos > 0);
			dirYField.setEnabled(pos > 0);

			if (pos == 0)
			    pluginConfig = null;

			else {
			    if (pos >= typeNames.length) {
				pluginConfig = ((Plugin) plugins.
				  get(pos-typeNames.length)).getConfigPanel();
			    }
			    else pluginConfig = dfltPluginConfig;
			    configPanel.add(pluginConfig);
			}

			UIUtilities.findWindow(configPanel).pack();
		    }
		});

	    visRow.add(visModeChoice);
	    visRow.add(visTypeChoice);
	    visRow.add(new BLabel("x"));
	    visRow.add(dirXField);
	    visRow.add(new BLabel("y"));
	    visRow.add(dirYField);

	    dirXField.setEnabled(false);
	    dirYField.setEnabled(false);

	}

	return configPanel;
    }

    public void set()
    {
	if (configPanel == null) getConfigPanel();

	visMode = visModeChoice.getSelectedIndex();
	visType = visTypeChoice.getSelectedIndex();
	dirX = dirXField.getValue();
	dirY = dirYField.getValue();
	visPct = visPctField.getValue();

	visPct = (Math.max(Math.min((float) visPctField.getValue(), 100), 0)
		  -50)*Math.PI/100;
    }

    public void reset()
    {
	visModeChoice.setSelectedIndex(visMode);
	visTypeChoice.setSelectedIndex(visType);
	dirXField.setValue(dirX);
	dirYField.setValue(dirY);
	visPctField.setValue(50+(100*visPct/Math.PI));
    }

    public void bind(ObjectInfo obj, Vec3 viewdir)
    {
	theObj = obj;

	if (viewDir == null) {
	    viewDir = new Vec3();
	    visDir = new Vec3();
	}

	viewDir.set(viewdir);

	visDir.set(Mat4.yrotation((dirX-50)*Math.PI/100).timesDirection(viewDir));
	visDir.set(Mat4.xrotation((dirY-50)*Math.PI/100).timesDirection(visDir));
	visDir.normalize();
    }

    public boolean isVisible(Vec3 pos, Vec3 norm, int dflt)
    { return (visibility(pos, norm, dflt) > 0); }

    public double visibility(Vec3 pos, Vec3 norm, int dflt)
    {
	switch (visMode == SHOW && visType == ALL ? dflt : visType) {
	case ALL:
	    return (visMode == SHOW ? 1.0 : 0.0);

	case FRONT:
	    if (visMode == SHOW)
		return (visDir.dot(norm) < visPct ? 1.0 : 0.0);
	    else return (visDir.dot(norm) > visPct ? 1.0 : 0.0);

	default:
	    return ((Visibility.Plugin)
	       plugins.get(visType-typeNames.length)).
		visibility(pos, norm, viewDir, visDir);
	}
    }

    /**
     *  plugin visibility interface
     */
    public abstract class Plugin
    {
	public abstract String getName();
	public abstract Widget getConfigPanel();
	public abstract void bind();
	public abstract double visibility(Vec3 pos, Vec3 norm, Vec3 viewdir, Vec3 visDir);
    }
}
