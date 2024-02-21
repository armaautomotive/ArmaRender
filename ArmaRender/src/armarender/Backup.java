/* Copyright (C) 2023 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package armarender;


import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.*;
import java.awt.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;


public class Backup extends BDialog {
    LayoutWindow window;
    
    private Scene scene;
    private BButton okButton, okButtonTest, cancelButton, inchToMMButton, mmToInchButton;
    private ValueField widthDistField, feedRateField, fastRateField;
    
    public Backup(LayoutWindow window){
        super(window, "Backup", true);
        this.window = window;
        scene = window.getScene();
        
        
        ScaleSettings scaleSettings = new ScaleSettings(window);
        String unitSymbol = scaleSettings.getSymbol();
        String unitLabel = scaleSettings.getLabel();
        String exportUnitLabelString = scaleSettings.getExportLabel();
        
        FormContainer content = new FormContainer(4, 11); // 10
        setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
        
        //content.setSize(300, 400);
        
        content.add(new BLabel("360 Degree Width:"), 0, 0);
        content.add(widthDistField = new ValueField(0.0, ValueField.NONE, 5), 1, 0);
        widthDistField.setValue(5.5); // TODO: Save the last entry in a property file.
        //content.add(inchToMMButton = Translate.button("in->mm", this, "inchToMM"), 2, 0);
        //content.add(mmToInchButton = Translate.button("mm->in", this, "mmToInch"), 3, 0);
        BLabel exportUnitLabel = new BLabel(exportUnitLabelString);
        exportUnitLabel.setAlignment(BLabel.WEST);
        exportUnitLabel.setTextPosition(BLabel.WEST); // Doesn't work
        content.add(exportUnitLabel, 2, 0);
        
        
        
        RowContainer buttons = new RowContainer();
        content.add(buttons, 0, 10, 4, 1, new LayoutInfo());
        buttons.add(okButton = Translate.button("ok", this, "doOk"));
        //buttons.add(okButtonTest = Translate.button("Ok Test", this, "doOkTest"));
        buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
        //makeObject();
        pack();
        UIUtilities.centerDialog(this, window);
        //updateComponents();
        setVisible(true);
        
    }
    
    
    
    /**
     * doOk
     *
     * Description: Handle dialog input. Launch export for selected object type.
     */
    private void doOk()
    {
        //if(isArc){
        //    exportArcTubeGCode(widthDistField.getValue());
        //} else {
        //    exportTubeGCode(widthDistField.getValue());
        //}
        dispose();
    }
    
    
}




