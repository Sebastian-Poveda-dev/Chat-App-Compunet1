##  Cómo ejecutar

En la **raíz del proyecto**:

```bash
# 1) Compilar
./gradlew clean build

# 2) Correr el servidor
./gradlew :server:run

# 3) Correr el cliente (en otra terminal)
./gradlew :client:run
```
al iniciar veras:
````
Enter your username: s
Available commands:
/msg <username> <message> - Send a direct message to a user
/msgg <groupname> <message> - Send a message to a group
/createg <groupname> <user1 , user2, user3...> - Create a new group
/joing <groupname> - Join an existing group
/leaveg <groupname> - Leave a group
/listg - List all groups you are a member of
/removeg <groupname> <user1 , user2, user3...> - Remove users from a group
/addg <groupname> <user1 , user2, user3...> - Add users to a group
/listu - List all users currently online
/exit - Exit the application
Enter command:
````


Integrantes:

- Santiago Estrada Martinez

- Sebastian Poveda

- Juan Manuel ramirez
