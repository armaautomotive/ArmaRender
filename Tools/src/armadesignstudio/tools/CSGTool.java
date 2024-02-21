/* Copyright (C) 2021 by Jon Taylor
   Copyright (C) 2001-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armadesignstudio.tools;

import armadesignstudio.*;
import armadesignstudio.animation.*;
import armadesignstudio.object.*;
import armadesignstudio.math.*;
import armadesignstudio.ui.*;
import buoy.widget.*;
import java.util.*;

/** The CSG tool creates Constructive Solid Geometry (CSG) objects. */

public class CSGTool implements ModellingTool
{
    private static int counter = 1;

    public CSGTool()
    {
    }

    /* Get the text that appear as the menu item.*/

    public String getName()
    {
    return Translate.text("menu.boolean");
    }

    /* See whether an appropriate set of objects is selected and either display an error
     message, or bring up the extrude window. */

    public void commandSelected(LayoutWindow window)
    {
        Scene scene = window.getScene();
        int selection[] = window.getSelectedIndices(), closedCount = 0;
        Vector<ObjectInfo> inputObj = new Vector<ObjectInfo>();
          
        CSGObject existingSelectedBoolean = null;
        ObjectInfo existingSelectedBooleanOI = null;
        ObjectInfo oiToAdd = null;

        for (int i = 0; i < selection.length; i++)
        {
            ObjectInfo obj = scene.getObject(selection[i]);
            if (obj.getObject().canSetTexture())
            {
                if (obj.getObject() instanceof TriangleMesh || obj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT)
                {
                    inputObj.addElement(obj);
                    if (obj.getObject().isClosed())
                      closedCount++;

                    if( obj.getObject() instanceof CSGObject  ){            // We are adding an object to an CSGObject
                        //System.out.println("  is BOOLEAN  " );
                        existingSelectedBoolean = (CSGObject)obj.getObject();
                        existingSelectedBooleanOI = obj;
                    }
                    if(obj.getObject() instanceof CSGObject == false){
                        oiToAdd = obj;
                    }
                }
            }
        }
          
          
        if (inputObj.size() < 2 || closedCount < 1)
        {
            new BStandardDialog("", UIUtilities.breakString("You must select two objects for boolean modelling, at least one of which must be solid."), BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
            return;
        }

        // TODO: If one of the selections is an CSGObject, don't create a new one but add ad additional. (Abort this apprach)
        if(existingSelectedBooleanOI != null){
            existingSelectedBoolean.addAdditionalObject(oiToAdd, CSGObject.UNION);
            existingSelectedBoolean.setCoordinateSystem( existingSelectedBooleanOI.getCoords() );
        }
          
          
        CSGObject newobj = new CSGObject(inputObj.elementAt(0), inputObj.elementAt(1), CSGObject.UNION);
          
        Vec3 center = newobj.centerObjects();
          
        if(existingSelectedBooleanOI != null){    // if one selected object is CSG, add additional object to it, don't create a new one.
            existingSelectedBoolean.setCoordinateSystem( existingSelectedBooleanOI.getCoords() );
            newobj = existingSelectedBoolean;
            
        }
          
        CSGDialog dial = new CSGDialog(window, newobj);     // newobj<CSGObject>
        if (!dial.clickedOk())
          return;
          
        if(existingSelectedBooleanOI == null){    // if two non CSG objects merged
            ObjectInfo info = new ObjectInfo(newobj, new CoordinateSystem(center, Vec3.vz(), Vec3.vy()), "Boolean "+(counter++));
            info.addTrack(new PositionTrack(info), 0);
            info.addTrack(new RotationTrack(info), 1);
            window.addObject(info, null);
            window.setSelection(scene.getNumObjects()-1);
            window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(scene.getNumObjects()-1)}));
        }
        window.updateImage();
    }
}
