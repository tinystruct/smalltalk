/**
 * SSE Client for SmallTalk
 * Handles real-time updates using Server-Sent Events (SSE)
 */
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
    }

    /**
     * Initialize the SSE connection
     * @param {string} meetingCode - The meeting Code
     */
    connect(meetingCode) {
        if (this.eventSource) {
            this.disconnect();
        }

        this.meetingCode = meetingCode;
        const url = `/?q=talk/stream/${meetingCode}`;
        console.log('Connecting to SSE endpoint:', url);

        try {
            this.eventSource = new EventSource(url);
            this.connected = true;
            this.reconnectAttempts = 0;
            console.log('SSE Connection established');

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
                console.error('EventSource readyState:', this.eventSource.readyState);
                console.error('Connection details:', {
                    url: url,
                    meetingCode: this.meetingCode,
                    reconnectAttempts: this.reconnectAttempts
                });
                
                this.connected = false;

                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    const delay = this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts - 1);
                    console.log(`Attempting to reconnect in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

                    setTimeout(() => {
                        this.connect(this.meetingCode);
                    }, delay);
                } else {
                    console.error('Max reconnect attempts reached, giving up');
                    // Call update with error data
                    if (typeof update === 'function') {
                        update({ error: 'Max reconnect attempts reached' });
                    }
                }
            };

        } catch (error) {
            console.error('Failed to create EventSource:', error);
            console.error('Connection details:', {
                url: url,
                meetingCode: this.meetingCode
            });
            // Call update with error data
            if (typeof update === 'function') {
                update({ error: 'Failed to create EventSource' });
            }
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

            // Call update with initial message
            console.log('Sending initial message to update:', this.pendingMessages[messageId]);
            if (typeof update === 'function') {
                update(this.pendingMessages[messageId]);
            }
        }

        // Handle incremental updates
        if (data.incremental && data.message) {
            // For incremental updates, append the new content
            this.pendingMessages[messageId].message += " " + data.message;
        } else if (data.message) {
            // For non-incremental updates, use the complete message
            this.pendingMessages[messageId].message = data.message;
        }

        // Update incremental flag
        this.pendingMessages[messageId].incremental = data.incremental || false;

        // Call update with the updated message
        const updateData = {
            ...this.pendingMessages[messageId],
            streaming: true
        };
        console.log('Sending updated message to update:', updateData);
        if (typeof update === 'function') {
            update(updateData);
        }

        // If this is the final message, mark it as final
        if (data.final) {
            console.log('Message is final, marking as complete:', messageId);
            this.pendingMessages[messageId].final = true;
            // Clean up the pending message after a delay
            setTimeout(() => {
                console.log('Cleaning up completed message:', messageId);
                delete this.pendingMessages[messageId];
            }, 1000);
        }
    }

    /**
     * Disconnect the SSE connection
     */
    disconnect() {
        if (this.eventSource) {
            console.log('Disconnecting SSE connection');
            this.eventSource.close();
            this.eventSource = null;
            this.connected = false;
        }
    }

    /**
     * Check if the browser supports SSE
     * @returns {boolean} - Whether SSE is supported
     */
    static isSupported() {
        return 'EventSource' in window;
    }
}
