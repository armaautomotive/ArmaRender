/* Copyright (C) 2021 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */



package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;
import armarender.view.CanvasDrawer;
import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import buoy.widget.*;
import armarender.ui.*;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JFrame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

//import javax.faces.event.*;
//import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import buoy.event.*;

public class PairDistanceAlign extends BDialog
{
    private LayoutWindow window;
    private BComboBox objChoice, pathChoice;
    private RadioButtonGroup pathGroup;
    private BRadioButton pathBox, xBox, yBox, zBox, vectorBox;
    private BCheckBox orientBox;
    private ValueField distField, xField, yField, zField, segField, angleField, tolField;
    private BButton okButton, cancelButton;
    private ObjectPreviewCanvas preview;
    private Vector objects, paths;

    private ValueField aPrimaryField;
    private ValueField bPrimaryField;

    private BRadioButton radioA;
    private BRadioButton radioB;

    private ValueField xDistField;
    private ValueField yDistField;
    private ValueField zDistField;

    private BCheckBox alignXcb;
    private BCheckBox alignYcb;
    private BCheckBox alignZcb;

    private BRadioButton alignFromCentreRadio;
    private BRadioButton alignFromTopRadio;
    private BRadioButton alignFromBottomRadio;
    private BRadioButton alignFromLeftRadio;
    private BRadioButton alignFromRightRadio;
    
    private BRadioButton alignToCentreRadio;
    private BRadioButton alignToTopRadio;
    private BRadioButton alignToBottomRadio;
    private BRadioButton alignToLeftRadio;
    private BRadioButton alignToRightRadio;
    
    private static ObjectInfo highlightedObject;
    private ObjectInfo oia;
    private ObjectInfo oib;

    private static Vec3 highlightedPoint;
    private Vec3 va = null;
    private Vec3 vb = null;
    
    /**
     * PairDistanceAlign
     *
     * Description:
     */
    public PairDistanceAlign(LayoutWindow window, ObjectInfo a, ObjectInfo b)
    {
      super(window, "Pair Distance Align", /* modal */ false);
      this.window = window;
      Scene scene = window.getScene();
      int selection[] = window.getSelectedIndices();
        
        //highlightedObject = null;
        setObjects(a, b);
        
        CoordinateSystem aCoords = a.getCoords();
        CoordinateSystem bCoords = b.getCoords();
        Vec3 aOrigin = aCoords.getOrigin();
        Vec3 bOrigin = bCoords.getOrigin();
        
        // Get bounds to draw as highlighted.
        //BoundingBox aBounds = a.getTranslatedBounds();
        a.setRenderMoveHighlight(true);
        highlightedObject = a;
        
        FormContainer content = new FormContainer(4, 17);
        setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
        
        content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
        content.add(new BLabel("Selection:"), 0, 0, 2, 1);
        
        RadioButtonGroup group = new RadioButtonGroup();
        
        boolean aState = false;
        boolean bState = false;
        if( highlightedObject == oia){ // Move A
            aState = true;
        } else {
            bState = true;
        }
        radioA = new BRadioButton( "A", aState, group );
        radioB = new BRadioButton( "B", bState, group );
        
        //BRadioButton arb = null;
        //BRadioButton brb = null;
        content.add(new BLabel("Move A:"), 2, 0);
        content.add(new BLabel("Move B:"), 2, 1);
        content.add(radioA, 3, 0);
        content.add(radioB, 3, 1);
        radioA.addEventLink(ValueChangedEvent.class, this, "updateMoveA");
        radioB.addEventLink(ValueChangedEvent.class, this, "updateMoveB");
        
        content.add(new BLabel("Distance:"), 0, 2);
        content.add(new BLabel("X:"), 2, 2);
        content.add(new BLabel("Y:"), 2, 3);
        content.add(new BLabel("Z:"), 2, 4);
        // TODO: get distances and populate fields.
        
        content.add(xDistField = new ValueField(aOrigin.x - bOrigin.x, ValueField.NONE, 5), 3, 2);
        content.add(yDistField = new ValueField(aOrigin.y - bOrigin.y, ValueField.NONE, 5), 3, 3);
        content.add(zDistField = new ValueField(aOrigin.z - bOrigin.z, ValueField.NONE, 5), 3, 4);
        xDistField.addEventLink(ValueChangedEvent.class, this, "updateDistanceX");
        yDistField.addEventLink(ValueChangedEvent.class, this, "updateDistanceY");
        zDistField.addEventLink(ValueChangedEvent.class, this, "updateDistanceZ");
        
        // Align X
        // Align Y
        // Align Z
        
        content.add(new BLabel("Align:"), 0, 5);
        content.add(new BLabel("X:"), 2, 5);
        content.add(new BLabel("Y:"), 2, 6);
        content.add(new BLabel("Z:"), 2, 7);
         alignXcb = new BCheckBox();
         alignYcb = new BCheckBox();
         alignZcb = new BCheckBox();
        content.add(alignXcb, 3, 5);
        content.add(alignYcb, 3, 6);
        content.add(alignZcb, 3, 7);
        alignXcb.addEventLink(ValueChangedEvent.class, this, "updateAlignX");
        alignYcb.addEventLink(ValueChangedEvent.class, this, "updateAlignY");
        alignZcb.addEventLink(ValueChangedEvent.class, this, "updateAlignZ");
        
        // align from
        content.add(new BLabel("From:"), 0, 8);
        RadioButtonGroup alignFromGroup = new RadioButtonGroup();
        alignFromCentreRadio = new BRadioButton( "Centre", true, alignFromGroup);
        alignFromTopRadio = new BRadioButton( "Top", false, alignFromGroup );
        alignFromBottomRadio = new BRadioButton( "Bottom", false, alignFromGroup );
        alignFromLeftRadio = new BRadioButton( "Left", false, alignFromGroup );
        alignFromRightRadio = new BRadioButton( "Right", false, alignFromGroup );
        content.add(alignFromTopRadio, 2, 9);
        content.add(alignFromCentreRadio, 2, 10);
        content.add(alignFromBottomRadio, 2, 11);
        content.add(alignFromLeftRadio, 0, 10); // 0 or 1
        content.add(alignFromRightRadio, 3, 10);
        
        // align to
        content.add(new BLabel("To:"), 0, 12);
        RadioButtonGroup alignToGroup = new RadioButtonGroup();
        alignToCentreRadio = new BRadioButton( "Centre", true, alignToGroup);
        alignToTopRadio = new BRadioButton( "Top", false, alignToGroup );
        alignToBottomRadio = new BRadioButton( "Bottom", false, alignToGroup );
        alignToLeftRadio = new BRadioButton( "Left", false, alignToGroup );
        alignToRightRadio = new BRadioButton( "Right", false, alignToGroup );
        content.add(alignToTopRadio, 2, 13);
        content.add(alignToCentreRadio, 2, 14);
        content.add(alignToBottomRadio, 2, 15);
        content.add(alignToLeftRadio, 0, 14); // 0 or 1
        content.add(alignToRightRadio, 3, 14);
        
        RowContainer buttons = new RowContainer();
        content.add(buttons, 0, 16, 4, 1, new LayoutInfo());
        
        buttons.add(okButton = Translate.button("ok", this, "doOk"));
        buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
        //makeObject();
        pack();
        //UIUtilities.centerDialog(this, window);
        UIUtilities.lowerRightDialog(this, window);
        //updateComponents();
        setVisible(true);
    }
    
    
    // LayoutWindow is BFrame
    // ObjectEditorWindow
    // EditingWindow is ???
    
    // CurveEditorWindow is MeshEditorWindow
    // MeshEditorWindow is ObjectEditorWindow
    // ObjectEditorWindow is BFrame
    public PairDistanceAlign(CurveEditorWindow window, Vec3 a, Vec3 b)
    {
      super(window, "Pair Distance Align", /* modal */ false);
      //this.window = window;
      Scene scene = window.getScene();
      //int selection[] = window.getSelectedIndices();
        
        //highlightedObject = null;
        setPoints(a, b);
        
        //CoordinateSystem aCoords = a.getCoords();
        //CoordinateSystem bCoords = b.getCoords();
        Vec3 aOrigin = a; // aCoords.getOrigin();
        Vec3 bOrigin = b; // bCoords.getOrigin();
        
        // Get bounds to draw as highlighted.
        //BoundingBox aBounds = a.getTranslatedBounds();
        //a.setRenderMoveHighlight(true);
        //highlightedObject = a;
        highlightedPoint = a;
        
        FormContainer content = new FormContainer(4, 10);
        setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
        
        content.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
        content.add(new BLabel("Selection:"), 0, 0, 2, 1);
        
        RadioButtonGroup group = new RadioButtonGroup();
        
        boolean aState = false;
        boolean bState = false;
        //if( highlightedObject == oia){ // Move A
        if(va == highlightedPoint){
            aState = true;
        } else {
            bState = true;
        }
        radioA = new BRadioButton( "A", aState, group );
        radioB = new BRadioButton( "B", bState, group );
        
        //BRadioButton arb = null;
        //BRadioButton brb = null;
        content.add(new BLabel("Move A:"), 2, 0);
        content.add(new BLabel("Move B:"), 2, 1);
        content.add(radioA, 3, 0);
        content.add(radioB, 3, 1);
        radioA.addEventLink(ValueChangedEvent.class, this, "updateMoveA");
        radioB.addEventLink(ValueChangedEvent.class, this, "updateMoveB");
        
        content.add(new BLabel("Distance:"), 0, 2);
        content.add(new BLabel("X:"), 2, 2);
        content.add(new BLabel("Y:"), 2, 3);
        content.add(new BLabel("Z:"), 2, 4);
        // TODO: get distances and populate fields.
        
        content.add(xDistField = new ValueField(aOrigin.x - bOrigin.x, ValueField.NONE, 5), 3, 2);
        content.add(yDistField = new ValueField(aOrigin.y - bOrigin.y, ValueField.NONE, 5), 3, 3);
        content.add(zDistField = new ValueField(aOrigin.z - bOrigin.z, ValueField.NONE, 5), 3, 4);
        xDistField.addEventLink(ValueChangedEvent.class, this, "updateDistanceX");
        yDistField.addEventLink(ValueChangedEvent.class, this, "updateDistanceY");
        zDistField.addEventLink(ValueChangedEvent.class, this, "updateDistanceZ");
        
        // Align X
        // Align Y
        // Align Z
        
        content.add(new BLabel("Align:"), 0, 5);
        content.add(new BLabel("X:"), 2, 5);
        content.add(new BLabel("Y:"), 2, 6);
        content.add(new BLabel("Z:"), 2, 7);
         alignXcb = new BCheckBox();
         alignYcb = new BCheckBox();
         alignZcb = new BCheckBox();
        content.add(alignXcb, 3, 5);
        content.add(alignYcb, 3, 6);
        content.add(alignZcb, 3, 7);
        alignXcb.addEventLink(ValueChangedEvent.class, this, "updateAlignX");
        alignYcb.addEventLink(ValueChangedEvent.class, this, "updateAlignY");
        alignZcb.addEventLink(ValueChangedEvent.class, this, "updateAlignZ");
        
        
        RowContainer buttons = new RowContainer();
        content.add(buttons, 0, 9, 4, 1, new LayoutInfo());
        
        buttons.add(okButton = Translate.button("ok", this, "doOk"));
        buttons.add(cancelButton = Translate.button("cancel", this, "dispose"));
        //makeObject();
        pack();
        //UIUtilities.centerDialog(this, window);
        UIUtilities.lowerRightDialog(this, window);
        //updateComponents();
        setVisible(true);
    }
    
    
    /**
     * setObjects
     *
     */
    public void setObjects(ObjectInfo a, ObjectInfo b){
        // xDistField = new ValueField(aOrigin.x - bOrigin.x, ValueField.NONE, 5)
        this.oia = a;
        this.oib = b;
    }
    
    public void setPoints(Vec3 a, Vec3 b){
        this.va = a;
        this.vb = b;
    }
    
    
    //
    public void deselectObject(){
        if(highlightedObject != null){
            highlightedObject.setRenderMoveHighlight(false);
            System.out.println("*** Deselect.");
        } else {
            System.out.println("*** Deselect: object is null. ");
        }
    }
    
    public void deselectPoints(){
        System.out.println("*** Deselect points.");
        
    }
    
    
    private void updateMoveA(){
        // radioA
        // highlightedObject
        // oia
        // oib
        CoordinateSystem aCoords = oia.getCoords();
        CoordinateSystem bCoords = oib.getCoords();
        Vec3 aOrigin = aCoords.getOrigin();
        Vec3 bOrigin = bCoords.getOrigin();
        
        oia.setRenderMoveHighlight(true);
        oib.setRenderMoveHighlight(false);
        highlightedObject = oia;
        
        // Update dialog
        xDistField.setValue(aOrigin.x - bOrigin.x);
        yDistField.setValue(aOrigin.y - bOrigin.y);
        zDistField.setValue(aOrigin.z - bOrigin.z);
        
        window.updateImage();
    }
    
    /**
     * updateMoveB
     *
     */
    private void updateMoveB(){
        // radioB
        CoordinateSystem aCoords = oia.getCoords();
        CoordinateSystem bCoords = oib.getCoords();
        Vec3 aOrigin = aCoords.getOrigin();
        Vec3 bOrigin = bCoords.getOrigin();
        
        oia.setRenderMoveHighlight(false);
        oib.setRenderMoveHighlight(true);
        highlightedObject = oib;
        
        // Update dialog
        xDistField.setValue(bOrigin.x - aOrigin.x);
        yDistField.setValue(bOrigin.y - aOrigin.y);
        zDistField.setValue(bOrigin.z - aOrigin.z);
        
        window.updateImage();
    }
    
    /**
     * updateDistanceX
     *
     * Description:
     */
    private void updateDistanceX(){
        double xDist = xDistField.getValue();
        if(oia != null){
            CoordinateSystem aCoords = oia.getCoords();
            CoordinateSystem bCoords = oib.getCoords();
            Vec3 aOrigin = aCoords.getOrigin();
            Vec3 bOrigin = bCoords.getOrigin();
            if( highlightedObject == oia){ // Move A
                double bX = bOrigin.x;
                aOrigin.x = bX + xDist;
                aCoords.setOrigin(aOrigin);
                // Update view
                window.updateImage();
                //System.out.println(" Update A ");
            } else {                        // Move B
                double aX = aOrigin.x;
                bOrigin.x = aX + xDist;
                bCoords.setOrigin(bOrigin);
                // Update view
                window.updateImage();
                //System.out.println(" Update B ");
            }
        }
        if(va != null){
            System.out.println(" updateDistanceX point");
        }
    }
    
    private void updateDistanceY(){
        double yDist = yDistField.getValue();
        CoordinateSystem aCoords = oia.getCoords();
        CoordinateSystem bCoords = oib.getCoords();
        Vec3 aOrigin = aCoords.getOrigin();
        Vec3 bOrigin = bCoords.getOrigin();
        if(highlightedObject == oia){ // Move A
            double bY = bOrigin.y;
            aOrigin.y = bY + yDist;
            aCoords.setOrigin(aOrigin);
            // Update view
            window.updateImage();
            //System.out.println(" Update A ");
        } else {                        // Move B
            double aY = aOrigin.y;
            bOrigin.y = aY + yDist;
            bCoords.setOrigin(bOrigin);
            // Update view
            window.updateImage();
            //System.out.println(" Update B ");
        }
    }
    
    private void updateDistanceZ(){
        double zDist = zDistField.getValue();
        CoordinateSystem aCoords = oia.getCoords();
        CoordinateSystem bCoords = oib.getCoords();
        Vec3 aOrigin = aCoords.getOrigin();
        Vec3 bOrigin = bCoords.getOrigin();
        if(highlightedObject == oia){ // Move A
            double bZ = bOrigin.z;
            aOrigin.z = bZ + zDist;
            aCoords.setOrigin(aOrigin);
            // Update view
            window.updateImage();
            //System.out.println(" Update A ");
        } else {                        // Move B
            double aZ = aOrigin.z;
            bOrigin.z = aZ + zDist;
            bCoords.setOrigin(bOrigin);
            // Update view
            window.updateImage();
            //System.out.println(" Update B ");
        }
    }
    
    private void updateAlignX(){
        //System.out.println(" updateAlignX() " + alignXcb.getState() );
        CoordinateSystem aCoords = oia.getCoords();
        CoordinateSystem bCoords = oib.getCoords();
        Vec3 aOrigin = aCoords.getOrigin();
        Vec3 bOrigin = bCoords.getOrigin();
        if(highlightedObject == oia){ // Move A
            double bX = bOrigin.x;
            aOrigin.x = bX;
            aCoords.setOrigin(aOrigin);
            // Update view
            window.updateImage();
        } else { // Move B
            double aX = aOrigin.x;
            bOrigin.x = aX;
            bCoords.setOrigin(bOrigin);
            // Update view
            window.updateImage();
        }
    }
    
    /**
     * updateAlignY
     */
    private void updateAlignY(){
        //System.out.println(" updateAlignX() " + alignXcb.getState() );
        CoordinateSystem aCoords = oia.getCoords();
        CoordinateSystem bCoords = oib.getCoords();
        Vec3 aOrigin = aCoords.getOrigin();
        Vec3 bOrigin = bCoords.getOrigin();
        
        if(highlightedObject == oia){ // Move A
            
            if(alignFromTopRadio.getState() == true){
                aOrigin.y = oia.getTopVec3().y;
                System.out.println(" fromTop a");
            }
            if(alignToBottomRadio.getState() == true){
                bOrigin.y = oib.getBottomVec3().y;
                System.out.println(" toBottom a");
            }
            
            double bY = bOrigin.y;
            aOrigin.y = bY;
            aCoords.setOrigin(aOrigin);
            // Update view
            window.updateImage();
        } else { // Move B
            
            if(alignFromTopRadio.getState() == true){
                bOrigin.y = oib.getTopVec3().y;
                System.out.println(" from top b ");
            }
            if(alignToBottomRadio.getState() == true){
                aOrigin.y = oia.getBottomVec3().y;
                System.out.println(" toBottom  b ");
            }
            
            double aY = aOrigin.y;
            bOrigin.y = aY;
            bCoords.setOrigin(bOrigin);
            // Update view
            window.updateImage();
        }
    }
    
    private void updateAlignZ(){
        //System.out.println(" updateAlignX() " + alignXcb.getState() );
        CoordinateSystem aCoords = oia.getCoords();
        CoordinateSystem bCoords = oib.getCoords();
        Vec3 aOrigin = aCoords.getOrigin();
        Vec3 bOrigin = bCoords.getOrigin();
        if(highlightedObject == oia){ // Move A
            double bZ = bOrigin.z;
            aOrigin.z = bZ;
            aCoords.setOrigin(aOrigin);
            // Update view
            window.updateImage();
        } else { // Move B
            double aZ = aOrigin.z;
            bOrigin.z = aZ;
            bCoords.setOrigin(bOrigin);
            // Update view
            window.updateImage();
        }
    }
    
    
    
    

    // Create the extruded object.
    
    private void makeObject()
    {
      ObjectInfo profile = (ObjectInfo) objects.elementAt(objChoice.getSelectedIndex());
      Curve path;
      CoordinateSystem pathCoords;
      
      if (pathBox.getState())
        {
          ObjectInfo info = (ObjectInfo) paths.elementAt(pathChoice.getSelectedIndex());
          path = (Curve) info.getObject();
          pathCoords = info.getCoords();
        }
      else
        {
          Vec3 dir = new Vec3();
          if (xBox.getState())
            dir.x = distField.getValue();
          else if (yBox.getState())
            dir.y = distField.getValue();
          else if (zBox.getState())
            dir.z = distField.getValue();
          else
            dir.set(xField.getValue(), yField.getValue(), zField.getValue());
          Vec3 v[] = new Vec3 [(int) segField.getValue()+1];
          float smooth[] = new float [v.length];
          for (int i = 0; i < v.length; i++)
            {
              v[i] = new Vec3(dir);
              v[i].scale(i/segField.getValue());
              smooth[i] = 1.0f;
            }
          path = new Curve(v, smooth, Mesh.INTERPOLATING, false);
          pathCoords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
        }
      Object3D obj;
        /*
      if (profile.getObject() == path)
        obj = null;
      else if (profile.getObject() instanceof TriangleMesh)
        obj = extrudeMesh((TriangleMesh) profile.getObject(), path, profile.getCoords(), pathCoords, angleField.getValue()*Math.PI/180.0, orientBox.getState());
      else if (profile.getObject() instanceof Curve)
        obj = extrudeCurve((Curve) profile.getObject(), path, profile.getCoords(), pathCoords, angleField.getValue()*Math.PI/180.0, orientBox.getState());
      else
        obj = extrudeMesh(profile.getObject().convertToTriangleMesh(tolField.getValue()), path, profile.getCoords(), pathCoords, angleField.getValue()*Math.PI/180.0, orientBox.getState());
      Texture tex = window.getScene().getDefaultTexture();
      obj.setTexture(tex, tex.getDefaultMapping(obj));
      preview.setObject(obj);
      preview.repaint();
        */
    }
    
    /** Enable or disable components, based on the current selections. */
    
    private void updateComponents()
    {
      distField.setEnabled(xBox.getState() || yBox.getState() || zBox.getState());
      xField.setEnabled(vectorBox.getState());
      yField.setEnabled(vectorBox.getState());
      zField.setEnabled(vectorBox.getState());
      pathChoice.setEnabled(pathBox.getState());
      segField.setEnabled(!pathBox.getState());
      orientBox.setEnabled(pathBox.getState());
      Object3D profile = ((ObjectInfo) objects.elementAt(objChoice.getSelectedIndex())).getObject();
      tolField.setEnabled(!(profile instanceof Curve || profile instanceof TriangleMesh));
      if (pathBox.getState())
        okButton.setEnabled(objects.elementAt(objChoice.getSelectedIndex()) != paths.elementAt(pathChoice.getSelectedIndex()));
      else
        okButton.setEnabled(true);
    }

    
    private void doOk()
    {
        /*
      //ObjectInfo profile = (ObjectInfo) objects.elementAt(objChoice.getSelectedIndex());
      CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
      if (profile.getObject() instanceof Mesh)
      {
        Vec3 offset = profile.getCoords().fromLocal().times(((Mesh) profile.getObject()).getVertices()[0].r).minus(coords.fromLocal().times(((Mesh) preview.getObject().getObject()).getVertices()[0].r));
        coords.setOrigin(coords.getOrigin().plus(offset));
      }
      window.addObject(preview.getObject().getObject(), coords, "Extruded Object ", null);
      window.setSelection(window.getScene().getNumObjects()-1);
      window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(window.getScene().getNumObjects()-1)}));
      window.updateImage();
      */
        dispose();
    }
}
