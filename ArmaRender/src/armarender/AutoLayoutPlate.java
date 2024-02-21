/* Copyright (C) 2023 by Jon Taylor
 
 This program is free software; you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation; either version 2 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE.  See the GNU General Public License for more details.*/

package armarender;

import armarender.*;
import armarender.texture.*;
import armarender.animation.*;
import armarender.math.*;
import armarender.ui.*;
import buoy.widget.*;
import java.io.*;
import armarender.view.*;
import armarender.texture.*;
import java.awt.Color;
import java.util.Vector;
import armarender.object.Cylinder;
import armarender.object.TriangleMesh.*;
import armarender.object.*;

public class AutoLayoutPlate {
    
    public AutoLayoutPlate(){
            
    }
    
    /**
     * layout
     *
     * Description:
     */
    public void layout(LayoutWindow window){
        System.out.println("Note: This function is not yet implemented.");
        Scene scene = window.getScene();
        Vector<ObjectInfo> objects = scene.getObjects();
        for(int i = 0; i < objects.size(); i++){
            ObjectInfo currObject = objects.elementAt(i);
            if(currObject.getObject() instanceof Curve &&
               currObject.getObject() instanceof ArcObject == false &&
               currObject.getPhysicalMaterialId() > -1)
            {
                System.out.println("   - " + currObject.getName()  + " mat: " + currObject.getPhysicalMaterialId() );
                
                CoordinateSystem layoutCoords = currObject.getLayoutCoords();
                CoordinateSystem modelingCoords = currObject.getModelingCoords();
                
                Vec3 layoutOrigin = layoutCoords.getOrigin();
                Vec3 layoutZ = layoutCoords.getZDirection();
                Vec3 layoutUp = layoutCoords.getUpDirection();
                
                Vec3 modelingOrigin = modelingCoords.getOrigin();
                Vec3 modelingZ = modelingCoords.getZDirection();
                Vec3 modelingUp = modelingCoords.getUpDirection();
                
                double distOrigin = layoutOrigin.distance(modelingOrigin);
                double distZ = layoutZ.distance(modelingZ);
                double distUp = layoutUp.distance(modelingUp);
                
                System.out.println("distOrigin: " + distOrigin + "  distZ: " + distZ + " distUp: " + distUp  );
                
                
                /*
                if(modelingCoords.equals(layoutCoords) == false){
                    System.out.println("      - diff XXXXXX ");
                } else {
                    System.out.println("      - Same, ok set inital layout. ");
                }
                */
            }
        }
    }
    
    /**
     * initalLayout
     *
     * Description:
     */
    public void initalLayout(){
        
    }
    
    public void findFreeSpace(){
        
    }
    
    public void orientFlat(){
        
    }
    
}

