package com.hsg.coffee.domain.tasteSpace.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.springframework.stereotype.Component;

@Component
public class PcaProjectionService {

    public double[][] project(List<double[]> vectors) {
        int count = vectors.size();
        if (count == 0) {
            return new double[0][3];
        }
        if (count == 1) {
            return new double[][]{{0, 0, 0}};
        }

        int dimensions = vectors.getFirst().length;
        double[] mean = new double[dimensions];
        for (double[] vector : vectors) {
            for (int column = 0; column < dimensions; column++) {
                mean[column] += vector[column] / count;
            }
        }

        double[][] centered = new double[count][dimensions];
        for (int row = 0; row < count; row++) {
            for (int column = 0; column < dimensions; column++) {
                centered[row][column] = vectors.get(row)[column] - mean[column];
            }
        }

        RealMatrix matrix = new Array2DRowRealMatrix(centered, false);
        RealMatrix gram = matrix.multiply(matrix.transpose());
        EigenDecomposition decomposition = new EigenDecomposition(gram);
        Integer[] eigenIndices = IntStream.range(0, count).boxed()
                .sorted(Comparator.comparingDouble((Integer index) -> decomposition.getRealEigenvalue(index)).reversed())
                .toArray(Integer[]::new);

        double[][] coordinates = new double[count][3];
        for (int axis = 0; axis < Math.min(3, count); axis++) {
            int eigenIndex = eigenIndices[axis];
            double scale = Math.sqrt(Math.max(0, decomposition.getRealEigenvalue(eigenIndex)));
            double[] eigenVector = decomposition.getEigenvector(eigenIndex).toArray();
            for (int row = 0; row < count; row++) {
                coordinates[row][axis] = eigenVector[row] * scale;
            }
        }
        normalize(coordinates);
        return coordinates;
    }

    private void normalize(double[][] coordinates) {
        double maximum = Arrays.stream(coordinates)
                .flatMapToDouble(Arrays::stream)
                .map(Math::abs)
                .max()
                .orElse(1);
        if (maximum < 1e-9) {
            for (int index = 0; index < coordinates.length; index++) {
                double angle = (Math.PI * 2 * index) / coordinates.length;
                coordinates[index][0] = Math.cos(angle);
                coordinates[index][1] = Math.sin(angle);
            }
            return;
        }
        for (double[] coordinate : coordinates) {
            for (int axis = 0; axis < 3; axis++) {
                coordinate[axis] /= maximum;
            }
        }
    }
}
