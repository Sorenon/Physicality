package net.sorenon.physicality.mesh;

import com.jme3.math.Plane;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

public record ConvexHull(List<Vector3f> points) {

    public ConvexHull slice(Plane clippingPlane, float epsilon) {
        Vector3f firstVertex = points.get(points.size() - 1);
        Vector3f endVertex;

        List<Vector3f> out = new ArrayList<>(points.size());

        float denomFirst = clippingPlane.getNormal().dot(firstVertex) + clippingPlane.getConstant() - epsilon;

        for (Vector3f point : points) {
            endVertex = point;

            float denomEnd = clippingPlane.getNormal().dot(endVertex) + clippingPlane.getConstant() - epsilon;

            if (denomFirst > 0) {
                if (denomEnd > 0) {
                    out.add(endVertex);
                } else {
                    out.add(lerp(firstVertex, endVertex, denomFirst / (denomFirst - denomEnd)));
                }
            } else {
                if (denomEnd > 0) {
                    out.add(lerp(firstVertex, endVertex, denomFirst / (denomFirst - denomEnd)));
                    out.add(endVertex);
                }
            }
            firstVertex = endVertex;
            denomFirst = denomEnd;
        }

        return new ConvexHull(out);
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        var result = new Vector3f();
        b.subtract(a, result);
        result.multLocal(t);

        return a.add(result, result);
    }

//    public static boolean isInFrontOfPlane(Vector3f point, Plane clippingPlane) {
//        return clippingPlane.getNormal().dot(point) + clippingPlane.getConstant() > 0;
//    }

    private double intersectRayPlane(Vector3f origin, Vector3f dir, Vector3f point, Vector3f normal) {
        float epsilon = 0.1f;

        float denom = normal.x * dir.x + normal.y * dir.y + normal.z * dir.z;
        if (denom > epsilon) {
            return -1.0;
        }
        double t = ((point.x - origin.x) * normal.x + (point.y - origin.y) * normal.y + (point.z - origin.z) * origin.z) / denom;
        if (t >= 0.0)
            return t;
        else
            return -1.0;
    }
}
