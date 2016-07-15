# (C) Copyright 2015 Hewlett-Packard Development Company, L.P.
# Copyright 2015 FUJITSU LIMITED

from __future__ import print_function
from keystoneauth1 import session as ks_session
from keystoneauth1.identity import v3
from keystoneclient.v3 import client
import sys


def _get_auth_plugin(auth_url, **kwargs):
    kwargs = {
        'username': kwargs.get('username'),
        'password': kwargs.get('password'),
        'project_name': kwargs.get('project_name'),
        'project_domain_id': kwargs.get('project_domain_id'),
        'user_domain_id': kwargs.get('user_domain_id'),
    }

    return v3.Password(auth_url=auth_url, **kwargs)


def get_default_domain(ks_client):
    """Get the default domain"""
    for domain in ks_client.domains.list():
        if domain.id == "default":
            return domain

    return None


def get_project(ks_client, project_name):
    """Get the project by name"""
    for project in ks_client.projects.list():
        if project.name == project_name:
            return project

    return None


def add_projects(ks_client, project_name):
    """Add the given project_names if they don't already exist"""
    default_domain = get_default_domain(ks_client)
    for project_name in project_name:
        if not get_project(ks_client, project_name):
            ks_client.projects.create(name=project_name,
                                      domain=default_domain,
                                      enabled=True)
            print("Created project '{}'".format(project_name))

    return True


def get_user(ks_client, user_name):
    for user in ks_client.users.list():
        if user.name == user_name:
            return user
    return None


def get_role(ks_client, role_name=None, role_id=None):
    for role in ks_client.roles.list():
        if role.name == role_name or role.id == role_id:
            return role
    return None


def add_users(ks_client, users):
    """Add the given users if they don't already exist"""
    for user in users:
        if not get_user(ks_client, user['username']):
            project_name = user['project']
            project = get_project(ks_client, project_name)

            password = user['password']
            if 'email' in user:
                email = user['email']
            else:
                email = None

            ks_client.users.create(name=user['username'], password=password,
                                   email=email, project_id=project.id)
            print("Created user '{}'".format(user['username']))
    return True


def add_user_roles(ks_client, users):
    """Add the roles for the users if they don't already have them"""
    for user in users:
        if 'role' not in user:
            continue
        role_name = user['role']
        keystone_user = get_user(ks_client, user['username'])
        project = get_project(ks_client, user['project'])
        for assignment in ks_client.role_assignments.list(user=keystone_user,
                                                          project=project):
            role = get_role(ks_client=ks_client, role_id=assignment.role['id'])
            if role.name == role_name:
                return True

        role = get_role(ks_client=ks_client, role_name=role_name)
        if not role:
            role = ks_client.roles.create(role_name)
            print("Created role '{}'".format(role_name))

        ks_client.roles.grant(user=keystone_user, role=role, project=project)
        print("Added role '{}' to user '{}'".format(role_name,
                                                    user['username']))
    return True


def add_service_endpoint(ks_client, name, description, type, url, region,
                         interface):
    """Add the Monasca service to the catalog with the specified endpoint,
    if it doesn't yet exist."""
    service_names = {service.name: service
                     for service in ks_client.services.list()}
    if name in service_names.keys():
        service = service_names[name]
    else:
        service = ks_client.services.create(name=name, type=type,
                                            description=description)
        print("Created service '{}' of type '{}'".format(name, type))

    for endpoint in ks_client.endpoints.list():
        if endpoint.service_id == service.id:
            if endpoint.url == url:
                if endpoint.interface == interface:
                    return True
            else:
                ks_client.endpoints.delete(id=endpoint.id)

    ks_client.endpoints.create(region=region, service=service, url=url,
                               interface=interface)

    print("Added service endpoint '{}' at '{}' (interface: '{}') "
          .format(name, url, interface))
    return True


def add_monasca_service():
    return True


def main(argv):
    """ Get credentials to create a keystoneauth Session to instantiate a
     Keystone Client and then call methods to add users, projects and roles"""
    users = [
        {'username': 'mini-mon',
         'project': 'mini-mon',
         'password': 'password',
         'role': 'monasca-user'},
        {'username': 'monasca-agent',
         'project': 'mini-mon',
         'password': 'password',
         'role': 'monasca-agent'},
        {'username': 'mini-mon',
         'project': 'mini-mon',
         'password': 'password',
         'role': 'admin'},
        {'username': 'admin',
         'project': 'admin',
         'password': 'secretadmin',
         'role': 'monasca-user'},
        {'username': 'demo',
         'project': 'demo',
         'password': 'secretadmin',
         'role': 'monasca-user'},
        {'username': 'monasca-read-only-user',
         'project': 'mini-mon',
         'password': 'password',
         'role': 'monasca-read-only-user'}
    ]

    service_host = argv[0]
    url = 'http://' + service_host + ':35357/v3'

    # FIXME(clenimar): to date, devstack doesn't set domain-related enviroment
    # variables. That's why we need that little workaround when getting those
    # from sys.argv.
    kwargs = {
        'username': argv[1],
        'password': argv[2],
        'project_name': argv[3],
        'project_domain_id': argv[4] if len(argv) > 4 else 'default',
        'user_domain_id': argv[5] if len(argv) > 5 else 'default'
    }

    auth_plugin = _get_auth_plugin(auth_url=url, **kwargs)
    session = ks_session.Session(auth=auth_plugin)
    ks_client = client.Client(session=session)

    projects = []
    for user in users:
        if 'project' in user and user['project'] not in projects:
            projects.append(user['project'])

    if not add_projects(ks_client, projects):
        return 1

    if not add_users(ks_client, users):
        return 1

    if not add_user_roles(ks_client, users):
        return 1

    monasca_url = 'http://' + service_host + ':8070/v2.0'

    endpoint_name = 'monasca'
    endpoint_description = 'Monasca monitoring service'
    endpoint_type = 'monitoring'
    endpoint_region = 'RegionOne'

    if not add_service_endpoint(ks_client, endpoint_name, endpoint_description,
                                endpoint_type, monasca_url, endpoint_region,
                                interface='public'):
        return 1

    if not add_service_endpoint(ks_client, endpoint_name, endpoint_description,
                                endpoint_type, monasca_url, endpoint_region,
                                interface='internal'):
        return 1

    if not add_service_endpoint(ks_client, endpoint_name, endpoint_description,
                                endpoint_type, monasca_url, endpoint_region,
                                interface='admin'):
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
