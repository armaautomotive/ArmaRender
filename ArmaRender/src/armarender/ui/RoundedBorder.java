/* Copyright (C) 2023 by Jon Taylor
   
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.ui;

import java.awt.*;
import javax.swing.border.*;

public class RoundedBorder implements Border {
    private int radius;
    private Color borderColor;

    public RoundedBorder(int radius, Color c) {
        this.radius = radius;
        this.borderColor = c;
    }

    public Insets getBorderInsets(Component c) {
        return new Insets(this.radius + 3, this.radius + 1 + 10, this.radius+2, this.radius + 10); // Top, Left, Bottom, Right
    }

    public boolean isBorderOpaque() {
        return false;
    }

    /**
     * paintBorder
     *
     * Description: 
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setStroke(new BasicStroke(2));
        g.setColor(borderColor);
        g.drawRoundRect(x + 1 + 2,
                        y + 4,
                        width-2 - 3,
                        height-6,
                        radius, radius); // X, Y, W, H
    }
}
