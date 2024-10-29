# How to debug

If you want to modify or debug the code locally, you can refer to the following methods. The following operations use [Intellij IDEA](https://www.jetbrains.com/idea/) as an example.

Debugging can be divided into two types: local debugging, where both the code and binary packages are on the local machine, and remote debugging, where the source code is local but the binary program is deployed remotely.

Below are the descriptions for each type:

## Local Debugging

### Some Settings

We assume that the locally deployed `Addax` is in the `/opt/app/addax/4.1.8` folder. There is a `job.json` configuration file in its `job` directory with the following content:

=== "job/job.json"

  ```json
  --8<-- "jobs/quickstart.json"
  ```

The above job file did not run as expected. We suspect that there is an issue with the `parseMixupFunctions` function of the `streamreader` plugin. I want to debug to see where the specific problem is.

### Configuring IDEA

Open the IDEA tool and the `addax` project source code. Open the `plugin/reader/streamreader/StreamReader.java` file, find the `parseMixupFunctions` function, and add a breakpoint at the function declaration by clicking on the left edge. As shown in the figure below:

![setup debug point](/images/debug-1.png)

Click on the `Run->Edit Configurations...` menu in `IDEA`. In the popped-up `Run/Debug Configurations` window, click the `+` button in the top left corner, then select `Application`. In the configuration box on the right, fill in the relevant information as follows:

- Name: Debug description name, you can fill it in as you like
- Run on: Select `Local machine`
- Build and run:
  - First box: Select the JDK version, currently strictly tested on version 1.8, it is recommended to choose version 1.8
  - Second box: Select the `addax-core` module
  - `Main class`: Fill in `com.wgzhao.addax.core.Engine`
  - Click `Modify options`, in the drop-down box that appears, select `Add VM Options`, and in the added `VM Options`, fill in `-Daddax.home=/opt/app/addax/4.1.8`
  - `Program arguments`: Fill in `-job job/job.json`
- `Working directory`: Fill in `/opt/app/addax/4.1.8`

Keep other settings unchanged, and click the `Apply` button. You will get a configuration similar to the one below:

![setup debug configuration](/images/debug-2.png)

Click the `OK` button to save the above configuration. Return to the main window of `IDEA`. In the window menu bar, to the right of the green `🔨`, you should see the description file you just configured, similar to the figure below:

![debug profile](/images/debug-3.png)

Click the green DEBUG bug button in the screenshot above to enter debugging. You will get a debug window similar to the one below:

![run debug](/images/debug-4.png)

## Remote Debugging

### Some Assumptions

Assume the program is deployed on a remote server and you need to debug the program running on the remote server. Assume the remote server IP address is `192.168.1.100`, and `Addax` is deployed in the `/opt/addax/4.0.3` directory. In its `job` folder, there is also a `job.json` file as described in the local debugging section.
Similarly, the above job file did not run as expected. We suspect that there is an issue with the `parseMixupFunctions` function of the `streamreader` plugin. I want to debug to see where the specific problem is.

Note: Remote debugging requires opening port `9999` on the server, so make sure port `9999` on the server is not occupied. If it is occupied, you need to change this port.

The modification method is as follows:

1. Open the `bin/addax.sh` script
2. Locate around line 24 and find the `address=0.0.0.0:9999` string
3. Change `9999` to another unused port
4. Save and exit

### Configuring IDEA

Open the IDEA tool and the `addax` project source code. Open the `plugin/reader/streamreader/StreamReader.java` file, find the `parseMixupFunctions` function, and add a breakpoint at the function declaration by clicking on the left edge. As shown in the figure below:

![setup debug point](/images/debug-1.png)

Click on the `Run->Edit Configurations...` menu in `IDEA`. In the popped-up `Run/Debug Configurations` window, click the `+` button in the top left corner, then select `Remote JVM Debug`. In the configuration box on the right, fill in the relevant information as follows:

- Name: Debug description name, you can fill it in as you like
- Configuration:
  - Host: Fill in the remote server IP address, here fill in `192.168.1.100`
  - Port: Fill in the debug port, here fill in `9999` or the port you changed to

Keep other settings unchanged, and click the `Apply` button. You will get a configuration similar to the one below:

![setup remote debug config](/images/debug-5.png)

Click the `OK` button to save and return to the main window of `IDEA`.

Make sure the description configuration you filled in the `Name` field is selected to the right of the green `🔨` in the window toolbar. If not, select the configuration from the drop-down box.

![remove debug profile](/images/debug-6.png)

### Running the Debug

Running remote debugging involves two steps: starting the program and connecting the debugging tool to the running program.

Run the following command on the remote server:

`bin/addax.sh -d job/job.json`

If it runs normally, you will get the following information:

```shell
bin/addax.sh -d job/job.json
Listening for transport dt_socket at address: 9999
```

This indicates that the program is listening on port `9999` and waiting for a connection.

Return to the `IDEA` window and click the green DEBUG bug button on the toolbar to start debugging. If it runs normally, you will get a debug window similar to the one below:

![running remote debug](/images/debug-7.png)
