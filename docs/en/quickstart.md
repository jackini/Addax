# Getting started

## Installation

### with docker

You can directly use the Docker image by executing the following command:

```shell
docker run -it --rm quay.io/wgzhao/addax:latest /opt/addax/bin/addax.sh /opt/addax/job/job.json
```

### with script

If you don't want to compile, you can execute the following command for a one-click installation (currently only supports Linux and macOS):

```shell
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/wgzhao/Addax/master/install.sh)"
```

For macOS, the default installation directory is `/usr/local/addax`. For Linux, it is `/opt/addax`.

### with git

You can choose to compile from source code with the following basic steps:

```shell
git clone https://github.com/wgzhao/addax.git
cd addax
mvn clean package
mvn package assembly:single
cd target/addax/addax-<version>
```

## Starting Your First Data Collection Task

To use `Addax` for data collection, you need to create a task configuration file in JSON format. Below is a simple configuration file. The task reads specified content from memory and prints it out. Save the file as `job/test.json`.

=== "job/test.json"

    ```json
    --8<-- "jobs/quickstart.json"
    ```

Save the above file as `job/test.json`.

Then execute the following command:

```shell
bin/addax.sh job/test.json
```

If there are no errors, you should see output similar to this:

```shell
--8<-- "output/quickstart.txt"
```
