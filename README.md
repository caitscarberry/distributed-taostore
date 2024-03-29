
Replicated TaoStore
========
NOTE FOR PEOPLE RUNNING EXPERIMENTS: This branch (master) is used to get load test data for efficient replication. The naive_solution branch is used to get load test data for naive replication _and_ no replication.

This is a modification of TaoStore that allows it to atomically replicate data across multiple machines. The system consists of three layers: TaoClient, TaoProxy, and TaoServer. Together, a pair of TaoProxy and TaoServer forms a _replica unit_. These pairs are specified at start-up by assigning each TaoProxy and TaoServer a unit ID. Each TaoProxy can only contact its corresponding TaoServer, and vice versa. A TaoServer is a storage server which stores data in a tree, and a TaoProxy writes to the TaoServer on behalf of TaoClients. Any TaoClient can contact any TaoProxy.

To perform a replicated read or write operation, a TaoClient must contact a majority of replica units.


***** 

**Basic Usage:**
All dependencies are included in the lib directory.

To compile, navigate to the repository directory and run `mvn package`.

To run:

 * Navigate to the `target` directory (generated by previous step).
 * TaoServer: `java -cp ../lib/commons-math3-3.6.1.jar:..ib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoServer.TaoServer --unit [int]`
 * TaoProxy: `java -cp ../lib/commons-math3-3.6.1.jar:..ib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoProxy.TaoProxy --unit [int]`
 * TaoClient:
    * Interactive mode: `java -cp ../lib/commons-math3-3.6.1.jar:..ib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoClient.TaoClient --id [int]`
    * Load test mode: `java -cp ../lib/commons-math3-3.6.1.jar:../lib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoClient.TaoClient --runType load_test --clients [int] --warmup_operations [int] --load_test_length [int] `

***** 
  
**Command Line Arguments:**
 * For TaoClient, TaoProxy, TaoServer
 * --config_file full_path_to_file
    * The full path to a config file 
    * Default file is *config.properties*
    * Any properties not specified in config file will take default values from `src/main/resources/Configuration/TaoDefaultConfigs`
 * For TaoClient and TaoProxy
  * --unit
    * The unit number of the TaoClient and TaoProxy.
    * Must be specified.
 * For TaoClient
 * --runType
    * Can be either *interactive* or *load_test*
    * Default is *interactive*
    * An interactive lets the user do manual operations 
    * A load test runs a load test
    * Note that a load test begins after the client does *warmup_operations* operations
 * --clients
    * Specifies the number of concurrent clients that should run during the load test
    * Default is 1
 * --warmup_operations
    * The amount of operations to perform before beginning the load test
    * Default is 100
 * --load_test_length
    * Length in milliseconds of load test
    * Default is 2 minutes
 * --id
    * Client ID
    * Only used in interactive mode.
    * If you run multiple interactive clients at once, they must have different IDs.

***** 

**Example Usage**
In this example, we'll start three replica units and one TaoClient.

First, check `src/main/resources/Configuration/TaoDefaultConfigs`. By default, the configuration is set up to run three replica units and a client on one machine. Verify that all port numbers listed in the config file are free on your machine. 

In separate terminals, from the `target` directory:

 * `java -cp ../lib/commons-math3-3.6.1.jar:../lib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoServer.TaoServer --unit 0`
 * `java -cp ../lib/commons-math3-3.6.1.jar:../lib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoProxy.TaoProxy --unit 0`
 * `java -cp ../lib/commons-math3-3.6.1.jar:../lib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoServer.TaoServer --unit 1`
 * `java -cp ../lib/commons-math3-3.6.1.jar:../lib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoProxy.TaoProxy --unit 1`
 * `java -cp ../lib/commons-math3-3.6.1.jar:../lib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoServer.TaoServer --unit 2`
 * `java -cp ../lib/commons-math3-3.6.1.jar:../lib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoProxy.TaoProxy --unit 2`

This will initialize all three replica units. After initialization is finished (indicated by "Finished init, running proxy" appearing in the terminal window of each TaoProxy instance), we can run the TaoClient. For this example, we'll be running the client in interactive mode.

In a new terminal, run `java -cp ../lib/commons-math3-3.6.1.jar:../lib/guava-19.0.jar:TaoServer-1.0-SNAPSHOT.jar TaoClient.TaoClient --id 0`. Follow the instructions given by the program to read and write to/from the system.

****
Usage notes:

 - Before running a load test, make sure that the max_client_id configuration property is at least 1 + clients.

