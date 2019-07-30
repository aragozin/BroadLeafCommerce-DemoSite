This is demo project and Java profiling workshops
=================================================

Start application
-----------------

    mvn -P run test

Application store front is available at [http://localhost:8080](http://localhost:8080).

Start application and load generator
------------------------------------

    mvn -P run_n_load test

Stopping processes
------------------

Following command would remove `pids` directory causing demo to stop.

    mvn clean

You can also remove individual files in `pids` directory to signal processes to stop.


Logs and diganostic
-------------------

Console output and GC logs could be found in `val/PROCNAME/logs`