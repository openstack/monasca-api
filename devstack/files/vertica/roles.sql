CREATE ROLE monasca_persister;
CREATE ROLE monasca_api;

GRANT USAGE ON SCHEMA MonMetrics TO monasca_persister;
GRANT USAGE ON SCHEMA MonAlarms TO monasca_persister;
GRANT ALL ON TABLE MonMetrics.Measurements TO monasca_persister;
GRANT ALL ON TABLE MonMetrics.Definitions TO monasca_persister;
GRANT ALL ON TABLE MonMetrics.Dimensions TO monasca_persister;
GRANT ALL ON TABLE MonMetrics.DefinitionDimensions TO monasca_persister;
GRANT ALL ON TABLE MonAlarms.StateHistory TO monasca_persister;

GRANT USAGE ON SCHEMA MonMetrics TO monasca_api;
GRANT USAGE ON SCHEMA MonAlarms TO monasca_api;
GRANT SELECT ON TABLE MonMetrics.Measurements TO monasca_api;
GRANT SELECT ON TABLE MonMetrics.Definitions TO monasca_api;
GRANT SELECT ON TABLE MonMetrics.Dimensions TO monasca_api;
GRANT SELECT ON TABLE MonMetrics.DefinitionDimensions TO monasca_api;
GRANT ALL ON TABLE MonAlarms.StateHistory TO monasca_api;
