/*  Copyright (C) 2022 by Jon Taylor
 
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.object;

import armarender.*;
import armarender.animation.*;
import armarender.material.*;
import armarender.math.*;
import armarender.texture.*;
import armarender.ui.*;
import buoy.widget.*;
import java.io.*;

/** Tree folder has no geometry. */

public class Folder extends Object3D
{
  private int action = 0;
    
  private static final Property PROPERTIES[] = new Property [] {
    
  };
    
    public static int FOLDER = 0;
    public static int UNION = 1;
    public static int SUBTRACT = 2;
    public static int INTERSECTION = 3;
    

  public Folder()
  {
    
  }

  public Object3D duplicate()
  {
    Folder obj = new Folder();
    return obj;
  }
  
  public void copyObject(Object3D obj)
  {
  }
  
  public void setAction(int a){
      this.action = a;
  }
    
    public int getAction(){
        return action;
    }

  @Override
  public boolean isEditable()
  {
    return true;
  }
  
  /* Allow the user to edit the cube's shape.
   *
   * - TODO: Add scaled values
   */
  
  

  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public Folder(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version > 1)
      throw new InvalidObjectException("");
      
    if(version == 1){
        try {
            action = in.readInt();
        } catch(Exception e){
            
        }
    }
    //halfx = in.readDouble();
    //halfy = in.readDouble();
    //halfz = in.readDouble();
    //bounds = new BoundingBox(-halfx, halfx, -halfy, halfy, -halfz, halfz);
  }

    
  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(1); // version.
    out.writeInt(action);
    //out.writeDouble(halfx);
    //out.writeDouble(halfy);
    //out.writeDouble(halfz);
  }

  public Property[] getProperties()
  {
    return (Property []) PROPERTIES.clone();
  }

  public Object getPropertyValue(int index)
  {
      /*
    switch (index)
    {
      case 0:
        return new Double(2.0*halfx);
      case 1:
        return new Double(2.0*halfy);
      case 2:
        return new Double(2.0*halfz);
    }
       */
    return null;
  }

  public void setPropertyValue(int index, Object value)
  {
      /*
    double val = ((Double) value).doubleValue();
    if (index == 0)
      setSize(val, 2.0*halfy, 2.0*halfz);
    else if (index == 1)
      setSize(2.0*halfx, val, 2.0*halfz);
    else if (index == 2)
      setSize(2.0*halfx, 2.0*halfy, val);
       */
  }

    
    public Keyframe getPoseKeyframe()
    {
      return new VectorKeyframe(2.0*1, 2.0*1, 2.0*1);
    }
    
    public void applyPoseKeyframe(Keyframe k)
    {
      VectorKeyframe key = (VectorKeyframe) k;
      
      //setSize(key.x, key.y, key.z);
    }
  
    
    public WireframeMesh getWireframeMesh()
    {
      
      return null;
    }
    
    public void setSize(double xsize, double ysize, double zsize)
    {
        
    }
    
    public BoundingBox getBounds()
    {
      return null;
    }
}
