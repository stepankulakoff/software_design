```sh
docker compose up -d --wait
mvn clean verify
```

```sh
java -jar rate-provider/target/rate-provider-1.0.0.jar --server.port=8081
```

```sh
java -jar rate-provider/target/rate-provider-1.0.0.jar --server.port=8082
```

```sh
java -jar rate-printer/target/rate-printer-1.0.0.jar
```

```sh
curl -s http://localhost:8081/rpc -H 'Content-Type: application/json' -d '{"jsonrpc":"2.0","method":"getRate","id":1}'
```

```sh
open http://localhost:9292
```

```sh
mvn -pl rate-printer clean verify
mvn -pl rate-provider clean verify
```

```sh
docker compose stop
```
