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

        try {
            this.eventSource = new EventSource(url);
            this.connected = true;
            this.reconnectAttempts = 0;
            console.log('SSE Connection established');

            // Message received
            this.eventSource.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    this.handleMessage(data);
                } catch (e) {
                    console.error('Error parsing SSE message:', e);
                }
            };

            // Handle specific event types
            this.eventSource.addEventListener('message', (event) => {
                try {
                    const data = JSON.parse(event.data);
                    this.handleMessage(data);
                } catch (e) {
                    console.error('Error parsing message event:', e);
                }
            });

            // Heartbeat to keep connection alive
            this.eventSource.addEventListener('heartbeat', (event) => {
                console.log('SSE Heartbeat received');
                try {
                    const data = JSON.parse(event.data);
                    this.triggerHandler('heartbeat', data);
                } catch (e) {
                    console.error('Error parsing heartbeat event:', e);
                }

                // Immediately request the next message
                this.pollForNextMessage();
            });

            // Error handling
            this.eventSource.onerror = (error) => {
                // 新增详细日志
                console.error('SSE Connection error:', error);
                console.error('EventSource readyState:', this.eventSource.readyState, 
                    this.eventSource.readyState === 0 ? 'CONNECTING' : 
                    this.eventSource.readyState === 1 ? 'OPEN' : 
                    this.eventSource.readyState === 2 ? 'CLOSED' : 'UNKNOWN');
                if (this.eventSource.readyState === EventSource.CLOSED) {
                    console.error('EventSource state: CLOSED');
                } else if (this.eventSource.readyState === EventSource.CONNECTING) {
                    console.error('EventSource state: CONNECTING');
                } else if (this.eventSource.readyState === EventSource.OPEN) {
                    console.error('EventSource state: OPEN');
                }
                // 检查网络状态
                if (!navigator.onLine) {
                    console.error('Browser is offline');
                }
                // 打印响应状态码（需在 Network 面板查看）
                // 建议：在 Network 面板找到 /talk/stream/{meetingCode}，确认响应头和状态码

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
                    this.triggerHandler('maxReconnectAttemptsReached', { error: 'Max reconnect attempts reached' });
                }
            };

            // Start polling for messages
            this.pollForNextMessage();
        } catch (error) {
            console.error('Failed to create EventSource:', error);
            this.triggerHandler('error', { error: 'Failed to create EventSource' });
        }
    }

    /**
     * Poll for the next message
     * This is needed because the tinystruct framework doesn't support
     * keeping the connection open for streaming
     */
    pollForNextMessage() {
        if (!this.connected || !this.meetingCode) return;

        // Use a timeout to avoid overwhelming the server
        setTimeout(() => {
            if (this.connected) {
                // The EventSource will automatically make a new request
                // when the previous one completes
            }
        }, 500); // Small delay to prevent rapid polling
    }

    /**
     * Disconnect the SSE connection
     */
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
            this.connected = false;
        }
    }

    /**
     * Handle incoming messages
     * @param {Object} data - The message data
     */
    handleMessage(data) {
        console.log('Received message:', data);
        // Check if this is a streaming message
        if (data.streaming) {
            console.log('Handling as streaming message');
            this.handleStreamingMessage(data);
        } else {
            // Regular message
            console.log('Handling as regular message');
            this.triggerHandler('message', data);
        }
    }

    /**
     * Handle streaming messages
     * @param {Object} data - The message data
     */
    handleStreamingMessage(data) {
        const messageId = data.id;
        console.log('Handling streaming message with ID:', messageId);

        // If this is the first chunk or we don't have this message yet
        if (!this.pendingMessages[messageId]) {
            this.pendingMessages[messageId] = {
                id: messageId,
                user: data.user,
                time: data.time,
                session_id: data.session_id,
                message: data.message || '',
                chunks: [],
                final: data.final || false,
                incremental: data.incremental || false
            };

            // Trigger initial message event
            this.triggerHandler('streamStart', this.pendingMessages[messageId]);
        }

        // Add the new chunk
        if (data.chunk) {
            this.pendingMessages[messageId].chunks.push(data.chunk);
        }

        // Update the full message
        // The backend should already handle proper spacing between chunks
        this.pendingMessages[messageId].message = data.message || this.pendingMessages[messageId].message;

        // Pass the incremental flag to indicate this is an update to an existing message
        this.pendingMessages[messageId].incremental = data.incremental || false;

        // Trigger chunk event
        this.triggerHandler('streamChunk', {
            id: messageId,
            chunk: data.chunk,
            message: this.pendingMessages[messageId].message,
            incremental: this.pendingMessages[messageId].incremental
        });

        // If this is the final message, mark it as final
        if (data.final) {
            this.pendingMessages[messageId].final = true;
        }
    }

    /**
     * Register a handler for a specific event
     * @param {string} event - The event name
     * @param {Function} handler - The handler function
     */
    on(event, handler) {
        if (!this.messageHandlers[event]) {
            this.messageHandlers[event] = [];
        }
        this.messageHandlers[event].push(handler);
    }

    /**
     * Trigger handlers for a specific event
     * @param {string} event - The event name
     * @param {Object} data - The event data
     */
    triggerHandler(event, data) {
        if (this.messageHandlers[event]) {
            this.messageHandlers[event].forEach(handler => {
                try {
                    handler(data);
                } catch (error) {
                    console.error(`Error in ${event} handler:`, error);
                }
            });
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
