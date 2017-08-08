package com.exampledemo.parsaniahardik.gpsdemocode;

import com.exampledemo.parsaniahardik.jama.Matrix;
import com.exampledemo.parsaniahardik.jkalman.JKalman;

public class KalmanGPS {
    private JKalman m_Kalman;
    private Matrix m_StateMatrix; // state [x, y, dx, dy, dxy]
    private Matrix m_CorrectedMatrix; // corrected state [x, y, dx, dy, dxy]
    private Matrix m_MeasurementMatrix; // measurement [x]

    public KalmanGPS() throws Exception{
        double dx, dy;

        m_Kalman = new JKalman(4, 2);

        // constant velocity
        dx = 0.2;
        dy = 0.2;

        m_StateMatrix = new Matrix(4, 1); // state [x, y, dx, dy, dxy]
        m_CorrectedMatrix = new Matrix(4, 1); // corrected state [x, y, dx, dy, dxy]

        m_MeasurementMatrix = new Matrix(2, 1); // measurement [x]
        m_MeasurementMatrix.set(0, 0, 0);
        m_MeasurementMatrix.set(1, 0, 0);

        // transitions for   x, y, dx, dy
        double[][] tr = {   {1, 0, dx, 0},
                            {0, 1, 0, dy},
                            {0, 0, 1, 0},
                            {0, 0, 0, 1} };

        m_Kalman.setTransition_matrix(new Matrix(tr));
        m_Kalman.setError_cov_post(m_Kalman.getError_cov_post().identity());
    }

    public void push(double x,double y) throws Exception{
        m_MeasurementMatrix.set(0, 0, x);
        m_MeasurementMatrix.set(1, 0, y);

        m_CorrectedMatrix = m_Kalman.Correct(m_MeasurementMatrix);
        m_StateMatrix = m_Kalman.Predict();
    }

    public double[] getPostion() throws Exception{
        double[] coordinate = new double[2];

        coordinate[0] = m_CorrectedMatrix.get(0,0);
        coordinate[1] = m_CorrectedMatrix.get(1,0);

        return coordinate;
    }

    public double[] getPrediction() throws Exception{
        double[] point = new double[2];
        point[0] = m_StateMatrix.get(0,0);
        point[1] = m_StateMatrix.get(1,0);

        return point;
    }
}