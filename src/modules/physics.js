// src/modules/physics.js

/**
 * Split-Operator Fourier Method
 * Time-Dependent Schrödinger Equation (TDSE) Integrator
 * 
 * Provides a physics substrate for executing wave-based manifold simulations.
 */
export class TDSEIntegrator {
  /**
   * @param {number} gridSize - Number of spatial grid points
   * @param {number} dx - Spatial step size
   * @param {number} dt - Time step size
   * @param {number} mass - Particle mass
   */
  constructor(gridSize, dx, dt, mass = 1.0) {
    this.N = gridSize;
    this.dx = dx;
    this.dt = dt;
    this.mass = mass;
    
    this.x = new Float64Array(this.N);
    this.k = new Float64Array(this.N);
    this.V = new Float64Array(this.N);
    
    // Complex wavefunction [re, im, re, im, ...]
    this.psi = new Float64Array(this.N * 2);
    
    // Propagators
    this.expV = new Float64Array(this.N * 2); 
    this.expT = new Float64Array(this.N * 2);
    
    this._initializeGrid();
  }

  _initializeGrid() {
    const L = this.N * this.dx;
    for (let i = 0; i < this.N; i++) {
        // Centered spatial grid
        this.x[i] = (i - this.N / 2) * this.dx;
        
        // Momentum grid
        let n = i < this.N / 2 ? i : i - this.N;
        this.k[i] = (2.0 * Math.PI / L) * n;
        
        // Kinetic propagator: exp(-i * (k^2 / 2m) * dt)
        const kineticEnergy = (this.k[i] * this.k[i]) / (2.0 * this.mass);
        const phaseT = -kineticEnergy * this.dt;
        this.expT[i * 2] = Math.cos(phaseT);
        this.expT[i * 2 + 1] = Math.sin(phaseT);
    }
  }

  /**
   * Set the potential energy landscape
   * @param {Function} potentialFn - A function V(x) mapping position to potential energy
   */
  setPotential(potentialFn) {
    for (let i = 0; i < this.N; i++) {
        this.V[i] = potentialFn(this.x[i]);
        
        // Potential propagator: exp(-i * V * dt / 2)
        const phaseV = -this.V[i] * this.dt / 2.0;
        this.expV[i * 2] = Math.cos(phaseV);
        this.expV[i * 2 + 1] = Math.sin(phaseV);
    }
  }

  /**
   * Initializes the wavefunction
   * @param {Function} realFn - Real part initialization function
   * @param {Function} [imagFn] - Imaginary part initialization function
   */
  setWavefunction(realFn, imagFn = () => 0) {
    let norm = 0;
    for (let i = 0; i < this.N; i++) {
        const re = realFn(this.x[i]);
        const im = imagFn(this.x[i]);
        this.psi[i * 2] = re;
        this.psi[i * 2 + 1] = im;
        norm += (re * re + im * im) * this.dx;
    }
    // Normalize to satisfy probability interpretation
    norm = Math.sqrt(norm);
    for (let i = 0; i < this.N * 2; i++) {
        this.psi[i] /= norm;
    }
  }

  /**
   * Discrete Fourier Transform
   * (Naive O(N^2) implementation for standalone simplicity. Replace with FFT for production scale.)
   * @param {number} direction - 1 for forward, -1 for inverse
   */
  _dft(direction) {
    const out = new Float64Array(this.N * 2);
    const theta0 = direction * 2 * Math.PI / this.N;
    
    for (let k = 0; k < this.N; k++) {
        let re = 0;
        let im = 0;
        for (let n = 0; n < this.N; n++) {
            const theta = theta0 * k * n;
            const cosT = Math.cos(theta);
            const sinT = Math.sin(theta);
            const inRe = this.psi[n * 2];
            const inIm = this.psi[n * 2 + 1];
            
            re += inRe * cosT - inIm * sinT;
            im += inRe * sinT + inIm * cosT;
        }
        out[k * 2] = re;
        out[k * 2 + 1] = im;
    }
    
    if (direction === -1) {
        for (let i = 0; i < this.N * 2; i++) {
            out[i] /= this.N;
        }
    }
    this.psi = out;
  }

  _applyPropagator(prop) {
    for (let i = 0; i < this.N; i++) {
        const psiRe = this.psi[i * 2];
        const psiIm = this.psi[i * 2 + 1];
        const pRe = prop[i * 2];
        const pIm = prop[i * 2 + 1];
        
        this.psi[i * 2] = psiRe * pRe - psiIm * pIm;
        this.psi[i * 2 + 1] = psiRe * pIm + psiIm * pRe;
    }
  }

  /**
   * Steps the simulation forward by one dt interval using the split-operator method.
   */
  step() {
    // 1. Half-step in position space
    this._applyPropagator(this.expV);
    
    // 2. Transform to momentum space
    this._dft(1);
    
    // 3. Full-step in momentum space
    this._applyPropagator(this.expT);
    
    // 4. Transform back to position space
    this._dft(-1);
    
    // 5. Half-step in position space
    this._applyPropagator(this.expV);
  }
  
  /**
   * Retrieves the current probability density function |Psi(x,t)|^2
   * @returns {Float64Array}
   */
  getProbabilityDensity() {
    const prob = new Float64Array(this.N);
    for (let i = 0; i < this.N; i++) {
        const re = this.psi[i * 2];
        const im = this.psi[i * 2 + 1];
        prob[i] = re * re + im * im;
    }
    return prob;
  }
}
