package com.ece.dsp;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;

public class EcgProcessor {

    // 1. Synthetic ECG Generator with Realistic Morphological Waveforms & Artifacts
    public static double[] generateSyntheticEcg(int samplingRate, double duration, double noiseLevel, double bpmTarget) {
        int totalPoints = (int) (samplingRate * duration);
        double[] signal = new double[totalPoints];
        double period = 60.0 / bpmTarget; // Dynamic heartbeat interval

        for (int i = 0; i < totalPoints; i++) {
            double t = (double) i / samplingRate;
            double phase = t % period;

            // P-QRS-T Physiological Waveform Synthesis
            double pWave = 0.12 * Math.exp(-Math.pow((phase - 0.12) / 0.025, 2));
            double qWave = -0.15 * Math.exp(-Math.pow((phase - 0.22) / 0.008, 2));
            double rWave = 1.65 * Math.exp(-Math.pow((phase - 0.24) / 0.009, 2));
            double sWave = -0.35 * Math.exp(-Math.pow((phase - 0.26) / 0.012, 2));
            double tWave = 0.28 * Math.exp(-Math.pow((phase - 0.42) / 0.040, 2));

            double cleanECG = pWave + qWave + rWave + sWave + tWave;

            // Noise Interference: 50Hz Grid Noise + Low-Freq Respiratory Baseline Wander
            double powerlineNoise = noiseLevel * Math.sin(2 * Math.PI * 50 * t);
            double baselineWander = 0.20 * Math.sin(2 * Math.PI * 0.3 * t);

            signal[i] = cleanECG + powerlineNoise + baselineWander;
        }
        return signal;
    }

    // 2. 2nd-Order Digital Butterworth Low-Pass Filter
    public static double[] applyButterworthLowPass(double[] input, double fs, double fc) {
        double[] output = new double[input.length];
        double c = Math.tan(Math.PI * fc / fs);
        double a0 = 1.0 + Math.SQRT2 * c + c * c;
        double b0 = (c * c) / a0;
        double b1 = 2.0 * b0;
        double b2 = b0;
        double a1 = (2.0 * (c * c - 1.0)) / a0;
        double a2 = (1.0 - Math.SQRT2 * c + c * c) / a0;

        for (int i = 2; i < input.length; i++) {
            output[i] = b0 * input[i] + b1 * input[i - 1] + b2 * input[i - 2]
                      - a1 * output[i - 1] - a2 * output[i - 2];
        }
        return output;
    }

    // 3. 50Hz Narrow Band-Stop Notch Filter
    public static double[] applyNotchFilter(double[] input, double fs, double f0) {
        double[] output = new double[input.length];
        double w0 = 2 * Math.PI * f0 / fs;
        double bw = 2.0; // Bandwidth
        double r = 1.0 - (Math.PI * bw / fs);

        double b0 = 1.0;
        double b1 = -2.0 * Math.cos(w0);
        double b2 = 1.0;
        double a1 = -2.0 * r * Math.cos(w0);
        double a2 = r * r;

        for (int i = 2; i < input.length; i++) {
            output[i] = b0 * input[i] + b1 * input[i - 1] + b2 * input[i - 2]
                      - a1 * output[i - 1] - a2 * output[i - 2];
        }
        return output;
    }

    // 4. Pan-Tompkins QRS Complex Peak Detection Algorithm
    public static List<Integer> panTompkinsPeakDetection(double[] filteredSignal, int fs) {
        int n = filteredSignal.length;
        
        // Step A: 5-Point Derivative Filter
        double[] deriv = new double[n];
        for (int i = 2; i < n - 2; i++) {
            deriv[i] = (2 * filteredSignal[i + 1] + filteredSignal[i + 2] 
                      - 2 * filteredSignal[i - 1] - filteredSignal[i - 2]) * (fs / 8.0);
        }

        // Step B: Signal Squaring
        double[] squared = new double[n];
        for (int i = 0; i < n; i++) {
            squared[i] = deriv[i] * deriv[i];
        }

        // Step C: Moving Window Integration (~150ms window)
        int window = (int) (0.15 * fs);
        double[] integrated = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            int count = 0;
            for (int j = Math.max(0, i - window); j <= i; j++) {
                sum += squared[j];
                count++;
            }
            integrated[i] = sum / count;
        }

        // Step D: Dynamic Adaptive Thresholding
        List<Integer> peaks = new ArrayList<>();
        double maxVal = 0;
        for (double v : integrated) if (v > maxVal) maxVal = v;
        double threshold = maxVal * 0.35;

        int minDistance = (int) (0.35 * fs); // Refractory period limit
        int lastPeak = -minDistance;

        for (int i = 1; i < n - 1; i++) {
            if (integrated[i] > threshold && integrated[i] > integrated[i - 1] && integrated[i] > integrated[i + 1]) {
                if (i - lastPeak > minDistance) {
                    peaks.add(i);
                    lastPeak = i;
                }
            }
        }
        return peaks;
    }

    // 5. Clinical Metric: Heart Rate Variability (HRV - RMSSD)
    public static double calculateHRV(List<Integer> peaks, int fs) {
        if (peaks.size() < 2) return 0.0;
        
        List<Double> rrIntervalsMs = new ArrayList<>();
        for (int i = 1; i < peaks.size(); i++) {
            double intervalMs = ((double) (peaks.get(i) - peaks.get(i - 1)) / fs) * 1000.0;
            rrIntervalsMs.add(intervalMs);
        }

        double sumSuccessiveDiffs = 0.0;
        for (int i = 1; i < rrIntervalsMs.size(); i++) {
            double diff = rrIntervalsMs.get(i) - rrIntervalsMs.get(i - 1);
            sumSuccessiveDiffs += diff * diff;
        }

        return Math.sqrt(sumSuccessiveDiffs / (rrIntervalsMs.size() - 1));
    }

    // 6. Fast Fourier Transform (FFT) Power Spectrum Computation
    public static double[] computeFFTSpectrum(double[] signal) {
        int nextPowerOfTwo = 1;
        while (nextPowerOfTwo < signal.length) {
            nextPowerOfTwo <<= 1;
        }

        double[] padded = new double[nextPowerOfTwo];
        System.arraycopy(signal, 0, padded, 0, signal.length);

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] complexTransformed = transformer.transform(padded, TransformType.FORWARD);

        int spectrumSize = nextPowerOfTwo / 2;
        double[] spectrum = new double[spectrumSize];
        for (int i = 0; i < spectrumSize; i++) {
            spectrum[i] = complexTransformed[i].abs();
        }
        return spectrum;
    }
}
