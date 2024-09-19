/* A WireframeMesh represents an object to be rendered to the screen as a wireframe.  It is
   described by an array of vertices and a list of lines to be drawn betwee them. */



package armarender;

import armarender.math.*;

public class WireframeMesh
{
  public Vec3 vert[];
  public int from[], to[];
  
  public WireframeMesh(Vec3 vert[], int from[], int to[])
  {    
    this.vert = vert;
    this.from = from;
    this.to = to;
  }
}
