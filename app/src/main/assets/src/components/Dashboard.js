// src/components/Dashboard.js
import { kernel } from '../kernel.js';
import { animate } from 'https://cdn.jsdelivr.net/npm/motion@11.11.13/+esm';

export class KernelDashboard extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: 'open' });
        
        // Inject Tailwind CSS inside the Shadow Root dynamically
        const styleLink = document.createElement('link');
        styleLink.rel = 'stylesheet';
        styleLink.href = 'https://cdn.tailwindcss.com';
        this.shadowRoot.appendChild(styleLink);

        // Inject Lucide icons script compatibility inside the Shadow Root
        const lucideScript = document.createElement('script');
        lucideScript.src = "https://unpkg.com/lucide@latest";
        this.shadowRoot.appendChild(lucideScript);
        
        const container = document.createElement('div');
        container.className = "w-full space-y-4 pt-4 border-t border-slate-800/60";
        container.innerHTML = `
            <div class="flex items-center gap-2 mb-4">
                <span class="p-1.5 bg-yellow-500/10 text-yellow-500 border border-yellow-500/20 rounded-lg">
                    <svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="m12 3-1.912 5.886H3.88l4.917 3.57L6.886 18.34 12 14.77l5.114 3.57-1.91-5.885 4.916-3.57h-6.208L12 3z"/>
                    </svg>
                </span>
                <div>
                    <h2 class="text-xs font-semibold uppercase tracking-widest text-slate-400 font-serif-alchemist">Sovereign Kernel CQRS Engine</h2>
                    <p class="text-[9px] text-slate-500">Append-Only Event Log & Database State Projections</p>
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- PANEL 1: Real-time Event Log -->
                <div id="panel-event-log" style="opacity: 0; transform: translateY(24px) scale(0.97);" class="bg-slate-950/80 border border-slate-850 rounded-2xl p-5 flex flex-col h-96">
                    <h3 class="text-[11px] font-bold text-slate-400 uppercase tracking-wider pb-3 border-b border-slate-800/85 mb-3 flex items-center justify-between">
                        <span>Real-time Event Log (Append-Only)</span>
                        <span class="px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-[9px] font-mono-code text-cyan-400">CQRS_LOG</span>
                    </h3>
                    <div class="flex-1 overflow-y-auto space-y-1.5 font-mono-code text-[11px] pr-2" id="log-list"></div>
                </div>

                <!-- PANEL 2: Active State Projections -->
                <div id="panel-projections" style="opacity: 0; transform: translateY(24px) scale(0.97);" class="bg-slate-950/80 border border-slate-850 rounded-2xl p-5 flex flex-col h-96">
                    <h3 class="text-[11px] font-bold text-slate-400 uppercase tracking-wider pb-3 border-b border-slate-800/85 mb-3 flex items-center justify-between">
                        <span>Active Projections (State Views)</span>
                        <span class="px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-[9px] font-mono-code text-yellow-400">VIEW_MODEL</span>
                    </h3>
                    <div class="flex-1 overflow-y-auto space-y-4 font-mono-code text-[11px] pr-2 text-slate-300" id="projections-list"></div>
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

        // Animate grid panels smoothly using Framer Motion (Motion bundle) on entrance
        const logPanel = this.shadowRoot.getElementById('panel-event-log');
        const projPanel = this.shadowRoot.getElementById('panel-projections');

        if (logPanel && projPanel) {
            animate(
                logPanel, 
                { opacity: [0, 1], y: [24, 0], scale: [0.97, 1] }, 
                { duration: 0.8, ease: [0.16, 1, 0.3, 1], delay: 0.1 }
            );
            animate(
                projPanel, 
                { opacity: [0, 1], y: [24, 0], scale: [0.97, 1] }, 
                { duration: 0.8, ease: [0.16, 1, 0.3, 1], delay: 0.22 }
            );
        }
    }

    disconnectedCallback() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
    }

    render() {
        const events = kernel.getEventLog();
        const domains = kernel.getRegisteredDomains();

        // Limit to max 50 recent events, reverse to show news-first
        const recentEvents = events.slice(-50).reverse();

        if (recentEvents.length === 0) {
            this.logList.innerHTML = `<div class="text-slate-650 italic">Waiting for incoming domain sequence events...</div>`;
        } else {
            this.logList.innerHTML = recentEvents.map(e => `
                <div class="py-1 border-b border-slate-900/40 hover:bg-slate-900/30 transition text-slate-300">
                    <span class="text-slate-600">[SN#${e.sequenceNumber}]</span> 
                    <span class="text-pink-400 font-bold">${e.type}</span> 
                    → <span class="text-emerald-400 font-medium">${e.domainId}</span>
                    ${e.payload ? `<span class="block text-slate-500 text-[10px] bg-slate-900/50 p-2 rounded border border-slate-900 mt-1">${JSON.stringify(e.payload)}</span>` : ''}
                </div>
            `).join('');
        }

        if (domains.length === 0) {
            this.projectionsList.innerHTML = `<div class="text-slate-650 italic">Active system projections registering...</div>`;
        } else {
            this.projectionsList.innerHTML = domains.map(domain => {
                const state = kernel.getState(domain);
                return `
                    <div class="p-3 bg-slate-900/40 border border-slate-900 rounded-xl space-y-1.5">
                        <div class="flex justify-between items-center">
                            <span class="text-cyan-400 font-medium font-serif-alchemist uppercase tracking-wider text-xs">${domain}</span>
                            <span class="inline-flex items-center gap-1 text-[9px] text-yellow-500 font-semibold bg-yellow-500/10 px-1.5 py-0.5 rounded border border-yellow-500/20">
                                <span class="w-1.5 h-1.5 rounded-full bg-yellow-400 animate-ping"></span> Subscribed
                            </span>
                        </div>
                        <pre class="bg-[#020617] p-3 rounded-lg border border-slate-800/50 text-[10px] text-slate-300 overflow-x-auto whitespace-pre-wrap">${JSON.stringify(state, null, 2)}</pre>
                    </div>
                `;
            }).join('');
        }
    }
}

// Register the custom element
if (!customElements.get('kernel-dashboard')) {
    customElements.define('kernel-dashboard', KernelDashboard);
}
}
