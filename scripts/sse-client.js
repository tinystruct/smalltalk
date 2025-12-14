/**
 * SSE Client for SmallTalk
 * Handles real-time updates using Server-Sent Events (SSE)
 */

// Suppress browser extension errors that interfere with SSE
(function () {
    'use strict';

    // Suppress chrome extension errors - only if the API exists
    if (typeof chrome !== 'undefined' &&
        chrome.runtime &&
        chrome.runtime.onMessage &&
        chrome.runtime.onMessage.addListener) {
        try {
            const originalAddListener = chrome.runtime.onMessage.addListener;
            chrome.runtime.onMessage.addListener = function (...args) {
                try {
                    return originalAddListener.apply(this, args);
                } catch (e) {
                    // Suppress extension errors
                    console.debug('Suppressed extension error:', e.message);
                }
            };
        } catch (e) {
            // Silently fail if we can't override the listener
            console.debug('Could not override chrome extension listener:', e.message);
        }
    }

    // Global error handler for uncaught extension errors
    window.addEventListener('error', function (event) {
        if (event.message &&
            (event.message.includes('Extension context invalidated') ||
                event.message.includes('Could not establish connection') ||
                event.message.includes('Receiving end does not exist'))) {
            event.preventDefault();
            console.debug('Suppressed extension-related error:', event.message);
            return true;
        }
    });
})();

class SSEClient {
    constructor() {
        this.eventSource = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 2000; // Start with 2 seconds
        this.messageHandlers = {};
        this.meetingCode = null;
        this.connected = false;
        this.pendingMessages = {}; // Store streaming messages by ID
        this.messageQueue = new Map(); // Queue for message updates
        this.updateInterval = null; // Interval for smooth updates
        this.eventHandlers = {}; // Store event handlers
    }

    /**
     * Register an event handler
     * @param {string} event - The event name
     * @param {Function} handler - The event handler function
     */
    on(event, handler) {
        if (!this.eventHandlers[event]) {
            this.eventHandlers[event] = [];
        }
        this.eventHandlers[event].push(handler);
    }

    /**
     * Trigger an event
     * @param {string} event - The event name
     * @param {*} data - The event data
     */
    trigger(event, data) {
        if (this.eventHandlers[event]) {
            this.eventHandlers[event].forEach(handler => {
                try {
                    handler(data);
                } catch (error) {
                    console.error(`Error in ${event} event handler:`, error);
                }
            });
        }
    }

    /**
     * Initialize the SSE connection
     * @param {string} meetingCode - The meeting Code
     */
    connect(meetingCode) {
        // Check if EventSource is supported
        if (!SSEClient.isSupported()) {
            console.error('SSE is not supported in this browser');
            if (typeof update === 'function') {
                update({ error: 'SSE is not supported in this browser' });
            }
            return;
        }

        if (this.eventSource) {
            this.disconnect();
        }

        this.meetingCode = meetingCode;
        const url = `/?q=talk/stream/${meetingCode}`;
        console.log('Connecting to SSE endpoint:', url);

        try {
            this.eventSource = new EventSource(url);

            // Wait for the connection to open before marking as connected
            this.eventSource.addEventListener('open', () => {
                console.log('SSE connection established successfully');
                this.connected = true;
                this.reconnectAttempts = 0;
                this.trigger('connected');
            });

            // Start the update interval for smooth rendering
            this.startUpdateInterval();

            // Message received
            this.eventSource.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    // Handle streaming messages
                    if (data.streaming || data.incremental) {
                        this.handleStreamingMessage(data);
                    } else {
                        // Regular message - call update directly
                        if (typeof update === 'function') {
                            update(data);
                        }
                    }
                } catch (e) {
                    console.error('Error parsing SSE message:', e);
                }
            };

            // Heartbeat to keep connection alive
            this.eventSource.addEventListener('heartbeat', (event) => {
                console.log('SSE Heartbeat received:', event.data);
                try {
                    const data = JSON.parse(event.data);
                    console.log('Parsed heartbeat data:', data);
                    // Call the global update function with heartbeat data
                    if (typeof update === 'function') {
                        update(data);
                    }
                } catch (e) {
                    console.error('Error parsing heartbeat event:', e);
                    console.error('Raw heartbeat data that caused error:', event.data);
                }
            });

            // Error handling
            this.eventSource.onerror = (error) => {
                console.error('SSE Connection error:', error);

                // Check the readyState to determine the type of error
                const readyState = this.eventSource ? this.eventSource.readyState : null;
                console.error('EventSource readyState:', readyState);

                // ReadyState values: 0 = CONNECTING, 1 = OPEN, 2 = CLOSED
                let errorMessage = 'Connection error';

                if (readyState === EventSource.CLOSED || readyState === 2) {
                    errorMessage = 'Connection closed by server';
                } else if (readyState === EventSource.CONNECTING || readyState === 0) {
                    errorMessage = 'Unable to establish connection';
                }

                console.error('Connection details:', {
                    url: url,
                    meetingCode: this.meetingCode,
                    reconnectAttempts: this.reconnectAttempts,
                    errorMessage: errorMessage
                });

                this.connected = false;
                this.stopUpdateInterval();
                this.trigger('disconnected');

                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    const delay = this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts - 1);
                    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);

                    setTimeout(() => {
                        this.connect(this.meetingCode);
                    }, delay);
                } else {
                    console.error('Max reconnect attempts reached. Connection failed permanently.');
                    // Call update with error data
                    if (typeof update === 'function') {
                        update({
                            error: 'Connection failed after multiple attempts. Please refresh the page.',
                            type: 'connection_error'
                        });
                    }
                }
            };

        } catch (error) {
            console.error('Failed to create EventSource:', error);
            console.error('Connection details:', {
                url: url,
                meetingCode: this.meetingCode
            });
            this.connected = false;
            this.trigger('disconnected');
            // Call update with error data
            if (typeof update === 'function') {
                update({ error: 'Failed to create EventSource' });
            }
        }
    }

    /**
     * Start the update interval for smooth rendering
     */
    startUpdateInterval() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }

        this.updateInterval = setInterval(() => {
            this.processMessageQueue();
        }, 50); // Update every 50ms for smooth rendering
    }

    /**
     * Stop the update interval
     */
    stopUpdateInterval() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }

    /**
     * Process the message queue and update the UI
     */
    processMessageQueue() {
        if (this.messageQueue.size === 0) return;

        for (const [messageId, messageData] of this.messageQueue) {
            if (typeof update === 'function') {
                update(messageData);
            }
            this.messageQueue.delete(messageId);
        }
    }

    /**
     * Handle streaming messages
     * @param {Object} data - The message data
     */
    handleStreamingMessage(data) {
        const messageId = data.id;
        console.log('Handling streaming message with ID:', messageId);
        console.log('Streaming message data:', data);

        // If this is the first chunk or we don't have this message yet
        if (!this.pendingMessages[messageId]) {
            console.log('Creating new pending message for ID:', messageId);
            this.pendingMessages[messageId] = {
                id: messageId,
                user: data.user,
                time: data.time,
                session_id: data.session_id,
                message: '',
                final: data.final || false,
                incremental: data.incremental || false
            };

            // Queue initial message
            this.messageQueue.set(messageId, { ...this.pendingMessages[messageId] });
        }

        // Handle incremental updates
        if (data.incremental && data.message) {
            // For incremental updates, append the new content
            this.pendingMessages[messageId].message += data.message;
        } else if (data.message) {
            // For non-incremental updates, use the complete message
            this.pendingMessages[messageId].message = data.message;
        }

        // Update incremental flag
        this.pendingMessages[messageId].incremental = data.incremental || false;

        // Queue the updated message
        this.messageQueue.set(messageId, {
            ...this.pendingMessages[messageId],
            streaming: true
        });

        // If this is the final message, mark it as final
        if (data.final) {
            console.log('Message is final, marking as complete:', messageId);
            this.pendingMessages[messageId].final = true;

            // Queue final update
            this.messageQueue.set(messageId, {
                ...this.pendingMessages[messageId],
                streaming: true
            });

            // Clean up the pending message after a delay
            setTimeout(() => {
                console.log('Cleaning up completed message:', messageId);
                delete this.pendingMessages[messageId];
            }, 1000);
        }
    }

    /**
     * Disconnect from the SSE endpoint
     */
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
        this.connected = false;
        this.stopUpdateInterval();
        this.trigger('disconnected');
    }

    /**
     * Check if the browser supports SSE
     * @returns {boolean} - Whether SSE is supported
     */
    static isSupported() {
        return 'EventSource' in window;
    }
}
