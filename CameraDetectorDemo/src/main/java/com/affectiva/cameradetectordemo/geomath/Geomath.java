package com.affectiva.cameradetectordemo.geomath;

import android.graphics.Point;
import android.graphics.PointF;

/**
 * Created by brucexia on 2016-12-20.
 */

public class Geomath {
    public static PointF centroid(PointF[] points) {
        float centroidX = 0, centroidY = 0;

        for (PointF knot : points) {
            centroidX += knot.x;
            centroidY += knot.y;
        }
        return new PointF(centroidX / points.length, centroidY / points.length);
    }

}
