/* Copyright (C) 2021-2022 by Jon Taylor
   Copyright (C) 1999-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.math;

import java.io.*;

/** A Vec3 represents a 3 component vector. */

public class Vec3
{
  public double x, y, z;
  public int f;
    public String comment = "";
    
    // Pair align move highlight object to be moved.
    private boolean renderMoveHighlight = false;

  /** Create a new Vec3 whose x, y, and z components are all equal to 0.0. */

  public Vec3()
  {
  }

  /** Create a new Vec3 with the specified x, y, and z components. */

  public Vec3(double xval, double yval, double zval)
  {
    x = xval;
    y = yval;
    z = zval;
  }

  public Vec3(double xval, double yval, double zval, int fval){
    x = xval;
    y = yval;
    z = zval;
    f = fval; 
  }

  
  /** Create a new Vec3 identical to another one. */

  public Vec3(Vec3 v)
  {
    x = v.x;
    y = v.y;
    z = v.z;
    f = v.f;
  }
  
  /** Set the x, y, and z components of this Vec3. */

  public final void set(double xval, double yval, double zval)
  {
    x = xval;
    y = yval;
    z = zval;
  }
  
  /** Set this Vec3 to be identical to another one. */
  
  public final void set(Vec3 v)
  {
    x = v.x;
    y = v.y;
    z = v.z;
  }
  
  /** Calculate the dot product of this vector with another one. */

  public final double dot(Vec3 v)
  {
    return x*v.x + y*v.y + z*v.z;
  }
  
  /** Calculate the cross product of this vector with another one. */

  public final Vec3 cross(Vec3 v)
  {
    return new Vec3(y * v.z - z * v.y,
                    z * v.x - x * v.z,
                    x * v.y - y * v.x);
  }
  
  /** Calculate the sum of this vector and another one. */

  public final Vec3 plus(Vec3 v)
  {
    return new Vec3(x+v.x, y+v.y, z+v.z);
  }
  
  /** Calculate the difference between this vector and another one. */

  public final Vec3 minus(Vec3 v)
  {
    return new Vec3(x-v.x, y-v.y, z-v.z);
  }
  
  /** Create a new Vec3 by multiplying each component of this one by a constant. */
  
  public final Vec3 times(double d)
  {
    return new Vec3(x*d, y*d, z*d);
  }


  /** Determine whether two vectors are identical. */

  @Override
  public final boolean equals(Object o)
  {
    if (o instanceof Vec3) {
      Vec3 v = (Vec3) o;
      return (v.x == x && v.y == y && v.z == z);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return Float.floatToIntBits((float) x);
  }
  
  /** Calculate the length of this vector. */
  
  public final double length()
  {
    return Math.sqrt(x*x+y*y+z*z);
  }
  
  /** Calculate the square of the length of this vector. */

  public final double length2()
  {
    return x*x+y*y+z*z;
  }
  
  /** Add another Vec3 to this one. */
  
  public final void add(Vec3 v)
  {
    x += v.x;
    y += v.y;
    z += v.z;
  }

  /** Subtract another Vec3 from this one. */
  
  public final void subtract(Vec3 v)
  {
    x -= v.x;
    y -= v.y;
    z -= v.z;
  }

  /** Multiply each component of this vector by the corresponding component of another vector. */
  
  public final void multiply(Vec3 v)
  {
    x *= v.x;
    y *= v.y;
    z *= v.z;
  }
    
  public final void divide(Vec3 v){
      x /= v.x;
      y /= v.y;
      z /= v.z;
  }

  /** Multiply each component of this vector by a constant. */
  
  public final void scale(double d)
  {
    x *= d;
    y *= d;
    z *= d;
  }
    
    public final void divideBy(double d)
    {
          x /= d;
          y /= d;
          z /= d;
    }

  /** Scale each component of this vector so that it has a length of 1.  If this vector has a length
      of 0, this method has no effect. */
  
  public final void normalize()
  {
    double len = Math.sqrt(x*x+y*y+z*z);
    
    if (len > 0.0)
      {
        x /= len;
        y /= len;
        z /= len;
      }
  }
    
    public Vec3 getNormal()
    {
      double len = Math.sqrt(x*x+y*y+z*z);
      if (len > 0.0)
        {
          x /= len;
          y /= len;
          z /= len;
        }
        return this;
    }
  
    
    /**
     * getAngle
     *
     * Description: get an angle between the vectors of the current vector and b.
     *  Usage: 
     *
     *
     * @param: Vec3 a.
     * @param: double angle in radians
     */
    public double getAngle(Vec3 b){ 
        double angle = 0;
        Vec3 tempA = new Vec3(this);
        Vec3 tempB = new Vec3(b);
        tempA.normalize();
        tempB.normalize();
        double aDot = tempA.dot(tempB);
        double a1 = Math.acos(aDot);
        double degrees = Math.toDegrees(a1);
        return a1;
    }
    
    public double getAngleX(Vec3 b){
        double angle = 0;
        Vec3 tempA = new Vec3(this);
        Vec3 tempB = new Vec3(b);
        angle = (double) (Math.atan2(tempB.y - tempA.y, tempB.z - tempA.z));
        if(angle < 0){
            angle += (Math.PI * 2);
        }
        angle -= (Math.PI / 2); // rotate 90 degrees
        if(angle < 0){
            angle += (Math.PI * 2);
        }
        if(angle > (Math.PI * 2)){
            angle -= (Math.PI * 2);
        }
        return angle;
    }
    public double getAngleY(Vec3 b){
        double angle = 0;
        Vec3 tempA = new Vec3(this);
        Vec3 tempB = new Vec3(b);
        angle = (double) (Math.atan2(tempB.z - tempA.z, tempB.x - tempA.x));
        if(angle < 0){
            angle += (Math.PI * 2);
        }
        
        angle -= (Math.PI / 2); // rotate 90 degrees
        if(angle < 0){
            angle += (Math.PI * 2);
        }
        if(angle > (Math.PI * 2)){
            angle -= (Math.PI * 2);
        }
         
        return angle;
    }
    public double getAngleZ(Vec3 b){
        double angle = 0;
        Vec3 tempA = new Vec3(this);
        Vec3 tempB = new Vec3(b);
        //System.out.println(" tempA.x " + tempA.x + " tempA.y " +tempA.y + " tempB.x " + tempB.x + " tempB.y " + tempB.y );
        angle = (double) (Math.atan2(tempB.y - tempA.y, tempB.x - tempA.x));
        if(angle < 0){
            angle += (Math.PI * 2);
        }
        angle -= (Math.PI / 2); // rotate 90 degrees
        if(angle < 0){
            angle += (Math.PI * 2);
        }
        if(angle > (Math.PI * 2)){
            angle -= (Math.PI * 2);
        }
        return angle;
    }
    public double getAngleZ2(Vec3 b){
        double angle = 0;
        Vec3 tempA = new Vec3(this);
        Vec3 tempB = new Vec3(b);
        //System.out.println(" tempA.x " + tempA.x + " tempA.y " +tempA.y + " tempB.x " + tempB.x + " tempB.y " + tempB.y );
        angle = (double) (Math.atan2(tempB.y - tempA.y, tempB.x - tempA.x));
        if(angle < 0){
            angle += (Math.PI * 2);
        }
        //angle -= (Math.PI / 2); // rotate 90 degrees
        //if(angle < 0){
        //    angle += (Math.PI * 2);
        //}
        //if(angle > (Math.PI * 2)){
        //    angle -= (Math.PI * 2);
        //}
        return angle;
    }
    public double reverseAngle(double angle){
        angle = 360 - angle;
        if(angle < 0){
            angle += 360;
        }
        return angle;
    }
    
    public double getAngleThree(Vec3 b, Vec3 c){
        double angle = 0;
        Vec3 tempA = new Vec3(this);
        Vec3 tempB = new Vec3(b);
        Vec3 tempC = new Vec3(c);
        tempA.normalize();
        tempB.normalize();
        tempC.normalize();
        
        // not implemented
        
        return angle;
    }
    
    /*
    public double AngleBetween(Vec3D b) // Vector3D a
    {
        
        this.minus(b)
        
        return 2.0d * Math.atan( (a-b).Length / (a+b).Length);
    }
    */
    
    public final Vec3 midPoint(Vec3 otherPoint){
        Vec3 midpoint = new Vec3(
         (x + otherPoint.x) / 2,
         (y + otherPoint.y) / 2,
         (z + otherPoint.z) / 2
        );
        return midpoint;
    }
    
  /** Calculate the Euclidean distance between this vector and another one. */
  
  public final double distance(Vec3 v)
  {
    return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y)+(v.z-z)*(v.z-z));
  }

  /** Calculate the square of the Euclidean distance between this vector and another one. */
  
  public final double distance2(Vec3 v)
  {
    return (v.x-x)*(v.x-x)+(v.y-y)*(v.y-y)+(v.z-z)*(v.z-z);
  }

  /** Create a 2 component vector by removing one axis of this one.
      @param which     the axis to drop (0=X, 1=Y, 2=Z)
  */

  public final Vec2 dropAxis(int which)
  {
    if (which == 0)
      return new Vec2(y, z);
    else if (which == 1)
      return new Vec2(x, z);
    else
      return new Vec2(x, y);
  }
    
  public String toString()
  {
    return "Vec3: " + x + ", " + y + ", " + z;
  }
  
  /** Create a unit vector which points in the X direction. */
  
  public static Vec3 vx()
  {
    return new Vec3(1.0, 0.0, 0.0);
  }

  /** Create a unit vector which points in the Y direction. */
  
  public static Vec3 vy()
  {
    return new Vec3(0.0, 1.0, 0.0);
  }

  /** Create a unit vector which points in the Z direction. */
  
  public static Vec3 vz()
  {
    return new Vec3(0.0, 0.0, 1.0);
  }

  /** Create a Vec3 by reading in information that was written by writeToFile(). */
  
  public Vec3(DataInputStream in) throws IOException
  {
    x = in.readDouble();
    y = in.readDouble();
    z = in.readDouble();
  }
  
  /** Write out a serialized representation of this object. */

  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeDouble(x);
    out.writeDouble(y);
    out.writeDouble(z);
  }
    
  public void setRenderMoveHighlight(boolean highlight){
      this.renderMoveHighlight = highlight;
  }
    
    
}
