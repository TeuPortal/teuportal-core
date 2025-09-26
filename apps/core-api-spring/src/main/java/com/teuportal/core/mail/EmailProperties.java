package com.teuportal.core.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    public enum Provider {
        SMTP
    }

    private boolean enabled = false;
    private Provider provider = Provider.SMTP;
    private String defaultFrom = "";
    private final Smtp smtp = new Smtp();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getDefaultFrom() {
        return defaultFrom;
    }

    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    public Smtp getSmtp() {
        return smtp;
    }

    public static class Smtp {
        private String host = "";
        private int port = 587;
        private boolean auth = false;
        private boolean startTls = true;
        private String username = "";
        private String password = "";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isAuth() {
            return auth;
        }

        public void setAuth(boolean auth) {
            this.auth = auth;
        }

        public boolean isStartTls() {
            return startTls;
        }

        public void setStartTls(boolean startTls) {
            this.startTls = startTls;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
