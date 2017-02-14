package com.steveperkins.config;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TODO: Document
 */
public class PropertiesClient {

    /**
     * TODO: Document
     */
    public static Map<String, String> loadProperties() {

        // Validate system properties
        final String environment = Optional.ofNullable(System.getProperty("ENVIRONMENT")).orElse("local");
        final String application = Optional.ofNullable(System.getProperty("APPLICATION"))
                .orElseThrow(() -> new RuntimeException("The system property \"APPLICATION\" is not set"));
        final String consulHost = Optional.ofNullable(System.getProperty("CONSUL_HOST"))
                .orElseThrow(() -> new RuntimeException("The system property \"CONSUL_HOST\" is not set"));
        final String vaultUrl = Optional.ofNullable(System.getProperty("VAULT_URL"))
                .orElseThrow(() -> new RuntimeException("The system property \"VAULT_URL\" is not set"));
        final String vaultUsername = Optional.ofNullable(System.getProperty("VAULT_USERNAME"))
                .orElseThrow(() -> new RuntimeException("The system property \"VAULT_USERNAME\" is not set"));
        final String vaultPassword = Optional.ofNullable(System.getProperty("VAULT_PASSWORD"))
                .orElseThrow(() -> new RuntimeException("The system property \"VAULT_PASSWORD\" is not set"));

        // TODO
        if (environment.equals("local")) {
            return fromLocal();
        }

        try {
            // Connect with Consul and Vault
            final ConsulClient consul = new ConsulClient(consulHost);
            final Vault vault = vaultConnection(vaultUrl, vaultUsername, vaultPassword);

            // Iterate over all properties stored by Consul in this environment's and application's path
            final String consulPath = String.format("%s/%s", environment, application);
            final Map<String, String> properties = new HashMap<>();
            for (final GetValue property : consul.getKVValues(consulPath).getValue()) {

                // Load each key-value property from Consul...
                final String key = property.getKey().substring(property.getKey().lastIndexOf('/') + 1);
                String value = property.getDecodedValue();

                // ... and if the value references a Vault path, then read its true value from Vault.
                if (value.startsWith("__vault__")) {
                    final String vaultPath = value.substring(9);
                    final LogicalResponse logicalResponse = vault.logical().read(vaultPath);
                    value = logicalResponse.getData().get(key);
                }

                properties.put(key, value);
            }
            return properties;
        } catch (VaultException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: Implement, and the document
     *
     * @return
     */
    private static Map<String, String> fromLocal() {
        return null;
    }

    private static Vault vaultConnection(final String vaultUrl, final String vaultUsername, final String vaultPassword)
            throws VaultException {
        final VaultConfig authVaultConfig = new VaultConfig(vaultUrl);
        final Vault authVault = new Vault(authVaultConfig);
        final AuthResponse authResponse = authVault.auth().loginByUserPass(vaultUsername, vaultPassword);
        final String token = authResponse.getAuthClientToken();

        final VaultConfig vaultConfig = new VaultConfig(vaultUrl, token);
        return new Vault(vaultConfig);
    }

}
