/**
 * Intersect
 * This code fails unit tests. Replaced with Intersect2
 */

package armarender;

import armarender.object.*;
import armarender.math.*;


public class Intersect {

	public Intersect(){
        
        // Vec3() double x, y, z;
        // public boolean intersects(Vec3 p0, Vec3 p1, Vec3 f1, Vec3 f2, Vec3 f3);

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
    
    
    /**
     * intersects
     *
     */
    public static boolean intersects(Vec3 p0, Vec3 p1, Vec3 f1, Vec3 f2, Vec3 f3) {
        // Normal of line
        Vec3 lineNormal = new Vec3(p0.x - p1.x, p0.y - p1.y, p0.z - p1.z);
        // Normal of Face
        Vec3 faceNormal = normal(f1, f2, f3);
        // Normal of a point of Line and point of Face
        Vec3 ap = new Vec3(f1.x - p0.x, f1.y - p0.y, f1.z - p0.z);

        double t = dotProduct(ap, faceNormal) / dotProduct(lineNormal, faceNormal);

        Vec3 intersectionPoint = new Vec3(p0.x + t * lineNormal.x, p0.y + t * lineNormal.y, p0.z + t * lineNormal.z);

        if(dotProduct(lineNormal,faceNormal)==0.0){
            // Test if Line is perpendicular to Face's Normal
            return false;
        } else{
            return isPointInPolygon(intersectionPoint, f1, f2, f3) && isPointOnLine(p0,p1,intersectionPoint);
        }
    }

    /**
     * normal
     *
     * Desciption:
     */
    public static Vec3 normal(Vec3 vectorA, Vec3 vectorB, Vec3 vectorC){
        Vec3 v1 = new Vec3(vectorB.x - vectorA.x, vectorB.y - vectorA.y, vectorB.z - vectorA.z);
        Vec3 v2 = new Vec3(vectorC.x - vectorA.x, vectorC.y - vectorA.y, vectorC.z - vectorA.z);

        double x = v1.y * v2.z - v1.z * v2.y;
        double y = v1.z * v2.x - v1.x * v2.z;
        double z = v1.x * v2.y - v1.y * v2.x;

        return new Vec3(x,y,z);
    }

    /**
     * dotProduct
     *
     */
    public static double dotProduct(Vec3 vector1, Vec3 vector2){
        double result = (vector1.x * vector2.x) + (vector1.y * vector2.y) + (vector1.z * vector2.z);
        return result;
    }

    /**
     * isPointInPolygon
     *
     */
    public static boolean isPointInPolygon(Vec3 intersectPoint, Vec3 faceA, Vec3 faceB, Vec3 faceC){
        Vec3 v1 = new Vec3(faceB.x - faceA.x, faceB.y - faceA.y, faceB.z - faceA.z);
        Vec3 v2 = new Vec3(faceC.x - faceA.x, faceC.y - faceA.y, faceC.z - faceA.z);
        Vec3 v3 = new Vec3(intersectPoint.x - faceA.x, intersectPoint.y - faceA.y, intersectPoint.z - faceA.z);

        double dot00 = dotProduct(v1, v1);
        double dot01 = dotProduct(v1, v2);
        double dot02 = dotProduct(v1, v3);
        double dot11 = dotProduct(v2, v2);
        double dot12 = dotProduct(v2, v3);

        double invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
        double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return (u >= 0) && (v >= 0) && (u + v < 1);
    }

    /**
     * isPointOnLine
     *
     */
    public static boolean isPointOnLine(Vec3 lineA, Vec3 lineB, Vec3 testPoint){
        boolean isTestPointWithin;

        if(lineA.x != lineB.x){
            isTestPointWithin = isWithin(lineA.x, testPoint.x, lineB.x);
        } else {
            isTestPointWithin = isWithin(lineA.y, testPoint.y, lineB.y);
        }

        return isTestPointWithin;
    }

    /**
     * isWithin
     *
     */
    public static boolean isWithin(double min, double test, double max){
        return (min <= test && test <= max) || (max <= test && test <= min);
    }
    
}
