package net.bigyous.gptgodmc.GPT.Json;

import java.util.ArrayList;
import java.util.List;

// https://ai.google.dev/api/caching#Content
public class Content {
    public enum Role {
        user,
        model
    }

    private ArrayList<Part> parts = new ArrayList<>();
    private Role role;

    public Content() {
        role = Role.user;
    }

    public Content(Role role, ArrayList<Part> parts) {
        this.parts = parts;
        this.role = role;
    }

    public Content(Role role, Part[] parts) {
        this.role = role;
        for (Part part : parts) {
            this.parts.add(part);
        }
    }

    public Content(ArrayList<Part> parts) {
        this.parts = parts;
        role = Role.user;
    }

    public Content(String message) {
        this.role = Role.user;
        this.parts = new ArrayList<Part>();
        this.parts.add(new Part(message));
    }

    public Content(String[] messages) {
        this.role = Role.user;
        this.parts = new ArrayList<Part>();
        for (String msg : messages) {
            this.parts.add(new Part(msg));
        }
    }

    public Content(Role role, String message) {
        this.role = role;
        this.parts = new ArrayList<Part>();
        this.parts.add(new Part(message));
    }

    public Content(Role role, String[] messages) {
        this.role = role;
        this.parts = new ArrayList<Part>();
        for (String msg : messages) {
            this.parts.add(new Part(msg));
        }
    }

    public Content(Role role, List<String> messages) {
        this.role = role;
        this.parts = new ArrayList<Part>();
        for (String msg : messages) {
            this.parts.add(new Part(msg));
        }
    }

    public ArrayList<Part> getParts() {
        return parts;
    }

    public void setParts(ArrayList<Part> parts) {
        this.parts = parts;
    }

    public Role getRole() {
        return role;
    }

    // calculates and returns the token count of this content
    public int countTokens() {
        int accumulator = 0;
        for (Part part : this.parts) {
            accumulator += part.countTokens();
        }
        return accumulator;
    }
}