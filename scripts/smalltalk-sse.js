/**
 * Enhanced SmallTalk class with SSE support
 */
class SmallTalkSSE {
    constructor(meetingCode) {
        this.user = struct.get("user");
        this.oldTitle = document.title;
        this.isRunning = false;
        this.sseClient = new SSEClient();
        this.streamingMessages = {};
        this.meetingCode = meetingCode;
        this.init();
    }

    init() {
        this.setupEventHandlers();
        this.setupModalHandlers();

        // Initialize SSE if we have a user
        if (this.user) {
            this.initSSE();
        }

        // Start traditional polling as fallback
        this.startPolling();
    }

    initSSE() {
        // Check if SSE is supported
        if (!SSEClient.isSupported()) {
            console.warn('Server-Sent Events not supported by this browser, falling back to polling');
            return;
        }

        // Get session ID from the page
        const sessionId = this.getSessionId();
        if (!sessionId) {
            console.warn('No session ID found, cannot initialize SSE');
            return;
        }

        // Set up SSE event handlers
        this.setupSSEHandlers();

        // Connect to SSE stream
        this.sseClient.connect(sessionId);
    }

    getSessionId() {
        // Try to get session ID from the page
        // This depends on how your application stores the session ID
        // You might need to adjust this based on your specific implementation
        return document.cookie.replace(/(?:(?:^|.*;\s*)JSESSIONID\s*\=\s*([^;]*).*$)|^.*$/, "$1");
    }

    setupSSEHandlers() {
        // Handle connection events
        this.sseClient.on('connected', (data) => {
            console.log('Connected to SSE stream');
        });

        // Handle regular messages
        this.sseClient.on('message', (data) => {
            this.update(data);
        });

        // Handle streaming start
        this.sseClient.on('streamStart', (data) => {
            console.log('Stream start event received:', data);

            // Remove any existing chat-gpt messages with the same ID
            $(`.chat-gpt[data-message-id="${data.id}"]`).remove();

            // Check if we already have a placeholder for this message (for incremental updates)
            if (!this.streamingMessages[data.id]) {
                // Create a placeholder for the streaming message
                const placeholder = this.createMessageElement(data);

                // Add a content span if it doesn't exist
                if (placeholder.find('.content').length === 0) {
                    placeholder.append('<span class="content"></span>');
                }

                // Initialize with the thinking indicator
                if (data.message) {
                    placeholder.find('.content').html(data.message);
                } else {
                    placeholder.find('.content').html('<span class="typing-indicator">Thinking</span>');
                }

                this.streamingMessages[data.id] = {
                    element: placeholder,
                    content: data.message || '',
                    accumulatedContent: data.message || ''
                };
            }
        });

        // Handle streaming chunks
        this.sseClient.on('streamChunk', (data) => {
            console.log('Stream chunk event received:', data);
            // Handle streaming message updates
            if (this.streamingMessages[data.id]) {
                // Get the current message state
                const messageState = this.streamingMessages[data.id];
                const newChunk = data.message;

                // Append the new chunk to the accumulated content with proper spacing
                if (messageState.accumulatedContent.length > 0 &&
                    !this.endsWithWhitespace(messageState.accumulatedContent) &&
                    !this.startsWithPunctuation(newChunk) &&
                    !this.startsWithWhitespace(newChunk)) {
                    messageState.accumulatedContent += ' ';
                }
                messageState.accumulatedContent += newChunk;

                // Update the content property
                messageState.content = messageState.accumulatedContent;

                // Update the UI - only update the content, not the entire message
                const placeholder = messageState.element;
                placeholder.find('.content').html(this.formatContent(messageState.accumulatedContent));
                this.scrollToBottom();

                // If this is the final chunk, clean up
                if (data.final) {
                    // Remove from tracking
                    delete this.streamingMessages[data.id];

                    // Update title to indicate new message
                    this.toggleTitle();
                }
            } else if (data.incremental) {
                // This is an incremental update but we don't have a placeholder yet
                // Create a placeholder for the streaming message
                const placeholder = this.createMessageElement({
                    id: data.id,
                    user: 'ChatGPT',
                    time: data.time || new Date().toLocaleTimeString(),
                    session_id: data.session_id
                });

                // Add a content span if it doesn't exist
                if (placeholder.find('.content').length === 0) {
                    placeholder.append('<span class="content"></span>');
                }

                // Update the content
                placeholder.find('.content').html(data.message);

                // Store for future updates
                this.streamingMessages[data.id] = {
                    element: placeholder,
                    content: data.message,
                    accumulatedContent: data.message
                };

                this.scrollToBottom();

                // If this is the final chunk, clean up
                if (data.final) {
                    // Remove from tracking
                    delete this.streamingMessages[data.id];

                    // Update title to indicate new message
                    this.toggleTitle();
                }
            }
        });

        // Handle streaming end - this is now handled in the streamChunk handler
        this.sseClient.on('streamEnd', (data) => {
            console.log('Stream end event received:', data);
            // This event is no longer used, but we keep the handler for backward compatibility
        });

        // Handle errors and reconnection
        this.sseClient.on('error', (data) => {
            console.error('SSE error:', data);
            // Fall back to polling if SSE fails
            this.startPolling();
        });

        this.sseClient.on('maxReconnectAttemptsReached', (data) => {
            console.warn('SSE max reconnect attempts reached, falling back to polling');
            // Fall back to polling
            this.startPolling();
        });
    }

    startPolling() {
        // Only start polling if SSE is not connected
        if (!this.sseClient.connected) {
            console.log('Starting polling as fallback');
            this.autoupdate();
            setInterval(() => this.autoupdate(), 5000);
        }
    }

    setupEventHandlers() {
        $(document).ready(() => {
            if (this.user) {
                this.command("greeting");
            } else {
                $("#modal").modal();
            }

            $("#send").click(() => this.save());

            $("#text").keydown((event) => {
                if (event.which === 13 && !event.shiftKey) {
                    event.preventDefault();
                    this.save();
                }
            });

            $(window).focus(() => {
                document.title = this.oldTitle;
            });

            // Setup ChatGPT toggle button
            $("#chatgpt-toggle").change((event) => {
                localStorage.setItem('chatgpt-mode', event.target.checked ? 'enabled' : 'disabled');
                this.updateChatGPTToggleState();
            });

            // Initialize toggle state from localStorage
            this.updateChatGPTToggleState();

            // Check if we need to show the topic modal
            // Only show it if the user hasn't set a topic before and this isn't a page reload after login
            const hasJustLoggedIn = localStorage.getItem('justLoggedIn') === 'true';
            const loginTimestamp = parseInt(localStorage.getItem('loginTimestamp') || '0');
            const currentTime = Date.now();
            const isRecentLogin = (currentTime - loginTimestamp) < 10000; // Within 10 seconds of login

            console.log('Topic check - Has topic:', !!$("#topic").text(), 'Just logged in:', hasJustLoggedIn, 'Recent login:', isRecentLogin);

            if (!$("#topic").text() && !(hasJustLoggedIn && isRecentLogin)) {
                $("#topic_modal").modal();
            } else {
                console.log('Skipping topic dialog - Topic exists or user just logged in');
            }

            // Clear the login flag after a delay to ensure it persists through page load
            setTimeout(function() {
                localStorage.removeItem('justLoggedIn');
                localStorage.removeItem('loginTimestamp');
                console.log('Login flags cleared');
            }, 15000); // Clear after 15 seconds
        });
    }

    setupModalHandlers() {
        this.setupNicknameModal();
        this.setupTopicModal();
    }

    updateChatGPTToggleState() {
        const chatGPTMode = localStorage.getItem('chatgpt-mode') || 'disabled';
        $("#chatgpt-toggle").prop('checked', chatGPTMode === 'enabled');
    }

    setupNicknameModal() {
        $("#dialog_submit").click(() => {
            const nickname = $("#nickname").val();
            this.startSession(nickname);
        });

        $("#nickname").keyup((event) => {
            if (event.which === 13) {
                $("#dialog_submit").click();
            }
        });
    }

    setupTopicModal() {
        $("#topic_submit").click(() => {
            const topic = $("#brief").val();
            this.saveTopic(topic);
        });
    }

    async startSession(nickname) {
        try {
            await $.ajax({
                type: "POST",
                url: `/?q=talk/start/${nickname}`
            });

            $("#modal").modal('hide');
            this.user = nickname;
            struct.save("user", nickname);
            this.command("greeting");

            // Initialize SSE after user is set
            this.initSSE();
        } catch (error) {
            console.error('Failed to start session:', error);
        }
    }

    async saveTopic(topic) {
        try {
            await $.ajax({
                type: "POST",
                url: "/?q=talk/topic",
                data: { topic }
            });

            $("#topic_modal").modal('hide');
            $("#topic").html(topic);
        } catch (error) {
            console.error('Failed to save topic:', error);
        }
    }

    save() {
        let message = $("#text").html();
        if (!message || message.trim() === '') return;

        let image = "";

        // Check if ChatGPT mode is enabled via the toggle button
        const chatGPTMode = localStorage.getItem('chatgpt-mode') === 'enabled';

        if (chatGPTMode) {
            // Format message for ChatGPT
            message = "@ChatGPT " + $("#text").text();

            // Extract any images
            $("#text").find("img").each(function() {
                image = $(this).attr("src");
            });
        }

        // Clear input field immediately for better UX
        $("#text").html('');

        // Send the message
        $.ajax({
            type: "POST",
            url: "/?q=talk/save",
            dataType: "json",
            data: {
                text: message,
                time: new Date().getTime(),
                image,
                stream: chatGPTMode ? "true" : "false" // Enable streaming for ChatGPT messages
            }
        }).fail((error) => {
            console.error('Failed to save message:', error);
        });
    }

    async command(cmd) {
        try {
            await $.ajax({
                type: "POST",
                url: "/?q=talk/command",
                dataType: "json",
                data: { cmd }
            });
        } catch (error) {
            this.handleCommandError(error.responseJSON);
        }
    }

    handleCommandError(error) {
        if (!error) return;

        const errorHandlers = {
            'session-timeout': () => this.handleSessionTimeout(),
            'expired': () => this.handleExpiredSession(),
            'missing user': () => this.handleMissingUser()
        };

        const handler = errorHandlers[error.error];
        if (handler) handler();
    }

    handleSessionTimeout() {
        this.appendSystemMessage(`${this.user} left from this conversation.`);
        if (confirm("Your session is timed out, Do you want to reload the conversation?")) {
            this.reloadConversation();
        }
    }

    handleExpiredSession() {
        this.appendSystemMessage("It's an expired conversation, you have to refresh the current page or request a new meeting code.");
    }

    handleMissingUser() {
        if (this.user) {
            this.reloadConversation();
        } else {
            $("#modal").modal();
        }
    }

    appendSystemMessage(message) {
        const li = $("<li />").html("System Notification: " + message);
        $('#messages').append(li);
        this.scrollToBottom();
    }

    scrollToBottom() {
        $('#list').animate({
            scrollTop: $('#messages').height()
        }, 'fast');
    }

    async autoupdate() {
        if (this.isRunning || this.sseClient.connected) return;

        this.isRunning = true;
        try {
            const data = await $.getJSON("?q=talk/update/"+this.meetingCode + "/" + this.getSessionId());
            this.update(data);
        } catch (error) {
            this.handleAutoupdateError(error);
        } finally {
            this.isRunning = false;
        }
    }

    handleAutoupdateError(error) {
        if (error.responseJSON) {
            const errorData = error.responseJSON;
            if (errorData.error === "expired") {
                this.handleExpiredSession();
            } else if (errorData.error === "session-timeout") {
                this.handleSessionTimeout();
            }
        }
    }

    update(data) {
        if (data.message) {
            this.updateMessage(data);
        } else if (data.cmd) {
            this.updateCommand(data);
        }
    }

    updateMessage(data) {
        // Skip if this is a streaming message that we're already tracking
        if (data.streaming && this.streamingMessages[data.id]) {
            return;
        }

        // For non-incremental messages, create a new message element
        const placeholder = this.createMessageElement(data);
        if (data.user === 'ChatGPT') {
            this.handleChatGPTMessage(placeholder, data.message);
        } else {
            this.handleUserMessage(placeholder, data.message);
        }
    }

    createMessageElement(data) {
        // Ensure data.id exists - generate a unique ID if not provided
        if (!data.id) {
            data.id = 'msg_' + new Date().getTime() + '_' + Math.floor(Math.random() * 1000);
            console.log('Generated message ID:', data.id);
        }

        // Check if this message already exists (for incremental updates)
        const existingMessage = $(`#messages li[data-message-id="${data.id}"]`);
        if (existingMessage.length > 0) {
            return existingMessage;
        }

        // Remove any existing chat-gpt messages with the same ID
        $(`.chat-gpt[data-message-id="${data.id}"]`).remove();

        // Create a new message element with the pre-wrapper class
        const placeholder = $('<li class="pre-wrapper" />');
        placeholder.attr('data-message-id', data.id); // Add message ID for easy lookup
        console.log('Creating new message element with ID:', data.id);

        placeholder.html(`
            <span class='time'>${data.time}</span>
            <span class='name'>${data.user}</span>:
        `);

        // Don't add the content span here - we'll add it when needed

        $('#messages').append(placeholder);
        return placeholder;
    }

    handleChatGPTMessage(placeholder, message) {
        // Make sure the placeholder has a content span
        if (placeholder.find('.content').length === 0) {
            placeholder.append('<span class="content"></span>');
        }

        if (message.includes("<placeholder-image>")) {
            this.handleMessageWithPlantUML(placeholder, message);
        } else {
            this.animateMessage(placeholder, message);
        }
    }

    animateMessage(placeholder, message) {
        placeholder.find('.content').html(message);
        this.scrollToBottom();
        this.toggleTitle();
    }

    handleMessageWithPlantUML(placeholder, message) {
        // Handle PlantUML diagrams
        const parts = message.split("<placeholder-image>");
        if (parts.length >= 2) {
            const beforeImage = parts[0];
            const afterImage = parts.slice(1).join("<placeholder-image>");

            // Display the text before the image
            placeholder.find('.content').html(beforeImage);

            // Add a loading indicator
            const loadingIndicator = $('<div class="loading-indicator">Loading diagram...</div>');
            placeholder.find('.content').append(loadingIndicator);

            // Extract the PlantUML code
            const plantUmlCode = this.extractPlantUmlCode(message);
            if (plantUmlCode) {
                this.renderPlantUML(plantUmlCode, (imageUrl) => {
                    // Remove loading indicator
                    loadingIndicator.remove();

                    // Add the image
                    const img = $('<img>').attr('src', imageUrl).addClass('plantuml-diagram');
                    placeholder.find('.content').append(img);

                    // Add the text after the image
                    placeholder.find('.content').append(afterImage);

                    this.scrollToBottom();
                });
            } else {
                // If no PlantUML code found, just show the full message
                loadingIndicator.remove();
                placeholder.find('.content').html(message);
                this.scrollToBottom();
            }
        } else {
            // If the message doesn't actually contain an image placeholder
            placeholder.find('.content').html(message);
            this.scrollToBottom();
        }
    }

    extractPlantUmlCode(message) {
        const startTag = "@startuml";
        const endTag = "@enduml";

        const startIndex = message.indexOf(startTag);
        const endIndex = message.indexOf(endTag);

        if (startIndex !== -1 && endIndex !== -1 && endIndex > startIndex) {
            return message.substring(startIndex, endIndex + endTag.length);
        }

        return null;
    }

    renderPlantUML(plantUmlCode, callback) {
        // This is a placeholder - you would need to implement the actual PlantUML rendering
        // This could be done via a server endpoint or a client-side library

        // For now, we'll just simulate a delay and return a placeholder image
        setTimeout(() => {
            callback('https://via.placeholder.com/300x200?text=PlantUML+Diagram');
        }, 1000);
    }

    handleUserMessage(placeholder, message) {
        // Make sure the placeholder has a content span
        if (placeholder.find('.content').length === 0) {
            placeholder.append('<span class="content"></span>');
        }

        placeholder.find('.content').html(message);
        this.scrollToBottom();
        this.toggleTitle();
    }

    updateCommand(data) {
        // Handle command updates
        if (data.cmd === "clear") {
            $('#messages').empty();
        }
    }

    toggleTitle() {
        document.title = document.title === this.oldTitle ?
            'New Message(s) Received!' : this.oldTitle;
    }

    // Helper methods for handling text formatting

    /**
     * Check if a string ends with whitespace
     * @param {string} str - The string to check
     * @returns {boolean} - Whether the string ends with whitespace
     */
    endsWithWhitespace(str) {
        return str.length > 0 && /\s$/.test(str);
    }

    /**
     * Check if a string starts with whitespace
     * @param {string} str - The string to check
     * @returns {boolean} - Whether the string starts with whitespace
     */
    startsWithWhitespace(str) {
        return str.length > 0 && /^\s/.test(str);
    }

    /**
     * Check if a string starts with punctuation
     * @param {string} str - The string to check
     * @returns {boolean} - Whether the string starts with punctuation
     */
    startsWithPunctuation(str) {
        return str.length > 0 && /^[.,;:!?)]/.test(str);
    }

    /**
     * Format content for display
     * @param {string} content - The content to format
     * @returns {string} - The formatted content
     */
    formatContent(content) {
        // Apply any additional formatting here
        // For example, you could convert markdown to HTML
        return content;
    }

    reloadConversation() {
        window.location.reload();
    }
}
