// src/kernel.js

/**
 * @template PayloadType
 * @typedef {Object} DomainEvent
 * @property {string} type - The unique type or name of the event.
 * @property {string} domainId - The ID of the domain this event belongs to.
 * @property {PayloadType} [payload] - Optional custom data payload for the event.
 * @property {number} [timestamp] - Unix timestamp generated when the event is appended.
 * @property {number} [sequenceNumber] - The monotonic sequence index in the global event log.
 */

/**
 * A reducer function that computes a new state from the previous state and an incident event.
 * @template State
 * @callback ProjectionReducer
 * @param {State | null} state
 * @param {DomainEvent} event
 * @returns {State}
 */

/**
 * A callback function triggered whenever a projection's state is updated.
 * @template State
 * @callback ProjectionCallback
 * @param {State} state
 */

/**
 * Function to unsubscribe a registered projection callback.
 * @callback UnsubscribeCallback
 */

/**
 * @template State
 * @typedef {Object} ProjectionEntry
 * @property {ProjectionReducer<State>} reducer
 * @property {State | null} state
 * @property {ProjectionCallback<State>[]} subscribers
 */

/**
 * Core Kernel for domain-of-domains system
 * Implements an append-only event log and CQRS projection patterns.
 */
class Kernel {
  constructor() {
    /** @type {DomainEvent[]} */
    this.eventLog = [];
    /** @type {Map<string, ProjectionEntry<any>>} */
    this.projections = new Map(); // domainId -> { reducer, state, subscribers }
  }

  /**
   * Appends a new event to the global event log and updates corresponding projections.
   * @param {DomainEvent} event - The event object.
   * @returns {DomainEvent}
   */
  appendEvent(event) {
    if (!event || !event.type || !event.domainId) {
      throw new Error("Invalid event. Must have 'type' and 'domainId'");
    }
    
    /** @type {DomainEvent} */
    const storedEvent = {
      ...event,
      timestamp: Date.now(),
      sequenceNumber: this.eventLog.length
    };

    // Append to global log
    this.eventLog.push(storedEvent);

    // Update projections
    this._updateProjections(storedEvent);
    
    return storedEvent;
  }

  /**
   * Subscribes a projection reducer to a specific domain.
   * @template State
   * @param {string} domainId - The ID of the domain.
   * @param {ProjectionReducer<State>} reducer - Reducer function: (state, event) => newState
   * @param {State | null} [initialState=null] - The initial state for the projection.
   * @returns {(callback: ProjectionCallback<State>) => UnsubscribeCallback | null}
   */
  subscribeProjection(domainId, reducer, initialState = null) {
    if (!this.projections.has(domainId)) {
      /** @type {ProjectionEntry<State>} */
      const entry = {
        reducer,
        state: initialState,
        subscribers: []
      };
      this.projections.set(domainId, entry);
      
      // Seed with past events to catch up to current state
      this._seedProjection(domainId);
    }
    
    const projection = this.projections.get(domainId);

    // Return a subscribe function for UI/Systems to listen to state changes
    return (callback) => {
      if (typeof callback !== 'function') return null;
      
      projection.subscribers.push(callback);
      // Immediate callback with current state
      callback(projection.state);

      // Return unsubscribe function
      return () => {
        projection.subscribers = projection.subscribers.filter(cb => cb !== callback);
      };
    };
  }

  /**
   * Retrieves the current state of a domain projection.
   * @template State
   * @param {string} domainId 
   * @returns {State | null} The projection state or null if not registered.
   */
  getState(domainId) {
    const projection = this.projections.get(domainId);
    return projection ? projection.state : null;
  }

  /**
   * Retrieves the full append-only event log.
   * @returns {DomainEvent[]}
   */
  getEventLog() {
    return [...this.eventLog];
  }

  /**
   * Retrieves a list of all currently registered projection domain IDs.
   * @returns {string[]}
   */
  getRegisteredDomains() {
    return Array.from(this.projections.keys());
  }

  /**
   * @param {string} domainId 
   * @private
   */
  _seedProjection(domainId) {
    const projection = this.projections.get(domainId);
    if (!projection) return;
    const domainEvents = this.eventLog.filter(e => e.domainId === domainId);
    
    for (const event of domainEvents) {
      projection.state = projection.reducer(projection.state, event);
    }
  }

  /**
   * @param {DomainEvent} event 
   * @private
   */
  _updateProjections(event) {
    const projection = this.projections.get(event.domainId);
    if (!projection) return; // Domain has no active projections yet

    projection.state = projection.reducer(projection.state, event);
    
    // Notify subscribers of state change
    for (const subscriber of projection.subscribers) {
      try {
        subscriber(projection.state);
      } catch (err) {
        console.error(`Error in projection subscriber for domain ${event.domainId}:`, err);
      }
    }
  }
}

export const kernel = new Kernel();
