/**
 * Copyright 2024
 */
//package com.ct.admin.api.controller.iot;
package armarender;

import armarender.object.*;
import armarender.math.*;

public class Intersect2 {

	// Define a small value for floating-point comparisons
	private static final double EPSILON = 1e-9;

    public Intersect2(){
        Vec3 faceA = new Vec3(-1.00, 0.437, -0.012);        // Face
        Vec3 faceB = new Vec3(-0.848, 0.437, -0.012);
        Vec3 faceC = new Vec3(-1.00, 0.411, 0.08);
        
        Vec3 lineA = new Vec3(-0.930, 0.495, 0.032);        // Intersecting line
        Vec3 lineB = new Vec3(-0.973, 0.353, 0.0);
        
        Vec3 lineN = new Vec3(-0.908, 0.494, 0.062);        // Non intersecting line adjacent
        Vec3 lineO = new Vec3(-0.951, 0.353, 0.031);
        
        Vec3 lineP = new Vec3(-0.954, 0.433, 0.025);        // Non intersecting line front of
        Vec3 lineQ = new Vec3(-0.910, 0.575, 0.056);
        
        Vec3 parallelLineA = new Vec3(-1.00, 0.437, 0.5);
        Vec3 parallelLineB = new Vec3(-0.848, 0.437, 0.5);

        Vec3 planeLineA = new Vec3(-0.99, 0.42, 0.05);
        Vec3 planeLineB = new Vec3(-0.98, 0.43, 0.02);
     
        System.out.println(" Test 1: " + (intersects(lineA, lineB, faceA, faceB, faceC) == true) ); // Intersection
        System.out.println(" Test 2: " + (intersects(lineA, lineB, faceC, faceB, faceA) == true) ); // Intersection with reversed face
        
        System.out.println(" Test 3: " + (intersects(lineN, lineO, faceA, faceB, faceC) == false) ); // No Intersection adjacent
        System.out.println(" Test 4: " + (intersects(lineN, lineO, faceC, faceB, faceA) == false) ); // No Intersection with reversed face
        
        System.out.println(" Test 5: " + (intersects(lineP, lineQ, faceA, faceB, faceC) == false) ); // No Intersection in front
        System.out.println(" Test 6: " + (intersects(lineP, lineQ, faceC, faceB, faceA) == false) ); // No Intersection with reversed face
        
        System.out.println("Test 7a (parallel): " + (intersects(parallelLineA, parallelLineB, faceA, faceB, faceC) == false));
        System.out.println("Test 7b (parallel): " + (intersects(parallelLineA, parallelLineB, faceB, faceC, faceA) == false));
        System.out.println("Test 7c (parallel): " + (intersects(parallelLineA, parallelLineB, faceC, faceB, faceA) == false));
        System.out.println("Test 8a (coplanar): " + (intersects(planeLineA, planeLineB, faceA, faceB, faceC) == false));
        System.out.println("Test 8b (coplanar): " + (intersects(planeLineA, planeLineB, faceB, faceC, faceA) == false));
        System.out.println("Test 8c (coplanar): " + (intersects(planeLineA, planeLineB, faceC, faceB, faceA) == false));
        
        Vec3 faceD = new Vec3( -1.87324, -0.0091993 + 0, 2.46558 );
        Vec3 faceE = new Vec3( -0.016987, 1.24315 + 0, 1.22842 );
        Vec3 faceF = new Vec3( 1.88876, 0.015551 + 0, 2.46558 );
        Vec3 lineS = new Vec3(1.12966, 0.33059, -2.43643);        // Non intersecting line front of
        Vec3 lineT = new Vec3(1.12966, 0.33059, -2.49349);
        System.out.println("Test D (): " + (intersects(lineS, lineT, faceD, faceE, faceF) == false));
        System.out.println("Test D2 (): " + (intersects(lineS, lineT, faceF, faceD, faceE ) == false));
    }
    
	
    //
	// Function to check if a point is on the same side of the plane as the normal
	//
	public static boolean sameSide(Vec3 p1, Vec3 p2, Vec3 a, Vec3 b, Vec3 c) {
		Vec3 ab = b.minus(a); //  subtract(b, a);
        Vec3 ac = c.minus(a);  //subtract(c, a);
        Vec3 ap1 = p1.minus(a); // subtract(p1, a);
        Vec3 ap2 = p2.minus(a); // subtract(p2, a);

        Vec3 cross1 = ab.cross(ap1); // crossProduct(ab, ap1);
        Vec3 cross2 = ab.cross(ap2); //  crossProduct(ab, ap2);

		//return dotProduct(cross1, ac) * dotProduct(cross2, ac) >= 0;
        return (cross1.dot(ac) * cross2.dot(ac)) >= 0;
	}

	// Function to check if a point is inside the polygon
	public static boolean pointInPolygon(Vec3 p, Vec3 a, Vec3 b, Vec3 c) {
		return sameSide(p, a, b, c, p) && sameSide(p, b, a, c, p) && sameSide(p, c, a, b, p);
	}

	// Function to check if a line segment intersects a three-point polygon face
	public static boolean intersects(Vec3 start, Vec3 end, Vec3 a, Vec3 b, Vec3 c) {
        Vec3 direction = end.minus(start);  //  subtract(end, start);
        Vec3 ab = b.minus(a); // subtract(b, a);
        Vec3 ac = c.minus(a); //subtract(c, a);

        Vec3 pVec = direction.cross(ac); // crossProduct(direction, ac);
		double det = ab.dot(pVec); //dotProduct(ab, pVec);

		// If the determinant is close to zero, the line and the plane are parallel
		if (Math.abs(det) < EPSILON) {
			return false;
		}

		double invDet = 1.0 / det;

        Vec3 tVec = start.minus(a); //subtract(start, a);
		double u = tVec.dot(pVec) * invDet; //dotProduct(tVec, pVec) * invDet;

		// Check if u is within the valid range [0, 1]
		if (u < 0.0 || u > 1.0) {
			return false;
		}

        Vec3 qVec = tVec.cross(ab); //crossProduct(tVec, ab);
		double v = direction.dot(qVec) * invDet;  //dotProduct(direction, qVec) * invDet;

		// Check if v is within the valid range [0, 1]
		if (v < 0.0 || u + v > 1.0) {
			return false;
		}

		// Calculate the intersection point
		double t = ac.dot(qVec) * invDet;  //dotProduct(ac, qVec) * invDet;

		// Check if t is within the valid range [0, 1]
		return t >= 0.0 && t <= 1.0;
	}
    
    
    /**
     * intersection
     * Description: get the point of intersection between a line segent and polygon.
     */
    public static Vec3 getIntersection(Vec3 start, Vec3 end, Vec3 a, Vec3 b, Vec3 c){
        Vec3 intersection = new Vec3();
        
        Vec3 direction = end.minus(start);  //  subtract(end, start);
        Vec3 ab = b.minus(a); // subtract(b, a);
        Vec3 ac = c.minus(a); //subtract(c, a);

        Vec3 pVec = direction.cross(ac); // crossProduct(direction, ac);
        double det = ab.dot(pVec); //dotProduct(ab, pVec);

        // If the determinant is close to zero, the line and the plane are parallel
        if (Math.abs(det) < EPSILON) {
            return null;
        }

        double invDet = 1.0 / det;

        Vec3 tVec = start.minus(a); //subtract(start, a);
        double u = tVec.dot(pVec) * invDet; //dotProduct(tVec, pVec) * invDet;

        // Check if u is within the valid range [0, 1]
        if (u < 0.0 || u > 1.0) {
            return null;
        }

        Vec3 qVec = tVec.cross(ab); //crossProduct(tVec, ab);
        double v = direction.dot(qVec) * invDet;  //dotProduct(direction, qVec) * invDet;

        // Check if v is within the valid range [0, 1]
        if (v < 0.0 || u + v > 1.0) {
            return null;
        }

        // Calculate the intersection point
        double t = ac.dot(qVec) * invDet;  // dotProduct(ac, qVec) * invDet;
        
        // Check if t is within the valid range [0, 1]
        //return t >= 0.0 && t <= 1.0;
        if(t >= 0.0 && t <= 1.0){
            // t is percentage losition along line (start, end)
            intersection = new Vec3(direction);
            intersection = intersection.times(t);
            intersection.add(start);
        }
        
        return intersection;
    }
    
    /**
     * intersectPoint
     * Description: closest point between two line segments on the first line.
     *
     * @param: Vec3 a1 - First line point 1.
     * @param: Vec3 b2 - First line point 2.
     * @param: Vec3 b1 - Second line point 1.
     * @param: Vec3 b2 - Second line point 2.
     */
    public static Vec3 closestPoint( Vec3 a1, Vec3 a2, Vec3 b1, Vec3 b2 ){
        Vec3 result = new Vec3();
        
        Vec3 aNormal = new Vec3(a1);
        aNormal.subtract(a2);
        aNormal.normalize();
        Vec3 bNormal = new Vec3(b1);
        bNormal.subtract(b2);
        bNormal.normalize();
        
        Vec3 p1 = new Vec3(a1);
        Vec3 p2 = new Vec3(a2);
        Vec3 p3 = new Vec3(b1);
        Vec3 p4 = new Vec3(b2);
        
        //Vec3 cn = bNormal.cross(aNormal);
        //cn.normalize();
        Vec3 p43 = new Vec3(p4.x - p3.x, p4.y - p3.y, p4.z - p3.z);
        //checkArgument(!(abs(p43.x) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
        //                 abs(p43.y) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
        //                 abs(p43.z) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA), MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
        Vec3 p21 = new Vec3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
        //checkArgument(!(abs(p21.x) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
        //                 abs(p21.y) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
        //                 abs(p21.z) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA), MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
        Vec3 p13 = new Vec3(p1.x - p3.x, p1.y - p3.y, p1.z - p3.z);
        double d1343 = p13.x * p43.x + p13.y * p43.y + p13.z * p43.z;
        double d4321 = p43.x * p21.x + p43.y * p21.y + p43.z * p21.z;
        double d4343 = p43.x * p43.x + p43.y * p43.y + p43.z * p43.z;
        double d2121 = p21.x * p21.x + p21.y * p21.y + p21.z * p21.z;
        double denom = d2121 * d4343 - d4321 * d4321;
        //checkArgument(abs(denom) >= NUMBERS_SHOULD_BE_DIFFERENT_DELTA, MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
        double d1321 = p13.x * p21.x + p13.y * p21.y + p13.z * p21.z;
        double numer = d1343 * d4321 - d1321 * d4343;

        double mua = numer / denom;
        double mub = (d1343 + d4321 * mua) / d4343;

        //return new LineSegment3D(
        //     new Point3d(p1.x+mua*p21.x, p1.y+mua*p21.y, p1.z+mua*p21.z),
        //     new Point3d(p3.x+mub*p43.x, p3.y+mub*p43.y, p3.z+mub*p43.z));
        
        result = new Vec3( p1.x+mua*p21.x, p1.y+mua*p21.y, p1.z+mua*p21.z );
        return result;
    }
    
    /**
     * closestPoints
     * Description: More useful version of closestPoint, returns both closest points on each line.
     *
     *@param: Vec3
     *@param: Vec3
     *@param:
     *@param:
     */
    public static Vec3[] closestPoints( Vec3 a1, Vec3 a2, Vec3 b1, Vec3 b2 ){
        Vec3[] results = new Vec3[2];
        
        Vec3 aNormal = new Vec3(a1);
        aNormal.subtract(a2);
        aNormal.normalize();
        Vec3 bNormal = new Vec3(b1);
        bNormal.subtract(b2);
        bNormal.normalize();
        
        Vec3 p1 = new Vec3(a1);
        Vec3 p2 = new Vec3(a2);
        Vec3 p3 = new Vec3(b1);
        Vec3 p4 = new Vec3(b2);
        
        //Vec3 cn = bNormal.cross(aNormal);
        //cn.normalize();
        Vec3 p43 = new Vec3(p4.x - p3.x, p4.y - p3.y, p4.z - p3.z);
        //checkArgument(!(abs(p43.x) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
        //                 abs(p43.y) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
        //                 abs(p43.z) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA), MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
        Vec3 p21 = new Vec3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
        //checkArgument(!(abs(p21.x) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
        //                 abs(p21.y) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA &&
        //                 abs(p21.z) < NUMBERS_SHOULD_BE_DIFFERENT_DELTA), MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
        Vec3 p13 = new Vec3(p1.x - p3.x, p1.y - p3.y, p1.z - p3.z);
        double d1343 = p13.x * p43.x + p13.y * p43.y + p13.z * p43.z;
        double d4321 = p43.x * p21.x + p43.y * p21.y + p43.z * p21.z;
        double d4343 = p43.x * p43.x + p43.y * p43.y + p43.z * p43.z;
        double d2121 = p21.x * p21.x + p21.y * p21.y + p21.z * p21.z;
        double denom = d2121 * d4343 - d4321 * d4321;
        //checkArgument(abs(denom) >= NUMBERS_SHOULD_BE_DIFFERENT_DELTA, MSG_INVALID_POINTS_FOR_INTERSECTION_CALCULATION);
        double d1321 = p13.x * p21.x + p13.y * p21.y + p13.z * p21.z;
        double numer = d1343 * d4321 - d1321 * d4343;

        double mua = numer / denom;
        double mub = (d1343 + d4321 * mua) / d4343;

        //return new LineSegment3D(
        //     new Point3d(p1.x+mua*p21.x, p1.y+mua*p21.y, p1.z+mua*p21.z),
        //     new Point3d(p3.x+mub*p43.x, p3.y+mub*p43.y, p3.z+mub*p43.z));
        
        results[0] = new Vec3( p1.x+mua*p21.x, p1.y+mua*p21.y, p1.z+mua*p21.z );
        results[1] = new Vec3( p3.x+mub*p43.x, p3.y+mub*p43.y, p3.z+mub*p43.z );
        return results;
    }
    
    
    /**
     * closestPoint
     *
     * Description: Find closest point on an infinite line (a,b) to a given point (b).
     * Does include points outside range line (a/b).
     */
    public static Vec3 closestPoint( Vec3 a1, Vec3 a2, Vec3 b ){
        Vec3 result = new Vec3();
        Vec3 aNormal = new Vec3(a2);
        aNormal.subtract(a1);
        aNormal.normalize();
        Vec3 w = new Vec3(b);
        w.subtract(a1);
        result = aNormal.times( w.dot(aNormal) );
        result.add(a1);
        return result;
    }
    
    
    /**
     * closestPointToLineSegment
     *
     * Description: returns point along line segment closest to given point (b).
     * Similar to closestPoint but adds line segment bounds.
     */
    public static Vec3 closestPointToLineSegment( Vec3 a1, Vec3 a2, Vec3 b ){
        Vec3 result = new Vec3();
        Vec3 aNormal = new Vec3(a2);
        aNormal.subtract(a1);
        aNormal.normalize();
        Vec3 w = new Vec3(b);
        w.subtract(a1);
        result = aNormal.times( w.dot(aNormal) );
        result.add(a1);
        
        // Bounds check.
        double segmentLength = a1.distance(a2);
        double closestToADist = result.distance(a1);
        double closestToBDist = result.distance(a2);
        if(closestToADist > segmentLength || closestToBDist > segmentLength ){ // outside bounds.
            if(closestToADist > closestToBDist){
                result = a2;
            } else {
                result = a1;
            }
        }
        return result;
    }
    
    
    
    /**
     * closestPointToPolygon
     *
     * Description: Find the closest point on a polygon to a given point b.
     *
     * @param Vec3 a1 - first point of polygon.
     * @param Vec3 a2 - second point of polygon.
     * @param Vec3 a3 - third point of polygon.
     * @param Vec3 b - point being measured for distance to face.
     * @return Vec3 - Closest point on face of polygon to given point (b).
     */
    public static Vec3 closestPointToPolygon( Vec3 a1, Vec3 a2, Vec3 a3, Vec3 b ){
        Vec3 result = new Vec3();
        //Vec3 closestA1A2 = new Vec3( closestPoint( a1, a2, b ) );
        //Vec3 closestA2A3 = new Vec3( closestPoint( a2, a3, b ) );
        //Vec3 closestA3A1 = new Vec3( closestPoint( a3, a1, b ) );
        Vec3 closestA1A2 = new Vec3( closestPointToLineSegment( a1, a2, b ) );
        Vec3 closestA2A3 = new Vec3( closestPointToLineSegment( a2, a3, b ) );
        Vec3 closestA3A1 = new Vec3( closestPointToLineSegment( a3, a1, b ) );
        
        //Vec3 midFace = new Vec3(closestA1A2);
        //midFace.add(closestA2A3);
        //midFace.add(closestA3A1);
        //midFace.divideBy(3);
        
        //Vec3 midA1A2 = a1.midPoint(a2);
        
        Vec3 faceNormal = getFaceNormal(a1, a2, a3);
        Vec3 faceNormalizedBOffset = new Vec3(b);
        faceNormalizedBOffset.add(faceNormal);
        // The point b and faceNormalizedBOffset define a line that intersects with the face at 90 degrees.
        
        result = closestPoint( b, faceNormalizedBOffset, closestA1A2 ); //
        
        // Bounds check.
        boolean passesThrough = infiniteLinePassesThrough( b, faceNormalizedBOffset, a1, a2, a3 );
        //System.out.println("passesThrough " + passesThrough);
        if(passesThrough == false){
            // Choose closest edge from point b.
            double edgeADist = closestA1A2.distance(b);
            double edgeBDist = closestA2A3.distance(b);
            double edgeCDist = closestA3A1.distance(b);
            
            if( edgeADist <= edgeBDist && edgeADist <= edgeCDist ){
                result = closestA1A2;
            }
            if( edgeBDist <= edgeADist && edgeBDist <= edgeCDist ){
                result = closestA2A3;
            }
            if( edgeCDist <= edgeADist && edgeCDist <= edgeBDist ){
                result = closestA3A1;
            }
        }
        // distanceToFace( Vec3 pos1, Vec3 pos2, Vec3 pos3, Vec3 point ) // not implemented.
        return result;
    }
    
    
    
    public static Vec3 getFaceNormal(Vec3 a1, Vec3 a2, Vec3 a3){
        Vec3 result = new Vec3();
        Vec3 p1 = a2.minus(a1);
        Vec3 p2 = a3.minus(a1);
        Vec3 n = p1.cross(p2);
        n.normalize();
        result = n;
        return result;
    }
    
    
    /**
     * inBounds
     * Description: A compliment to closestPoint. Used to check if the boint b is within the bounds of the segment (a,b).
     * closestPoint will return the closest point along an infinite line, we want to restrict to range of line segment.
     *
     * @param: a1 - line segment start.
     * @param: a2
     * @param: b
     * @return:
     */
    public static boolean inBounds( Vec3 a1, Vec3 a2, Vec3 b ){
        boolean result = false;
        BoundingBox lineSegmentBounds = new BoundingBox(a1, a2);
        if( lineSegmentBounds.contains(b) ){
            result = true;
        }
        return result;
    }
    
    /**
     * shortDistance
     *
     * Description: Find shortest distance from line to point.
     */
    public static double shortDistance(Vec3 line_point1, Vec3 line_point2, Vec3 point)
    {
        Vec3 AB = line_point2.minus(line_point1);
        Vec3 AC = point.minus(line_point1);
        double area = (AB.cross(AC)).magnitude();
        double CD = area / AB.magnitude();
        return CD;
    }
    
    
    /**
    * intersectPoint
    *
    * Description: find point on plane
    */
    public static Vec3 intersectPoint(Vec3 rayVector, Vec3 rayPoint, Vec3 planeNormal, Vec3 planePoint) {
        //Vec3D diff = rayPoint.minus(planePoint);
        // new Vector3D(x - v.x, y - v.y, z - v.z);
        Vec3 diff = new Vec3(rayPoint.x - planePoint.x,  rayPoint.y - planePoint.y, rayPoint.z - planePoint.z);
        //double prod1 = diff.dot(planeNormal);
        double prod1 = diff.x * planeNormal.x + diff.y * planeNormal.y + diff.z * planeNormal.z;  //  x * v.x + y * v.y + z * v.z;
        //double prod2 = rayVector.dot(planeNormal);
        double prod2 = rayVector.x * planeNormal.x + rayVector.y * planeNormal.y + rayVector.z * planeNormal.z;
        double prod3 = prod1 / prod2;
        //return rayPoint.minus(rayVector.times(prod3));
        Vec3 t = new Vec3(rayVector.x * prod3, rayVector.y * prod3, rayVector.z * prod3);
        return new Vec3( rayPoint.x - t.x, rayPoint.y - t.y, rayPoint.z - t.z );
    }
    
    /**
     * distanceToFace
     *
     * Description: DEPRICATE
     * @return:
     */
    public static double distanceToFace( Vec3 pos1, Vec3 pos2, Vec3 pos3, Vec3 point ){
        double dist = -1;
        double a = pos1.y * (pos2.z - pos3.z) + pos2.y * (pos3.z - pos1.z) + pos3.y * (pos1.z - pos2.z);
        double b = pos1.z * (pos2.x - pos3.x) + pos2.z * (pos3.x - pos1.x) + pos3.z * (pos1.x - pos2.x);
        double c = pos1.x * (pos2.y - pos3.y) + pos2.x * (pos3.y - pos1.y) + pos3.x * (pos1.y - pos2.y);
        double d = -(pos1.x * (pos2.y * pos3.z - pos3.y * pos2.z) +
                pos2.x * (pos3.y * pos1.z - pos1.y * pos3.z) +
                pos3.x * (pos1.y * pos2.z - pos2.y * pos1.z));
        
        dist = Math.abs(a * point.x + b * point.y + c * point.z + d) / Math.sqrt(a * a + b * b + c * c);
        return dist;
    }
    
    
    /**
     * infiniteLinePassesThrough
     *
     * Description: determine if an infinite line passes through a polygon face.
     * similar to intersects but without start end line segment bounds constraingts.
     */
    public static boolean infiniteLinePassesThrough(Vec3 start, Vec3 end, Vec3 a, Vec3 b, Vec3 c) {
        Vec3 direction = end.minus(start);  //  subtract(end, start);
        Vec3 ab = b.minus(a); // subtract(b, a);
        Vec3 ac = c.minus(a); //subtract(c, a);

        Vec3 pVec = direction.cross(ac); // crossProduct(direction, ac);
        double det = ab.dot(pVec); //dotProduct(ab, pVec);

        // If the determinant is close to zero, the line and the plane are parallel
        if (Math.abs(det) < EPSILON) {
            return false;
        }

        double invDet = 1.0 / det;

        Vec3 tVec = start.minus(a); //subtract(start, a);
        double u = tVec.dot(pVec) * invDet; //dotProduct(tVec, pVec) * invDet;

        // Check if u is within the valid range [0, 1]
        if (u < 0.0 || u > 1.0) {
            return false;
        }

        Vec3 qVec = tVec.cross(ab); //crossProduct(tVec, ab);
        double v = direction.dot(qVec) * invDet;  //dotProduct(direction, qVec) * invDet;

        // Check if v is within the valid range [0, 1]
        if (v < 0.0 || u + v > 1.0) {
            return false;
        } else {
            return true;
        }

        // Calculate the intersection point
        //double t = ac.dot(qVec) * invDet;  //dotProduct(ac, qVec) * invDet;

        // Check if t is within the valid range [0, 1]
        //return t >= 0.0 && t <= 1.0;
    }
    
    
}



