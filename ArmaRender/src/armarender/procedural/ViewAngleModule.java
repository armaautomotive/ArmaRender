/* Copyright (C) 2003-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.procedural;

import armarender.*;
import armarender.math.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This is a Module which outputs the viewing angle. */

public class ViewAngleModule extends Module
{
  private boolean abs;
  private PointInfo point;

  public ViewAngleModule(Point position)
  {
    super(Translate.text("menu.viewAngleModule"), new IOPort [] {}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {Translate.text("menu.viewAngleModule")})}, 
      position);
  }

  /* Cache the PointInfo object to have access to the angle later on. */

  public void init(PointInfo p)
  {
    point = p;
  }

  /* This module outputs the value of the view angle. */
  
  public double getAverageValue(int which, double blur)
  {
    if (abs && point.viewangle < 0.0)
      return -point.viewangle;
    return point.viewangle;
  }

  /* The angle is always considered to be exact. */

  public double getValueError(int which, double blur)
  {
    return 0.0;
  }

  /* We always return no gradient, since there is not enough information to actually calculate it. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    grad.set(0.0, 0.0, 0.0);
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    ViewAngleModule mod = new ViewAngleModule(new Point(bounds.x, bounds.y));
    mod.abs = abs;
    mod.layout();
    return mod;
  }
  
  /* Allow the user to set the parameters. */
  
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final BCheckBox absBox = new BCheckBox(Translate.text("outputAbsValue"), abs);
    absBox.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        abs = absBox.getState();
        editor.updatePreview();
      }
    });
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), Translate.text("selectOutputProperties"),
      new Widget [] {absBox}, new String [] {null});
    if (!dlg.clickedOk())
      return false;
    return true;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeBoolean(abs);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    abs = in.readBoolean();
  }
}
