/* Copyright (C) 2000-2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.texture;

import armarender.*;
import armarender.animation.*;
import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** CylindricalMapping is a Mapping2D which wraps the texture around a cylinder. */

public class CylindricalMapping extends NonlinearMapping2D
{
  CoordinateSystem coords;
  double xscale, yscale, ax, ay, dy;
  Mat4 toLocal, fromLocal;
  boolean transform;
  TextureParameter xparam, yparam, zparam;

  public CylindricalMapping(Object3D obj, Texture theTexture)
  {
    super(obj, theTexture);
    coords = new CoordinateSystem(new Vec3(), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0));
    xscale = 360.0;
    yscale = 1.0;
    dy = 0.5;
    findCoefficients();
  }

  public static String getName()
  {
    return "Cylindrical";
  }

  /* Calculate the mapping coefficients. */
  
  void findCoefficients()
  {
    ax = -180.0/(Math.PI*xscale);
    ay = 1.0/yscale;
    toLocal = coords.toLocal();
    fromLocal = coords.fromLocal();
    transform = (fromLocal.m11 != 1.0 || fromLocal.m22 != 1.0 || fromLocal.m33 != 1.0);
  }

  /** Get a vector whose components contain the scale factors for the mapping. */
  
  public Vec2 getScale()
  {
    return new Vec2(xscale, yscale);
  }
  
  /** Set the scale factors for the mapping. */
  
  public void setScale(Vec2 scale)
  {
    xscale = scale.x;
    yscale = scale.y;
    findCoefficients();
  }
  
  /** Get a vector whose components contain the rotation angles for the mapping. */
  
  public Vec3 getRotations()
  {
    double angles[] = coords.getRotationAngles();
    return new Vec3(angles[0], angles[1], angles[2]);
  }
  
  /** Set the rotation angles for the mapping. */
  
  public void setRotations(Vec3 angles)
  {
    coords.setOrientation(angles.x, angles.y, angles.z);
    findCoefficients();
  }
  
  /** Get the offset distance. */
  
  public double getOffset()
  {
    return dy;
  }
  
  /** Set the offset distance. */
  
  public void setOffset(double dist)
  {
    dy = dist;
  }
  
  /* Methods from TextureMapping. */

  public RenderingTriangle mapTriangle(int v1, int v2, int v3, int n1, int n2, int n3, Vec3 vert[])
  {
    Vec3 c1 = toLocal.timesDirection(vert[v1]), c2 = toLocal.timesDirection(vert[v2]), c3 = toLocal.timesDirection(vert[v3]);
    return new Nonlinear2DTriangle(v1, v2, v3, n1, n2, n3, c1, c2, c3);
  }

  /** This method is called once the texture parameters for the vertices of a triangle
      are known. */

  public void setParameters(RenderingTriangle tri, double p1[], double p2[], double p3[], RenderingMesh mesh)
  {
    ((Nonlinear2DTriangle) tri).setParameters(p1, p2, p3, mesh);
  }

  public void getTextureSpec(Vec3 pos, TextureSpec spec, double angle, double size, double t, double param[])
  {
    double px, py, pz, x, y, z, r, s, theta;
    
    if (!appliesToFace(angle > 0.0))
      {
        spec.diffuse.setRGB(0.0f, 0.0f, 0.0f);
        spec.specular.setRGB(0.0f, 0.0f, 0.0f);
        spec.transparent.setRGB(1.0f, 1.0f, 1.0f);
        spec.emissive.setRGB(0.0f, 0.0f, 0.0f);
        spec.roughness = spec.cloudiness = 0.0;
        spec.bumpGrad.set(0.0, 0.0, 0.0);
        return;
      }
    if (coordsFromParams && numTextureParams < param.length && param[numTextureParams] != Double.MAX_VALUE)
      {
        px = param[numTextureParams];
        py = param[numTextureParams+1];
        pz = param[numTextureParams+2];
      }
    else
      {
        px = pos.x;
        py = pos.y;
        pz = pos.z;
      }
    x = toLocal.m11*px + toLocal.m12*py + toLocal.m13*pz;
    y = toLocal.m21*px + toLocal.m22*py + toLocal.m23*pz;
    z = toLocal.m31*px + toLocal.m32*py + toLocal.m33*pz;
    theta = FastMath.atan(z/x);
    if (x < 0.0)
      theta += Math.PI;
    r = Math.sqrt(x*x+z*z);
    texture.getTextureSpec(spec, theta*ax, y*ay+dy, Math.abs(size*ax/r), Math.abs(size*ay), angle, t, param);
    if (texture.hasComponent(Texture.BUMP_COMPONENT))
      {
        s = spec.bumpGrad.x*ax/r;
        spec.bumpGrad.set(-s*z, spec.bumpGrad.y*ay, s*x);
        if (transform)
          fromLocal.transform(spec.bumpGrad);
      }
  }

  public void getTransparency(Vec3 pos, RGBColor trans, double angle, double size, double t, double param[])
  {
    double px, py, pz, x, y, z, r, theta;
    
    if (!appliesToFace(angle > 0.0))
      {
        trans.setRGB(1.0f, 1.0f, 1.0f);
        return;
      }
    if (coordsFromParams && numTextureParams < param.length && param[numTextureParams] != Double.MAX_VALUE)
      {
        px = param[numTextureParams];
        py = param[numTextureParams+1];
        pz = param[numTextureParams+2];
      }
    else
      {
        px = pos.x;
        py = pos.y;
        pz = pos.z;
      }
    x = toLocal.m11*px + toLocal.m12*py + toLocal.m13*pz;
    y = toLocal.m21*px + toLocal.m22*py + toLocal.m23*pz;
    z = toLocal.m31*px + toLocal.m32*py + toLocal.m33*pz;
    theta = FastMath.atan(z/x);
    if (x < 0.0)
      theta += Math.PI;
    r = Math.sqrt(x*x+z*z);
    texture.getTransparency(trans, theta*ax, y*ay+dy, Math.abs(size*ax/r), Math.abs(size*ay), angle, t, param);
  }

  public double getDisplacement(Vec3 pos, double size, double t, double param[])
  {
    double px, py, pz, x, y, z, r, theta;
    
    if (coordsFromParams && numTextureParams < param.length && param[numTextureParams] != Double.MAX_VALUE)
      {
        px = param[numTextureParams];
        py = param[numTextureParams+1];
        pz = param[numTextureParams+2];
      }
    else
      {
        px = pos.x;
        py = pos.y;
        pz = pos.z;
      }
    x = toLocal.m11*px + toLocal.m12*py + toLocal.m13*pz;
    y = toLocal.m21*px + toLocal.m22*py + toLocal.m23*pz;
    z = toLocal.m31*px + toLocal.m32*py + toLocal.m33*pz;
    theta = FastMath.atan(z/x);
    if (x < 0.0)
      theta += Math.PI;
    r = Math.sqrt(x*x+z*z);
    return texture.getDisplacement(theta*ax, y*ay+dy, Math.abs(size*ax/r), Math.abs(size*ay), t, param);
  }

  public Mat4 getPreTransform()
  {
    return toLocal;
  }

  public void getSpecIntermed(TextureSpec spec, double x, double y, double z, double size, double angle, double t, double param[])
  {
    double r = Math.sqrt(x*x+z*z), s, theta = FastMath.atan(z/x);
    if (x < 0.0)
      theta += Math.PI;
    else if (Double.isNaN(theta))
      theta = (z > 0.0 ? 0.5*Math.PI : -0.5*Math.PI); // Deal with x == 0
    texture.getTextureSpec(spec, theta*ax, y*ay+dy, Math.abs(size*ax/r), Math.abs(size*ay), angle, t, param);
    if (texture.hasComponent(Texture.BUMP_COMPONENT))
      {
        s = spec.bumpGrad.x*ax/r;
        spec.bumpGrad.set(-s*z, spec.bumpGrad.y*ay, s*x);
        if (transform)
          fromLocal.transform(spec.bumpGrad);
      }
  }

  public void getTransIntermed(RGBColor trans, double x, double y, double z, double size, double angle, double t, double param[])
  {
    double r = Math.sqrt(x*x+z*z), theta = FastMath.atan(z/x);
    if (x < 0.0)
      theta += Math.PI;
    else if (Double.isNaN(theta))
      theta = (z > 0.0 ? 0.5*Math.PI : -0.5*Math.PI); // Deal with x == 0
    texture.getTransparency(trans, theta*ax, y*ay+dy, Math.abs(size*ax/r), Math.abs(size*ay), angle, t, param);
  }

  public double getDisplaceIntermed(double x, double y, double z, double size, double t, double param[])
  {
    double r = Math.sqrt(x*x+z*z), theta = FastMath.atan(z/x);
    if (x < 0.0)
      theta += Math.PI;
    else if (Double.isNaN(theta))
      theta = (z > 0.0 ? 0.5*Math.PI : -0.5*Math.PI); // Deal with x == 0
    return texture.getDisplacement(theta*ax, y*ay+dy, Math.abs(size*ax/r), Math.abs(size*ay), t, param);
  }
  
  /** Given a Mesh to which this mapping has been applied, return the texture coordinates at
      each vertex. */
  
  public Vec2 [] findTextureCoordinates(Mesh mesh)
  {
    TextureParameter param[] = mesh.getParameters();
    ParameterValue paramValue[] = mesh.getParameterValues();
    VertexParameterValue xval = null, yval = null, zval = null;
    if (coordsFromParams)
      for (int i = 0; i < param.length; i++)
        {
          if (param[i].equals(xparam))
            xval = (VertexParameterValue) paramValue[i];
          else if (param[i].equals(yparam))
            yval = (VertexParameterValue) paramValue[i];
          else if (param[i].equals(zparam))
            zval = (VertexParameterValue) paramValue[i];
        }
    MeshVertex vert[] = mesh.getVertices();
    Vec2 uv[] = new Vec2 [vert.length];
    for (int i = 0; i < vert.length; i++)
      {
        double x, y, z;
        if (xval == null)
          {
            x = vert[i].r.x;
            y = vert[i].r.y;
            z = vert[i].r.z;
          }
        else
          {
            x = xval.getValue()[i];
            y = yval.getValue()[i];
            z = zval.getValue()[i];
          }
        double theta = FastMath.atan(z/x);
        if (x < 0.0)
          theta += Math.PI;
        if (Double.isNaN(theta))
          theta = 0.0;
        uv[i] = new Vec2(theta*ax, y*ay+dy);
      }
    return uv;
  }

  public TextureMapping duplicate()
  {
    return duplicate(object, texture);
  }

  public TextureMapping duplicate(Object3D obj, Texture tex)
  {
    CylindricalMapping map = new CylindricalMapping(obj, tex);
    
    map.coords = coords.duplicate();
    map.dy = dy;
    map.xscale = xscale;
    map.yscale = yscale;
    map.findCoefficients();
    map.coordsFromParams = coordsFromParams;
    map.numTextureParams = numTextureParams;
    map.setAppliesTo(appliesTo());
    map.xparam = xparam;
    map.yparam = yparam;
    map.zparam = zparam;
    return map;
  }
  
  public void copy(TextureMapping mapping)
  {
    CylindricalMapping map = (CylindricalMapping) mapping; 
    
    coords = map.coords.duplicate();
    dy = map.dy;
    xscale = map.xscale;
    yscale = map.yscale;
    findCoefficients();
    coordsFromParams = map.coordsFromParams;
    numTextureParams = map.numTextureParams;
    setAppliesTo(map.appliesTo());
    xparam = map.xparam;
    yparam = map.yparam;
    zparam = map.zparam;
  }

  /* Get the list of texture parameters associated with this mapping and its texture.
     That includes the texture's parameters, and possibly parameters for the texture
     coordinates. */
  
  public TextureParameter [] getParameters()
  {
    if (!coordsFromParams)
      return getTexture().getParameters();
    TextureParameter tp[] = getTexture().getParameters();
    numTextureParams = tp.length;
    TextureParameter p[] = new TextureParameter [numTextureParams+3];
    System.arraycopy(tp, 0, p, 0, numTextureParams);
    if (xparam == null)
      {
        xparam = new TextureParameter(this, "X", -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        yparam = new TextureParameter(this, "Y", -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        zparam = new TextureParameter(this, "Z", -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        xparam.type = TextureParameter.X_COORDINATE;
        yparam.type = TextureParameter.Y_COORDINATE;
        zparam.type = TextureParameter.Z_COORDINATE;
        xparam.assignNewID();
        yparam.assignNewID();
        zparam.assignNewID();
      }
    p[numTextureParams] = xparam;
    p[numTextureParams+1] = yparam;
    p[numTextureParams+2] = zparam;
    return p;
  }

  public Widget getEditingPanel(Object3D obj, MaterialPreviewer preview)
  {
    return new Editor(obj, preview);
  }
  
  public CylindricalMapping(DataInputStream in, Object3D obj, Texture theTexture) throws IOException, InvalidObjectException
  {
    super(obj, theTexture);

    short version = in.readShort();
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    coords = new CoordinateSystem(in);
    dy = in.readDouble();
    xscale = in.readDouble();
    yscale = in.readDouble();
    findCoefficients();
    coordsFromParams = in.readBoolean();
    if (version == 1)
      setAppliesTo(in.readShort());
  }
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeShort(1);
    coords.writeToFile(out);
    out.writeDouble(dy);
    out.writeDouble(xscale);
    out.writeDouble(yscale);
    out.writeBoolean(coordsFromParams);
    out.writeShort(appliesTo());
  }

  /* Editor is an inner class for editing the mapping. */

  class Editor extends FormContainer
  {
    ValueField xrotField, yrotField, zrotField, xscaleField, yscaleField, ytransField;
    BCheckBox coordsFromParamsBox;
    BComboBox applyToChoice;
    Object3D theObject;
    MaterialPreviewer preview;

    public Editor(Object3D obj, MaterialPreviewer preview)
    {
      super(3, 8);
      theObject = obj;
      this.preview = preview;
      
      // Add the various components to the Panel.
      
      LayoutInfo leftLayout = new LayoutInfo(LayoutInfo.EAST, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null);
      LayoutInfo rightLayout = new LayoutInfo(LayoutInfo.WEST, LayoutInfo.NONE, new Insets(0, 5, 0, 0), null);
      add(Translate.label("Width"), 0, 0, leftLayout);
      add(xscaleField = new ValueField(xscale, ValueField.NONZERO, 5), 1, 0);
      add(new BLabel("("+Translate.text("degrees")+")"), 2, 0, rightLayout);
      add(Translate.label("Height"), 0, 1, leftLayout);
      add(yscaleField = new ValueField(yscale, ValueField.NONZERO, 5), 1, 1);
      add(Translate.label("Offset"), 0, 2, leftLayout);
      add(ytransField = new ValueField(dy, ValueField.NONZERO, 5), 1, 2);
      add(new BLabel(Translate.text("Rotation")+":"), 0, 3, 3, 1);
      double angles[] = coords.getRotationAngles();
      RowContainer rotationRow = new RowContainer();
      rotationRow.add(new BLabel("X"));
      rotationRow.add(xrotField = new ValueField(angles[0], ValueField.NONE, 5));
      rotationRow.add(new BLabel("Y"));
      rotationRow.add(yrotField = new ValueField(angles[1], ValueField.NONE, 5));
      rotationRow.add(new BLabel("Z"));
      rotationRow.add(zrotField = new ValueField(angles[2], ValueField.NONE, 5));
      add(rotationRow, 0, 4, 3, 1);
      RowContainer applyRow = new RowContainer();
      applyRow.add(new BLabel(Translate.text("applyTo")+":"));
      applyRow.add(applyToChoice = new BComboBox(new String [] {
        Translate.text("frontAndBackFaces"),
        Translate.text("frontFacesOnly"),
        Translate.text("backFacesOnly")
      }));
      add(applyRow, 0, 5, 3, 1);
      applyToChoice.setSelectedIndex(appliesTo());
      add(coordsFromParamsBox = new BCheckBox(Translate.text("bindTexToSurface"), coordsFromParams), 0, 7, 3, 1);
      coordsFromParamsBox.setEnabled(theObject instanceof Mesh || theObject instanceof Actor);
      xscaleField.addEventLink(ValueChangedEvent.class, this);
      yscaleField.addEventLink(ValueChangedEvent.class, this);
      ytransField.addEventLink(ValueChangedEvent.class, this);
      xrotField.addEventLink(ValueChangedEvent.class, this);
      yrotField.addEventLink(ValueChangedEvent.class, this);
      zrotField.addEventLink(ValueChangedEvent.class, this);
      coordsFromParamsBox.addEventLink(ValueChangedEvent.class, this);
      applyToChoice.addEventLink(ValueChangedEvent.class, this);
    }

    private void processEvent()
    {
      xscale = xscaleField.getValue();
      yscale = yscaleField.getValue();
      dy = ytransField.getValue();
      coords.setOrientation(xrotField.getValue(), yrotField.getValue(), zrotField.getValue());
      findCoefficients();
      coordsFromParams = coordsFromParamsBox.getState();
      setAppliesTo((short) applyToChoice.getSelectedIndex());
      preview.setTexture(getTexture(), CylindricalMapping.this);
      preview.render();
    }
  }
}
