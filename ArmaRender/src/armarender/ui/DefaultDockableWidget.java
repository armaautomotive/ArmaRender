/* Copyright (C) 2023 by Jon Taylor
   Copyright (C) 2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.ui;

import buoyx.docking.*;
import buoy.widget.*;

import java.awt.*;

/**
 * This is a DockableWidget subclass that paints its border using colors obtained from ThemeManager.
 */

public class DefaultDockableWidget extends DockableWidget
{
  public DefaultDockableWidget()
  {
  }

  public DefaultDockableWidget(Widget content, String label)
  {
    super(content, label);
  }

  protected void paintBorder(Graphics2D g)
  {
    Rectangle bounds = getBounds();
    Insets insets = getBorderInsets();
    //g.setPaint(new GradientPaint(0, 0, ThemeManager.getDockableBarColor1(),
    //                             0, insets.top, ThemeManager.getDockableBarColor2()));
      //g.setPaint(new GradientPaint(0, 0,  new Color(240, 240, 240),
      //                             0, insets.top, new Color(230, 230, 230)));
      
      g.setPaint(new GradientPaint(0, 0,  new Color(72, 133, 245),
                                   0, insets.top, new Color(68, 129, 241)));
      
    g.fillRect(0, 0, bounds.width, insets.top);
    g.setColor(ThemeManager.getDockableBarColor2().darker());
    g.drawLine(0, insets.top-1, bounds.width, insets.top-1);
    g.setColor(ThemeManager.getDockableTitleColor());
    if (getLabel() != null)
    {
      FontMetrics fm = getComponent().getFontMetrics(getComponent().getFont());
        
        g.setPaint(new Color(255, 255, 255));
      g.drawString(getLabel(), 2, fm.getMaxAscent()+2);
    }
  }
}
