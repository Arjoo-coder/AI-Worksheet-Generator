package com.university.worksheet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Central AI provider configuration, bound from the "app.ai.*" prefix in
 * application.yml.
 *
 * Default provider is LOCAL OLLAMA (native /api/chat), not OpenAI. Ollama
 * requires no API key. An OpenAI-compatible provider remains supported as a
 * fallback (see AiService) purely for robustness/future flexibility, but is
 * never the default and is never required.
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** "ollama" (default) uses the native /api/chat format and never sends Authorization. */
    private String provider = "ollama";

    private String apiUrl;

    /** Left blank for local Ollama. Only used (and only sent) for non-Ollama providers. */
    private String apiKey;

    private String model;
    private int timeoutSeconds = 600;
    private int maxTokens = 1200;
    private int maxContentChars = 60000;
    private int maxRetries = 1;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getMaxContentChars() { return maxContentChars; }
    public void setMaxContentChars(int maxContentChars) { this.maxContentChars = maxContentChars; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public boolean isOllama() {
        return provider == null || provider.isBlank() || "ollama".equalsIgnoreCase(provider.trim());
    }
}
