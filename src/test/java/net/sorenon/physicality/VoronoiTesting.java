package net.sorenon.physicality;

import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.waveware.delaunator.DPoint;
import org.waveware.delaunator.Delaunator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class VoronoiTesting {
    public static void main(String[] strings) throws IOException {
//        PerlinSimplexNoise perlinSimplexNoise = new PerlinSimplexNoise(new XoroshiroRandomSource(2), ImmutableList.of(0));

        var image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

        var random = new XoroshiroRandomSource(4);
        float jitter = 0.5f;

        var dPointList = new ArrayList<DPoint>();

        for (int x = 0; x < 25; x++) {
            for (int y = 0; y < 25; y++) {
                dPointList.add(new DPoint(x + jitter * (random.nextFloat() - random.nextFloat()), y + jitter * (random.nextFloat() - random.nextFloat())));
            }
        }

        Delaunator delaunator = new Delaunator(dPointList);

        int numTriangles = delaunator.halfedges.length / 3;
        var centroids = new DPoint[numTriangles];
        for (int t = 0; t < numTriangles; t++) {
            double sumOfX = 0;
            double sumOfY = 0;
            for (int i = 0; i < 3; i++) {
                var point = dPointList.get(3 * t + i);
                sumOfX += point.x;
                sumOfY += point.y;
            }
            centroids[t] = new DPoint(sumOfX, sumOfY);
        }



        for (var point : dPointList) {
            int xOut = (int) Math.round((point.x / 24f) * 199f);
            int yOut = (int) Math.round((point.y / 24f) * 199f);

            if (xOut >= 0 && xOut < 200 && yOut >= 0 && yOut < 200) {
                image.setRGB(xOut, yOut, Color.WHITE.getRGB());
            }
        }

        ImageIO.write(image, "png", new File("saved.png"));
    }
}