package com.devil.turnpage;

import android.graphics.PointF;

public class GeometryUtil {
	/**
	 * Description : 求解直线P1P2和直线P3P4的交点坐标
	 */
	public static PointF getCross(PointF p1, PointF p2, PointF p3, PointF p4) {
		float dx12 = p2.x - p1.x;
		float dx34 = p4.x - p3.x;
		if (dx12 == 0 && dx34 == 0) {
			throw new ArithmeticException("two parallel line has not cross");
		}

		float k1 = 0, b1 = 0, k2 = 0, b2 = 0;
		PointF crossp = new PointF();

		if (dx12 != 0) {
			k1 = (p2.y - p1.y) / dx12;
			b1 = ((p2.x * p1.y) - (p1.x * p2.y)) / dx12;
		}
		if (dx34 != 0) {
			k2 = (p4.y - p3.y) / dx34;
			b2 = ((p4.x * p3.y) - (p3.x * p4.y)) / dx34;
		}

		if (dx12 == 0) {
			crossp.x = p1.x;
			crossp.y = k2 * crossp.x + b2;
		} else if (dx34 == 0) {
			crossp.x = p3.x;
			crossp.y = k1 * crossp.x + b1;
		} else {
			crossp.x = (b2 - b1) / (k1 - k2);
			crossp.y = k1 * crossp.x + b1;
		}
		return crossp;
	}
}
