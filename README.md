# 🫀 CardioPulse DSP | Enterprise Biomedical Signal Engine

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Containerized-blue.svg)](https://www.docker.com/)

CardioPulse DSP is a full-stack, cloud-deployed **Java Biomedical Signal Processing Engine**. It synthesizes real-time ECG (Electrocardiogram) telemetry corrupted by environmental electrical grid noise and physiological movement, processes it through digital filter cascades, and performs high-accuracy QRS detection and spectral analytics.

---

## 📐 System Architecture & DSP Pipeline

```mermaid
graph TD
    %% Nodes
    A[Noisy ECG Telemetry Generator] -->|50Hz Grid Noise + Baseline Wander| B[2nd-Order IIR Butterworth LPF]
    B -->|High Frequencies Attenuated| C[50Hz Narrow Band-Stop Notch Filter]
    
    subgraph Pan-Tompkins QRS Detection Engine
        C --> D[5-Point Derivative Filter]
        D --> E[Signal Squaring Unit]
        E --> F[Moving Window Integration]
        F --> G[Adaptive Dynamic Thresholding]
    end
    
    G --> H[R-Peak Indices & Timestamps]
    H --> I[BPM & Heart Rate Variability Calculation]
    C --> J[Fast Fourier Transform Spectrum]

    %% Styling
    classDef default fill:#151c28,stroke:#3b82f6,stroke-width:2px,color:#fff;
    classDef highlight fill:#10b981,stroke:#059669,stroke-width:2px,color:#fff;
    class H,I,J highlight;
