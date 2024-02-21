/* Copyright (C) 2021 Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.view;

import buoy.widget.*;
import buoy.event.*;
import armarender.*;
import armarender.ui.*;
import java.awt.Font;
import java.awt.Color;
import javax.swing.Icon;
import javax.swing.BorderFactory;

/**
 * This is a ViewerControl for adjusting the scale of the view.
 */

public class ViewerLeftButtonControl implements ViewerControl
{
  public Widget createWidget(final ViewerCanvas view)
  {
      Icon leftIcon = ThemeManager.getIcon("left");
      //Button btnFront = new Button("F");
      final BButton envButton = new BButton(leftIcon);
      envButton.getComponent().setBorder( BorderFactory.createEmptyBorder(1, 2, 0, 2) ); // top, left, bottom, right
      
      envButton.addEventLink(CommandEvent.class, new Object()
      {
        void processEvent()
        {
            System.out.println("Left Button");
            
            // How do I know which view this is from???
            
            view.setOrientation(2); // 2 is left
            
        }
      });
      
    
      view.addEventLink(ViewChangedEvent.class, new Object() {
      void processEvent()
      {
        //if (view.isPerspective() || view.getBoundCamera() != null)
          //scaleField.setEnabled(false);
        //else
        //{
          //scaleField.setEnabled(true);
        //  if (view.getScale() != scaleField.getValue())
            //scaleField.setValue(view.getScale());
        //}
      }
    });
    
    return envButton;
  }


  public String getName()
  {
    return Translate.text("View Left");
  }
    
}
