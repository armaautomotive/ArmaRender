/* Copyright (C) 2005-2006 by Stefan Gustavson and Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.math;

/**
 * This class implements Ken Perlin's "simplex noise" algorithm.  It is based on an
 * implementation is by Stefan Gustavson, found in his article, "Simplex Noise Demystified"
 * (http://staffwww.itn.liu.se/~stegu/simplexnoise/simplexnoise.pdf).  It has been modified
 * to improve performance, and methods to calculate the gradient of the noise function and
 * a vector valued noise function have been added.
 * <p>
 * This is a function which varies smoothly between -1.0 and 1.0, with all of the variation
 * happening on a length scale of about 1.  Typically, you will want to add several octaves
 * of this function together to create a fractal noise function.  Methods are also provided
 * for calculating the gradient of the noise function, and a closely related noise function
 * which is vector valued.
 * <p>
 * Note: it is generally recommended to use the {@link Noise} class instead of calling this
 * one directly, since that allows the preferred noise generator to be changed.
 */

public class SimplexNoise {  // Simplex noise in 2D, 3D and 4D
  private static Grad grad3[] = {new Grad(1,1,0),new Grad(-1,1,0),new Grad(1,-1,0),new Grad(-1,-1,0),
                                 new Grad(1,0,1),new Grad(-1,0,1),new Grad(1,0,-1),new Grad(-1,0,-1),
                                 new Grad(0,1,1),new Grad(0,-1,1),new Grad(0,1,-1),new Grad(0,-1,-1)};
  private static Grad grad4[]= {new Grad(0,1,1,1),new Grad(0,1,1,-1),new Grad(0,1,-1,1),new Grad(0,1,-1,-1),
                   new Grad(0,-1,1,1),new Grad(0,-1,1,-1),new Grad(0,-1,-1,1),new Grad(0,-1,-1,-1),
                   new Grad(1,0,1,1),new Grad(1,0,1,-1),new Grad(1,0,-1,1),new Grad(1,0,-1,-1),
                   new Grad(-1,0,1,1),new Grad(-1,0,1,-1),new Grad(-1,0,-1,1),new Grad(-1,0,-1,-1),
                   new Grad(1,1,0,1),new Grad(1,1,0,-1),new Grad(1,-1,0,1),new Grad(1,-1,0,-1),
                   new Grad(-1,1,0,1),new Grad(-1,1,0,-1),new Grad(-1,-1,0,1),new Grad(-1,-1,0,-1),
                   new Grad(1,1,1,0),new Grad(1,1,-1,0),new Grad(1,-1,1,0),new Grad(1,-1,-1,0),
                   new Grad(-1,1,1,0),new Grad(-1,1,-1,0),new Grad(-1,-1,1,0),new Grad(-1,-1,-1,0)};
  private static short p[] = {151,160,137,91,90,15,
  131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
  190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
  88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
  77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
  102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
  135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
  5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
  223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
  129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
  251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
  49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
  138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};
  // To remove the need for index wrapping, double the permutation table length
  private static short perm[] = new short[512];
  private static short permMod12[] = new short[512];
  static {
    for(int i=0; i<512; i++)
    {
      perm[i]=p[i & 255];
      permMod12[i] = (short) (perm[i]%12);
    }
  }
  // A lookup table to traverse the simplex around a given point in 4D.
  // Details can be found where this table is used, in the 4D noise method.
  private static int simplex[][] = {
    {0,1,2,3},{0,1,3,2},{0,0,0,0},{0,2,3,1},{0,0,0,0},{0,0,0,0},{0,0,0,0},{1,2,3,0},
    {0,2,1,3},{0,0,0,0},{0,3,1,2},{0,3,2,1},{0,0,0,0},{0,0,0,0},{0,0,0,0},{1,3,2,0},
    {0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},
    {1,2,0,3},{0,0,0,0},{1,3,0,2},{0,0,0,0},{0,0,0,0},{0,0,0,0},{2,3,0,1},{2,3,1,0},
    {1,0,2,3},{1,0,3,2},{0,0,0,0},{0,0,0,0},{0,0,0,0},{2,0,3,1},{0,0,0,0},{2,1,3,0},
    {0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},
    {2,0,1,3},{0,0,0,0},{0,0,0,0},{0,0,0,0},{3,0,1,2},{3,0,2,1},{0,0,0,0},{3,1,2,0},
    {2,1,0,3},{0,0,0,0},{0,0,0,0},{0,0,0,0},{3,1,0,2},{0,0,0,0},{3,2,0,1},{3,2,1,0}};
  // Skewing and unskewing factors for 2, 3, and 4 dimensions
  private static final double F2 = 0.5*(Math.sqrt(3.0)-1.0);
  private static final double G2 = (3.0-Math.sqrt(3.0))/6.0;
  private static final double F3 = 1.0/3.0;
  private static final double G3 = 1.0/6.0;
  private static final double F4 = (Math.sqrt(5.0)-1.0)/4.0;
  private static final double G4 = (5.0-Math.sqrt(5.0))/20.0;
  // This method is a *lot* faster than using (int)Math.floor(x)
  private static int fastfloor(double x) {
    return x>0 ? (int)x : (int)x-1;
  }
  private static double dot(Grad g, double x, double y) {
    return g.x*x + g.y*y; }
  private static double dot(Grad g, double x, double y, double z) {
    return g.x*x + g.y*y + g.z*z; }
  private static double dot(Grad g, double x, double y, double z, double w) {
    return g.x*x + g.y*y + g.z*z + g.w*w; }
  /**
   * Calculate the noise value at a point in 2D space.
   */
  public static double noise(double xin, double yin) {
    double n0, n1, n2; // Noise contributions from the three corners
    // Skew the input space to determine which simplex cell we're in
    double s = (xin+yin)*F2; // Hairy factor for 2D
    int i = fastfloor(xin+s);
    int j = fastfloor(yin+s);
    double t = (i+j)*G2;
    double X0 = i-t; // Unskew the cell origin back to (x,y) space
    double Y0 = j-t;
    double x0 = xin-X0; // The x,y distances from the cell origin
    double y0 = yin-Y0;
    // For the 2D case, the simplex shape is an equilateral triangle.
    // Determine which simplex we are in.
    int i1, j1; // Offsets for second (middle) corner of simplex in (i,j) coords
    if(x0>y0) {i1=1; j1=0;} // lower triangle, XY order: (0,0)->(1,0)->(1,1)
    else {i1=0; j1=1;}      // upper triangle, YX order: (0,0)->(0,1)->(1,1)
    // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
    // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
    // c = (3-sqrt(3))/6
    double x1 = x0 - i1 + G2; // Offsets for middle corner in (x,y) unskewed coords
    double y1 = y0 - j1 + G2;
    double x2 = x0 - 1.0 + 2.0 * G2; // Offsets for last corner in (x,y) unskewed coords
    double y2 = y0 - 1.0 + 2.0 * G2;
    // Work out the hashed gradient indices of the three simplex corners
    int ii = i & 255;
    int jj = j & 255;
    int gi0 = permMod12[ii+perm[jj]];
    int gi1 = permMod12[ii+i1+perm[jj+j1]];
    int gi2 = permMod12[ii+1+perm[jj+1]];
    // Calculate the contribution from the three corners
    double t0 = 0.5 - x0*x0-y0*y0;
    if(t0<0) n0 = 0.0;
    else {
      t0 *= t0;
      n0 = t0 * t0 * dot(grad3[gi0], x0, y0);  // (x,y) of grad3 used for 2D gradient
    }
    double t1 = 0.5 - x1*x1-y1*y1;
    if(t1<0) n1 = 0.0;
    else {
      t1 *= t1;
      n1 = t1 * t1 * dot(grad3[gi1], x1, y1);
    }
    double t2 = 0.5 - x2*x2-y2*y2;
    if(t2<0) n2 = 0.0;
    else {
      t2 *= t2;
      n2 = t2 * t2 * dot(grad3[gi2], x2, y2);
    }
    // Add contributions from each corner to get the final noise value.
    // The result is scaled to return values in the interval [-1,1].
    return 70.0 * (n0 + n1 + n2);
  }
  /**
   * Calculate the gradient of the noise function at a point in 2D space.
   *
   * @param gradient      this is set equal to the gradient of the noise function
   * @param xin           the x coordinate at which to evaluate the function
   * @param yin           the y coordinate at which to evaluate the function
   */
  public static void noiseGradient(Vec2 gradient, double xin, double yin) {
    // Skew the input space to determine which simplex cell we're in
    double s = (xin+yin)*F2; // Hairy factor for 2D
    int i = fastfloor(xin+s);
    int j = fastfloor(yin+s);
    double t = (i+j)*G2;
    double X0 = i-t; // Unskew the cell origin back to (x,y) space
    double Y0 = j-t;
    double x0 = xin-X0; // The x,y distances from the cell origin
    double y0 = yin-Y0;
    // For the 2D case, the simplex shape is an equilateral triangle.
    // Determine which simplex we are in.
    int i1, j1; // Offsets for second (middle) corner of simplex in (i,j) coords
    if(x0>y0) {i1=1; j1=0;} // lower triangle, XY order: (0,0)->(1,0)->(1,1)
    else {i1=0; j1=1;}      // upper triangle, YX order: (0,0)->(0,1)->(1,1)
    // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
    // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
    // c = (3-sqrt(3))/6
    double x1 = x0 - i1 + G2; // Offsets for middle corner in (x,y) unskewed coords
    double y1 = y0 - j1 + G2;
    double x2 = x0 - 1.0 + 2.0 * G2; // Offsets for last corner in (x,y) unskewed coords
    double y2 = y0 - 1.0 + 2.0 * G2;
    // Work out the hashed gradient indices of the three simplex corners
    int ii = i & 255;
    int jj = j & 255;
    int gi0 = permMod12[ii+perm[jj]];
    int gi1 = permMod12[ii+i1+perm[jj+j1]];
    int gi2 = permMod12[ii+1+perm[jj+1]];
    // Calculate the contribution from the three corners
    double t0 = 0.5 - x0*x0-y0*y0;
    if(t0>0) {
      double t0_2 = t0*t0;
      double t0_3 = t0_2*t0;
      double t0_4 = t0_2*t0_2;
      Grad gr = grad3[gi0];
      double d = dot(gr, x0, y0);
      gradient.x += t0_4*gr.x-8*t0_3*x0*d;
      gradient.y += t0_4*gr.y-8*t0_3*y0*d;
    }
    double t1 = 0.5 - x1*x1-y1*y1;
    if(t1>0) {
      double t1_2 = t1*t1;
      double t1_3 = t1_2*t1;
      double t1_4 = t1_2*t1_2;
      Grad gr = grad3[gi1];
      double d = dot(gr, x1, y1);
      gradient.x += t1_4*gr.x-8*t1_3*x1*d;
      gradient.y += t1_4*gr.y-8*t1_3*y1*d;
    }
    double t2 = 0.5 - x2*x2-y2*y2;
    if(t2>0) {
      double t2_2 = t2*t2;
      double t2_3 = t2_2*t2;
      double t2_4 = t2_2*t2_2;
      Grad gr = grad3[gi2];
      double d = dot(gr, x2, y2);
      gradient.x += t2_4*gr.x-8*t2_3*x2*d;
      gradient.y += t2_4*gr.y-8*t2_3*y2*d;
    }
    // The result is scaled to return values in the interval [-1,1].
    gradient.scale(70.0);
  }
  /**
   * Calculate the noise value at a point in 3D space.
   */
  public static double noise(double xin, double yin, double zin) {
    double n0, n1, n2, n3; // Noise contributions from the four corners
    // Skew the input space to determine which simplex cell we're in
    double s = (xin+yin+zin)*F3; // Very nice and simple skew factor for 3D
    int i = fastfloor(xin+s);
    int j = fastfloor(yin+s);
    int k = fastfloor(zin+s);
    double t = (i+j+k)*G3;
    double X0 = i-t; // Unskew the cell origin back to (x,y,z) space
    double Y0 = j-t;
    double Z0 = k-t;
    double x0 = xin-X0; // The x,y,z distances from the cell origin
    double y0 = yin-Y0;
    double z0 = zin-Z0;
    // For the 3D case, the simplex shape is a slightly irregular tetrahedron.
    // Determine which simplex we are in.
    int i1, j1, k1; // Offsets for second corner of simplex in (i,j,k) coords
    int i2, j2, k2; // Offsets for third corner of simplex in (i,j,k) coords
    if(x0>=y0) {
      if(y0>=z0)
        { i1=1; j1=0; k1=0; i2=1; j2=1; k2=0; } // X Y Z order
        else if(x0>=z0) { i1=1; j1=0; k1=0; i2=1; j2=0; k2=1; } // X Z Y order
        else { i1=0; j1=0; k1=1; i2=1; j2=0; k2=1; } // Z X Y order
      }
    else { // x0<y0
      if(y0<z0) { i1=0; j1=0; k1=1; i2=0; j2=1; k2=1; } // Z Y X order
      else if(x0<z0) { i1=0; j1=1; k1=0; i2=0; j2=1; k2=1; } // Y Z X order
      else { i1=0; j1=1; k1=0; i2=1; j2=1; k2=0; } // Y X Z order
    }
    // A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in (x,y,z),
    // a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in (x,y,z), and
    // a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in (x,y,z), where
    // c = 1/6.
    double x1 = x0 - i1 + G3; // Offsets for second corner in (x,y,z) coords
    double y1 = y0 - j1 + G3;
    double z1 = z0 - k1 + G3;
    double x2 = x0 - i2 + 2.0*G3; // Offsets for third corner in (x,y,z) coords
    double y2 = y0 - j2 + 2.0*G3;
    double z2 = z0 - k2 + 2.0*G3;
    double x3 = x0 - 1.0 + 3.0*G3; // Offsets for last corner in (x,y,z) coords
    double y3 = y0 - 1.0 + 3.0*G3;
    double z3 = z0 - 1.0 + 3.0*G3;
    // Work out the hashed gradient indices of the four simplex corners
    int ii = i & 255;
    int jj = j & 255;
    int kk = k & 255;
    int gi0 = permMod12[ii+perm[jj+perm[kk]]];
    int gi1 = permMod12[ii+i1+perm[jj+j1+perm[kk+k1]]];
    int gi2 = permMod12[ii+i2+perm[jj+j2+perm[kk+k2]]];
    int gi3 = permMod12[ii+1+perm[jj+1+perm[kk+1]]];
    // Calculate the contribution from the four corners
    double t0 = 0.6 - x0*x0 - y0*y0 - z0*z0;
    if(t0<0) n0 = 0.0;
    else {
      t0 *= t0;
      n0 = t0 * t0 * dot(grad3[gi0], x0, y0, z0);
    }
    double t1 = 0.6 - x1*x1 - y1*y1 - z1*z1;
    if(t1<0) n1 = 0.0;
    else {
      t1 *= t1;
      n1 = t1 * t1 * dot(grad3[gi1], x1, y1, z1);
    }
    double t2 = 0.6 - x2*x2 - y2*y2 - z2*z2;
    if(t2<0) n2 = 0.0;
    else {
      t2 *= t2;
      n2 = t2 * t2 * dot(grad3[gi2], x2, y2, z2);
    }
    double t3 = 0.6 - x3*x3 - y3*y3 - z3*z3;
    if(t3<0) n3 = 0.0;
    else {
      t3 *= t3;
      n3 = t3 * t3 * dot(grad3[gi3], x3, y3, z3);
    }
    // Add contributions from each corner to get the final noise value.
    // The result is scaled to stay just inside [-1,1]
    return 32.0*(n0 + n1 + n2 + n3);
  }
  /**
   * Calculate the gradient of the noise function at a point in 3D space.
   *
   * @param gradient      this is set equal to the gradient of the noise function
   * @param xin           the x coordinate at which to evaluate the function
   * @param yin           the y coordinate at which to evaluate the function
   * @param zin           the z coordinate at which to evaluate the function
   */
  public static void noiseGradient(Vec3 gradient, double xin, double yin, double zin) {
    // Skew the input space to determine which simplex cell we're in
    double s = (xin+yin+zin)*F3; // Very nice and simple skew factor for 3D
    int i = fastfloor(xin+s);
    int j = fastfloor(yin+s);
    int k = fastfloor(zin+s);
    double t = (i+j+k)*G3;
    double X0 = i-t; // Unskew the cell origin back to (x,y,z) space
    double Y0 = j-t;
    double Z0 = k-t;
    double x0 = xin-X0; // The x,y,z distances from the cell origin
    double y0 = yin-Y0;
    double z0 = zin-Z0;
    // For the 3D case, the simplex shape is a slightly irregular tetrahedron.
    // Determine which simplex we are in.
    int i1, j1, k1; // Offsets for second corner of simplex in (i,j,k) coords
    int i2, j2, k2; // Offsets for third corner of simplex in (i,j,k) coords
    if(x0>=y0) {
      if(y0>=z0)
        { i1=1; j1=0; k1=0; i2=1; j2=1; k2=0; } // X Y Z order
        else if(x0>=z0) { i1=1; j1=0; k1=0; i2=1; j2=0; k2=1; } // X Z Y order
        else { i1=0; j1=0; k1=1; i2=1; j2=0; k2=1; } // Z X Y order
      }
    else { // x0<y0
      if(y0<z0) { i1=0; j1=0; k1=1; i2=0; j2=1; k2=1; } // Z Y X order
      else if(x0<z0) { i1=0; j1=1; k1=0; i2=0; j2=1; k2=1; } // Y Z X order
      else { i1=0; j1=1; k1=0; i2=1; j2=1; k2=0; } // Y X Z order
    }
    // A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in (x,y,z),
    // a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in (x,y,z), and
    // a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in (x,y,z), where
    // c = 1/6.
    double x1 = x0 - i1 + G3; // Offsets for second corner in (x,y,z) coords
    double y1 = y0 - j1 + G3;
    double z1 = z0 - k1 + G3;
    double x2 = x0 - i2 + 2.0*G3; // Offsets for third corner in (x,y,z) coords
    double y2 = y0 - j2 + 2.0*G3;
    double z2 = z0 - k2 + 2.0*G3;
    double x3 = x0 - 1.0 + 3.0*G3; // Offsets for last corner in (x,y,z) coords
    double y3 = y0 - 1.0 + 3.0*G3;
    double z3 = z0 - 1.0 + 3.0*G3;
    // Work out the hashed gradient indices of the four simplex corners
    int ii = i & 255;
    int jj = j & 255;
    int kk = k & 255;
    int gi0 = permMod12[ii+perm[jj+perm[kk]]];
    int gi1 = permMod12[ii+i1+perm[jj+j1+perm[kk+k1]]];
    int gi2 = permMod12[ii+i2+perm[jj+j2+perm[kk+k2]]];
    int gi3 = permMod12[ii+1+perm[jj+1+perm[kk+1]]];
    // Calculate the contribution from the four corners
    gradient.set(0.0, 0.0, 0.0);
    double t0 = 0.6 - x0*x0 - y0*y0 - z0*z0;
    if(t0>0) {
      double t0_2 = t0*t0;
      double t0_3 = t0_2*t0;
      double t0_4 = t0_2*t0_2;
      Grad gr = grad3[gi0];
      double d = dot(gr, x0, y0, z0);
      gradient.x += t0_4*gr.x-8*t0_3*x0*d;
      gradient.y += t0_4*gr.y-8*t0_3*y0*d;
      gradient.z += t0_4*gr.z-8*t0_3*z0*d;
    }
    double t1 = 0.6 - x1*x1 - y1*y1 - z1*z1;
    if(t1>0) {
      double t1_2 = t1*t1;
      double t1_3 = t1_2*t1;
      double t1_4 = t1_2*t1_2;
      Grad gr = grad3[gi1];
      double d = dot(gr, x1, y1, z1);
      gradient.x += t1_4*gr.x-8*t1_3*x1*d;
      gradient.y += t1_4*gr.y-8*t1_3*y1*d;
      gradient.z += t1_4*gr.z-8*t1_3*z1*d;
    }
    double t2 = 0.6 - x2*x2 - y2*y2 - z2*z2;
    if(t2>0) {
      double t2_2 = t2*t2;
      double t2_3 = t2_2*t2;
      double t2_4 = t2_2*t2_2;
      Grad gr = grad3[gi2];
      double d = dot(gr, x2, y2, z2);
      gradient.x += t2_4*gr.x-8*t2_3*x2*d;
      gradient.y += t2_4*gr.y-8*t2_3*y2*d;
      gradient.z += t2_4*gr.z-8*t2_3*z2*d;
    }
    double t3 = 0.6 - x3*x3 - y3*y3 - z3*z3;
    if(t3>0) {
      double t3_2 = t3*t3;
      double t3_3 = t3_2*t3;
      double t3_4 = t3_2*t3_2;
      Grad gr = grad3[gi3];
      double d = dot(gr, x3, y3, z3);
      gradient.x += t3_4*gr.x-8*t3_3*x3*d;
      gradient.y += t3_4*gr.y-8*t3_3*y3*d;
      gradient.z += t3_4*gr.z-8*t3_3*z3*d;
    }
    // The result is scaled to stay just inside [-1,1]
    gradient.scale(32.0);
  }
  /**
   * Calculate a vector valued noise function at a point in 3D space.  This function is
   * closely related to the simplex noise function, but is generally less smooth.  Nonetheless,
   * it is useful in many cases where a random vector is needed at each point in space.
   * The length of the vector is typically on the order of 1.
   *
   * @param v      this is set equal to the value of the noise function
   * @param xin    the x coordinate at which to evaluate the function
   * @param yin    the y coordinate at which to evaluate the function
   * @param zin    the z coordinate at which to evaluate the function
   */
  public static void noiseVector(Vec3 v, double xin, double yin, double zin) {
    // Skew the input space to determine which simplex cell we're in
    double s = (xin+yin+zin)*F3; // Very nice and simple skew factor for 3D
    int i = fastfloor(xin+s);
    int j = fastfloor(yin+s);
    int k = fastfloor(zin+s);
    double t = (i+j+k)*G3;
    double X0 = i-t; // Unskew the cell origin back to (x,y,z) space
    double Y0 = j-t;
    double Z0 = k-t;
    double x0 = xin-X0; // The x,y,z distances from the cell origin
    double y0 = yin-Y0;
    double z0 = zin-Z0;
    // For the 3D case, the simplex shape is a slightly irregular tetrahedron.
    // Determine which simplex we are in.
    int i1, j1, k1; // Offsets for second corner of simplex in (i,j,k) coords
    int i2, j2, k2; // Offsets for third corner of simplex in (i,j,k) coords
    if(x0>=y0) {
      if(y0>=z0)
        { i1=1; j1=0; k1=0; i2=1; j2=1; k2=0; } // X Y Z order
        else if(x0>=z0) { i1=1; j1=0; k1=0; i2=1; j2=0; k2=1; } // X Z Y order
        else { i1=0; j1=0; k1=1; i2=1; j2=0; k2=1; } // Z X Y order
      }
    else { // x0<y0
      if(y0<z0) { i1=0; j1=0; k1=1; i2=0; j2=1; k2=1; } // Z Y X order
      else if(x0<z0) { i1=0; j1=1; k1=0; i2=0; j2=1; k2=1; } // Y Z X order
      else { i1=0; j1=1; k1=0; i2=1; j2=1; k2=0; } // Y X Z order
    }
    // A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in (x,y,z),
    // a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in (x,y,z), and
    // a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in (x,y,z), where
    // c = 1/6.
    double x1 = x0 - i1 + G3; // Offsets for second corner in (x,y,z) coords
    double y1 = y0 - j1 + G3;
    double z1 = z0 - k1 + G3;
    double x2 = x0 - i2 + 2.0*G3; // Offsets for third corner in (x,y,z) coords
    double y2 = y0 - j2 + 2.0*G3;
    double z2 = z0 - k2 + 2.0*G3;
    double x3 = x0 - 1.0 + 3.0*G3; // Offsets for last corner in (x,y,z) coords
    double y3 = y0 - 1.0 + 3.0*G3;
    double z3 = z0 - 1.0 + 3.0*G3;
    // Work out the hashed gradient indices of the four simplex corners
    int ii = i & 255;
    int jj = j & 255;
    int kk = k & 255;
    int gi0 = permMod12[ii+perm[jj+perm[kk]]];
    int gi1 = permMod12[ii+i1+perm[jj+j1+perm[kk+k1]]];
    int gi2 = permMod12[ii+i2+perm[jj+j2+perm[kk+k2]]];
    int gi3 = permMod12[ii+1+perm[jj+1+perm[kk+1]]];
    // Calculate the contribution from the four corners
    v.set(0.0, 0.0, 0.0);
    double t0 = 0.6 - x0*x0 - y0*y0 - z0*z0;
    if(t0>0) {
      t0 *= t0;
      t0 *= t0;
      Grad gr = grad3[gi0];
      v.x += t0*gr.x;
      v.y += t0*gr.y;
      v.z += t0*gr.z;
    }
    double t1 = 0.6 - x1*x1 - y1*y1 - z1*z1;
    if(t1>0) {
      t1 *= t1;
      t1 *= t1;
      Grad gr = grad3[gi1];
      v.x += t1*gr.x;
      v.y += t1*gr.y;
      v.z += t1*gr.z;
    }
    double t2 = 0.6 - x2*x2 - y2*y2 - z2*z2;
    if(t2>0) {
      t2 *= t2;
      t2 *= t2;
      Grad gr = grad3[gi2];
      v.x += t2*gr.x;
      v.y += t2*gr.y;
      v.z += t2*gr.z;
    }
    double t3 = 0.6 - x3*x3 - y3*y3 - z3*z3;
    if(t3>0) {
      t3 *= t3;
      t3 *= t3;
      Grad gr = grad3[gi3];
      v.x += t3*gr.x;
      v.y += t3*gr.y;
      v.z += t3*gr.z;
    }
    v.scale(6.0);
  }
  /**
   * Calculate the noise value at a point in 4D space.
   */
  public static double noise(double x, double y, double z, double w) {

    double n0, n1, n2, n3, n4; // Noise contributions from the five corners
    // Skew the (x,y,z,w) space to determine which cell of 24 simplices we're in
    double s = (x + y + z + w) * F4; // Factor for 4D skewing
    int i = fastfloor(x + s);
    int j = fastfloor(y + s);
    int k = fastfloor(z + s);
    int l = fastfloor(w + s);
    double t = (i + j + k + l) * G4; // Factor for 4D unskewing
    double X0 = i - t; // Unskew the cell origin back to (x,y,z,w) space
    double Y0 = j - t;
    double Z0 = k - t;
    double W0 = l - t;
    double x0 = x - X0;  // The x,y,z,w distances from the cell origin
    double y0 = y - Y0;
    double z0 = z - Z0;
    double w0 = w - W0;
    // For the 4D case, the simplex is a 4D shape I won't even try to describe.
    // To find out which of the 24 possible simplices we're in, we need to
    // determine the magnitude ordering of x0, y0, z0 and w0.
    // The method below is a good way of finding the ordering of x,y,z,w and
    // then find the correct traversal order for the simplex were in.
    // First, six pair-wise comparisons are performed between each possible pair
    // of the four coordinates, and the results are used to add up binary bits
    // for an integer index.
    int c1 = (x0 > y0) ? 32 : 0;
    int c2 = (x0 > z0) ? 16 : 0;
    int c3 = (y0 > z0) ? 8 : 0;
    int c4 = (x0 > w0) ? 4 : 0;
    int c5 = (y0 > w0) ? 2 : 0;
    int c6 = (z0 > w0) ? 1 : 0;
    int c = c1 + c2 + c3 + c4 + c5 + c6;
    int i1, j1, k1, l1; // The integer offsets for the second simplex corner
    int i2, j2, k2, l2; // The integer offsets for the third simplex corner
    int i3, j3, k3, l3; // The integer offsets for the fourth simplex corner
    // simplex[c] is a 4-vector with the numbers 0, 1, 2 and 3 in some order.
    // Many values of c will never occur, since e.g. x>y>z>w makes x<z, y<w and x<w
    // impossible. Only the 24 indices which have non-zero entries make any sense.
    // We use a thresholding to set the coordinates in turn from the largest magnitude.
    // The number 3 in the "simplex" array is at the position of the largest coordinate.
    i1 = simplex[c][0]>=3 ? 1 : 0;
    j1 = simplex[c][1]>=3 ? 1 : 0;
    k1 = simplex[c][2]>=3 ? 1 : 0;
    l1 = simplex[c][3]>=3 ? 1 : 0;
    // The number 2 in the "simplex" array is at the second largest coordinate.
    i2 = simplex[c][0]>=2 ? 1 : 0;
    j2 = simplex[c][1]>=2 ? 1 : 0;
    k2 = simplex[c][2]>=2 ? 1 : 0;
    l2 = simplex[c][3]>=2 ? 1 : 0;
    // The number 1 in the "simplex" array is at the second smallest coordinate.
    i3 = simplex[c][0]>=1 ? 1 : 0;
    j3 = simplex[c][1]>=1 ? 1 : 0;
    k3 = simplex[c][2]>=1 ? 1 : 0;
    l3 = simplex[c][3]>=1 ? 1 : 0;
    // The fifth corner has all coordinate offsets = 1, so no need to look that up.
    double x1 = x0 - i1 + G4; // Offsets for second corner in (x,y,z,w) coords
    double y1 = y0 - j1 + G4;
    double z1 = z0 - k1 + G4;
    double w1 = w0 - l1 + G4;
    double x2 = x0 - i2 + 2.0*G4; // Offsets for third corner in (x,y,z,w) coords
    double y2 = y0 - j2 + 2.0*G4;
    double z2 = z0 - k2 + 2.0*G4;
    double w2 = w0 - l2 + 2.0*G4;
    double x3 = x0 - i3 + 3.0*G4; // Offsets for fourth corner in (x,y,z,w) coords
    double y3 = y0 - j3 + 3.0*G4;
    double z3 = z0 - k3 + 3.0*G4;
    double w3 = w0 - l3 + 3.0*G4;
    double x4 = x0 - 1.0 + 4.0*G4; // Offsets for last corner in (x,y,z,w) coords
    double y4 = y0 - 1.0 + 4.0*G4;
    double z4 = z0 - 1.0 + 4.0*G4;
    double w4 = w0 - 1.0 + 4.0*G4;
    // Work out the hashed gradient indices of the five simplex corners
    int ii = i & 255;
    int jj = j & 255;
    int kk = k & 255;
    int ll = l & 255;
    int gi0 = perm[ii+perm[jj+perm[kk+perm[ll]]]] % 32;
    int gi1 = perm[ii+i1+perm[jj+j1+perm[kk+k1+perm[ll+l1]]]] % 32;
    int gi2 = perm[ii+i2+perm[jj+j2+perm[kk+k2+perm[ll+l2]]]] % 32;
    int gi3 = perm[ii+i3+perm[jj+j3+perm[kk+k3+perm[ll+l3]]]] % 32;
    int gi4 = perm[ii+1+perm[jj+1+perm[kk+1+perm[ll+1]]]] % 32;
    // Calculate the contribution from the five corners
    double t0 = 0.6 - x0*x0 - y0*y0 - z0*z0 - w0*w0;
    if(t0<0) n0 = 0.0;
    else {
      t0 *= t0;
      n0 = t0 * t0 * dot(grad4[gi0], x0, y0, z0, w0);
    }
   double t1 = 0.6 - x1*x1 - y1*y1 - z1*z1 - w1*w1;
    if(t1<0) n1 = 0.0;
    else {
      t1 *= t1;
      n1 = t1 * t1 * dot(grad4[gi1], x1, y1, z1, w1);
    }
   double t2 = 0.6 - x2*x2 - y2*y2 - z2*z2 - w2*w2;
    if(t2<0) n2 = 0.0;
    else {
      t2 *= t2;
      n2 = t2 * t2 * dot(grad4[gi2], x2, y2, z2, w2);
    }
   double t3 = 0.6 - x3*x3 - y3*y3 - z3*z3 - w3*w3;
    if(t3<0) n3 = 0.0;
    else {
      t3 *= t3;
      n3 = t3 * t3 * dot(grad4[gi3], x3, y3, z3, w3);
    }
   double t4 = 0.6 - x4*x4 - y4*y4 - z4*z4 - w4*w4;
    if(t4<0) n4 = 0.0;
    else {
      t4 *= t4;
      n4 = t4 * t4 * dot(grad4[gi4], x4, y4, z4, w4);
    }
    // Sum up and scale the result to cover the range [-1,1]
    return 27.0 * (n0 + n1 + n2 + n3 + n4);
  }

  private static class Grad
  {
    double x, y, z, w;

    Grad(double x, double y, double z)
    {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    Grad(double x, double y, double z, double w)
    {
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
    }
  }
}
