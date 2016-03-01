mv "${WORKSPACE}/target/elixir-beacon-0.0.1-SNAPSHOT.jar" "${WORKSPACE}/target/elixirbeacon-service.jar"
scp "${WORKSPACE}/target/elixirbeacon-service.jar"  omartinez@apptest01:
ssh omartinez@apptest01 "sudo bash -c '/etc/init.d/elixirbeaconservice_test stop && sleep 30'"
ssh omartinez@apptest01 "sudo bash -c 'cp /home/omartinez/elixirbeacon-service.jar /microservices/elixirbeaconservice/test/ && chown -R elixirbeaconservice.elixirbeaconservice /microservices/elixirbeaconservice/test/'"
ssh omartinez@apptest01 "sudo bash -c 'sleep 30 && cd /microservices/elixirbeaconservice/test/ && nohup /etc/init.d/elixirbeaconservice_test start > /dev/null 2>&1 &'"
