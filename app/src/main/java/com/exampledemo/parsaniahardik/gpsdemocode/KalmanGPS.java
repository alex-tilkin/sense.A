package com.exampledemo.parsaniahardik.gpsdemocode;

import com.exampledemo.parsaniahardik.jama.Matrix;
import com.exampledemo.parsaniahardik.jkalman.JKalman;

public class KalmanGPS {

    //region Fields
    private JKalman m_Kalman = null;
    private Matrix m_StateVector;
    private Matrix m_CorrectionVector;
    private Matrix m_MeasurementVector;
    private int samplingThreshold = 50;
    private int sampleCnt = 0;
    //endregion

    //region API
    public KalmanGPS() throws Exception{
        m_Kalman = new JKalman(4, 2);

        m_StateVector = new Matrix(4, 1); // [x, y, dx, dy]
        m_CorrectionVector = new Matrix(4, 1); // [x, y, dx, dy]

        m_MeasurementVector = new Matrix(2, 1); //  [x]
        m_MeasurementVector.set(0, 0, 0);
        m_MeasurementVector.set(1, 0, 0);

        // transitions for x, y, dx, dy
        m_Kalman.setTransition_matrix(new Matrix(new double[][]{{1, 0, 1, 0},
                                                                {0, 1, 0, 1},
                                                                {0, 0, 1, 0},
                                                                {0, 0, 0, 1} }));

        m_Kalman.setError_cov_post(m_Kalman.getError_cov_post().identity());
        m_Kalman.setState_post(m_Kalman.getState_post());
    }

    public void push(double longitude,double latitude) throws Exception{
        m_MeasurementVector.set(0, 0, longitude);
        m_MeasurementVector.set(1, 0, latitude);

        m_CorrectionVector = m_Kalman.Correct(m_MeasurementVector);
        m_StateVector = m_Kalman.Predict();
    }

    public double[] getCoordinate() throws Exception {
        return sampleCnt++ < samplingThreshold ? getMeasuredPosition() : getCorrectedPosition();
    }
    //endregion

    //region Helpers
    private double[] getCorrectedPosition() throws Exception{
        double[] coordinate = new double[2];

        coordinate[0] = m_CorrectionVector.get(0,0);
        coordinate[1] = m_CorrectionVector.get(1,0);

        return coordinate;
    }

    private double[] getMeasuredPosition() throws Exception{
        double[] point = new double[2];
        point[0] = m_MeasurementVector.get(0,0);
        point[1] = m_MeasurementVector.get(1,0);

        return point;
    }

    private double[] getPredictedPosition() throws Exception{
        double[] point = new double[2];
        point[0] = m_StateVector.get(0,0);
        point[1] = m_StateVector.get(1,0);

        return point;
    }
    //endregion
}