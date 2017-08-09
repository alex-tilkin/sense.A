package com.exampledemo.parsaniahardik.gpsdemocode;

import com.exampledemo.parsaniahardik.jama.Matrix;
import com.exampledemo.parsaniahardik.jkalman.JKalman;

public class KalmanGPS {

    //region Fields
    private JKalman m_Kalman;
    private Matrix m_StateMatrix; // state [x, y, dx, dy, dxy]
    private Matrix m_CorrectionMatrix; // corrected state [x, y, dx, dy, dxy]
    private Matrix m_MeasurementMatrix; // measurement [x]
    private int samplingThreshold = 100;
    private int sampleCnt = 0;
    //endregion

    //region API
    public KalmanGPS() throws Exception{
        double dx, dy;

        m_Kalman = new JKalman(4, 2);

        // constant velocity
        dx = 0.2;
        dy = 0.2;

        m_StateMatrix = new Matrix(4, 1); // state [x, y, dx, dy, dxy]
        m_CorrectionMatrix = new Matrix(4, 1); // corrected state [x, y, dx, dy, dxy]

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

    public void push(double longitude,double latitude) throws Exception{
        m_MeasurementMatrix.set(0, 0, longitude);
        m_MeasurementMatrix.set(1, 0, latitude);

        m_StateMatrix = m_Kalman.Predict();
        m_CorrectionMatrix = m_Kalman.Correct(m_MeasurementMatrix);
    }

    public double[] getCoordinate() throws Exception {
        return sampleCnt++ < samplingThreshold ? getMeasuredPosition() : getCorrectedPosition();
    }
    //endregion

    //region Helpers
    private double[] getCorrectedPosition() throws Exception{
        double[] coordinate = new double[2];

        coordinate[0] = m_CorrectionMatrix.get(0,0);
        coordinate[1] = m_CorrectionMatrix.get(1,0);

        return coordinate;
    }

    private double[] getMeasuredPosition() throws Exception{
        double[] point = new double[2];
        point[0] = m_MeasurementMatrix.get(0,0);
        point[1] = m_MeasurementMatrix.get(1,0);

        return point;
    }

    private double[] getPredictedPosition() throws Exception{
        double[] point = new double[2];
        point[0] = m_StateMatrix.get(0,0);
        point[1] = m_StateMatrix.get(1,0);

        return point;
    }
    //endregion
}