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

public class ViewerFrontButtonControl implements ViewerControl
{
  public Widget createWidget(final ViewerCanvas view)
  {
      Icon frontIcon = ThemeManager.getIcon("front");
      //BButton addButton = new BButton(addFolderIcon);
      //Button btnFront = new Button("F");
      //final BButton envButton = new BButton("Front");
      final BButton envButton = new BButton(frontIcon);
      
      envButton.getComponent().setBorder( BorderFactory.createEmptyBorder(1, 2, 0, 2) ); // top, left, bottom, right
      
      envButton.addEventLink(CommandEvent.class, new Object()
      {
        void processEvent()
        {
            System.out.println("Front Button");
            // How do I know which view this is from???
            view.setOrientation(0); // 0 is front
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
      
      //Font testFont = new Font("TimesRoman", Font.PLAIN, 8);
      //envButton.setFont(testFont);
      //Font font = envButton.getFont();
      //int fontSize = font.getSize();
      //System.out.println(" font size: " + fontSize );
      //font = font.deriveFont(8);
      //fontSize = font.getSize();
      //System.out.println(" -font size: " + fontSize );
      //envButton.setBackground(Color.RED);
    return envButton;
  }


  public String getName()
  {
    return Translate.text("View Front");
  }
    
}
