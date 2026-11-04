# passgen_app

Installs the Java 21 runtime on Ubuntu, copies the Password Generator API JAR to
`/opt/passgen`, writes `/etc/passgen/application.properties` and runs the app as the
`passgen` systemd service.

The skeleton follows `ansible-galaxy init passgen_app`.

| Variable | Default | Meaning |
|---|---|---|
| `app_port` | `8081` | Port the API listens on |
| `app_max_password_length` | `64` | `passgen.max-length` |
| `app_java_opts` | `-Xmx256m` | JVM options |
| `app_artifact` | `""` | JAR to deploy; empty = newest `target/password-generator-api-*.jar` |

Handlers: `Reload systemd` (unit file changed) and `Restart passgen` (JAR, config or unit
changed). A run that changes nothing restarts nothing.
