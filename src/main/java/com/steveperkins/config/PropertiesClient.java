package com.steveperkins.config;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class PropertiesClient {

    /**
     * <p>Called by the application, at startup or when updating config properties with their current values
     * during runtime.  Loads all non-secret properties from Consul.  When the value found in Consul
     * contains a Vault path (i.e. it's a secret property), then the true value is loaded from Vault.</p>
     *
     * <p>This method expects to find several JVM system properties... telling it which Consul and Vault
     * instances to access and for which environment and application.  If these properties are not found,
     * then the application is presumable running in local mode (i.e. on a developer's workstation), and
     * so the library loads properties from a <code>local.properties</code> file in the classpath instead.</p>
     *
     * @return A Map of property key-value pairs
     */
    public static Map<String, String> loadProperties() {

        // Validate system properties
        final String environment = Optional.ofNullable(System.getProperty("ENVIRONMENT")).orElse("local");
        if (environment.equals("local")) {
            return fromLocal();
        }
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

    private static Map<String, String> fromLocal() {
        final Properties properties = new Properties();
        try (final InputStream input = PropertiesClient.class.getResourceAsStream("/local.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Map<String, String> returnValue = new HashMap<>();
        for (final Enumeration props = properties.propertyNames(); props.hasMoreElements(); ) {
            final String key = props.nextElement().toString();
            returnValue.put(key, properties.getProperty(key));
        }
        return returnValue;
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
