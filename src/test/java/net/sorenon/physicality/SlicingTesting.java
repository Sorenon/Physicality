package net.sorenon.physicality;


import com.google.common.collect.ImmutableList;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import net.sorenon.physicality.mesh.ConvexHull;

public class SlicingTesting {

    public static void main(String[] args) {
        Plane plane = new Plane(new Vector3f(0, 1, 0), 0);

        ConvexHull hull = new ConvexHull(ImmutableList.of(new Vector3f(0, 505, 0), new Vector3f(0, 0.4f, 3), new Vector3f(0, -2, 3)));

        ConvexHull split = hull.slice(plane, 0.1f);

        int i = 0;
    }
}
