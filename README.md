```sh
docker compose up -d --wait
mvn clean verify
docker compose --profile apps up -d --wait --force-recreate service1 service2 client
```

```sh
open http://localhost:3000/d/currency-homework
```

```sh
curl -u admin:homework http://localhost:3000/api/health
open http://localhost:9090/targets
open http://localhost:9292
```

```sh
curl -s http://localhost:8081/rpc -H 'Content-Type: application/json' -H 'X-Client-Id: manual' -d '{"jsonrpc":"2.0","method":"getRate","id":1}'
curl -s http://localhost:8081/actuator/prometheus
```

```sh
docker compose logs -f service1 service2 client
```

```sh
docker compose --profile apps stop
```
