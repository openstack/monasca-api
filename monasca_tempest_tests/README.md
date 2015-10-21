# Introduction
The Monasca Tempest Tests use the [OpenStack Tempest Plugin Interface](http://docs.openstack.org/developer/tempest/plugin.html). This README describes how to configure and run them using a variety of methods.
Currently the monasca-vagrant environment is needed to run the tests. Instructions on setting up a monasca-vagrant environment can be found here: https://github.com/openstack/monasca-vagrant. This document will be updated when 
the environment is switched to the DevStack environment.

# Configuring to run the Monasca Tempest Tests
1. Clone the OpenStack Tempest repo, and cd to it.

   ```
   git clone https://git.openstack.org/openstack/tempest.git
   cd tempest
   ```
2. Create a virtualenv for running the Tempest tests and activate it. For example in the Tempest root dir

    ```
    virtualenv .venv
    source .venv/bin/activate
    ``` 
3. Install the Tempest requirements in the virtualenv.

    ```
    pip install -r requirements.txt -r test-requirements.txt
    pip install nose
    ```
4. Create ```etc/tempest.conf``` in the Tempest root dir by running the following command:

    ```
    oslo-config-generator --config-file  etc/config-generator.tempest.conf  --output-file etc/tempest.conf
    ```

    Add the following sections to ```tempest.conf``` for testing using the monasca-vagrant environment.

   ```
    [identity]

    username = mini-mon
    password = password
    tenant_name = mini-mon
    domain_name = default
    admin_username = admin
    admin_password = admin
    admin_domain_name = default
    admin_tenant_name = admin
    alt_username = mini-mon
    alt_password = password
    alt_tenant_name = mini-mon
    use_ssl = False
    auth_version = v3
    uri = http://192.168.10.5:5000/v2.0/
    uri_v3 = http://192.168.10.5:35357/v3/

    [auth]

    allow_tenant_isolation = true
    tempest_roles = monasca-user

    ```

    Edit the the variable values in the identity section to match your particular monasca-vagrant environment.

5. Create ```etc/logging.conf``` in the Tempest root dir by making a copying ```logging.conf.sample```.

6. Clone the monasca-api repo in a directory somewhere outside of the Tempest root dir.

7. Install the monasca-api in your venv, which will also register
   the Monasca Tempest Plugin as, monasca_tests.

   cd into the monasa-api root directory. Making sure that the tempest virtual env is still active,
   run the following command.

    ```    
    python setup.py install
    ```

See the [OpenStack Tempest Plugin Interface](http://docs.openstack.org/developer/tempest/plugin.html), for more details on Tempest Plugins and the plugin registration process.

# Running the Monasca Tempest Tests
The Monasca Tempest Tests can be run using a variety of methods including:
1. [Testr](https://wiki.openstack.org/wiki/Testr)
2. [Os-testr](http://docs.openstack.org/developer/os-testr/)
3. [PyCharm]([Os-testr](https://www.jetbrains.com/pycharm/)

## Run the tests from the CLI using testr

[Testr](https://wiki.openstack.org/wiki/Testr) is a test runner that can be used to run the Tempest tests.

1. In the Tempest root dir, create a list of the Monasca Tempest Tests in a file.

    ```
    testr list-tests monasca_tempest_tests > monasca_tempest_tests

    ```

2. Run the tests using testr

    ```
    testr run --load-list=monasca_tempest_tests
    ```
You can also use testr to create a list of specific tests for your needs.

## Run the tests from the CLI using os-testr (no file necessary)
[Os-testr](http://docs.openstack.org/developer/os-testr/) is a test wrapper that can be used to run the Monasca Tempest tests.

1. In the Tempest root dir:

    ```
    ostestr --regex monasca_tempest_tests
    ````

## Running/Debugging the Monasca Tempest Tests in PyCharm
Assuming that you have already created a PyCharm project for the ```monasca-api``` do the following:
1. In PyCharm, Edit Configurations and add a new Python tests configuration by selecting Python tests->Nosetests.
2. Name the test. For example TestVersions.
3. Set the path to the script with the tests to run. For example, ~/repos/monasca-api/monasca_tempest_tests/api/test_versions.py
4. Set the name of the Class to test. For example TestVersions.
5. Set the working directory to your local root Tempest repo. For example, ~/repos/tempest.
6. Select the Python interpreter for your project to be the same as the one virtualenv created above. For example, ~/repos/tempest/.venv
7. Run the tests. You should also be able to debug them.
8. Step and repeat for other tests.

# References
This section provides a few additional references that might be useful:
* [Tempest - The OpenStack Integration Test Suite](http://docs.openstack.org/developer/tempest/overview.html#quickstart)
* [Tempest Configuration Guide](https://github.com/openstack/tempest/blob/master/doc/source/configuration.rst#id1)
* [OpenStack Tempest Plugin Interface](http://docs.openstack.org/developer/tempest/plugin.html)

In addition to the above references, another source of information is the following OpenStack projects:
* [Manila Tempest Tests](https://github.com/openstack/manila/tree/master/manila_tempest_tests)
* [Congress Tempest Tests](https://github.com/openstack/congress/tree/master/congress_tempest_tests).
In particular, the Manila Tempest Tests were used as a reference implementation to develop the Monasca Tempest Tests. There is also a wiki [HOWTO use tempest with manila](https://wiki.openstack.org/wiki/Manila/docs/HOWTO_use_tempest_with_manila) that might be useful for Monasca too.

# Issues
* Update documentation for testing using Devstack when available.
* Consider changing from monasca_tempest_tests to monasca_api_tempest_tests.
