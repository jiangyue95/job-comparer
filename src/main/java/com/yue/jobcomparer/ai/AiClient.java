package com.yue.jobcomparer.ai;

public interface AiClient {
    /**
     * Send a prompt to the AI provider and return the raw text reponse.
     *
     * @param prompt the prompt text
     * @return the AI's response as a string
     */
    String chat(String prompt);
}
