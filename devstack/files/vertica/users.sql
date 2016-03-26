CREATE USER mon_api IDENTIFIED BY 'password';
GRANT monasca_api TO mon_api;
ALTER USER mon_api DEFAULT ROLE monasca_api;

CREATE USER mon_persister IDENTIFIED BY 'password';
GRANT monasca_persister TO mon_persister;
ALTER USER mon_persister DEFAULT ROLE monasca_persister;
