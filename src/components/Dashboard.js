// src/components/Dashboard.js
import { kernel } from '../kernel.js';

export class KernelDashboard extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: 'open' });
        
        const style = document.createElement('style');
        style.textContent = `
            :host {
                display: block;
                font-family: 'Inter', system-ui, sans-serif;
                background: rgba(15, 23, 42, 0.9);
                color: #e2e8f0;
                padding: 1.5rem;
                border-radius: 12px;
                border: 1px solid rgba(255, 255, 255, 0.1);
                box-shadow: 0 4px 30px rgba(0, 0, 0, 0.5);
                backdrop-filter: blur(10px);
            }
            h2 { 
                margin-top: 0; 
                color: #00e5ff; 
                font-weight: 600;
                letter-spacing: 1px;
                text-transform: uppercase;
                font-size: 1rem;
            }
            .grid {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 1.5rem;
            }
            .panel {
                background: #1e293b;
                padding: 1rem;
                border-radius: 8px;
                border: 1px solid #334155;
                display: flex;
                flex-direction: column;
                height: 400px;
            }
            h3 {
                color: #94a3b8;
                font-size: 0.85rem;
                text-transform: uppercase;
                margin-top: 0;
                border-bottom: 1px solid #334155;
                padding-bottom: 0.5rem;
            }
            .log-stream, .projections-stream {
                flex: 1;
                overflow-y: auto;
                font-family: 'Courier New', Courier, monospace;
                font-size: 0.8rem;
            }
            .event-item {
                border-bottom: 1px solid rgba(255,255,255,0.05);
                padding: 0.5rem 0;
                transition: background 0.2s;
            }
            .event-item:hover {
                background: rgba(255,255,255,0.02);
            }
            .event-type { color: #f472b6; font-weight: bold; }
            .event-domain { color: #a3e635; }
            .event-payload { 
                color: #94a3b8; 
                margin-top: 0.25rem; 
                display: block; 
                word-wrap: break-word; 
            }
            .proj-item {
                margin-bottom: 1rem;
            }
            .proj-domain {
                color: #38bdf8;
                font-weight: bold;
                font-size: 0.9rem;
            }
            .proj-state {
                background: #020617;
                padding: 0.5rem;
                border-radius: 4px;
                color: #cbd5e1;
                margin-top: 0.25rem;
                white-space: pre-wrap;
                overflow-x: auto;
                border: 1px solid rgba(255,255,255,0.05);
            }
            /* Custom Scrollbar */
            ::-webkit-scrollbar { width: 6px; height: 6px; }
            ::-webkit-scrollbar-track { background: transparent; }
            ::-webkit-scrollbar-thumb { background: #475569; border-radius: 3px; }
            ::-webkit-scrollbar-thumb:hover { background: #64748b; }
        `;
        
        this.shadowRoot.appendChild(style);
        
        const container = document.createElement('div');
        container.innerHTML = `
            <h2>System Kernel Dashboard</h2>
            <div class="grid">
                <div class="panel">
                    <h3>Real-time Event Log (Append-Only)</h3>
                    <div class="log-stream" id="log-list"></div>
                </div>
                <div class="panel">
                    <h3>Active Projections (CQRS)</h3>
                    <div class="projections-stream" id="projections-list"></div>
                </div>
            </div>
        `;
        this.shadowRoot.appendChild(container);
        
        this.logList = this.shadowRoot.getElementById('log-list');
        this.projectionsList = this.shadowRoot.getElementById('projections-list');
        this.updateInterval = null;
    }

    connectedCallback() {
        this.updateInterval = setInterval(() => this.render(), 100);
        this.render();
    }

    disconnectedCallback() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
    }

    render() {
        const events = kernel.getEventLog();
        const domains = kernel.getRegisteredDomains();

        // Slice to max 50 to prevent DOM overload, reverse for newest-first
        const recentEvents = events.slice(-50).reverse();

        this.logList.innerHTML = recentEvents.map(e => `
            <div class="event-item">
                <span style="color:#64748b">[${e.sequenceNumber}]</span> 
                <span class="event-type">${e.type}</span> 
                → <span class="event-domain">${e.domainId}</span>
                ${e.payload ? \`<span class="event-payload">$\{JSON.stringify(e.payload)}</span>\` : ''}
            </div>
        `).join('');

        this.projectionsList.innerHTML = domains.map(domain => {
            const state = kernel.getState(domain);
            return `
                <div class="proj-item">
                    <div class="proj-domain">${domain} <span style="font-size:0.7rem;color:#facc15">(Subscribed)</span></div>
                    <div class="proj-state">${JSON.stringify(state, null, 2)}</div>
                </div>
            `;
        }).join('');
    }
}

// Register the custom element
if (!customElements.get('kernel-dashboard')) {
    customElements.define('kernel-dashboard', KernelDashboard);
}
