package net.bigyous.gptgodmc.GPT.Json;

public class GenerateContentResponse {
    // https://ai.google.dev/api/generate-content#UsageMetadata
    public class UsageMetadata {
        private int promptTokenCount;
        private int cachedContentTokenCount;
        private int candidatesTokenCount;
        private int totalTokenCount;

        public int getPromptTokenCount() {
            return promptTokenCount;
        }

        public int getCachedContentTokenCount() {
            return cachedContentTokenCount;
        }

        public int getCandidatesTokenCount() {
            return candidatesTokenCount;
        }

        public int getTotalTokenCount() {
            return totalTokenCount;
        }

    }

    Candidate[] candidates;
    // PromptFeedback promptFeedback;
    UsageMetadata usageMetadata = new UsageMetadata();

    GoogError error;

    // shortcut to just return the first candidate response
    public String getText() {
        return candidates[0].getText();
    }

    public Candidate[] getCandidates() {
        return candidates;
    }

    public UsageMetadata getUsageMetadata() {
        return usageMetadata;
    }

    public GoogError getError() {
        return this.error;
    }

    public boolean isError() {
        return this.error != null;
    }
}

class PromptFeedback {

}

class SafetyRating {
    // Define the structure of SafetyRating object
}

class CitationMetadata {
    // Define the structure of CitationMetadata object
}

// https://ai.google.dev/api/generate-content#LogprobsResult
class LogprobsResult {
    // Define the structure of LogprobsResult object
}
