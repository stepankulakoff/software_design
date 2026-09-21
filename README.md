```sh
cp -n env/dev.env.example env/dev.env
cp -n env/prod.env.example env/prod.env
```

```sh
docker compose --env-file env/dev.env up -d --wait
scripts/build.sh hw5-1
```

```sh
scripts/release.sh hw5-1 dev
scripts/release.sh hw5-1 prod
scripts/run.sh releases/dev-hw5-1
scripts/run.sh releases/prod-hw5-1
```

```sh
open http://localhost:3000/d/currency-homework
open http://localhost:13000/d/currency-homework
open http://localhost:9090/targets
open http://localhost:19090/targets
```

```sh
docker compose --project-directory releases/dev-hw5-1 --profile apps ps
docker compose --project-directory releases/prod-hw5-1 --profile apps ps
```

```sh
curl -s http://localhost:8081/rpc -H 'Content-Type: application/json' -H 'X-Client-Id: manual' -d '{"jsonrpc":"2.0","method":"getRate","id":1}'
curl -s http://localhost:18081/rpc -H 'Content-Type: application/json' -H 'X-Client-Id: manual' -d '{"jsonrpc":"2.0","method":"getRate","id":1}'
```

```sh
docker compose --project-directory releases/dev-hw5-1 logs -f service1 service2 client
```

```sh
docker compose --project-directory releases/dev-hw5-1 stop service1
docker compose --project-directory releases/dev-hw5-1 start service1
```

```sh
docker compose --project-directory releases/dev-hw5-1 --profile apps stop
docker compose --project-directory releases/prod-hw5-1 --profile apps stop
```
