CREATE TABLE db_properties (
  installation_date    NUMBER      NOT NULL,
  artifactory_version  VARCHAR(30) NOT NULL,
  artifactory_revision INT,
  artifactory_release  NUMBER,
  CONSTRAINT db_properties_pk PRIMARY KEY (installation_date)
);

CREATE TABLE artifactory_servers (
  server_id    VARCHAR(41) NOT NULL,
  start_time           NUMBER      NOT NULL,
  context_url   VARCHAR(255),
  membership_port             INT,
  server_state    VARCHAR(12) NOT NULL,
  server_role     VARCHAR(12) NOT NULL,
  last_heartbeat NUMBER      NOT NULL,
  artifactory_version  VARCHAR(30) NOT NULL,
  artifactory_revision INT,
  artifactory_release  NUMBER,
  artifactory_running_mode     VARCHAR(12) NOT NULL,
  license_hash VARCHAR(41) NOT NULL,
  CONSTRAINT artifactory_servers_pk PRIMARY KEY (server_id)
);