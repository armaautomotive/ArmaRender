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

public class MultiPaneTool extends EditingTool
{
  static int counter = 1;
  private boolean shiftDown;
  private Point clickPoint;
  private ObjectInfo objInfo;
  
  public MultiPaneTool(LayoutWindow fr)
  {
    super(fr);
    initButton("multiPane_48");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("multiPaneTool.helpText"));
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
    return Translate.text("multiPaneTool.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    clickPoint = e.getPoint();
    shiftDown = e.isShiftDown();
      
    System.out.println("Parallel clicked.");
      
      
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
      
  }
  
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    objInfo = null;
  }
    
    
    public void iconSingleClicked(MouseClickedEvent e)
    {
        // Get curent view and set display mode.
        ((LayoutWindow)theWindow).setSplitView(true);
        //((LayoutWindow)theWindow).toggleViewsCommand();
        
        // Set default tool or set tool bar focus
        //((LayoutWindow)theWindow). setTool (  ((LayoutWindow)theWindow).getDefaultTool() );
        
        // trigger window size recalculate
        //((LayoutWindow)theWindow).getView().viewChanged(true);
        
        // Redraw
        //((LayoutWindow)theWindow).updateImage();
    }
}
