# (C) Copyright 2015 Hewlett-Packard Development Company, L.P.
# Copyright 2015 FUJITSU LIMITED

from __future__ import print_function
from keystoneclient.v2_0 import client
import sys


def get_token(url, cacert, username, password, tenant_name):
    if not username or not password:
        print('If token is not given, keystone_admin and keystone_admin_password must be given', file=sys.stderr)
        return False

    if not tenant_name:
        print('If token is not given, keystone_admin_project must be given', file=sys.stderr)
        return False

    kwargs = {
        'username': username,
        'password': password,
        'tenant_name': tenant_name,
        'auth_url': url,
        'cacert': cacert
    }

    key = client.Client(**kwargs)
    token = key.auth_token
    return token


def get_tenant(key, tenant_name):
    """Get the tenant by name"""
    for tenant in key.tenants.list():
        if tenant.name == tenant_name:
            return tenant

    return None


def add_tenants(key, tenant_names):
    """Add the given tenant_names if they don't already exist"""
    for tenant_name in tenant_names:
        if not get_tenant(key, tenant_name):
            key.tenants.create(tenant_name=tenant_name, enabled=True)
            print("Created tenant/project '{}'".format(tenant_name))

    return True


def get_user(key, user_name):
    for user in key.users.list():
        if user.name == user_name:
            return user
    return None


def get_role(key, role_name):
    for role in key.roles.list():
        if role.name == role_name:
            return role
    return None


def add_users(key, users):
    """Add the given users if they don't already exist"""
    for user in users:
        if not get_user(key, user['username']):
            tenant_name = user['project']
            tenant = get_tenant(key, tenant_name)

            password = user['password']
            if 'email' in user:
                email = user['email']
            else:
                email = None

            key.users.create(name=user['username'], password=password,
                             email=email, tenant_id=tenant.id)
            print("Created user '{}'".format(user['username']))
    return True


def add_user_roles(key, users):
    """Add the roles for the users if they don't already have them"""
    for user in users:
        if 'role' not in user:
            continue
        role_name = user['role']
        keystone_user = get_user(key, user['username'])
        tenant = get_tenant(key, user['project'])
        for role in key.roles.roles_for_user(keystone_user, tenant=tenant):
            if role.name == role_name:
                return True

        role = get_role(key, role_name)
        if not role:
            role = key.roles.create(role_name)
            print("Created role '{}'".format(role_name))

        key.roles.add_user_role(keystone_user, role, tenant)
        print("Added role '{}' to user '{}'".format(role_name, user['username']))
    return True

def add_service_endpoint(key, name, description, type, url, region):
    """Add the Monasca service to the catalog with the specified endpoint, if it doesn't yet exist."""
    service_names = {service.name: service.id for service in key.services.list()}
    if name in service_names.keys():
        service_id = service_names[name]
    else:
        service = key.services.create(name=name, service_type=type, description=description)
        print("Created service '{}' of type '{}'".format(name, type))
        service_id = service.id

    for endpoint in key.endpoints.list():
        if endpoint.service_id == service_id:
            if endpoint.publicurl == url and endpoint.adminurl == url and endpoint.internalurl == url:
                return True
            else:
                key.endpoints.delete(endpoint.id)

    key.endpoints.create(region=region, service_id=service_id, publicurl=url, adminurl=url, internalurl=url)
    print("Added service endpoint '{}' at '{}'".format(name, url))
    return True


def add_monasca_service():
    return True


def main(argv):
    """ Get token if needed and then call methods to add tenants, users and roles """
    users = [{'username': 'mini-mon', 'project': 'mini-mon', 'password': 'password', 'role': 'monasca-user'},
             {'username': 'monasca-agent', 'project': 'mini-mon', 'password': 'password', 'role': 'monasca-agent'},
             {'username': 'demo', 'project': 'demo', 'password': 'secretadmin', 'role': 'monasca-user'}]

    url = 'http://127.0.0.1:35357/v2.0'

    token = None

    cacert = None

    if not token:
        username = 'admin'
        password = 'secretadmin'
        tenant_name = 'admin'
        token = get_token(url, cacert, username, password, tenant_name)

    key = client.Client(token=token, endpoint=url, cacert=cacert)

    tenants = []
    for user in users:
        if 'project' in user and user['project'] not in tenants:
            tenants.append(user['project'])

    if not add_tenants(key, tenants):
        return 1

    if not add_users(key, users):
        return 1

    if not add_user_roles(key, users):
        return 1

    service_host = argv[0]
    monasca_url = 'http://' + service_host + ':8070/v2.0'

    if not add_service_endpoint(key, 'monasca', 'Monasca monitoring service', 'monitoring', monasca_url, 'RegionOne'):
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
