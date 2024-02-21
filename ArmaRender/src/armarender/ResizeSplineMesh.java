/* Copyright (C) 2021, 2023 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;

public class ResizeSplineMesh extends BDialog
{
    private BFrame fr;
    Scene scene;
    private LayoutWindow window;
    
    //JTextField insertRowTextField = new JTextField(5);
    //JTextField insertColumnTextField = new JTextField(5);
    private ValueField insertRowTextField, insertColumnTextField;
    
    public ResizeSplineMesh(BFrame parent, LayoutWindow window, Scene theScene)
    {
      super(parent, "Resize Mesh", true);
      
      fr = parent;
        scene = theScene;
        this.window = window;
        
        FormContainer mainContent = new FormContainer(1, 2);
        
        FormContainer content = new FormContainer(9, 10);
        //FormContainer content = new FormContainer(new double [] {1}, new double [] {1, 0, 0, 0});
        setContent(BOutline.createEmptyBorder(mainContent, UIUtilities.getStandardDialogInsets()));
        
        mainContent.add( content, 0, 0 );
        
        content.add(Translate.button("^", this, "expandTop"), 2, 0);
        content.add(Translate.button("⌄", this, "contractTop"), 2, 1);
        content.add(Translate.button("<", this, "expandLeft"), 0, 2);
        content.add(Translate.button(">", this, "contractLeft"), 1, 2);
        content.add(new BLabel(" Mesh "), 3, 2);
        content.add(Translate.button("<", this, "contractRight"), 4, 2);
        content.add(Translate.button(">", this, "expandRight"), 5, 2);
        content.add(Translate.button("^", this, "contractBottom"), 2, 4);
        content.add(Translate.button("⌄", this, "expandBottom"), 2, 5);
        
        
        content.add(insertRowTextField = new ValueField(1, ValueField.POSITIVE, 5), 1, 6);
        content.add(Translate.button("Insert Row", this, "insertRow"), 2, 6);
        content.add(insertColumnTextField = new ValueField(1, ValueField.POSITIVE, 5), 3, 6);
        content.add(Translate.button("Insert Column", this, "insertColumn"), 4, 6);
        
        //RowContainer choiceRow = new RowContainer();
        //content.add(choiceRow, 0, 1);
        
        RowContainer row = new RowContainer();
        //content.add(row, 0, 6);
        row.add(Translate.button("ok", this, "dispose"));
        row.add(Translate.button("cancel", this, "doCancel"));
        mainContent.add(row, 0, 1 );
        
        // Show the dialog.

        pack();
        UIUtilities.centerDialog(this, parent);
        setVisible(true);
    }
    
    
    /**
     * expandLeft
     *
     * Description: Add another colum to the left hand side of the mesh.
     */
    public void expandLeft(){
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                Vec3 newV[][] = new Vec3[width + 1][height];
                for(int tX = 0; tX < width + 1; tX++){
                    for(int tY = 0; tY < height; tY++){
                        // If has source
                        if(tX > 0){
                            int index = (tY * (width-0)) + (tX - 1);
                            
                            Vec3 vec = new Vec3();
                            if(index < verts.length){
                                vec = new Vec3(verts[index].r);
                            }
                            
                            newV[tX][tY] = vec;
                        } else {    // add new value
                            Vec3 closestVec = new Vec3(verts[(tY * (width-0)) + (tX)].r);
                            closestVec.x -= 0.5;    // arbitrary, later use part orientation to outset these new points.
                            newV[tX][tY] = new Vec3(closestVec.x, closestVec.y, closestVec.z);
                        }
                    }
                }
                float uSmoothness[] = new float[width + 1];
                float vSmoothness[] = new float[height];
                for(int i = 0; i < uSmoothness.length; i++){
                    uSmoothness[i] = 1;
                }
                for(int i = 0; i < vSmoothness.length; i++){
                    vSmoothness[i] = 1;
                }
                sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                scene.objectModified(sMesh);
                window.updateImage();
            }
        }
    }
    
    /**
     * contractLeft
     *
     * Description: Contract mesh by removing left side column.
     */
    public void contractLeft(){
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                if(width > 2){
                    Vec3 newV[][] = new Vec3[width - 1][height];
                    for(int tX = 0; tX < width - 1; tX++){
                        for(int tY = 0; tY < height; tY++){
                            
                            newV[tX][tY] = new Vec3(verts[(tY * width) + (tX + 1)].r);
                            
                        }
                    }
                    float uSmoothness[] = new float[width - 1];
                    float vSmoothness[] = new float[height];
                    for(int i = 0; i < uSmoothness.length; i++){
                        uSmoothness[i] = 1;
                    }
                    for(int i = 0; i < vSmoothness.length; i++){
                        vSmoothness[i] = 1;
                    }
                    sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                    scene.objectModified(sMesh);
                    window.updateImage();
                }
            }
        }
    }
    
    
    
    /**
     * expandRight
     *
     * Description: Add another colum to the right hand side of the mesh.
     */
    public void expandRight(){
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                Vec3 newV[][] = new Vec3[width + 1][height];
                for(int tX = 0; tX < width + 1; tX++){
                    for(int tY = 0; tY < height; tY++){
                        // If has source
                        if(tX < width){
                            newV[tX][tY] = new Vec3(verts[(tY * width) + tX].r);
                        } else {    // add new value
                            Vec3 closestVec = new Vec3(verts[(tY * width) + (tX-1)].r);
                            closestVec.x += 0.5;    // arbitrary, later use part orientation to outset these new points.
                            newV[tX][tY] = new Vec3(closestVec.x, closestVec.y, closestVec.z);
                        }
                    }
                }
                float uSmoothness[] = new float[width + 1];
                float vSmoothness[] = new float[height];
                for(int i = 0; i < uSmoothness.length; i++){
                    uSmoothness[i] = 1;
                }
                for(int i = 0; i < vSmoothness.length; i++){
                    vSmoothness[i] = 1;
                }
                sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                scene.objectModified(sMesh);
                window.updateImage();
            }
        }
    }
    
    /**
     * contractRight
     *
     * Description: Contract mesh by removing right side column.
     */
    public void contractRight(){
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                if(width > 2){
                    Vec3 newV[][] = new Vec3[width - 1][height];
                    for(int tX = 0; tX < width - 1; tX++){
                        for(int tY = 0; tY < height; tY++){
                            newV[tX][tY] = new Vec3(verts[(tY * width) + tX].r);
                        }
                    }
                    float uSmoothness[] = new float[width - 1];
                    float vSmoothness[] = new float[height];
                    for(int i = 0; i < uSmoothness.length; i++){
                        uSmoothness[i] = 1;
                    }
                    for(int i = 0; i < vSmoothness.length; i++){
                        vSmoothness[i] = 1;
                    }
                    sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                    scene.objectModified(sMesh);
                    window.updateImage();
                }
            }
        }
    }
    
    
    /**
     * expandBottom
     *
     * Description: Add another row to the bottom of the mesh.
     */
    public void expandBottom(){
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                Vec3 newV[][] = new Vec3[width][height + 1];
                for(int tX = 0; tX < width; tX++){
                    for(int tY = 0; tY < height + 1; tY++){
                        // If has source
                        if(tY > 0){
                            int index = ( (tY - 1) * (width)) + tX;
                            
                            Vec3 vec = new Vec3(tX,tY,0);
                            if(index < verts.length){
                                vec = new Vec3(verts[index].r);
                            }
                            
                            newV[tX][tY] = vec;
                        } else {    // y == 0, add new value
                            Vec3 closestVec = new Vec3(verts[( (tY + 0) * (width-0)) + (tX)].r);
                            closestVec.y -= 0.5;    // arbitrary, later use part orientation to outset these new points.
                            newV[tX][tY] = new Vec3(closestVec.x, closestVec.y, closestVec.z);
                        }
                    }
                }
                float uSmoothness[] = new float[width];
                float vSmoothness[] = new float[height + 1];
                for(int i = 0; i < uSmoothness.length; i++){
                    uSmoothness[i] = 1;
                }
                for(int i = 0; i < vSmoothness.length; i++){
                    vSmoothness[i] = 1;
                }
                sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                scene.objectModified(sMesh);
                window.updateImage();
            }
        }
    }
    
    /**
     * contractBottom
     *
     * Description: Contract mesh by removing lower row.
     */
    public void contractBottom(){
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                if(height > 2){
                    Vec3 newV[][] = new Vec3[width][height - 1];
                    for(int tX = 0; tX < width; tX++){
                        for(int tY = 0; tY < height - 1; tY++){
                            newV[tX][tY] = new Vec3(verts[( (tY + 1) * width) + tX].r);
                        }
                    }
                    float uSmoothness[] = new float[width];
                    float vSmoothness[] = new float[height - 1];
                    for(int i = 0; i < uSmoothness.length; i++){
                        uSmoothness[i] = 1;
                    }
                    for(int i = 0; i < vSmoothness.length; i++){
                        vSmoothness[i] = 1;
                    }
                    sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                    scene.objectModified(sMesh);
                    window.updateImage();
                }
            }
        }
    }
    
    
    /**
     * expandTop
     *
     * Description: Add another row on the top of the mesh.
     */
    public void expandTop(){
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){ // armarender.object.SplineMesh
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                Vec3 newV[][] = new Vec3[width][height + 1];
                for(int tX = 0; tX < width; tX++){
                    for(int tY = 0; tY < height + 1; tY++){
                        // If has source
                        if(tY < height){
                            int index = ( (tY - 0) * (width)) + tX;
                            
                            Vec3 vec = new Vec3(tX,tY,0);
                            if(index < verts.length){
                                vec = new Vec3(verts[index].r);
                            }
                            
                            newV[tX][tY] = vec;
                        } else {    // add new value
                            Vec3 closestVec = new Vec3(verts[( (tY - 1) * (width-0)) + (tX)].r);
                            closestVec.y += 0.5;    // arbitrary, later use part orientation to outset these new points.
                            newV[tX][tY] = new Vec3(closestVec.x, closestVec.y, closestVec.z);
                        }
                    }
                }
                float uSmoothness[] = new float[width];
                float vSmoothness[] = new float[height + 1];
                for(int i = 0; i < uSmoothness.length; i++){
                    uSmoothness[i] = 1;
                }
                for(int i = 0; i < vSmoothness.length; i++){
                    vSmoothness[i] = 1;
                }
                sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                scene.objectModified(sMesh);
                window.updateImage();
            }
        }
    }
    
    
    /**
     * contractTop
     *
     * Description: Contract mesh by removing top side row.
     */
    public void contractTop(){
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){ // armarender.object.SplineMesh
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                if(height > 2){
                    Vec3 newV[][] = new Vec3[width][height - 1];
                    for(int tX = 0; tX < width; tX++){
                        for(int tY = 0; tY < height - 1; tY++){
                            newV[tX][tY] = new Vec3(verts[( (tY) * width) + tX].r);
                        }
                    }
                    float uSmoothness[] = new float[width];
                    float vSmoothness[] = new float[height - 1];
                    for(int i = 0; i < uSmoothness.length; i++){
                        uSmoothness[i] = 1;
                    }
                    for(int i = 0; i < vSmoothness.length; i++){
                        vSmoothness[i] = 1;
                    }
                    sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                    scene.objectModified(sMesh);
                    window.updateImage();
                }
            }
        }
    }
    
    /**
     * insertColumn
     *
     * Description: Insert a column into a mesh at a given index.
     */
    public void insertColumn(){
        System.out.println("Insert Column: " + insertRowTextField.getValue());
        int insertIndex = (int)insertColumnTextField.getValue();
        if(insertIndex == 0){
            return;
        }
        double offsetDistance = 0.5;
        
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){ // armarender.object.SplineMesh
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                
                Vec3 a = verts[0].r;
                Vec3 b = verts[1].r;
                double rowWidth = a.distance(b);
                offsetDistance = rowWidth / 2;
                //System.out.println("offsetDistance " + offsetDistance);
                
                Vec3 newV[][] = new Vec3[width + 1][height];
                for(int tX = 0; tX < width + 1; tX++){
                    for(int tY = 0; tY < height; tY++){
                        //System.out.println(" x " + tX + " y " + tY);
                        if(tX == insertIndex){                                   // insert new row, use an adjacent row for values.
                            int index = (tY * (width-0)) + (tX - 1); // - 1
                            
                            Vec3 vec = new Vec3();
                            if(index < verts.length){
                                vec = new Vec3(verts[index].r);
                            }
                            vec.x += offsetDistance;
                            newV[tX][tY] = vec;
                        } else {                                                // copy origional mesh data
                            
                            Vec3 closestVec = null;
                            
                            if(tX > insertIndex){
                                closestVec = new Vec3(verts[(tY * (width-0)) + (tX - 1)].r);
                            } else {
                                closestVec = new Vec3(verts[(tY * (width-0)) + (tX)].r);
                            }
                            //closestVec.x -= 0.5;    // arbitrary, later use part orientation to outset these new points.
                            newV[tX][tY] = new Vec3(closestVec.x, closestVec.y, closestVec.z);
                        }
                    }
                }
                float uSmoothness[] = new float[width + 1];
                float vSmoothness[] = new float[height];
                for(int i = 0; i < uSmoothness.length; i++){
                    uSmoothness[i] = 1;
                }
                for(int i = 0; i < vSmoothness.length; i++){
                    vSmoothness[i] = 1;
                }
                sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                scene.objectModified(sMesh);
                window.updateImage();
            }
        }
    }
    
    /**
     * insertRow
     *
     * Description: Insert a row into a mesh at a given index.
     */
    public void insertRow(){
        System.out.println("Insert Row: " + insertColumnTextField.getValue());
        int insertIndex = (int)insertRowTextField.getValue();
        if(insertIndex == 0){
            return;
        }
        double offsetDistance = 0.5;
        
        // Get selected
        int selection [] = scene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = scene.getObject(selection[0]);
            //System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            if(co instanceof armarender.object.SplineMesh){ // armarender.object.SplineMesh
                //System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                MeshVertex[] verts = sMesh.getVertices();
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                
                Vec3 a = verts[0].r;
                Vec3 b = verts[width-0].r;
                double rowHeight = a.distance(b);
                offsetDistance = rowHeight / 2;
                
                Vec3 newV[][] = new Vec3[width][height + 1];
                for(int tX = 0; tX < width; tX++){
                    for(int tY = 0; tY < height + 1; tY++){
                        // If has source
                        if(tY == insertIndex){
                            int index = ( (tY - 1) * (width)) + tX;
                            
                            Vec3 vec = new Vec3(tX,tY,0);
                            if(index < verts.length){
                                vec = new Vec3(verts[index].r);
                            }
                            vec.y += offsetDistance;
                            newV[tX][tY] = vec;
                        } else {    // y == 0, add new value
                            Vec3 closestVec = null;
                            
                            if(tY > insertIndex){
                                closestVec = new Vec3(verts[( (tY - 1) * (width-0)) + (tX)].r);
                            } else {
                                closestVec = new Vec3(verts[( (tY + 0) * (width-0)) + (tX)].r);
                            }
                            //closestVec.y -= 0.5;    // arbitrary, later use part orientation to outset these new points.
                            newV[tX][tY] = new Vec3(closestVec.x, closestVec.y, closestVec.z);
                        }
                    }
                }
                float uSmoothness[] = new float[width];
                float vSmoothness[] = new float[height + 1];
                for(int i = 0; i < uSmoothness.length; i++){
                    uSmoothness[i] = 1;
                }
                for(int i = 0; i < vSmoothness.length; i++){
                    vSmoothness[i] = 1;
                }
                sMesh.rebuild(newV, uSmoothness, vSmoothness, sMesh.getSmoothingMethod(), sMesh.isUClosed(), sMesh.isVClosed());
                scene.objectModified(sMesh);
                window.updateImage();
            }
        }
    }
    
    private void doCancel()
    {
      dispose();
    }
}
