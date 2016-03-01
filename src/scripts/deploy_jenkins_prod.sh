mv "${WORKSPACE}/target/elixir-beacon-0.0.1-SNAPSHOT.jar" "${WORKSPACE}/target/elixir-beacon.jar"
scp "${WORKSPACE}/target/elixir-beacon.jar"  omartinez@app:
ssh omartinez@app "sudo bash -c '/etc/init.d/elixirbeaconservice_prod stop && sleep 30'"
ssh omartinez@app "sudo bash -c 'cp /home/omartinez/elixir-beacon.jar /microservices/elixirbeaconservice/ && chown -R elixirbeaconservice.elixirbeaconservice /microservices/elixirbeaconservice/'"
ssh omartinez@app "sudo bash -c 'sleep 30 && cd /microservices/elixirbeaconservice/ && nohup /etc/init.d/elixirbeaconservice_prod start > /dev/null 2>&1 &'"
