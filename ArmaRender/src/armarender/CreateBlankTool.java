/*  Copyright (C) 2021 by Jon Taylor
    Copyright (C) 1999-2007 by Peter Eastman

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
import buoy.widget.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Vector;

/** CreateMirrorPlaneTool is an EditingTool used for creating Curve objects. */

public class CreateBlankTool extends EditingTool
{
  static int counter = 1;
  private Vector<Vec3> clickPoint;
  private Vector<Float> smoothness;
  private int smoothing;
  private MirrorPlaneObject theCurve;
  private CoordinateSystem coords;
  public static final int HANDLE_SIZE = 3;

  public CreateBlankTool(EditingWindow fr)
  {
    super(fr);
      initButton("blank_48");
      
    smoothing = Mesh.APPROXIMATING;
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("createBlankTool.helpText"));
  }

  public void deactivate()
  {
    super.deactivate();
    addToScene();
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
      return "Blank."; // Translate.text("createBlankTool.tipText");
  }

  public boolean hilightSelection()
  {
    return (clickPoint == null);
  }
  
  public void drawOverlay(ViewerCanvas view)
  {
    
  }
  
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    
    //theWindow.updateImage();
  }
  
  /** When the user presses Enter, add the curve to the scene. */
  
  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    
  }

  /** Add the curve to the scene. */
  
  private void addToScene()
  {
    System.out.println(" addToScene ");
      // theCurve = Curve ;
      // CoordinateSystem coords
    boolean addCurve = (theCurve != null);
    if (addCurve)
      {
        // Make new coords to represent the dimension?
          
        ObjectInfo info = new ObjectInfo(theCurve, coords, "Mirror Plane "+(counter++));
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
          
        // create geometry of dimension.
          
          
        UndoRecord undo = new UndoRecord(theWindow, false);
        int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
        
        ((LayoutWindow) theWindow).addObject(info, undo);
          
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        
        theWindow.setUndoRecord(undo);
          
        ((LayoutWindow) theWindow).setSelection(theWindow.getScene().getNumObjects()-1);
      }
    clickPoint = null;
    smoothness = null;
    theCurve = null;
    coords = null;
    if (addCurve)
      theWindow.updateImage();
  }

  /**
   *
   *
   */
  public void iconDoubleClicked()
  {
      /*
    BComboBox smoothingChoice = new BComboBox(new String [] {
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    if (smoothing == Mesh.INTERPOLATING)
      smoothingChoice.setSelectedIndex(0);
    else
      smoothingChoice.setSelectedIndex(1);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("selectCurveSmoothing"),
                new Widget [] {smoothingChoice},
                new String [] {Translate.text("Smoothing Method")});
    if (!dlg.clickedOk())
      return;
    if (smoothingChoice.getSelectedIndex() == 0)
      smoothing = Mesh.INTERPOLATING;
    else
      smoothing = Mesh.APPROXIMATING;
       */
  }
      
}
