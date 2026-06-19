// src/components/ReactDashboard.jsx
import React, { useState, useEffect } from 'react';
import { kernel } from '../kernel.js';

export default function KernelDashboard() {
  const [events, setEvents] = useState([]);
  const [projections, setProjections] = useState([]);

  useEffect(() => {
    // Poll to keep everything in sync with the kernel state
    const updateState = () => {
      const currentEvents = kernel.getEventLog().slice(-50).reverse();
      const currentDomains = kernel.getRegisteredDomains();
      const currentProjections = currentDomains.map(domain => ({
        domain,
        state: kernel.getState(domain)
      }));

      setEvents(currentEvents);
      setProjections(currentProjections);
    };

    updateState();
    const interval = setInterval(updateState, 100);
    return () => clearInterval(interval);
  }, []);

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>System Kernel Dashboard (React)</h2>
      <div style={styles.grid}>
        <div style={styles.panel}>
          <h3 style={styles.panelTitle}>Real-time Event Log (Append-Only)</h3>
          <div style={styles.stream}>
            {events.map((e, idx) => (
              <div key={`${e.sequenceNumber}-${idx}`} style={styles.eventItem}>
                <span style={styles.seq}>[{e.sequenceNumber}]</span>{' '}
                <span style={styles.type}>{e.type}</span>{' '}
                → <span style={styles.domain}>{e.domainId}</span>
                {e.payload && (
                  <span style={styles.payload}>{JSON.stringify(e.payload)}</span>
                )}
              </div>
            ))}
            {events.length === 0 && (
              <div style={{ color: '#64748b' }}>No events logged yet.</div>
            )}
          </div>
        </div>
        <div style={styles.panel}>
          <h3 style={styles.panelTitle}>Active Projections (CQRS)</h3>
          <div style={styles.stream}>
            {projections.map((proj) => (
              <div key={proj.domain} style={styles.projItem}>
                <div style={styles.projDomain}>
                  {proj.domain} <span style={styles.subscribed}>(Subscribed)</span>
                </div>
                <div style={styles.projState}>
                  {JSON.stringify(proj.state, null, 2)}
                </div>
              </div>
            ))}
            {projections.length === 0 && (
              <div style={{ color: '#64748b' }}>No active projections.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: {
    display: 'block',
    fontFamily: "'Inter', system-ui, sans-serif",
    background: 'rgba(15, 23, 42, 0.9)',
    color: '#e2e8f0',
    padding: '1.5rem',
    borderRadius: '12px',
    border: '1px solid rgba(255, 255, 255, 0.1)',
    boxShadow: '0 4px 30px rgba(0, 0, 0, 0.5)',
    backdropFilter: 'blur(10px)',
  },
  title: {
    marginTop: 0,
    color: '#00e5ff',
    fontWeight: 600,
    letterSpacing: '1px',
    textTransform: 'uppercase',
    fontSize: '1rem',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '1.5rem',
  },
  panel: {
    background: '#1e293b',
    padding: '1rem',
    borderRadius: '8px',
    border: '1px solid #334155',
    display: 'flex',
    flexDirection: 'column',
    height: '400px',
  },
  panelTitle: {
    color: '#94a3b8',
    fontSize: '0.85rem',
    textTransform: 'uppercase',
    marginTop: 0,
    borderBottom: '1px solid #334155',
    paddingBottom: '0.5rem',
  },
  stream: {
    flex: 1,
    overflowY: 'auto',
    fontFamily: "'Courier New', Courier, monospace",
    fontSize: '0.8rem',
  },
  eventItem: {
    borderBottom: '1px solid rgba(255,255,255,0.05)',
    padding: '0.5rem 0',
  },
  seq: { color: '#64748b' },
  type: { color: '#f472b6', fontWeight: 'bold' },
  domain: { color: '#a3e635' },
  payload: {
    color: '#94a3b8',
    marginTop: '0.25rem',
    display: 'block',
    wordWrap: 'break-word',
  },
  projItem: {
    marginBottom: '1rem',
  },
  projDomain: {
    color: '#38bdf8',
    fontWeight: 'bold',
    fontSize: '0.9rem',
  },
  subscribed: {
    fontSize: '0.7rem',
    color: '#facc15',
    marginLeft: '0.5rem',
  },
  projState: {
    background: '#020617',
    padding: '0.5rem',
    borderRadius: '4px',
    color: '#cbd5e1',
    marginTop: '0.25rem',
    whiteSpace: 'pre-wrap',
    overflowX: 'auto',
    border: '1px solid rgba(255,255,255,0.05)',
  },
};
