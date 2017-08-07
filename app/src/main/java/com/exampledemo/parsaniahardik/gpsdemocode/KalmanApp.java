package com.exampledemo.parsaniahardik.gpsdemocode;

import com.exampledemo.parsaniahardik.jama.Matrix;
import com.exampledemo.parsaniahardik.jkalman.JKalman;

import org.osmdroid.util.GeoPoint;

/**
 * Created by Alex on 07/08/2017.
 */

public class KalmanApp{
    private JKalman m_JKalman;
    private double m_X = -1;
    private double m_Y = -1;

    // Use Accelerometer
    private double m_Dx = 0;
    private double m_Dy = 0;

    private Matrix m_S = new Matrix(4, 1); // state [x, y, dx, dy, dxy]
    private Matrix m_C = new Matrix(4, 1); // corrected state [x, y, dx, dy, dxy]
    private Matrix m = new Matrix(2, 1); // measurement [x]

    // transitions for x, y, dx, dy
    private double[][] m_Tr = { {1, 0, 1, 0},
            {0, 1, 0, 1},
            {0, 0, 1, 0},
            {0, 0, 0, 1} };

    public KalmanApp() throws Exception {
        m_JKalman = new JKalman(4, 2);
        m.set(0, 0, m_X);
        m.set(1, 0, m_Y);
        m_JKalman.setTransition_matrix(new Matrix(m_Tr));
        m_JKalman.setError_cov_post(m_JKalman.getError_cov_post().identity());
    }

    public GeoPoint Evaluate(GeoPoint gPt) {
        // First time only
        if(m_X == -1 && m_Y == -1)
        {
            m_X = gPt.getLongitude();
            m_Y = gPt.getLatitude();

            return gPt;
        }
        m_S =  m_JKalman.Predict();
        m.set(0, 0, m.get(0, 0) + m_Dx + gPt.getLongitude());
        m.set(1, 0, m.get(1, 0) + m_Dy + gPt.getLatitude());
        m_C = m_JKalman.Correct(m);

        /*
        m_C.get(0, 0); // X
        m_C.get(1, 0); // Y
        m_C.get(2, 0); // DX
        m_C.get(3, 0); // DY
        */

        return new GeoPoint(m_C.get(0, 0), m_C.get(1, 0));
    }
}
