```sh
cd ~/Desktop/homework
mvn compile dependency:copy-dependencies
docker start homework-zookeeper
```

```sh
java -cp 'target/classes:target/dependency/*' ru.homework.Server 1099 1100
```

```sh

java -cp 'target/classes:target/dependency/*' ru.homework.Server 2099 2100
```

```sh
java -cp 'target/classes:target/dependency/*' ru.homework.Client
```
