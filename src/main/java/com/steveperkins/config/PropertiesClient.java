package com.steveperkins.config;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TODO: Document
 */
public class PropertiesClient {

    /**
     * TODO: Document
     */
    public Map<String, String> loadProperties() {
        final String environment = Optional.ofNullable(System.getenv("ENVIRONMENT")).orElse("local");
        final String application = Optional.ofNullable(System.getenv("APPLICATION"))
                .orElseThrow(() -> new RuntimeException("The environment variable \"APPLICATION\" is not set"));
        final String consulHost = Optional.ofNullable(System.getenv("CONSUL_HOST"))
                .orElseThrow(() -> new RuntimeException("The environment variable \"CONSUL_HOST\" is not set"));
        final String vaultUrl = Optional.ofNullable(System.getenv("VAULT_URL"))
                .orElseThrow(() -> new RuntimeException("The environment variable \"VAULT_URL\" is not set"));
        final String vaultUsername = Optional.ofNullable(System.getenv("VAULT_USERNAME"))
                .orElseThrow(() -> new RuntimeException("The environment variable \"VAULT_USERNAME\" is not set"));
        final String vaultPassword = Optional.ofNullable(System.getenv("VAULT_PASSWORD"))
                .orElseThrow(() -> new RuntimeException("The environment variable \"VAULT_PASSWORD\" is not set"));

        if (environment.equals("local")) {
            return fromLocal();
        }

        final ConsulClient consulClient = new ConsulClient(consulHost);
        final String consulPath = String.format("%s/%s", environment, application);
        final List<GetValue> properties = consulClient.getKVValues(consulPath).getValue();
        return properties.stream()
                .map(prop -> {
                    final String key = prop.getKey().substring(prop.getKey().lastIndexOf('/') + 1);
                    String value = prop.getValue();
                    if (value.startsWith("__vault__")) {
                        // TODO:  Read from Vault with path value.substring(9)
                    }
                    return new AbstractMap.SimpleEntry<>(key, value);
                }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private Map<String, String> fromLocal() {
        return null;
    }

}
