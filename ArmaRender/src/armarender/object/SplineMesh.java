/* Copyright (C) 2021-2023 Jon Taylor
   Copyright (C) 1999-2012 by Peter Eastman

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
import java.lang.ref.*;
import java.util.*;
import java.awt.*; // Color

/** The SplineMesh class represents a parametric surface defined as a tensor product of
    spline curves.  Depending on the selected smoothing method, the surface may either 
    interpolate or approximate the vertices of the control mesh. */

/**
 * TODO: Draw x, y, start and end for orientation indicators for modifying mesh.
 */

public class SplineMesh extends Object3D implements Mesh
{  
  MeshVertex vertex[];
  Skeleton skeleton;
  boolean uclosed, vclosed;
  BoundingBox bounds;
  int usize, vsize, cachedUSize, cachedVSize, smoothingMethod;
  float usmoothness[], vsmoothness[];
  SoftReference<RenderingMesh> cachedMesh;
  SoftReference<WireframeMesh> cachedWire;

  private static final int MAX_SUBDIVISIONS = 20;
  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("menu.smoothingMethod"), new Object[] {
      Translate.text("menu.interpolating"), Translate.text("menu.approximating")
    }, Translate.text("menu.shading")),
    new Property(Translate.text("menu.closed"), new Object[] {
      Translate.text("menu.udirection"), Translate.text("menu.vdirection"), Translate.text("menu.both"), Translate.text("menu.neither")
    }, Translate.text("menu.neither"))
  };

  /** v is an array containing the points of the control mesh, with the first index 
  corresponding to the U direction, and the second to the V direction.  The two smoothness
  arrays give the smoothness values along the U and V directions respectively. */

  public SplineMesh(Vec3 v[][], float usmoothness[], float vsmoothness[], int smoothingMethod, boolean uclosed, boolean vclosed)
  {
      //System.out.println("SplineMesh");
    this.smoothingMethod = smoothingMethod;
    this.uclosed = uclosed;
    this.vclosed = vclosed;
    setSkeleton(new Skeleton());
    MeshVertex vert[][] = new MeshVertex [v.length][v[0].length];
    for (int i = 0; i < v.length; i++){                             // 
        for (int j = 0; j < v[0].length; j++){
            vert[i][j] = new MeshVertex(v[i][j]);
        }
    }
    setShape(vert, usmoothness, vsmoothness);
  }
  
  protected SplineMesh()
  {
      //System.out.println("SplineMesh");
  }

  public Object3D duplicate()
  {
    SplineMesh mesh = new SplineMesh();
    mesh.copyObject(this);
    return mesh;
  }

  public void copyObject(Object3D obj)
  {
    SplineMesh mesh = (SplineMesh) obj;
    
    texParam = null;
    vertex = new MeshVertex [mesh.vertex.length];
    for (int i = 0; i < mesh.vertex.length; i++)
      vertex[i] = new MeshVertex(mesh.vertex[i]);
    usmoothness = new float [mesh.usize];
    for (int i = 0; i < mesh.usize; i++)
      usmoothness[i] = mesh.usmoothness[i];
    vsmoothness = new float [mesh.vsize];
    for (int i = 0; i < mesh.vsize; i++)
      vsmoothness[i] = mesh.vsmoothness[i];
    setSmoothingMethod(mesh.getSmoothingMethod());
    if (skeleton == null)
      skeleton = mesh.skeleton.duplicate();
    else
      skeleton.copy(mesh.skeleton);
    usize = mesh.usize;
    vsize = mesh.vsize;
    uclosed = mesh.uclosed;
    vclosed = mesh.vclosed;
    copyTextureAndMaterial(obj);
  }
    
    
    public void rebuild(Vec3 v[][], float usmoothness[], float vsmoothness[], int smoothingMethod, boolean uclosed, boolean vclosed){
        this.smoothingMethod = smoothingMethod;
        this.uclosed = uclosed;
        this.vclosed = vclosed;
        setSkeleton(new Skeleton());
        MeshVertex vert[][] = new MeshVertex [v.length][v[0].length];
        for (int i = 0; i < v.length; i++){                             //
            for (int j = 0; j < v[0].length; j++){
                vert[i][j] = new MeshVertex(v[i][j]);
            }
        }
        setShape(vert, usmoothness, vsmoothness);
        
        // ***
        //clearCachedMeshes();
        cachedMesh = null;
        cachedWire = null;
        bounds = null;
    }
    
    // DEPRICATE
    public void expandRight(){
        int width = getUSize();
        int height = getVSize();
        int currSize = vertex.length;
        int newWidth = width + 1;
        int newHeight = height;
        int newSize = (newWidth * newHeight);
        
        // Load points to new struct.
        MeshVertex [] newVertex = new MeshVertex[newSize];
        
        for(int i = 0; i < newVertex.length; i++){
            //int oldU =
            //int oldV =
            //MeshVertex v = mesh.vertex[  ];
        }
        
        // recalucate normals?
    }
    
    /**
     * getObjectInfo
     *
     * Description: Calculate area of SplineMesh surface.
     */
    public String getObjectInfo(double scale){
        String info = "";
        Vec3 [] verts = this.getVertexPositions();
        int width = getUSize();
        int height = getVSize();
        double area = 0;
        
        // Calculate area of each cell.
        // Iterate Cells.
        for(int w = 1; w < width; w++){
            for(int h = 1; h < height; h++){
                int aIndex = (w - 1) + ( (h - 1) * width ); // TOP LEFT
                int bIndex = (w - 0) + ( (h - 1) * width ); // TOP RIGHT
                int cIndex = (w - 1) + ( (h - 0) * width ); // BOTTOM LEFT
                int dIndex = (w - 0) + ( (h - 0) * width ); // BOTTOM Right
                Vec3 vecA = verts[aIndex];
                Vec3 vecB = verts[bIndex];
                Vec3 vecC = verts[cIndex];
                Vec3 vecD = verts[dIndex];
                
                double abLen = vecA.distance(vecB) * scale;
                double bcLen = vecB.distance(vecC) * scale;
                double caLen = vecC.distance(vecA) * scale;
                double areaFirst = calculateArea(abLen, bcLen, caLen);
                
                double cbLen = vecC.distance(vecB) * scale;
                double bdLen = vecB.distance(vecD) * scale;
                double dcLen = vecD.distance(vecC) * scale;
                double areaSecond = calculateArea(cbLen, bdLen, dcLen);
                
                System.out.println(" w: "+ (w-1) +" h: " + (h-1) + " aIndex: " + aIndex + "  l: " + verts.length + " area " + areaFirst + " : " + areaSecond);
                area += areaFirst;
                area += areaSecond;
                
                //System.out.println("     a " + vecA + " b " + vecB + " c " + vecC + " d " + vecD    );
            }
        }
        
        info += "Area: " + area + "\n";
        
        return info;
    }
    
    /**
     * Calculates area of a triangle using length of its 3 sides
     */
    private double calculateArea(double a, double b, double c) {
        double p = (a+b+c)/2;
        return Math.sqrt(p*(p-a)*(p-b)*(p-c));
    }
    
    
    /**
     * drawEditObject
     *
     * Description: If enabled, Draw vertex markers to allow for modification from main view.
     * JDT work in progress. Allow editing verticies from the main canvas.
     */
    public void drawEditObject(ViewerCanvas canvas){
        
        Camera theCamera = canvas.getCamera();
        MeshVertex v[] = ((Mesh) this).getVertices();
        Color col = new Color(0, 255, 0);
        Color green = new Color(23, 200, 6);        // Green
        Color selected_col = new Color(180, 80, 80);
        Color unselected_col = new Color(0, 255, 0);
        
        Color bottomColor = new Color(138,43,226); // purple 138,43,226
        Color leftColor = new Color(255, 160, 0);  // Orange
        
        col = unselected_col;
        
        int HANDLE_SIZE = 5;
        boolean isSelected = false;
        int sel[];
        if(canvas == null){
            System.out.println(" *** CANVAS NULL *** ");
        }
        if(canvas.getScene() == null){
            System.out.println(" *** CANVAS SCENE NULL *** ");
            return;
        }
        sel = canvas.getScene().getSelection(); // check if no selection?
        for (int i = 0; i < sel.length; i++)
        {
            ObjectInfo info = canvas.getScene().getObject(sel[i]);
            //System.out.println("    info.getObject() : " + info.getObject() + " = " + this );
            if(info.getObject() == this){
                isSelected = true;
                
                //System.out.println(" SPLINE MESH selected ");
            }
        }
        for (int i = 0; i < v.length; i++){
            //if (selected[i] && theCamera.getObjectToView().timesZ(v[i].r) > theCamera.getClipDistance())
            //{
            if(isSelected){
                //System.out.println(" SPLINE MESH ");
                
                Vec2 p = theCamera.getObjectToScreen().timesXY(v[i].r);
                double z = theCamera.getObjectToView().timesZ(v[i].r);
                
                if( i < usize){
                    col = bottomColor;
                } else if( i % usize == 0  ){
                    col = leftColor;
                } else {
                    col = green;
                }
                
                canvas.renderBox(((int) p.x) - HANDLE_SIZE/2, ((int) p.y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, col);
                
                
                //System.out.println(" draw " + i + "  u " + usize + " v: " + vsize);
            }
        }
    }

    /**
     * drawFirstPoint
     *
     * Description: Draw the first point distinctivly. Some cases it is important to have orientation to mesh geometry.
     */
    public void drawFirstPoint(ViewerCanvas canvas){
        Camera theCamera = canvas.getCamera();
        MeshVertex v[] = ((Mesh) this).getVertices();
        Color col = new Color(0, 100, 100);
        boolean isSelected = false;
        int sel[];
        if(canvas.getScene() == null){
            return;
        }
        sel = canvas.getScene().getSelection();
        for (int i = 0; i < sel.length; i++)
        {
            ObjectInfo info = canvas.getScene().getObject(sel[i]);
            if(info.getObject() == this){
                isSelected = true;
            }
        }
        int HANDLE_SIZE = 5;
        if (isSelected && v.length > 1){
            Vec2 p = theCamera.getObjectToScreen().timesXY(v[0].r);
            double z = theCamera.getObjectToView().timesZ(v[0].r);
            canvas.renderBox(((int) p.x) - HANDLE_SIZE/2, ((int) p.y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, col);
        }
    }
    
    
  /** Calculate the (approximate) bounding box for the mesh. */

  void findBounds()
  {
    double minx, miny, minz, maxx, maxy, maxz;
    Vec3 vert[] = null;
    int i;

    if (cachedMesh != null)
    {
      RenderingMesh cached = cachedMesh.get();
      if (cached != null)
        vert = cached.vert;
    }
    if (vert == null && cachedWire != null)
    {
      WireframeMesh cached = cachedWire.get();
      if (cached != null)
        vert = cached.vert;
      vert = cached.vert;
    }
    if (vert == null)
      vert = getWireframeMesh().vert;

    minx = maxx = vert[0].x;
    miny = maxy = vert[0].y;
    minz = maxz = vert[0].z;
    for (i = 1; i < vert.length; i++)
      {
        if (vert[i].x < minx) minx = vert[i].x;
        if (vert[i].x > maxx) maxx = vert[i].x;
        if (vert[i].y < miny) miny = vert[i].y;
        if (vert[i].y > maxy) maxy = vert[i].y;
        if (vert[i].z < minz) minz = vert[i].z;
        if (vert[i].z > maxz) maxz = vert[i].z;
      }
    bounds = new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
  }

  /** Get the bounding box for the mesh.  This is always the bounding box for the unsmoothed
     control mesh.  If the smoothing method is set to approximating, the final surface may not
     actually touch the sides of this box.  If the smoothing method is set to interpolating,
     the final surface may actually extend outside this box. */

  public BoundingBox getBounds()
  {
    if (bounds == null)
      findBounds();
    return bounds;
  }

  /** Return the list of vertices for the mesh. */

  public MeshVertex[] getVertices()
  {
    return vertex;
  }
  
  /** Get a single vertex.
   *
   * Description:
   */
  public final MeshVertex getVertex(int u, int v)
  {
    return vertex[u+usize*v];
  }
  
  /** Get the size of the mesh in the U direction. */
  
  public final int getUSize()
  {
    return usize;
  }
  
  /** Get the size of the mesh in the V direction. */
  
  public final int getVSize()
  {
    return vsize;
  }
  
  /** Get the smoothing method being used for this mesh. */

  public int getSmoothingMethod()
  {
    return smoothingMethod;
  }
  
  /** Get the array of U smoothness values. */

  public float [] getUSmoothness()
  {
    return usmoothness;
  }
  
  /** Get the array of V smoothness values. */

  public float [] getVSmoothness()
  {
    return vsmoothness;
  }
  
  /** Get a list of the positions of all vertices which define the mesh. */
  
  public Vec3 [] getVertexPositions()
  {
    Vec3 v[] = new Vec3 [vertex.length];
    for (int i = 0; i < v.length; i++)
      v[i] = new Vec3(vertex[i].r);
    return v;
  }

  /** Set the positions for all the vertices of the mesh. */

  public void setVertexPositions(Vec3 v[])
  {
      if(vertex.length != v.length){
          System.out.println("SplineMesh.setVertexPositions() Error: Vec3 size not matching. ");
          System.out.println("curr vertex.length: " + vertex.length);
          System.out.println("curr size usize " + usize + " " + vsize);
          System.out.println("param v.length: " + v.length);
          //System.out.println("This can happen when multiple mirrored spline mesh objects share the same name.");
          return;
      }
    for (int i = 0; i < v.length; i++)
      vertex[i].r = v[i];
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }
  
  /** Set the smoothing method. */

  public void setSmoothingMethod(int method)
  {
    smoothingMethod = method;
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }
  
  /** Set the smoothness values. */
  
  public void setSmoothness(float usmoothness[], float vsmoothness[])
  {
    this.usmoothness = usmoothness;
    this.vsmoothness = vsmoothness;
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }
  
  /** This method rebuilds the mesh based on new lists of vertices and smoothness values. */
  
  public void setShape(MeshVertex v[][], float usmoothness[], float vsmoothness[])
  {
    int i, j;
    
    usize = v.length;
    vsize = v[0].length;
    vertex = new MeshVertex [usize*vsize];
    for (i = 0; i < usize; i++)
      for (j = 0; j < vsize; j++)
        vertex[i+usize*j] = v[i][j];
    this.usmoothness = usmoothness;
    this.vsmoothness = vsmoothness;
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }
  
  /** Determine whether this mesh is closed in the U direction. */
  
  public boolean isUClosed()
  {
    return uclosed;
  }
  
  /** Determine whether this mesh is closed in the V direction. */
  
  public boolean isVClosed()
  {
    return vclosed;
  }
  
  /** Determine whether this mesh is completely closed. */
  
  public boolean isClosed()
  {
    if (!vclosed)
      {
        Vec3 v1 = vertex[0].r, v2 = vertex[usize*(vsize-1)].r;
        for (int i = 1; i < usize; i++)
          if (v1.distance2(vertex[i].r) > 1e-24 || v2.distance2(vertex[i+usize*(vsize-1)].r) > 1e-24)
            return false;
      }
    if (!uclosed)
      {
        Vec3 v1 = vertex[0].r, v2 = vertex[usize-1].r;
        for (int i = 1; i < vsize; i++)
          if (v1.distance2(vertex[i*usize].r) > 1e-24 || v2.distance2(vertex[i*usize+usize-1].r) > 1e-24)
            return false;
      }
    return true;
  }
  
  /** Set whether this mesh is closed in each direction. */
  
  public void setClosed(boolean u, boolean v)
  {
    uclosed = u;
    vclosed = v;
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }
  
  /** Set the size of the mesh.
   *
   * Description: scale mesh size
   */

  public void setSize(double xsize, double ysize, double zsize)
  {
    Vec3 size = getBounds().getSize();
    double xscale, yscale, zscale;
    
    if (size.x == 0.0)
      xscale = 1.0;
    else
      xscale = xsize / size.x;
    if (size.y == 0.0)
      yscale = 1.0;
    else
      yscale = ysize / size.y;
    if (size.z == 0.0)
      zscale = 1.0;
    else
      zscale = zsize / size.z;
    for (int i = 0; i < vertex.length; i++)
      {
        vertex[i].r.x *= xscale;
        vertex[i].r.y *= yscale;
        vertex[i].r.z *= zscale;
      }
    skeleton.scale(xscale, yscale, zscale);
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }

  public boolean isEditable()
  {
    return true;
  }

  public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    SplineMeshEditorWindow ed = new SplineMeshEditorWindow(parent, "Spline Mesh '"+ info.getName() +"'", info, cb, true);
    ed.setVisible(true);
      
      // Get camera coordinates.
      if(parent instanceof LayoutWindow){
          LayoutWindow layout = (LayoutWindow)parent;
          int currentViewIndex = layout.getCurrentView();
          CoordinateSystem viewCS = layout.getCurrentViewCoords();
          //System.out.println("viewCS " + viewCS.getOrigin() + "  - " +  viewCS.getZDirection()  );
          double scale = layout.getCurrentViewScale();    // Get view scale/zoom.
          boolean perspective = layout.isCurrentViewPerspective();
          
          ed.setCamera(viewCS, scale, perspective);
      }
  }

  public void editGesture(final EditingWindow parent, ObjectInfo info, Runnable cb, ObjectInfo realObject)
  {
    SplineMeshEditorWindow ed = new SplineMeshEditorWindow(parent, "Gesture '"+ info.getName() +"'", info, cb, false);
    ViewerCanvas views[] = ed.getAllViews();
    for (int i = 0; i < views.length; i++)
      ((MeshViewer) views[i]).setScene(parent.getScene(), realObject);
    ed.setVisible(true);
  }
  
  /** Get a MeshViewer which can be used for viewing this mesh. */
  
  public MeshViewer createMeshViewer(MeshEditController controller, RowContainer options)
  {
    return new SplineMeshViewer(controller, options);
  }
  
  /** Subdivide a spline mesh to a desired tolerance. */
    public static SplineMesh subdivideMesh(SplineMesh mesh, double tol)
    {
        //System.out.println("subdivideMesh no info");
        return subdivideMesh(mesh, tol, null);
    }
    
  public static SplineMesh subdivideMesh(SplineMesh mesh, double tol, ObjectInfo info)
  {
      //System.out.println("subdivideMesh");
      
    SplineMesh newmesh = new SplineMesh();
    int usize = mesh.usize, vsize = mesh.vsize;
    MeshVertex v[][] = new MeshVertex [vsize][usize], newv[][];
    int numParam = (mesh.texParam == null ? 0 : mesh.texParam.length);
    double param[][][] = new double [vsize][usize][numParam], newparam[][][];
    float newus[];
    Object output[];
    
    for (int i = 0; i < usize; i++)
      for (int j = 0; j < vsize; j++)
        v[j][i] = new MeshVertex(mesh.vertex[i+usize*j]);
    for (int k = 0; k < numParam; k++)
      if (mesh.paramValue[k] instanceof VertexParameterValue)
      {
        double val[] = ((VertexParameterValue) mesh.paramValue[k]).getValue();
        for (int i = 0; i < usize; i++)
          for (int j = 0; j < vsize; j++)
            param[j][i][k] = val[i+usize*j];
      }

    // First subdivide along the u direction.
    
    if (usize == 2)
      output = new Object [] {v, mesh.usmoothness, param};
    else if (mesh.smoothingMethod == INTERPOLATING)
      output = interpOneAxis(v, mesh.usmoothness, param, mesh.uclosed, tol);
    else
      output = approxOneAxis(v, mesh.usmoothness, param, mesh.uclosed, tol, info);
    
    newv = (MeshVertex [][]) output[0];
    newus = (float []) output[1];
    newparam = (double [][][]) output[2];
    
    // Now transpose the matrix, and subdivide along the v direction.
    
    v = new MeshVertex [newv[0].length][newv.length];
    for (int i = 0; i < newv.length; i++)
      for (int j = 0; j < newv[0].length; j++)
        v[j][i] = newv[i][j];
    param = new double [newparam[0].length][newparam.length][newparam[0][0].length];
    for (int i = 0; i < newparam.length; i++)
      for (int j = 0; j < newparam[0].length; j++)
        for (int k = 0; k < newparam[0][0].length; k++)
          param[j][i][k] = newparam[i][j][k];
    if (vsize == 2)
      output = new Object [] {v, mesh.vsmoothness, param};
    else if (mesh.smoothingMethod == INTERPOLATING)
      output = interpOneAxis(v, mesh.vsmoothness, param, mesh.vclosed, tol);
    else
      output = approxOneAxis(v, mesh.vsmoothness, param, mesh.vclosed, tol, info);
    
    // Set all the fields of the mesh.
    
    v = (MeshVertex [][]) output[0];
    newmesh.usize = v.length;
    newmesh.vsize = v[0].length;
    newmesh.vertex = new MeshVertex [newmesh.usize*newmesh.vsize];
    for (int i = 0; i < newmesh.usize; i++)
      for (int j = 0; j < newmesh.vsize; j++)
        newmesh.vertex[i+newmesh.usize*j] = v[i][j];
    newmesh.usmoothness = newus;
    newmesh.vsmoothness = (float []) output[1];
    newmesh.uclosed = mesh.uclosed;
    newmesh.vclosed = mesh.vclosed;
    newmesh.smoothingMethod = mesh.smoothingMethod;
    newmesh.skeleton = mesh.skeleton.duplicate();
    newmesh.copyTextureAndMaterial(mesh);
    param = (double [][][]) output[2];
    for (int k = 0; k < numParam; k++)
      if (newmesh.paramValue[k] instanceof VertexParameterValue)
      {
        double val[] = new double [newmesh.usize*newmesh.vsize];
        for (int i = 0; i < newmesh.usize; i++)
          for (int j = 0; j < newmesh.vsize; j++)
            val[i+newmesh.usize*j] = param[i][j][k];
        newmesh.paramValue[k] = new VertexParameterValue(val);
      }
    return newmesh;
  }
  
  /** This is called by subdivideMesh().  It takes a matrix of points and performs interpolating
     subdivision along the second dimension.  It returns an array containing three elements:
     the subdivided matrix, the new list of smoothness values, and the new list of parameter values. */

  private static Object [] interpOneAxis(MeshVertex v[][], float s[], double param[][][], boolean closed, double tol)
  {
      System.out.println("interpOneAxis");
    boolean refine[], newrefine[];
    float news[];
    int numParam = param[0][0].length;
    double paramTemp[] = new double [numParam], newparam[][][];
    Vec3 temp;
    MeshVertex newv[][];
    int i, j, k, count, p1, p3, p4;
    double tol2 = tol*tol;
    
    if (closed)
      refine = new boolean [v[0].length];
    else
      refine = new boolean [v[0].length-1];
    for (i = 0; i < refine.length; i++)
      refine[i] = true;
    count = refine.length;
    int iterations = 0;
    do
    {
      newrefine = new boolean [refine.length+count];
      newv = new MeshVertex [v.length][v[0].length+count];
      news = new float [v[0].length+count];
      newparam = new double [v.length][v[0].length+count][numParam];
      for (i = 0, k = 0; i < refine.length; i++)
        {          
          // Existing points remain unchanged.
          
          for (j = 0; j < v.length; j++)
          {
            newv[j][k] = v[j][i];
            newparam[j][k] = param[j][i];
          }
          news[k] = Math.min(s[i]*2.0f, 1.0f);
          
          // Now calculate positions for the new points.
          
          k++;
          if (refine[i])
            {
              p1 = i-1;
              if (p1 < 0)
                {
                  if (closed)
                    p1 = v[0].length-1;
                  else
                    p1 = 0;
                }
              p3 = i+1;
              if (p3 == v[0].length)
                {
                  if (closed)
                    p3 = 0;
                  else
                    p3 = v[0].length-1;
                }
              p4 = i+2;
              if (p4 >= v[0].length)
                {
                  if (closed)
                    p4 %= v[0].length;
                  else
                    p4 = v[0].length-1;
                }
              for (j = 0; j < v.length; j++)
                {
                  newv[j][k] = calcInterpPoint(v[j], s, param[j], paramTemp, p1, i, p3, p4);
                  for (int m = 0; m < numParam; m++)
                    newparam[j][k][m] = paramTemp[m];
                  if (v[j][i].r.distance2(newv[j][k].r) > tol2 && v[j][p3].r.distance2(newv[j][k].r) > tol2)
                    {
                      temp = v[j][i].r.plus(v[j][p3].r).times(0.5);
                      if (temp.distance2(newv[j][k].r) > tol2)
                        newrefine[k] = newrefine[(k-1+newrefine.length)%newrefine.length] = true;
                    }
                }
              news[k] = 1.0f;
              k++;
            }
        }
      if (!closed)
        for (j = 0; j < v.length; j++)
        {
          newv[j][k] = v[j][i];
          newparam[j][k] = param[j][i];
        }
      
      // Count the number of rows which are not yet converged.
      
      count = 0;
      for (j = 0; j < newrefine.length; j++)
        if (newrefine[j])
          count++;
      v = newv;
      s = news;
      param = newparam;
      refine = newrefine;
    } while (count > 0 && ++iterations < MAX_SUBDIVISIONS);
      
    //System.out.println(" interpOneAxis cuount: " + count );
      
    return new Object [] {v, s, param};
  }

  /** This is called by subdivideMesh().  It takes a matrix of points and performs approximating
     subdivision along the second dimension.  It returns an array containing three elements:
     the subdivided matrix, the new list of smoothness values, and the new list of parameter values. */

  private static Object [] approxOneAxis(MeshVertex v[][], float s[], double param[][][], boolean closed, double tol, ObjectInfo info)
  {
      //System.out.println("approxOneAxis");
      LayoutModeling layout = new LayoutModeling();
    boolean refine[], newrefine[];
    float news[];
    int numParam = param[0][0].length;
    double paramTemp[] = new double [numParam], newparam[][][];
    Vec3 temp;
    MeshVertex newv[][];
    int i, j, k, count, p1, p3;
    
    refine = new boolean [v[0].length];
    for (i = 0; i < refine.length; i++)
      refine[i] = true;
    if (closed)
      count = refine.length;
    else
      {
        count = refine.length-1;
        refine[0] = refine[refine.length-1] = false;
      }
      
      
      
      //
      // Calculate angle between points
      // This is used as an example of getting the angle between edge segments.
      // Edges with an angle above 15 degrees we want to record and modify the subdivided mesh.
      //
      if(info != null){
          CoordinateSystem c = null;
          Mat4 mat4 = null;
          c = layout.getCoords(info);
          mat4 = c.duplicate().fromLocal();
          
          // If angle between prev and next point is too high don't refine.
          //System.out.println(" i " + i + " refine[i] " +  refine[i] + " refine[p3] " + refine[p3] + "  v[0].length: " + v[0].length );
          for (int ii = 0; ii < refine.length; ii++){
              for (int jj = 1; jj < v.length - 1 && (ii == 0 || ii == refine.length -1) ; jj++){
                  MeshVertex mv = v[jj][ii];
                  Vec3 vec = new Vec3(mv.r);
                  mat4.transform(vec);
                  
                  MeshVertex mvPrev = v[jj - 1][ii];
                  Vec3 vecPrev = new Vec3(mvPrev.r);
                  mat4.transform(vecPrev);
                  
                  MeshVertex mvNext = v[jj + 1][ii];
                  Vec3 vecNext = new Vec3(mvNext.r);
                  mat4.transform(vecNext);
                  
                  vecPrev.subtract(vec);
                  vecNext.subtract(vec);
                  
                  double angle = vecPrev.getAngle(vecNext);
                  angle = Math.toDegrees(angle);
                  angle = 180 - angle;
                  
                  if(angle > 15){
                      //System.out.println("         *** " + refine[i] +  " " + newrefine[i]);
                      //ex[ j ] = true;   // i: 0 j: 2
                      System.out.println("       angle: " + angle + " i: " + ii + " j: " + jj  + " > 15 degrees  ");
                  }
                  //System.out.println("        vec " + vec  + "     angle: " + angle);
              }
          }
      }
      
      
      
    int iterations = 0;
    do
    {
      newrefine = new boolean [refine.length+count];
      newv = new MeshVertex [v.length][v[0].length+count];
      news = new float [v[0].length+count];
      newparam = new double [v.length][v[0].length+count][numParam];
      for (i = 0, k = 0; i < refine.length; i++)
        {
          p1 = i-1;
          if (p1 < 0)
            {
              if (closed)
                p1 = refine.length-1;
              else
                p1 = 0;
            }
          p3 = i+1;
          if (p3 == refine.length)
            {
              if (closed)
                p3 = 0;
              else
                p3 = refine.length-1;
            }
          
          // Calculate the new positions for existing points.
          
          if (!refine[i])
            for (j = 0; j < v.length; j++)
            {
              newv[j][k] = v[j][i];
              newparam[j][k] = param[j][i];
            }
          else
            for (j = 0; j < v.length; j++)
              {
                newv[j][k] = calcApproxPoint(v[j], s, param[j], paramTemp, p1, i, p3);
                for (int m = 0; m < numParam; m++)
                  newparam[j][k][m] = paramTemp[m];
                temp = newv[j][k].r.minus(v[j][i].r);
                if (temp.length2() > tol*tol)
                  newrefine[k] = newrefine[(k-1+newrefine.length)%newrefine.length] = newrefine[(k+1)%newrefine.length] = true;
              }
          news[k] = Math.min(s[i]*2.0f, 1.0f); // bounds error
          if (!closed && i == refine.length-1)
            break;
          
          // Now calculate positions for the new points.
          
          k++;
          if (refine[i] || refine[p3])
            {
              for (j = 0; j < v.length; j++)
              {
                  //if( (i == 2 && j == 0 && iterations == 0) == false ){ // experimenting... hard coded
                  if( true ){
                  
                    newv[j][k] = MeshVertex.blend(v[j][i], v[j][p3], 0.5, 0.5);
                    for (int m = 0; m < numParam; m++)
                      newparam[j][k][m] = 0.5*(param[j][i][m]+param[j][p3][m]);
                      
                  } else {
                      
                      newv[j][k] = MeshVertex.blend(v[j][i], v[j][p3], 0.8, 0.2);
                      //newv[j][k].r.x -= 1;
                      for (int m = 0; m < numParam; m++){
                        newparam[j][k][m] = 0.5*(param[j][i][m]+param[j][p3][m]);
                      }
                  }
                      
              }
              news[k] = 1.0f;
              k++;
            }
        }
      
      // Count the number of rows which are not yet converged.
      
      count = 0;
      for (j = 0; j < newrefine.length-1; j++)
        if (newrefine[j] || newrefine[j+1])
          count++;
      if (closed)
        if (newrefine[0] || newrefine[newrefine.length-1])
          count++;
      v = newv;
      s = news;
      param = newparam;
      refine = newrefine;
    } while (count > 0 && ++iterations < MAX_SUBDIVISIONS);
      
    //System.out.println(" approxOneAxis cuount: " + count );
      
    return new Object [] {v, s, param};
  }

  /** The following two routines are used by subdivideMesh to calculate new point positions 
     for interpolating and approximating subdivision.  v is an array of vertices, s is
     the array of smoothness values for them, and i, j, k, and m are the indices of the points
     from which the new point will be calculated. */
  
  public static MeshVertex calcInterpPoint(MeshVertex v[], float s[], double oldParam[][], double newParam[], int i, int j, int k, int m)
  {
    double w1, w2, w3, w4;
    
    w1 = -0.0625*s[j];
    w2 = 0.5-w1;
    w4 = -0.0625*s[k];
    w3 = 0.5-w4;
    
    MeshVertex vt = new MeshVertex (new Vec3(w1*v[i].r.x + w2*v[j].r.x + w3*v[k].r.x + w4*v[m].r.x,
                        w1*v[i].r.y + w2*v[j].r.y + w3*v[k].r.y + w4*v[m].r.y,
                        w1*v[i].r.z + w2*v[j].r.z + w3*v[k].r.z + w4*v[m].r.z));
    for (int n = 0; n < newParam.length; n++)
      newParam[n] = w1*oldParam[i][n]+w2*oldParam[j][n]+w3*oldParam[k][n]+w4*oldParam[m][n];
    if (v[j].ikJoint == v[k].ikJoint)
      {
        vt.ikJoint = v[j].ikJoint;
        vt.ikWeight = 0.5*(v[j].ikWeight+v[k].ikWeight);
      }
    else if (v[j].ikWeight > v[k].ikWeight)
      {
        vt.ikJoint = v[j].ikJoint;
        vt.ikWeight = v[j].ikWeight;
      }
    else
      {
        vt.ikJoint = v[k].ikJoint;
        vt.ikWeight = v[k].ikWeight;
      }
    return vt;
  }

  public static MeshVertex calcApproxPoint(MeshVertex v[], float s[], double oldParam[][], double newParam[], int i, int j, int k)
  {
    double w1 = 0.125*s[j], w2 = 1.0-2.0*w1;
    
    MeshVertex vt = new MeshVertex (new Vec3(w1*v[i].r.x + w2*v[j].r.x + w1*v[k].r.x,
                            w1*v[i].r.y + w2*v[j].r.y + w1*v[k].r.y,
                            w1*v[i].r.z + w2*v[j].r.z + w1*v[k].r.z));
    for (int n = 0; n < newParam.length; n++)
      newParam[n] = w1*oldParam[i][n]+w2*oldParam[j][n]+w1*oldParam[k][n];
    vt.ikJoint = v[j].ikJoint;
    vt.ikWeight = v[j].ikWeight;
    return vt;
  }

  public WireframeMesh getWireframeMesh()
  {
    Vec3 point[];
    int i, j, k, udim, vdim, from[], to[];

    if (cachedWire != null)
    {
      WireframeMesh cached = cachedWire.get();
      if (cached != null)
        return cached;
    }
    
    // First get the array of points.

    RenderingMesh cached = null;
    if (cachedMesh != null)
      cached = cachedMesh.get();
    if (cached != null)
      {
        point = cached.vert;
        udim = cachedUSize;
        vdim = cachedVSize;
      }
    else
      {
          // I want more detail.
          // System.out.println(" SplineMesh surface error: " + ArmaDesignStudio.getPreferences().getInteractiveSurfaceError());
          // ArmaDesignStudio.getPreferences().getInteractiveSurfaceError() 0.05
          
        SplineMesh newmesh = subdivideMesh(this, ArmaRender.getPreferences().getInteractiveSurfaceError());
        
        
          
        
        cachedUSize = udim = newmesh.usize;
        cachedVSize = vdim = newmesh.vsize;
        point = new Vec3 [newmesh.vertex.length];
        for (i = 0; i < point.length; i++)
          point[i] = newmesh.vertex[i].r;
      }
    
    // Determine how many lines there will be.
    
    i = udim*(vdim-1) + vdim*(udim-1);
    if (uclosed)
      i += vdim;
    if (vclosed)
      i += udim;
    
    // Build the list of lines.
    
    from = new int [i];
    to = new int [i];
    k = 0;
    for (i = 0; i < udim-1; i++)
      for (j = 0; j < vdim-1; j++)
        {
          from[k] = from[k+1] = i+udim*j;
          to[k++] = i+1+udim*j;
          to[k++] = i+udim*(j+1);
        }
    for (i = 0; i < udim-1; i++)
      {
        from[k] = i+udim*(vdim-1);
        to[k++] = i+1+udim*(vdim-1);
      }
    for (i = 0; i < vdim-1; i++)
      {
        from[k] = udim-1+udim*i;
        to[k++] = udim-1+udim*(i+1);
      }
    if (uclosed)
      for (i = 0; i < vdim; i++)
        {
          from[k] = i*udim;
          to[k++] = i*udim+udim-1;
        }
    if (vclosed)
      for (i = 0; i < udim; i++)
        {
          from[k] = i;
          to[k++] = i+udim*(vdim-1);
        }
    WireframeMesh wire = new WireframeMesh(point, from, to);
    cachedWire = new SoftReference<WireframeMesh>(wire);
    return wire;
  }

    /**
     *
     *
     */
  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
      //System.out.println("getRenderingMesh  *******");
      
    float us[], vs[];
    Vec3 point[], norm[];
    int i, j, k, u1, u2, v1, v2, udim, vdim, normIndex[][][];
    RenderingTriangle tri[];
    RenderingMesh mesh;

    if (interactive && cachedMesh != null)
    {
      RenderingMesh cached = cachedMesh.get();
      if (cached != null)
        return cached;
    }
    
    // First get the array of points.
      
      // Collect origional mesh edges in arrays. Used for snapping to edges after subdivide.
      Vec3 [] verts = this.getVertexPositions();
      Vec3[] edgeU1 = new Vec3[this.usize];   // Bottom row
      Vec3[] edgeUN = new Vec3[this.usize];   // Top row
      Vec3[] edgeV1 = new Vec3[this.vsize];   // Left col
      Vec3[] edgeVN = new Vec3[this.vsize];   // Right col
      for(int e = 0; e < this.usize; e++){
          int uIndex = e;         // u = width
          int vIndex = 0;         // v = height
          int pointIndex1 = (uIndex) + ( (vIndex) * this.usize );
          if(pointIndex1 < verts.length){
              edgeU1[e] = verts[pointIndex1];                                     //
              //System.out.println(" edgeU1 e " + e + " - " + edgeU1[e] );
          }
          vIndex = this.vsize - 1;
          int pointIndexN = (uIndex) + ( (vIndex) * this.usize );
          if(pointIndexN < verts.length){
              edgeUN[e] = verts[pointIndexN];
              //System.out.println(" edgeUN e " + e + " - " + edgeUN[e] );
          }
      }
      for(int e = 0; e < this.vsize; e++){
          int uIndex = 0;         // u = width
          int vIndex = e;         // v = height
          int pointIndex1 = (uIndex) + ( (vIndex) * this.usize );
          if(pointIndex1 < verts.length){
              edgeV1[e] = verts[pointIndex1];
              //System.out.println(" edgeV1 e " + e + " - " + edgeV1[e] );
          }
          uIndex = this.usize - 1;
          int pointIndexN = (uIndex) + ( (vIndex) * this.usize );
          if(pointIndexN < verts.length){
              edgeVN[e] = verts[pointIndexN];
              //System.out.println(" edgeVN e " + e + " - " + edgeVN[e] );
          }
      }
      
      for(int s = 0; s < 3; s++){             // Subdivide the edge point array three times.
          edgeU1 = subdivideArray(edgeU1);
          edgeUN = subdivideArray(edgeUN);
          edgeV1 = subdivideArray(edgeV1);
          edgeVN = subdivideArray(edgeVN);
      }
    
    SplineMesh newmesh = subdivideMesh(this, tol, info);
      
      //
      // Snap edges of subdivided mesh to origional edges.
      newmesh = snapMeshEdges(newmesh, edgeU1, edgeUN, edgeV1, edgeVN);
      
      
    us = newmesh.usmoothness;
    vs = newmesh.vsmoothness;
    cachedUSize = udim = newmesh.usize;
    cachedVSize = vdim = newmesh.vsize;
    point = new Vec3 [newmesh.vertex.length];
    for (i = 0; i < point.length; i++)
      point[i] = newmesh.vertex[i].r;
    
    // Construct the list of normals.
    
    ArrayList<Vec3> normal = new ArrayList<Vec3>(point.length);
    normIndex = new int [udim][vdim][4];
    k = 0;
    for (i = 0; i < udim; i++)
      for (j = 0; j < vdim; j++)
        {
          u1 = i-1;
          if (u1 == -1)
            {
              if (uclosed)
                u1 = udim-1;
              else
                u1 = 0;
            }
          u2 = i+1;
          if (u2 == udim)
            {
              if (uclosed)
                u2 = 0;
              else
                u2 = i;
            }
          v1 = j-1;
          if (v1 == -1)
            {
              if (vclosed)
                v1 = vdim-1;
              else
                v1 = 0;
            }
          v2 = j+1;
          if (v2 == vdim)
            {
              if (vclosed)
                v2 = 0;
              else
                v2 = j;
            }
          if (us[i] < 1.0f && vs[j] < 1.0f) // Creases in both directions.
            {
              normal.add(calcNormal(point, i, j, u1, i, v1, j, udim));
              normal.add(calcNormal(point, i, j, i, u2, v1, j, udim));
              normal.add(calcNormal(point, i, j, u1, i, j, v2, udim));
              normal.add(calcNormal(point, i, j, i, u2, j, v2, udim));
              normIndex[i][j][0] = k++;
              normIndex[i][j][1] = k++;
              normIndex[i][j][2] = k++;
              normIndex[i][j][3] = k++;
            }
          else if (us[i] < 1.0f) // Crease in the u direction.
            {
              normal.add(calcNormal(point, i, j, u1, i, v1, v2, udim));
              normal.add(calcNormal(point, i, j, i, u2, v1, v2, udim));
              normIndex[i][j][0] = normIndex[i][j][2] = k++;
              normIndex[i][j][1] = normIndex[i][j][3] = k++;
            }
          else if ( vs[j] < 1.0f) // Crease in the v direction.
            {
              normal.add(calcNormal(point, i, j, u1, u2, v1, j, udim));
              normal.add(calcNormal(point, i, j, u1, u2, j, v2, udim));
              normIndex[i][j][0] = normIndex[i][j][1] = k++;
              normIndex[i][j][2] = normIndex[i][j][3] = k++;
            }
          else // Smooth vertex.
            {
              normal.add(calcNormal(point, i, j, u1, u2, v1, v2, udim));
              normIndex[i][j][0] = normIndex[i][j][1] = normIndex[i][j][2] = normIndex[i][j][3] = k++;
            }
        }
    norm = new Vec3 [normal.size()];
    for (i = 0; i < norm.length; i++)
      norm[i] = normal.get(i);
    
    // Determine how many triangles there will be.
    
    i = (udim-1)*(vdim-1);
    if (uclosed)
      i += vdim-1;
    if (vclosed)
      i += udim-1;
    if (uclosed && vclosed)
      i++;
    i *= 2;
    
    // Build the list of triangles.
    
    tri = new RenderingTriangle [i];
    k = 0;
    for (i = 0; i < udim-1; i++)
      for (j = 0; j < vdim-1; j++)
        {
          tri[k++] = texMapping.mapTriangle(i+udim*j, i+1+udim*j, i+1+udim*(j+1), normIndex[i][j][3], normIndex[i+1][j][2], normIndex[i+1][j+1][0], point);
          tri[k++] = texMapping.mapTriangle(i+udim*j, i+1+udim*(j+1), i+udim*(j+1), normIndex[i][j][3], normIndex[i+1][j+1][0], normIndex[i][j+1][1], point);
        }
    if (uclosed)
      for (i = 0; i < vdim-1; i++)
        {
          tri[k++] = texMapping.mapTriangle((i+1)*udim-1, i*udim, (i+1)*udim, normIndex[udim-1][i][3], normIndex[0][i][2], normIndex[0][i+1][0], point);
          tri[k++] = texMapping.mapTriangle((i+1)*udim-1, (i+1)*udim, (i+2)*udim-1, normIndex[udim-1][i][3], normIndex[0][i+1][0], normIndex[udim-1][i+1][1], point);
        }
    if (vclosed)
      for (i = 0; i < udim-1; i++)
        {
          tri[k++] = texMapping.mapTriangle(i+udim*(vdim-1), i+1+udim*(vdim-1), i+1, normIndex[i][vdim-1][3], normIndex[i+1][vdim-1][2], normIndex[i+1][0][0], point);
          tri[k++] = texMapping.mapTriangle(i+udim*(vdim-1), i+1, i, normIndex[i][vdim-1][3], normIndex[i+1][0][0], normIndex[i][0][1], point);
        }
    if (uclosed && vclosed)
      {
        tri[k++] = texMapping.mapTriangle(udim*vdim-1, udim*(vdim-1), 0, normIndex[udim-1][vdim-1][3], normIndex[0][vdim-1][2], normIndex[0][0][0], point);
        tri[k++] = texMapping.mapTriangle(udim*vdim-1, 0, udim-1, normIndex[udim-1][vdim-1][3], normIndex[0][0][0], normIndex[udim-1][0][1], point);
      }
    mesh = new RenderingMesh(point, norm, tri, texMapping, matMapping);
    mesh.setParameters(newmesh.paramValue);
    if (interactive)
      cachedMesh = new SoftReference<RenderingMesh>(mesh);
    return mesh;
  }
  
  // Calculate the normal vector at a point on the subdivided mesh.

  private Vec3 calcNormal(Vec3 point[], int u, int v, int u1, int u2, int v1, int v2, int udim)
  {
    double len1, len2;
    Vec3 vec1, vec2, norm;
    
    // Calculate the tangent vector along the u direction.
    
    vec1 = point[u1+udim*v].minus(point[u2+udim*v]);
    len1 = vec1.length2();
    if (len1 == 0.0)
      {
        vec1 = point[u1+udim*v1].minus(point[u2+udim*v1]);
        len1 = vec1.length2();
      }
    if (len1 == 0.0)
      {
        vec1 = point[u1+udim*v2].minus(point[u2+udim*v2]);
        len1 = vec1.length2();
      }
    
    // Calculate the tangent vector along the v direction.
    
    vec2 = point[u+udim*v1].minus(point[u+udim*v2]);
    len2 = vec2.length2();
    if (len2 == 0.0)
      {
        vec2 = point[u1+udim*v1].minus(point[u1+udim*v2]);
        len2 = vec2.length2();
      }
    if (len2 == 0.0)
      {
        vec2 = point[u2+udim*v1].minus(point[u2+udim*v2]);
        len2 = vec2.length2();
      }
    
    // Take the cross product to get the normal.
    
    if (len1 == 0.0 || len2 == 0.0)
      return new Vec3();  // This will only happen for *very* strange surfaces.
    norm = vec1.cross(vec2);
    norm.normalize();
    return norm;
    
  }

  @Override
  public int canConvertToTriangleMesh()
  {
    return APPROXIMATELY;
  }
  
  @Override
  public TriangleMesh convertToTriangleMesh(double tol)
  {
    //System.out.println(" SplineMesh convert: " + name);
    int i, j, k, udim, vdim, faces[][];
    TriangleMesh trimesh;
    float us[], vs[];
    Vec3 point[];
      
      
      // Collect origional mesh edges in arrays. Used for snapping to edges after subdivide.
              Vec3 [] verts = this.getVertexPositions();
              Vec3[] edgeU1 = new Vec3[this.usize];   // Bottom row
              Vec3[] edgeUN = new Vec3[this.usize];   // Top row
              Vec3[] edgeV1 = new Vec3[this.vsize];   // Left col
              Vec3[] edgeVN = new Vec3[this.vsize];   // Right col
              for(int e = 0; e < this.usize; e++){
                  int uIndex = e;         // u = width
                  int vIndex = 0;         // v = height
                  int pointIndex1 = (uIndex) + ( (vIndex) * this.usize );
                  if(pointIndex1 < verts.length){
                      edgeU1[e] = verts[pointIndex1];                                     //
                      //System.out.println(" edgeU1 e " + e + " - " + edgeU1[e] );
                  }
                  vIndex = this.vsize - 1;
                  int pointIndexN = (uIndex) + ( (vIndex) * this.usize );
                  if(pointIndexN < verts.length){
                      edgeUN[e] = verts[pointIndexN];
                      //System.out.println(" edgeUN e " + e + " - " + edgeUN[e] );
                  }
              }
              for(int e = 0; e < this.vsize; e++){
                  int uIndex = 0;         // u = width
                  int vIndex = e;         // v = height
                  int pointIndex1 = (uIndex) + ( (vIndex) * this.usize );
                  if(pointIndex1 < verts.length){
                      edgeV1[e] = verts[pointIndex1];
                      //System.out.println(" edgeV1 e " + e + " - " + edgeV1[e] );
                  }
                  uIndex = this.usize - 1;
                  int pointIndexN = (uIndex) + ( (vIndex) * this.usize );
                  if(pointIndexN < verts.length){
                      edgeVN[e] = verts[pointIndexN];
                      //System.out.println(" edgeVN e " + e + " - " + edgeVN[e] );
                  }
              }
              
              for(int s = 0; s < 3; s++){             // Subdivide the edge point array three times.
                  edgeU1 = subdivideArray(edgeU1);
                  edgeUN = subdivideArray(edgeUN);
                  edgeV1 = subdivideArray(edgeV1);
                  edgeVN = subdivideArray(edgeVN);
              }
      
      

    SplineMesh newmesh = subdivideMesh(this, tol);
      
      
      //
      // Snap edges of subdivided mesh to origional edges.
      newmesh = snapMeshEdges(newmesh, edgeU1, edgeUN, edgeV1, edgeVN);
      
      
    us = newmesh.usmoothness;
    vs = newmesh.vsmoothness;
    udim = newmesh.usize;
    vdim = newmesh.vsize;
    point = new Vec3 [newmesh.vertex.length];
    for (i = 0; i < point.length; i++)
      point[i] = newmesh.vertex[i].r;
    
    // Determine how many triangles there will be.
    
    i = (udim-1)*(vdim-1);
    if (uclosed)
      i += vdim-1;
    if (vclosed)
      i += udim-1;
    if (uclosed && vclosed)
      i++;
    i *= 2;
    
    // Build the list of triangles.
    
    faces = new int [i][];
    k = 0;
    for (i = 0; i < udim-1; i++)
      for (j = 0; j < vdim-1; j++)
        {
          faces[k++] = new int [] {i+udim*j, i+1+udim*j, i+1+udim*(j+1)};
          faces[k++] = new int [] {i+udim*j, i+1+udim*(j+1), i+udim*(j+1)};
        }
    if (uclosed)
      for (i = 0; i < vdim-1; i++)
        {
          faces[k++] = new int [] {(i+1)*udim-1, i*udim, (i+1)*udim};
          faces[k++] = new int [] {(i+1)*udim-1, (i+1)*udim, (i+2)*udim-1};
        }
    if (vclosed)
      for (i = 0; i < udim-1; i++)
        {
          faces[k++] = new int [] {i+udim*(vdim-1), i+1+udim*(vdim-1), i+1};
          faces[k++] = new int [] {i+udim*(vdim-1), i+1, i};
        }
    if (uclosed && vclosed)
      {
        faces[k++] = new int [] {udim*vdim-1, udim*(vdim-1), 0};
        faces[k++] = new int [] {udim*vdim-1, 0, udim-1};
      }
    trimesh = new TriangleMesh(point, faces);
    
    // Set the smoothness values for the edges of the triangle mesh.
    
    TriangleMesh.Edge ed[] = trimesh.getEdges();
    for (i = 0; i < ed.length; i++)
      {
        j = ed[i].v1 / udim;
        k = ed[i].v2 / udim;
        if (j == k)
          ed[i].smoothness = vs[j];
        else
          {
            j = ed[i].v1 % udim;
            ed[i].smoothness = us[j];
          }
      }
    
    // Copy over the texture, texture parameters, and material.
    
    trimesh.copyTextureAndMaterial(newmesh);
    return trimesh;
  }

  /** When setting the texture, we need to clear the caches. */
  
  @Override
  public void setTexture(Texture tex, TextureMapping mapping)
  {
    super.setTexture(tex, mapping);
    cachedMesh = null;
    cachedWire = null;
  }

  /** When setting the material, we need to clear the caches. */

  @Override
  public void setMaterial(Material mat, MaterialMapping map)
  {
    super.setMaterial(mat, map);
    cachedMesh = null;
  }

  /** When setting texture parameters, we need to clear the caches. */

  @Override
  public void setParameterValues(ParameterValue val[])
  {
    super.setParameterValues(val);
    cachedMesh = null;
  }

  /** When setting texture parameters, we need to clear the caches. */

  @Override
  public void setParameterValue(TextureParameter param, ParameterValue val)
  {
    super.setParameterValue(param, val);
    cachedMesh = null;
  }

  /** Get the skeleton for the object. */

  @Override
  public Skeleton getSkeleton()
  {
    return skeleton;
  }
  
  /** Set the skeleton for the object. */

  public void setSkeleton(Skeleton s)
  {
    skeleton = s;
  }

  /** The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */

  public SplineMesh(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    short version = in.readShort();

    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    usize = in.readInt();
    vsize = in.readInt();
    vertex = new MeshVertex [usize*vsize];
    usmoothness = new float [usize];
    vsmoothness = new float [vsize];
    if (version == 0)
      for (int i = 0; i < paramValue.length; i++)
        paramValue[i] = new VertexParameterValue(new double [vertex.length]);
    for (int i = 0; i < vertex.length; i++)
      {
        vertex[i] = new MeshVertex(new Vec3(in));
        vertex[i].ikJoint = in.readInt();
        vertex[i].ikWeight = in.readDouble();
        if (version == 0)
          for (int j = 0; j < paramValue.length; j++)
            ((VertexParameterValue) paramValue[j]).getValue()[i] = in.readDouble();
      }
    for (int i = 0; i < usize; i++)
      usmoothness[i] = in.readFloat();
    for (int i = 0; i < vsize; i++)
      vsmoothness[i] = in.readFloat();
    uclosed = in.readBoolean();
    vclosed = in.readBoolean();
    smoothingMethod = in.readInt();
    skeleton = new Skeleton(in);
    findBounds();
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);
    out.writeShort(1);
    out.writeInt(usize);
    out.writeInt(vsize);
    for (int i = 0; i < vertex.length; i++)
      {
        vertex[i].r.writeToFile(out);
        out.writeInt(vertex[i].ikJoint);
        out.writeDouble(vertex[i].ikWeight);
      }
    for (int i = 0; i < usize; i++)
      out.writeFloat(usmoothness[i]);
    for (int i = 0; i < vsize; i++)
      out.writeFloat(vsmoothness[i]);
    out.writeBoolean(uclosed);
    out.writeBoolean(vclosed);
    out.writeInt(smoothingMethod);
    skeleton.writeToStream(out);
  }
  
  /** If necessary, reorder the points in the mesh so that, when converted to a triangle mesh
     for rendering, the normals will be properly oriented. */
  
  public void makeRightSideOut()
  {
    Vec3 norm[] = getNormals(), result = new Vec3();

    for (int i = 0; i < norm.length; i++)
      {
        result.x += vertex[i].r.x*norm[i].x;
        result.y += vertex[i].r.y*norm[i].y;
        result.z += vertex[i].r.z*norm[i].z;
      }
    if (result.x + result.y + result.z < 0.0)
      reverseOrientation();
  }
  
  /** Reverse the points along one direction.  This will cause all the normal vectors to
     be flipped. */
  
  public void reverseOrientation()
  {
    MeshVertex swapVert;
    float swapSmooth;
    int i, j;
    
    for (i = 0; i < usize/2; i++)
      {
        for (j = 0; j < vsize; j++)
          {
            swapVert = vertex[i+usize*j];
            vertex[i+usize*j] = vertex[usize-1-i+usize*j];
            vertex[usize-1-i+usize*j] = swapVert;
          }
        swapSmooth = usmoothness[i];
        usmoothness[i] = usmoothness[usize-1-i];
        usmoothness[usize-1-i] = swapSmooth;
      }
    cachedMesh = null;
  }

  /** Get an array of normal vectors.  This calculates a single normal for each vertex,
     ignoring smoothness values. */
     
  public Vec3 [] getNormals()
  {
    Vec3 point[] = new Vec3 [vertex.length], norm[] = new Vec3 [vertex.length];
    int u1, u2, v1, v2, i, j;
    
    for (i = 0; i < vertex.length; i++)
      point[i] = vertex[i].r;
    for (i = 0; i < usize; i++)
      for (j = 0; j < vsize; j++)
        {
          u1 = i-1;
          if (u1 == -1)
            {
              if (uclosed)
                u1 = usize-1;
              else
                u1 = 0;
            }
          u2 = i+1;
          if (u2 == usize)
            {
              if (uclosed)
                u2 = 0;
              else
                u2 = i;
            }
          v1 = j-1;
          if (v1 == -1)
            {
              if (vclosed)
                v1 = vsize-1;
              else
                v1 = 0;
            }
          v2 = j+1;
          if (v2 == vsize)
            {
              if (vclosed)
                v2 = 0;
              else
                v2 = j;
            }
          norm[i+usize*j] = calcNormal(point, i, j, u1, u2, v1, v2, usize);
        }
    return norm;
  }

  public Property[] getProperties()
  {
    return (Property []) PROPERTIES.clone();
  }

  public Object getPropertyValue(int index)
  {
    if (index == 0)
      return PROPERTIES[0].getAllowedValues()[smoothingMethod-2];
    Object values[] = PROPERTIES[1].getAllowedValues();
    if (uclosed && !vclosed)
      return values[0];
    if (!uclosed && vclosed)
      return values[1];
    if (uclosed && vclosed)
      return values[2];
    return values[3];
  }

  public void setPropertyValue(int index, Object value)
  {
    if (index == 0)
    {
      Object values[] = PROPERTIES[0].getAllowedValues();
      for (int i = 0; i < values.length; i++)
        if (values[i].equals(value))
          setSmoothingMethod(i+2);
    }
    else
    {
      Object values[] = PROPERTIES[1].getAllowedValues();
      for (int i = 0; i < values.length; i++)
        if (values[i].equals(value))
          setClosed(i == 0 || i == 2, i == 1 || i == 2);
    }
  }

  /** Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new SplineMeshKeyframe(this);
  }
  
  /** Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    SplineMeshKeyframe key = (SplineMeshKeyframe) k;
    
    for (int i = 0; i < vertex.length; i++)
      vertex[i].r.set(key.vertPos[i]);
    System.arraycopy(key.usmoothness, 0, usmoothness, 0, usmoothness.length);
    System.arraycopy(key.vsmoothness, 0, vsmoothness, 0, vsmoothness.length);
    if (texParam != null && texParam.length > 0)
      for (int i = 0; i < texParam.length; i++)
        paramValue[i] = key.paramValue[i].duplicate();
    skeleton.copy(key.skeleton);
    cachedMesh = null;
    cachedWire = null;
    findBounds();
  }

  /** Allow SplineMeshes to be converted to Actors. */
  
  public boolean canConvertToActor()
  {
    return true;
  }
  
  /** SplineMeshes cannot be keyframed directly, since any change to mesh topology would
      cause all keyframes to become invalid.  Return an actor for this mesh. */
  
  public Object3D getPosableObject()
  {
    SplineMesh m = (SplineMesh) duplicate();
    return new Actor(m);
  }

  /** This class represents a pose of a SplineMesh. */
  
  public static class SplineMeshKeyframe extends MeshGesture
  {
    Vec3 vertPos[];
    float usmoothness[], vsmoothness[];
    ParameterValue paramValue[];
    Skeleton skeleton;
    SplineMesh mesh;

    public SplineMeshKeyframe(SplineMesh mesh)
    {
      this.mesh = mesh;
      skeleton = mesh.getSkeleton().duplicate();
      vertPos = new Vec3 [mesh.vertex.length];
      usmoothness = new float [mesh.usmoothness.length];
      vsmoothness = new float [mesh.vsmoothness.length];
      for (int i = 0; i < vertPos.length; i++)
        vertPos[i] = new Vec3(mesh.vertex[i].r);
      System.arraycopy(mesh.usmoothness, 0, usmoothness, 0, usmoothness.length);
      System.arraycopy(mesh.vsmoothness, 0, vsmoothness, 0, vsmoothness.length);
      paramValue = new ParameterValue [mesh.texParam.length];
      for (int i = 0; i < paramValue.length; i++)
        paramValue[i] = mesh.paramValue[i].duplicate();
    }
    
    private SplineMeshKeyframe()
    {
    }

    /** Get the Mesh this Gesture belongs to. */
    
    protected Mesh getMesh()
    {
      return mesh;
    }
    
    /** Get the positions of all vertices in this Gesture. */
    
    protected Vec3 [] getVertexPositions()
    {
      return vertPos;
    }
    
    /** Set the positions of all vertices in this Gesture. */
    
    protected void setVertexPositions(Vec3 pos[])
    {
      vertPos = pos;
    }

    /** Get the skeleton for this pose (or null if it doesn't have one). */
  
    public Skeleton getSkeleton()
    {
      return skeleton;
    }
  
    /** Set the skeleton for this pose. */
  
    public void setSkeleton(Skeleton s)
    {
      skeleton = s;
    }
    
    /** Create a duplicate of this keyframe. */
  
    public Keyframe duplicate()
    {
      return duplicate(mesh);
    }

    public Keyframe duplicate(Object owner)
    {
      SplineMeshKeyframe k = new SplineMeshKeyframe();
      if (owner instanceof SplineMesh)
        k.mesh = (SplineMesh) owner;
      else
        k.mesh = (SplineMesh) ((ObjectInfo) owner).getObject();
      k.skeleton = skeleton.duplicate();
      k.vertPos = new Vec3 [vertPos.length];
      k.usmoothness = new float [usmoothness.length];
      k.vsmoothness = new float [vsmoothness.length];
      for (int i = 0; i < vertPos.length; i++)
        k.vertPos[i] = new Vec3(vertPos[i]);
      System.arraycopy(usmoothness, 0, k.usmoothness, 0, usmoothness.length);
      System.arraycopy(vsmoothness, 0, k.vsmoothness, 0, vsmoothness.length);
      k.paramValue = new ParameterValue [paramValue.length];
      for (int i = 0; i < paramValue.length; i++)
        k.paramValue[i] = paramValue[i].duplicate();
      return k;
    }
  
    /** Get the list of graphable values for this keyframe. */
  
    public double [] getGraphValues()
    {
      return new double [0];
    }
  
    /** Set the list of graphable values for this keyframe. */
  
    public void setGraphValues(double values[])
    {
    }

    /** These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others.  These methods should never be called, since SplineMeshes
       can only be keyframed by converting them to Actors. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      return null;
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      return null;
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      return null;
    }

    /** Modify the mesh surface of a Gesture to be a weighted average of an arbitrary list of Gestures,
        averaged about this pose.  This method only modifies the vertex positions and texture parameters,
        not the skeleton, and all vertex positions are based on the offsets from the joints they are
        bound to.
        @param average   the Gesture to modify to be an average of other Gestures
        @param p         the list of Gestures to average
        @param weight    the weights for the different Gestures
    */
    
    public void blendSurface(MeshGesture average, MeshGesture p[], double weight[])
    {
      super.blendSurface(average, p, weight);
      SplineMeshKeyframe avg = (SplineMeshKeyframe) average;
      for (int i = 0; i < weight.length; i++)
      {
        SplineMeshKeyframe key = (SplineMeshKeyframe) p[i];
        for (int j = 0; j < usmoothness.length; j++)
          avg.usmoothness[j] += weight[i]*(key.usmoothness[j]-usmoothness[j]);
        for (int j = 0; j < vsmoothness.length; j++)
          avg.vsmoothness[j] += weight[i]*(key.vsmoothness[j]-vsmoothness[j]);
      }

      // Make sure all smoothness values are within legal bounds.
      
      for (int i = 0; i < avg.usmoothness.length; i++)
        {
          if (avg.usmoothness[i] < 0.0)
            avg.usmoothness[i] = 0.0f;
          if (avg.usmoothness[i] > 1.0)
            avg.usmoothness[i] = 1.0f;
        }
      for (int i = 0; i < avg.vsmoothness.length; i++)
        {
          if (avg.vsmoothness[i] < 0.0)
            avg.vsmoothness[i] = 0.0f;
          if (avg.vsmoothness[i] > 1.0)
            avg.vsmoothness[i] = 1.0f;
        }
    }

    /** Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof SplineMeshKeyframe))
        return false;
      SplineMeshKeyframe key = (SplineMeshKeyframe) k;
      for (int i = 0; i < vertPos.length; i++)
        if (!vertPos[i].equals(key.vertPos[i]))
          return false;
      for (int i = 0; i < paramValue.length; i++)
        if (!paramValue[i].equals(key.paramValue[i]))
          return false;
      for (int i = 0; i < usmoothness.length; i++)
        if (usmoothness[i] != key.usmoothness[i])
          return false;
      for (int i = 0; i < vsmoothness.length; i++)
        if (vsmoothness[i] != key.vsmoothness[i])
          return false;
      if (!skeleton.equals(key.skeleton))
        return false;
      return true;
    }
  
    /** Update the texture parameter values when the texture is changed. */
  
    public void textureChanged(TextureParameter oldParams[], TextureParameter newParams[])
    {
      ParameterValue newval[] = new ParameterValue [newParams.length];
      
      for (int i = 0; i < newParams.length; i++)
        {
          int j;
          for (j = 0; j < oldParams.length && !oldParams[j].equals(newParams[i]); j++);
          if (j == oldParams.length)
            {
              // This is a new parameter, so copy the value from the mesh.
              
              for (int k = 0; k < mesh.texParam.length; k++)
                if (mesh.texParam[k].equals(newParams[i]))
                {
                  newval[i] = mesh.paramValue[k].duplicate();
                  break;
                }
            }
          else
            {
              // This is an old parameter, so copy the values over.
              
              newval[i] = paramValue[j];
            }
        }
      paramValue = newval;
    }
  
    /** Get the value of a per-vertex texture parameter. */
    
    public ParameterValue getTextureParameter(TextureParameter p)
    {
      // Determine which parameter to get.
      
      for (int i = 0; i < mesh.texParam.length; i++)
        if (mesh.texParam[i].equals(p))
          return paramValue[i];
      return null;
    }
  
    /** Set the value of a per-vertex texture parameter. */
    
    public void setTextureParameter(TextureParameter p, ParameterValue value)
    {
      // Determine which parameter to set.
      
      int which;
      for (which = 0; which < mesh.texParam.length && !mesh.texParam[which].equals(p); which++);
      if (which == mesh.texParam.length)
        return;
      paramValue[which] = value;
    }
  
    /** Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeShort(1); // version
      out.writeInt(vertPos.length);
      for (int i = 0; i < vertPos.length; i++)
        vertPos[i].writeToFile(out);
      for (int i = 0; i < paramValue.length; i++)
      {
        out.writeUTF(paramValue[i].getClass().getName());
        paramValue[i].writeToStream(out);
      }
      out.writeInt(usmoothness.length);
      for (int i = 0; i < usmoothness.length; i++)
        out.writeFloat(usmoothness[i]);
      out.writeInt(vsmoothness.length);
      for (int i = 0; i < vsmoothness.length; i++)
        out.writeFloat(vsmoothness[i]);
      Joint joint[] = skeleton.getJoints();
      for (int i = 0; i < joint.length; i++)
        {
          joint[i].coords.writeToFile(out);
          out.writeDouble(joint[i].angle1.pos);
          out.writeDouble(joint[i].angle2.pos);
          out.writeDouble(joint[i].twist.pos);
          out.writeDouble(joint[i].length.pos);
        }
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public SplineMeshKeyframe(DataInputStream in, Object parent) throws IOException, InvalidObjectException
    {
      this();
      short version = in.readShort();
      if (version < 0 || version > 1)
        throw new InvalidObjectException("");
      mesh = (SplineMesh) parent;
      int numVert = in.readInt();
      vertPos = new Vec3 [numVert];
      paramValue = new ParameterValue [mesh.texParam.length];
      if (version == 0)
        for (int i = 0; i < paramValue.length; i++)
          paramValue[i] = new VertexParameterValue(new double [numVert]);
      for (int i = 0; i < numVert; i++)
        {
          vertPos[i] = new Vec3(in);
          if (version == 0)
            for (int j = 0; j < paramValue.length; j++)
              ((VertexParameterValue) paramValue[j]).getValue()[i] = in.readDouble();
        }
      if (version > 0)
        for (int i = 0; i < paramValue.length; i++)
          paramValue[i] = readParameterValue(in);
      usmoothness = new float [in.readInt()];
      for (int i = 0; i < usmoothness.length; i++)
        usmoothness[i] = in.readFloat();
      vsmoothness = new float [in.readInt()];
      for (int i = 0; i < vsmoothness.length; i++)
        vsmoothness[i] = in.readFloat();
      skeleton = mesh.getSkeleton().duplicate();
      Joint joint[] = skeleton.getJoints();
      for (int i = 0; i < joint.length; i++)
        {
          joint[i].coords = new CoordinateSystem(in);
          joint[i].angle1.pos = in.readDouble();
          joint[i].angle2.pos = in.readDouble();
          joint[i].twist.pos = in.readDouble();
          joint[i].length.pos = in.readDouble();
        }
    }
  }
    
    
    
    /**
     * subdivideArray
     *
     * Description: Dubdivide Vec3 points given in an array using an equal spacing.
     *
     *@param: Vec3[] input array.
     *@return: Vec3[] expanded array.
     */
    public Vec3[] subdivideArray(Vec3 [] input){
        Vec3 [] result = new Vec3[ (input.length * 2) - 1 ];
        Vector<Vec3> collect = new Vector<Vec3>();
        for(int i = 1; i < input.length; i++){
            Vec3 a = input[i - 1];
            Vec3 b = input[i - 0];
            Vec3 mid = a.midPoint(b);
            collect.addElement(a);
            collect.addElement(mid);
            if(i == input.length - 1){
                collect.addElement(b);
            }
        }
        for(int i = 0; i < collect.size(); i++){
            result[i] = collect.elementAt(i);
        }
        return result;
    }
    
    
    
    /**
     * snapMeshEdges
     *
     * Description: given a subdivided mesh and a set of subdivided edges snap the mesh edges to the closest (appropriate) edge array.
     *
     * @param: newmesh
     * @param: edgeU1 - Bottom row left to right.
     * @param:edgeUN - Top row left to right.
     * @param:edgeV1 - Left col, bottom to top.
     * @param:edgeVN - Right col, bottom to top.
     */
    public SplineMesh snapMeshEdges(SplineMesh newmesh, Vec3[] edgeU1, Vec3[] edgeUN, Vec3[] edgeV1, Vec3[] edgeVN){
        Vec3 [] verts = newmesh.getVertexPositions();
        double angleThreshold = 15.0;
        
        if(newmesh.smoothingMethod == INTERPOLATING){
            angleThreshold = 15.0;
        } else {
            angleThreshold = 8.0;
        }
        
        for(int e = 3; e < newmesh.usize; e++){                                     // Horizontal (Bottom and Top)
            int x = e - 3;
            int a = e - 2;
            int b = e - 1;
            int c = e - 0;
            
            //
            // Bottom
            //
            int vIndex = 0;         // v = height
            // angle between abc
            Vec3 xVec = new Vec3(verts[(e - 3) + ( (vIndex) * newmesh.usize )]);
            Vec3 aVec = new Vec3(verts[(e - 2) + ( (vIndex) * newmesh.usize )]);
            Vec3 bVec = new Vec3(verts[(e - 1) + ( (vIndex) * newmesh.usize )]);
            Vec3 cVec = new Vec3(verts[(e - 0) + ( (vIndex) * newmesh.usize )]);
            aVec.subtract(bVec);
            cVec.subtract(bVec);
            aVec.normalize();
            cVec.normalize();
            double edgeSegmentAngle = aVec.getAngle(cVec);
            edgeSegmentAngle = Math.toDegrees(edgeSegmentAngle);
            edgeSegmentAngle = 180 - edgeSegmentAngle;
            //System.out.println("" + b + " angle " + edgeSegmentAngle );
            
            // angle between xab
            xVec = new Vec3(verts[(e - 3) + ( (vIndex) * newmesh.usize )]);
            aVec = new Vec3(verts[(e - 2) + ( (vIndex) * newmesh.usize )]);
            bVec = new Vec3(verts[(e - 1) + ( (vIndex) * newmesh.usize )]);
            xVec.subtract(aVec);
            bVec.subtract(aVec);
            xVec.normalize();
            bVec.normalize();
            double prevEdgeSegmentAngle = xVec.getAngle(bVec);
            prevEdgeSegmentAngle = Math.toDegrees(prevEdgeSegmentAngle);
            prevEdgeSegmentAngle = 180 - prevEdgeSegmentAngle;
            //System.out.println("" + b + " rev angle " + prevEdgeSegmentAngle );
            
            int uIndex = e - 1; //e;         // u = width
            
            int pointIndex1 = (uIndex) + ( (vIndex) * newmesh.usize );
            if(pointIndex1 < verts.length && ( edgeSegmentAngle > angleThreshold || prevEdgeSegmentAngle > angleThreshold ) ){
                //System.out.println(" edgeU1 e " + e + " - " + edgeU1[e] );
                Vec3 currVec = verts[pointIndex1];
                // find closest edge point
                Vec3 closestEdgeVec = null;
                double closestEdgeVecDist = 99999;
                for (int f = 0; f < edgeU1.length; f++){
                    Vec3 currEdge = edgeU1[f];
                    double dist = currEdge.distance(currVec);
                    if(dist < closestEdgeVecDist){
                        closestEdgeVecDist = dist;
                        closestEdgeVec = currEdge;
                    }
                }
                if(closestEdgeVec != null){
                    //System.out.println(" MODIFY " + verts[pointIndex1] + " -> " + closestEdgeVec );
                    verts[pointIndex1] = closestEdgeVec;
                }
            }
            
            //
            // Top
            //
            vIndex = newmesh.vsize - 1;
             xVec = new Vec3(verts[(e - 3) + ( (vIndex) * newmesh.usize )]);
             aVec = new Vec3(verts[(e - 2) + ( (vIndex) * newmesh.usize )]);
             bVec = new Vec3(verts[(e - 1) + ( (vIndex) * newmesh.usize )]);
             cVec = new Vec3(verts[(e - 0) + ( (vIndex) * newmesh.usize )]);
            aVec.subtract(bVec);
            cVec.subtract(bVec);
            aVec.normalize();
            cVec.normalize();
             edgeSegmentAngle = aVec.getAngle(cVec);
            edgeSegmentAngle = Math.toDegrees(edgeSegmentAngle);
            edgeSegmentAngle = 180 - edgeSegmentAngle;
            //System.out.println("" + b + " angle " + edgeSegmentAngle );
            
            // angle between xab
            xVec = new Vec3(verts[(e - 3) + ( (vIndex) * newmesh.usize )]);
            aVec = new Vec3(verts[(e - 2) + ( (vIndex) * newmesh.usize )]);
            bVec = new Vec3(verts[(e - 1) + ( (vIndex) * newmesh.usize )]);
            xVec.subtract(aVec);
            bVec.subtract(aVec);
            xVec.normalize();
            bVec.normalize();
             prevEdgeSegmentAngle = xVec.getAngle(bVec);
            prevEdgeSegmentAngle = Math.toDegrees(prevEdgeSegmentAngle);
            prevEdgeSegmentAngle = 180 - prevEdgeSegmentAngle;
            int pointIndexN = (uIndex) + ( (vIndex) * newmesh.usize );
            if(pointIndexN < verts.length && ( edgeSegmentAngle > angleThreshold || prevEdgeSegmentAngle > angleThreshold )){
                //edgeUN[e] = verts[pointIndexN];
                //System.out.println(" edgeUN e " + e + " - " + edgeUN[e] );
                Vec3 currVec = verts[pointIndexN];
                // find closest edge point
                Vec3 closestEdgeVec = null;
                double closestEdgeVecDist = 99999;
                for (int f = 0; f < edgeUN.length; f++){
                    Vec3 currEdge = edgeUN[f];
                    double dist = currEdge.distance(currVec);
                    if(dist < closestEdgeVecDist){
                        closestEdgeVecDist = dist;
                        closestEdgeVec = currEdge;
                    }
                }
                if(closestEdgeVec != null){
                    //System.out.println(" MODIFY " + verts[pointIndexN] + " -> " + closestEdgeVec );
                    verts[pointIndexN] = closestEdgeVec;
                }
            }
        }
        
        //
        // vertical
        //
        for(int e = 3; e < newmesh.vsize; e++){
            int x = e - 3;
            int a = e - 2;
            int b = e - 1;
            int c = e - 0;
            
            //
            // Left
            //
            int uIndex = 0;         // u = width
            int vIndex = e;         // v = height
            
            //int vIndex = 0;         // v = height
            // angle between abc
            Vec3 xVec = new Vec3(verts[(uIndex) + ( (e - 3) * newmesh.usize )]);
            Vec3 aVec = new Vec3(verts[(uIndex) + ( (e - 2) * newmesh.usize )]);
            Vec3 bVec = new Vec3(verts[(uIndex) + ( (e - 1) * newmesh.usize )]);
            Vec3 cVec = new Vec3(verts[(uIndex) + ( (e - 0) * newmesh.usize )]);
            aVec.subtract(bVec);
            cVec.subtract(bVec);
            aVec.normalize();
            cVec.normalize();
            double edgeSegmentAngle = aVec.getAngle(cVec);
            edgeSegmentAngle = Math.toDegrees(edgeSegmentAngle);
            edgeSegmentAngle = 180 - edgeSegmentAngle;
            //System.out.println("" + b + " angle " + edgeSegmentAngle );
            
            // angle between xab
            xVec = new Vec3(verts[(uIndex) + ( (e - 3) * newmesh.usize )]);
            aVec = new Vec3(verts[(uIndex) + ( (e - 2) * newmesh.usize )]);
            bVec = new Vec3(verts[(uIndex) + ( (e - 1) * newmesh.usize )]);
            xVec.subtract(aVec);
            bVec.subtract(aVec);
            xVec.normalize();
            bVec.normalize();
            double prevEdgeSegmentAngle = xVec.getAngle(bVec);
            prevEdgeSegmentAngle = Math.toDegrees(prevEdgeSegmentAngle);
            prevEdgeSegmentAngle = 180 - prevEdgeSegmentAngle;
            //System.out.println("" + b + " rev angle " + prevEdgeSegmentAngle );
            
            int pointIndex1 = (uIndex) + ( (vIndex) * newmesh.usize );
            if(pointIndex1 < verts.length && ( edgeSegmentAngle > angleThreshold || prevEdgeSegmentAngle > angleThreshold )){
                //System.out.println(" edgeV1 e " + e + " - " + edgeV1[e] );
                Vec3 currVec = verts[pointIndex1];
                // find closest edge point
                Vec3 closestEdgeVec = null;
                double closestEdgeVecDist = 99999;
                for (int f = 0; f < edgeV1.length; f++){
                    Vec3 currEdge = edgeV1[f];
                    double dist = currEdge.distance(currVec);
                    if(dist < closestEdgeVecDist){
                        closestEdgeVecDist = dist;
                        closestEdgeVec = currEdge;
                    }
                }
                if(closestEdgeVec != null){
                    //System.out.println(" MODIFY " + verts[pointIndex1] + " -> " + closestEdgeVec );
                    verts[pointIndex1] = closestEdgeVec;
                }
            }
            
            //
            // Right
            //
            uIndex = newmesh.usize - 1;
            //vIndex = newmesh.vsize - 1;
             xVec = new Vec3(verts[(uIndex) + ( (e - 3) * newmesh.usize )]);
             aVec = new Vec3(verts[(uIndex) + ( (e - 2) * newmesh.usize )]);
             bVec = new Vec3(verts[(uIndex) + ( (e - 1) * newmesh.usize )]);
             cVec = new Vec3(verts[(uIndex) + ( (e - 0) * newmesh.usize )]);
            aVec.subtract(bVec);
            cVec.subtract(bVec);
            aVec.normalize();
            cVec.normalize();
             edgeSegmentAngle = aVec.getAngle(cVec);
            edgeSegmentAngle = Math.toDegrees(edgeSegmentAngle);
            edgeSegmentAngle = 180 - edgeSegmentAngle;
            //System.out.println("" + b + " angle " + edgeSegmentAngle );
            
            // angle between xab
            xVec = new Vec3(verts[(uIndex) + ( (e - 3) * newmesh.usize )]);
            aVec = new Vec3(verts[(uIndex) + ( (e - 2) * newmesh.usize )]);
            bVec = new Vec3(verts[(uIndex) + ( (e - 1) * newmesh.usize )]);
            xVec.subtract(aVec);
            bVec.subtract(aVec);
            xVec.normalize();
            bVec.normalize();
             prevEdgeSegmentAngle = xVec.getAngle(bVec);
            prevEdgeSegmentAngle = Math.toDegrees(prevEdgeSegmentAngle);
            prevEdgeSegmentAngle = 180 - prevEdgeSegmentAngle;
            
            int pointIndexN = (uIndex) + ( (vIndex) * newmesh.usize );
            if(pointIndexN < verts.length && ( edgeSegmentAngle > angleThreshold || prevEdgeSegmentAngle > angleThreshold ) ){
                //System.out.println(" edgeVN e " + e + " - " + edgeVN[e] );
                Vec3 currVec = verts[pointIndexN];
                // find closest edge point
                Vec3 closestEdgeVec = null;
                double closestEdgeVecDist = 99999;
                for (int f = 0; f < edgeVN.length; f++){
                    Vec3 currEdge = edgeVN[f];
                    double dist = currEdge.distance(currVec);
                    if(dist < closestEdgeVecDist){
                        closestEdgeVecDist = dist;
                        closestEdgeVec = currEdge;
                    }
                }
                if(closestEdgeVec != null){
                    //System.out.println(" MODIFY " + verts[pointIndex1] + " -> " + closestEdgeVec );
                    verts[pointIndexN] = closestEdgeVec;
                }
            }
        }
        newmesh.setVertexPositions(verts); // Update the mesh with the edges moved.
        return newmesh;
    }
}
