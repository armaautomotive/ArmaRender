/* Copyright (C) 2021 by Jon Taylor

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

/** Object nested in scene graph tree hiarchy to indicate objects held within will be added to the parent geometry by a union operation. */

public class Union extends Object3D
{
  double halfx, halfy, halfz;
  BoundingBox bounds;
  RenderingMesh cachedMesh;
  WireframeMesh cachedWire;

  private static final Property PROPERTIES[] = new Property [] {
    new Property("X Size", 0.0, Double.MAX_VALUE, 1.0),
      new Property("Y Size", 0.0, Double.MAX_VALUE, 1.0),
      new Property("Z Size", 0.0, Double.MAX_VALUE, 1.0)
  };

  public Union()
  {
    bounds = new BoundingBox(0, 0, 0, 0, 0, 0);
  }

  public Object3D duplicate()
  {
    Union obj = new Union();
    //obj.copyTextureAndMaterial(this);
    return obj;
  }
  
  public void copyObject(Object3D obj)
  {
      Union c = (Union) obj;
    Vec3 size = c.getBounds().getSize();
    
    //setSize(size.x, size.y, size.z);
    //UnioncopyTextureAndMaterial(obj);
    cachedMesh = null;
    cachedWire = null;
  }
  
  public BoundingBox getBounds()
  {
    return bounds;
  }

  public void setSize(double xsize, double ysize, double zsize)
  {
    halfx = xsize/2.0;
    halfy = ysize/2.0;
    halfz = zsize/2.0;
    bounds = new BoundingBox(-halfx, halfx, -halfy, halfy, -halfz, halfz);
    cachedMesh = null;
    cachedWire = null;
  }

  @Override
  public int canConvertToTriangleMesh()
  {
    return 0; // EXACTLY;
  }
  
  

  public WireframeMesh getWireframeMesh()
  {
    Vec3 vert[];
    int from[], to[];
    
    if (cachedWire != null)
      return cachedWire;
    vert = bounds.getCorners();
    from = new int [] {0, 2, 3, 1, 4, 6, 7, 5, 0, 1, 2, 3};
    to = new int [] {2, 3, 1, 0, 6, 7, 5, 4, 4, 5, 6, 7};
    return (cachedWire = new WireframeMesh(vert, from, to));
  }

  

  @Override
  public boolean isEditable()
  {
    return false;
  }
  
  /* Allow the user to edit the cube's shape. */
    /*
  @Override
  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    ValueField xField = new ValueField(2.0*halfx, ValueField.POSITIVE, 5);
    ValueField yField = new ValueField(2.0*halfy, ValueField.POSITIVE, 5);
    ValueField zField = new ValueField(2.0*halfz, ValueField.POSITIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editCubeTitle"),
      new Widget [] {xField, yField, zField}, new String [] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return;
    setSize(xField.getValue(), yField.getValue(), zField.getValue());
    cb.run();
  }
     */

  /* The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public Union(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    //halfx = in.readDouble();
    //halfy = in.readDouble();
    //halfz = in.readDouble();
    bounds = new BoundingBox(0, 0, 0, 0, 0, 0);
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(0);
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
    switch (index)
    {
      case 0:
        return new Double(2.0*halfx);
      case 1:
        return new Double(2.0*halfy);
      case 2:
        return new Double(2.0*halfz);
    }
    return null;
  }

  public void setPropertyValue(int index, Object value)
  {
    double val = ((Double) value).doubleValue();
    if (index == 0)
      setSize(val, 2.0*halfy, 2.0*halfz);
    else if (index == 1)
      setSize(2.0*halfx, val, 2.0*halfz);
    else if (index == 2)
      setSize(2.0*halfx, 2.0*halfy, val);
  }

  /* Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new VectorKeyframe(2.0*halfx, 2.0*halfy, 2.0*halfz);
  }
  
  /* Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    VectorKeyframe key = (VectorKeyframe) k;
    
    setSize(key.x, key.y, key.z);
  }
  
  /** This will be called whenever a new pose track is created for this object.  It allows
      the object to configure the track by setting its graphable values, subtracks, etc. */
  
  public void configurePoseTrack(PoseTrack track)
  {
    track.setGraphableValues(new String [] {"X Size", "Y Size", "Z Size"},
        new double [] {2.0*halfx, 2.0*halfy, 2.0*halfz}, 
        new double [][] {{0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}, {0.0, Double.MAX_VALUE}});
  }
  
  /* Allow the user to edit a keyframe returned by getPoseKeyframe(). */
  
  public void editKeyframe(EditingWindow parent, Keyframe k, ObjectInfo info)
  {
    VectorKeyframe key = (VectorKeyframe) k;
    ValueField xField = new ValueField(key.x, ValueField.POSITIVE, 5);
    ValueField yField = new ValueField(key.y, ValueField.POSITIVE, 5);
    ValueField zField = new ValueField(key.z, ValueField.POSITIVE, 5);
    ComponentsDialog dlg = new ComponentsDialog(parent.getFrame(), Translate.text("editCubeTitle"),
      new Widget [] {xField, yField, zField}, new String [] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return;
    key.set(xField.getValue(), yField.getValue(), zField.getValue());
  }
}
