/* Copyright (C) 2021 by Jon Taylor

   
 */

package armarender;

import armarender.animation.*;
import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;

import java.awt.*;

/** CreateCubeTool is an EditingTool used for creating Cube objects. */

public class ViewTool extends EditingTool
{
  static int counter = 1;
  private boolean shiftDown;
  private Point clickPoint;
  private ObjectInfo objInfo;
  
  public ViewTool(LayoutWindow fr)
  {
    super(fr);
    initButton("views_96");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("views.helpText"));
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
    return Translate.text("views.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    clickPoint = e.getPoint();
    shiftDown = e.isShiftDown();
      
    //System.out.println("ViewTool clicked.");
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
     
  }
  
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    //objInfo = null;
  }
    
    public void iconSingleClicked(MouseClickedEvent e)
    {
        
        // Get curent view and set display mode.
        
        int x = e.getX();
        int y = e.getY();
       
        
        
        // 35x8 - 63x28 TOP
        if(x >= 35 && y >= 8 && x <= 63 && y <= 28){
            ((LayoutWindow)theWindow).getView().setOrientation(4);
        }
        
        // 68x6 - 92x30 BACK
        if(x >= 68 && y >= 6 && x <= 92 && y <= 30){
            ((LayoutWindow)theWindow).getView().setOrientation(1);
        }
        
        // 5x39 - 28x62 LEFT
        if(x >= 5 && y >= 39 && x <= 28 && y <= 62){
            ((LayoutWindow)theWindow).getView().setOrientation(2);
        }
        
        // 69x36 - 93x64 RIGHT
        if(x >= 69 && y >= 36 && x <= 93 && y <= 64){
            ((LayoutWindow)theWindow).getView().setOrientation(3);
        }
        
        // 5x70 - 32x93 FRONT
        if(x >= 5 && y >= 70 && x <= 32 && y <= 93){
            ((LayoutWindow)theWindow).getView().setOrientation(0);
        }
        
        // 35x74 - 64x92 BOTTOM
        if(x >= 35 && y >= 74 && x <= 64 && y <= 92){
            ((LayoutWindow)theWindow).getView().setOrientation(5);
        }
        
        ((LayoutWindow)theWindow).getView().repaint();
        
    }
}
