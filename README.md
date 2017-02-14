config-client-lib
=================
This is one of three Git repositories, which work together to demonstrate using [Consul](https://www.consul.io) and 
[Vault](https://www.vaultproject.io) for configuration management:

* https://github.com/steve-perkins/config-properties - Contains:
  * Properties files for every environment (e.g. "dev", "staging", "production") in a hypothetical enterprise, and 
    every application within each environment.
  * A processor script, run by Gradle, which syncs all of the property information with Consul every time the Git 
    repository is updated.
* https://github.com/steve-perkins/config-client-lib - A client library which loads the appropriate config properties 
  for a given environment and application from Consul and Vault.
* https://github.com/steve-perkins/config-sample-app - A sample web application which retrieves its config properties 
  using the client library, and displays them in the browser.
  
This demo shows the use case of having two types of config properties:

1. **non-secret** values, which can and should be maintainable by developer teams (e.g. JDBC URL's).
2. **secret** values, which should only be viewable or maintainable by people with specialized access (e.g. 
   usernames and passwords)
   
The non-secret values are stored as-is in the `config-properties` repo, and loaded directly into Consul.  For *secret* 
values, Git (and Consul) store a Vault path for that property.  When the `config-client-lib` library encounters a 
secret, it loads the "true" value from this Vault path... and applications such as `config-sample-app` are none the 
wiser.

Setup
=====
1. Perform all of the steps outlined in the [config-properties](https://github.com/steve-perkins/config-properties) 
   project's README file.
2. Build this client lib, so that it will available in your local Maven dependency cache:

```
gradlew publishToMavenLocal
```

3. See [config-sample-app](https://github.com/steve-perkins/config-sample-app) project README for next steps.
