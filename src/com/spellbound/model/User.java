package com.spellbound.model;

import java.io.Serializable;

/**
 * Represents the authenticated user of SpellBound.
 * Implements Serializable so you can save "Session" data later.
 */
public class User implements Serializable {
    private String username;
    private String email;
    private String profileIconPath;
    private String tier; // Added for "Premium Plan" logic in UI

    /**
     * Standard constructor for full profiles.
     */
    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.tier = "Free"; // Default tier
        this.profileIconPath = "/com/spellbound/assets/default_user.png"; 
    }

    /**
     * Helper constructor for quick login (email only).
     */
    public User(String email) {
        this.email = (email != null && !email.isEmpty()) ? email : "guest@spellbound.com";
        // Logic: takes 'john' from 'john@email.com' and capitalizes it: 'John'
        String prefix = this.email.split("@")[0];
        this.username = prefix.substring(0, 1).toUpperCase() + prefix.substring(1).toLowerCase();
        this.tier = "Premium"; // Defaulting to premium for your current UI look
        this.profileIconPath = "/com/spellbound/assets/default_user.png";
    }

    // Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getIconPath() { return profileIconPath; }
    public String getTier() { return tier; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setIconPath(String path) { this.profileIconPath = path; }
    public void setTier(String tier) { this.tier = tier; }

    /**
     * Safely returns the first letter of the email for the UI icon.
     */
    public String getInitial() {
        if (username != null && !username.isEmpty()) {
            return username.substring(0, 1).toUpperCase();
        } else if (email != null && !email.isEmpty()) {
            return email.substring(0, 1).toUpperCase();
        }
        return "S"; // Fallback to "S" for SpellBound
    }

    @Override
    public String toString() {
        return "User: " + username + " (" + tier + ")";
    }
}