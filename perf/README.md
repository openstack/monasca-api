
monasca-api performance benchmarking
=============

Recommended Configuration
=============

Install
=======

Install JMeter

JMeter can be found at http://jmeter.apache.org/download_jmeter.cgi

add JMeter bin to the path: PATH=$PATH:~/.../bin

Monasca-query performance test
==============================

This test is designed to work with data created from persister-perf performance test but
can work with any monasca-api/db configuration.
monasca-api will need to have region configured to support test data.
JMeter uses monasca-api to query db backend.

Load monasca_query_test.jmx into jmeter.
Setup user defined variables for your environment.

    keystone_server       <ip>          keystone server ip address
    monasca-api_server    <ip>          monasca-api server ip address
    keystone_user         admin	    keystone user with monitoring permissions
    keystone_password     secretadmin   password for keystone user
    tenant_id             tenant_1	    tenant id set in monascas-api/keystone.

Run tests.
