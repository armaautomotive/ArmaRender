/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.animation.*;
import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;

import java.awt.*;

/** CreateCubeTool is an EditingTool used for creating Cube objects. */

public class ParallelViewTool extends EditingTool
{
  static int counter = 1;
  private boolean shiftDown;
  private Point clickPoint;
  private ObjectInfo objInfo;
  
  public ParallelViewTool(LayoutWindow fr)
  {
    super(fr);
    initButton("parallel_48");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("parallelViewTool.helpText"));
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
    return Translate.text("parallelViewTool.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    clickPoint = e.getPoint();
    shiftDown = e.isShiftDown();
      
    System.out.println("Parallel clicked.");
      
      
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
      /*
    if (objInfo == null)
    {
      // Create the cube.
      
      Scene theScene = ((LayoutWindow) theWindow).getScene();
      objInfo = new ObjectInfo(new Cube(1.0, 1.0, 1.0), new CoordinateSystem(), "Cube "+(counter++));
      objInfo.addTrack(new PositionTrack(objInfo), 0);
      objInfo.addTrack(new RotationTrack(objInfo), 1);
      UndoRecord undo = new UndoRecord(theWindow, false);
      int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
      ((LayoutWindow) theWindow).addObject(objInfo, undo);
      undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
      theWindow.setUndoRecord(undo);
      ((LayoutWindow) theWindow).setSelection(theScene.getNumObjects()-1);
    }
    
    // Determine the size and position for the cube.
    
    Scene theScene = ((LayoutWindow) theWindow).getScene();
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    Vec3 v1, v2, v3, orig, xdir, ydir, zdir;
    double xsize, ysize, zsize;
    
    if (shiftDown)
    {
      if (Math.abs(dragPoint.x-clickPoint.x) > Math.abs(dragPoint.y-clickPoint.y))
      {
        if (dragPoint.y < clickPoint.y)
          dragPoint.y = clickPoint.y - Math.abs(dragPoint.x-clickPoint.x);
        else
          dragPoint.y = clickPoint.y + Math.abs(dragPoint.x-clickPoint.x);
      }
      else
      {
        if (dragPoint.x < clickPoint.x)
          dragPoint.x = clickPoint.x - Math.abs(dragPoint.y-clickPoint.y);
        else
          dragPoint.x = clickPoint.x + Math.abs(dragPoint.y-clickPoint.y);
      }
    }
    v1 = cam.convertScreenToWorld(clickPoint, Camera.DEFAULT_DISTANCE_TO_SCREEN);
    v2 = cam.convertScreenToWorld(new Point(dragPoint.x, clickPoint.y), Camera.DEFAULT_DISTANCE_TO_SCREEN);
    v3 = cam.convertScreenToWorld(dragPoint, Camera.DEFAULT_DISTANCE_TO_SCREEN);
    orig = v1.plus(v3).times(0.5);
    if (dragPoint.x < clickPoint.x)
      xdir = v1.minus(v2);
    else
      xdir = v2.minus(v1);
    if (dragPoint.y < clickPoint.y)
      ydir = v3.minus(v2);
    else
      ydir = v2.minus(v3);
    xsize = xdir.length();
    ysize = ydir.length();
    xdir = xdir.times(1.0/xsize);
    ydir = ydir.times(1.0/ysize);
    zdir = xdir.cross(ydir);
    zsize = Math.min(xsize, ysize);

    // Update the size and position, and redraw the display.
    
    ((Cube) objInfo.getObject()).setSize(xsize, ysize, zsize);
    objInfo.getCoords().setOrigin(orig);
    objInfo.getCoords().setOrientation(zdir, ydir);
    objInfo.clearCachedMeshes();
    theWindow.setModified();
    theWindow.updateImage();
       */
  }
  
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    objInfo = null;
  }
    
    
    public void iconDoubleClicked()
    {
        //System.out.println("Parallel 2X clicked.");
        
    }
    
    public void iconSingleClicked(MouseClickedEvent e)
    {
        LayoutWindow lw = (LayoutWindow)theWindow;
        //System.out.println("Parallel  clicked.");
        
        // Get curent view and set display mode.
        lw.getView().setPerspective(false);
        
        // Windows need to be updated with the active tool
        ToolPalette tp = lw.getToolPalette();
        EditingTool selectedTool = tp.getSelectedTool();
        lw.setTool(selectedTool);
    }
}
