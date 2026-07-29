package com.ece.dsp;

import java.util.ArrayList;
import java.util.List;

public class EcgProcessor {

    // Generates P-QRS-T complex wave with powerline noise
    public static double[] generateSyntheticEcg(int samplingRate, double duration, double noiseLevel) {
        int totalPoints = (int) (samplingRate * duration);
        double[] signal = new double[totalPoints];

        for (int i = 0; i < totalPoints; i++) {
            double t = (double) i / samplingRate;
            double pQrsT = 0.1 * Math.sin(2 * Math.PI * 1.2 * t)
                    + 1.5 * Math.exp(-Math.pow((t % 0.8) - 0.2, 2) / 0.001)
                    - 0.3 * Math.exp(-Math.pow((t % 0.8) - 0.18, 2) / 0.0005)
                    + 0.4 * Math.exp(-Math.pow((t % 0.8) - 0.35, 2) / 0.005);
            
            double noise = noiseLevel * Math.sin(2 * Math.PI * 50 * t);
            signal[i] = pQrsT + noise;
        }
        return signal;
    }

    // Digital Low-Pass Moving Average Filter
    public static double[] applyFilter(double[] signal, int windowSize) {
        double[] filtered = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            double sum = 0.0;
            int count = 0;
            for (int j = Math.max(0, i - windowSize); j <= Math.min(signal.length - 1, i + windowSize); j++) {
                sum += signal[j];
                count++;
            }
            filtered[i] = sum / count;
        }
        return filtered;
    }

    // R-Peak detection
    public static List<Integer> detectPeaks(double[] signal, double threshold) {
        List<Integer> peaks = new ArrayList<>();
        for (int i = 1; i < signal.length - 1; i++) {
            if (signal[i] > threshold && signal[i] > signal[i - 1] && signal[i] > signal[i + 1]) {
                peaks.add(i);
            }
        }
        return peaks;
    }
}
