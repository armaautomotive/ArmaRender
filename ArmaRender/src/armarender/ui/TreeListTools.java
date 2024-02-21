/* Copyright (C) 2022 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package armarender.ui;

import armarender.*;
import buoy.event.*;
import buoy.widget.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import armarender.object.ObjectInfo;

/** This is a Widget which displays a . */

public class TreeListTools extends CustomWidget
{
    
    public Dimension getPreferredSize()
    {
      Dimension superPref = super.getPreferredSize();
      return new Dimension(1000, 20);
    }
    
    public Dimension getMinimumSize()
    {
      return new Dimension(10, 20);
    }

   

    private void paint(RepaintEvent ev)
    {
        Graphics2D g = ev.getGraphics();
        FontMetrics fm = g.getFontMetrics();
        Rectangle dim = getBounds();
        //int y = yoffset;
        
        Color black = new Color(50, 50, 50); // Color.black
        
        
        //Polygon openHandle = new Polygon(new int [] {-HANDLE_HEIGHT/2, HANDLE_HEIGHT/2, 0},
        //    new int [] {-HANDLE_WIDTH/2, -HANDLE_WIDTH/2, HANDLE_WIDTH/2}, 3);
        
        //g.drawPolygon(openHandle);
        g.drawRect(0,0,100,200);
        
        
        //icon.paintIcon(getComponent(), g, x, y + 2);
        
    }
    
}


