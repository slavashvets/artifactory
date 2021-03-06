CREATE TABLE unique_ids (
  index_type VARCHAR2(32)  NOT NULL,
  current_id NUMBER(19, 0) NOT NULL,
  CONSTRAINT unique_ids_pk PRIMARY KEY (index_type)
);

CREATE TABLE binaries (
  sha1       CHAR(40)      NOT NULL,
  md5        CHAR(32)      NOT NULL,
  bin_length NUMBER(19, 0) NOT NULL,
  CONSTRAINT binaries_pk PRIMARY KEY (sha1)
);
CREATE UNIQUE INDEX binaries_md5_idx ON binaries (md5);

CREATE TABLE binary_blobs (
  sha1 CHAR(40) NOT NULL,
  data BLOB,
  CONSTRAINT binary_blobs_pk PRIMARY KEY (sha1)
);

CREATE TABLE nodes (
  node_id       NUMBER(19, 0)  NOT NULL,
  node_type     NUMBER(5, 0)   NOT NULL,
  repo          VARCHAR2(64)   NOT NULL,
  node_path     VARCHAR2(1024) NOT NULL,
  node_name     VARCHAR2(255)  NOT NULL,
  depth         NUMBER(5, 0)   NOT NULL,
  created       NUMBER(19, 0)  NOT NULL,
  created_by    VARCHAR2(64),
  modified      NUMBER(19, 0)  NOT NULL,
  modified_by   VARCHAR2(64),
  updated       NUMBER(19, 0),
  bin_length    NUMBER(19, 0),
  sha1_actual   CHAR(40),
  sha1_original VARCHAR2(1024),
  md5_actual    CHAR(32),
  md5_original  VARCHAR2(1024),
  CONSTRAINT nodes_pk PRIMARY KEY (node_id),
  CONSTRAINT nodes_binaries_fk FOREIGN KEY (sha1_actual) REFERENCES binaries (sha1)
);
CREATE UNIQUE INDEX nodes_repo_path_name_idx ON nodes (repo, node_path, node_name);
CREATE INDEX nodes_node_path_idx ON nodes (node_path);
CREATE INDEX nodes_node_name_idx ON nodes (node_name);
CREATE INDEX nodes_sha1_actual_idx ON nodes (sha1_actual);
CREATE INDEX nodes_md5_actual_idx ON nodes (md5_actual);

CREATE TABLE node_props (
  prop_id    NUMBER(19, 0) NOT NULL,
  node_id    NUMBER(19, 0) NOT NULL,
  prop_key   VARCHAR2(255),
  prop_value VARCHAR2(2048),
  CONSTRAINT node_props_pk PRIMARY KEY (prop_id),
  CONSTRAINT node_props_nodes_fk FOREIGN KEY (node_id) REFERENCES nodes (node_id)
);
CREATE INDEX node_props_node_id_idx ON node_props (node_id);
CREATE INDEX node_props_prop_key_idx ON node_props (prop_key);
CREATE INDEX node_props_prop_value_idx ON node_props (prop_value);

CREATE TABLE node_meta_infos (
  node_id           NUMBER(19, 0) NOT NULL,
  props_modified    NUMBER(19, 0),
  props_modified_by VARCHAR2(64),
  CONSTRAINT node_meta_infos_pk PRIMARY KEY (node_id),
  CONSTRAINT node_meta_infos_nodes_fk FOREIGN KEY (node_id) REFERENCES nodes (node_id)
);

CREATE TABLE watches (
  watch_id NUMBER(19, 0) NOT NULL,
  node_id  NUMBER(19, 0) NOT NULL,
  username VARCHAR2(64)  NOT NULL,
  since    NUMBER(19, 0) NOT NULL,
  CONSTRAINT watches_pk PRIMARY KEY (watch_id),
  CONSTRAINT watches_nodes_fk FOREIGN KEY (node_id) REFERENCES nodes (node_id)
);
CREATE INDEX watches_node_id_idx ON watches (node_id);

CREATE TABLE stats (
  node_id            NUMBER(19, 0) NOT NULL,
  download_count     NUMBER(19, 0),
  last_downloaded    NUMBER(19, 0),
  last_downloaded_by VARCHAR2(64),
  CONSTRAINT stats_pk PRIMARY KEY (node_id),
  CONSTRAINT stats_nodes_fk FOREIGN KEY (node_id) REFERENCES nodes (node_id)
);

CREATE TABLE indexed_archives (
  archive_sha1        CHAR(40)      NOT NULL,
  indexed_archives_id NUMBER(19, 0) NOT NULL,
  CONSTRAINT indexed_archives_pk PRIMARY KEY (archive_sha1),
  CONSTRAINT indexed_archives_id_uq UNIQUE (indexed_archives_id),
  CONSTRAINT indexed_archives_binaries_fk FOREIGN KEY (archive_sha1) REFERENCES binaries (sha1)
);

CREATE TABLE archive_paths (
  path_id    NUMBER(19, 0) NOT NULL,
  entry_path VARCHAR2(1024),
  CONSTRAINT archive_paths_pk PRIMARY KEY (path_id)
);
CREATE UNIQUE INDEX archive_paths_path_idx ON archive_paths (entry_path);

CREATE TABLE archive_names (
  name_id    NUMBER(19, 0) NOT NULL,
  entry_name VARCHAR2(255),
  CONSTRAINT archive_names_pk PRIMARY KEY (name_id)
);
CREATE UNIQUE INDEX archive_names_name_idx ON archive_names (entry_name);

CREATE TABLE indexed_archives_entries (
  indexed_archives_id NUMBER(19, 0) NOT NULL,
  entry_path_id       NUMBER(19, 0) NOT NULL,
  entry_name_id       NUMBER(19, 0) NOT NULL,
  CONSTRAINT indexed_archives_entries_pk PRIMARY KEY (indexed_archives_id, entry_path_id, entry_name_id),
  CONSTRAINT indexed_archives_id_fk FOREIGN KEY (indexed_archives_id) REFERENCES indexed_archives (indexed_archives_id),
  CONSTRAINT entry_path_id_fk FOREIGN KEY (entry_path_id) REFERENCES archive_paths (path_id),
  CONSTRAINT entry_name_id_fk FOREIGN KEY (entry_name_id) REFERENCES archive_names (name_id)
);
CREATE INDEX indexed_entries_path_idx ON indexed_archives_entries (entry_path_id);
CREATE INDEX indexed_entries_name_idx ON indexed_archives_entries (entry_name_id);

CREATE TABLE tasks (
  task_type    VARCHAR2(32)   NOT NULL,
  task_context VARCHAR2(1024) NOT NULL
  -- CONSTRAINT pk_tasks PRIMARY KEY (task_type, task_context)
);
CREATE INDEX tasks_type_context_idx ON tasks (task_type, task_context);

CREATE TABLE configs (
  config_name VARCHAR2(255) NOT NULL,
  data        BLOB          NOT NULL,
  CONSTRAINT configs_pk PRIMARY KEY (config_name)
);

CREATE TABLE users (
  user_id           NUMBER(19, 0) NOT NULL,
  username          VARCHAR2(64)  NOT NULL,
  password          VARCHAR2(128),
  salt              VARCHAR2(128),
  email             VARCHAR2(128),
  gen_password_key  VARCHAR2(128),
  admin             NUMBER(5, 0),
  enabled           NUMBER(5, 0),
  updatable_profile NUMBER(5, 0),
  realm             VARCHAR2(255),
  private_key       VARCHAR2(512),
  public_key        VARCHAR2(255),
  last_login_time   NUMBER(19, 0),
  last_login_ip     VARCHAR2(42),
  last_access_time  NUMBER(19, 0),
  last_access_ip    VARCHAR2(42),
  bintray_auth      VARCHAR2(512),
  CONSTRAINT users_pk PRIMARY KEY (user_id)
);
CREATE UNIQUE INDEX users_username_idx ON users (username);

CREATE TABLE groups (
  group_id         NUMBER(19, 0) NOT NULL,
  group_name       VARCHAR2(64)  NOT NULL,
  description      VARCHAR2(1024),
  default_new_user NUMBER(5, 0),
  realm            VARCHAR2(255),
  realm_attributes VARCHAR2(512),
  CONSTRAINT groups_pk PRIMARY KEY (group_id)
);
CREATE UNIQUE INDEX groups_group_name_idx ON groups (group_name);

CREATE TABLE users_groups (
  user_id  NUMBER(19, 0) NOT NULL,
  group_id NUMBER(19, 0) NOT NULL,
  realm    VARCHAR2(255),
  CONSTRAINT users_groups_users_fk FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT users_groups_groups_fk FOREIGN KEY (group_id) REFERENCES groups (group_id)
);
CREATE UNIQUE INDEX users_groups_idx ON users_groups (user_id, group_id);

CREATE TABLE permission_targets (
  perm_target_id   NUMBER(19, 0) NOT NULL,
  perm_target_name VARCHAR2(64)  NOT NULL,
  includes         VARCHAR2(1024),
  excludes         VARCHAR2(1024),
  CONSTRAINT permission_targets_pk PRIMARY KEY (perm_target_id)
);
CREATE UNIQUE INDEX permission_targets_name_idx ON permission_targets (perm_target_name);

CREATE TABLE permission_target_repos (
  perm_target_id NUMBER(19, 0) NOT NULL,
  repo_key       VARCHAR2(64)  NOT NULL,
  CONSTRAINT permission_target_repos_fk FOREIGN KEY (perm_target_id) REFERENCES permission_targets (perm_target_id)
);

CREATE TABLE acls (
  acl_id         NUMBER(19, 0) NOT NULL,
  perm_target_id NUMBER(19, 0),
  modified       NUMBER(19, 0),
  modified_by    VARCHAR2(64),
  CONSTRAINT acls_pk PRIMARY KEY (acl_id),
  CONSTRAINT acls_permission_targets_fk FOREIGN KEY (perm_target_id) REFERENCES permission_targets (perm_target_id)
);
CREATE INDEX acls_perm_target_id_idx ON acls (perm_target_id);

CREATE TABLE aces (
  ace_id   NUMBER(19, 0) NOT NULL,
  acl_id   NUMBER(19, 0) NOT NULL,
  mask     NUMBER(5, 0)  NOT NULL,
  user_id  NUMBER(19, 0),
  group_id NUMBER(19, 0),
  CONSTRAINT aces_pk PRIMARY KEY (ace_id),
  CONSTRAINT aces_acls_fk FOREIGN KEY (acl_id) REFERENCES acls (acl_id),
  CONSTRAINT aces_users_fk FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT aces_groups_fk FOREIGN KEY (group_id) REFERENCES groups (group_id)
);

CREATE TABLE builds (
  build_id     NUMBER(19, 0) NOT NULL,
  build_name   VARCHAR2(255) NOT NULL,
  build_number VARCHAR2(255) NOT NULL,
  build_date   NUMBER(19, 0) NOT NULL,
  ci_url       VARCHAR2(1024),
  created      NUMBER(19, 0) NOT NULL,
  created_by   VARCHAR2(64),
  modified     NUMBER(19, 0),
  modified_by  VARCHAR2(64),
  CONSTRAINT builds_pk PRIMARY KEY (build_id)
);
CREATE UNIQUE INDEX builds_name_number_date_idx ON builds (build_name, build_number, build_date);

CREATE TABLE build_jsons (
  build_id        NUMBER(19, 0) NOT NULL,
  build_info_json BLOB,
  CONSTRAINT build_jsons_pk PRIMARY KEY (build_id),
  CONSTRAINT build_jsons_builds_fk FOREIGN KEY (build_id) REFERENCES builds (build_id)
);

CREATE TABLE build_promotions (
  build_id          NUMBER(19, 0) NOT NULL,
  created           NUMBER(19, 0) NOT NULL,
  created_by        VARCHAR2(64),
  status            VARCHAR2(64)  NOT NULL,
  repo              VARCHAR2(64),
  promotion_comment VARCHAR2(1024),
  ci_user           VARCHAR2(64),
  CONSTRAINT build_promotions_builds_fk FOREIGN KEY (build_id) REFERENCES builds (build_id)
);
CREATE UNIQUE INDEX build_promotions_created_idx ON build_promotions (build_id, created);
CREATE INDEX build_promotions_status_idx ON build_promotions (status);

CREATE TABLE build_props (
  prop_id    NUMBER(19, 0) NOT NULL,
  build_id   NUMBER(19, 0) NOT NULL,
  prop_key   VARCHAR2(255),
  prop_value VARCHAR2(2048),
  CONSTRAINT build_props_pk PRIMARY KEY (prop_id),
  CONSTRAINT build_props_builds_fk FOREIGN KEY (build_id) REFERENCES builds (build_id)
);
CREATE INDEX build_props_build_id_idx ON build_props (build_id);
CREATE INDEX build_props_prop_key_idx ON build_props (prop_key);
CREATE INDEX build_props_prop_value_idx ON build_props (prop_value);

CREATE TABLE build_modules (
  module_id      NUMBER(19, 0)  NOT NULL,
  build_id       NUMBER(19, 0)  NOT NULL,
  module_name_id VARCHAR2(1024) NOT NULL,
  CONSTRAINT build_modules_pk PRIMARY KEY (module_id),
  CONSTRAINT build_modules_builds_fk FOREIGN KEY (build_id) REFERENCES builds (build_id)
);
CREATE INDEX build_modules_build_id_idx ON build_modules (build_id);

CREATE TABLE build_artifacts (
  artifact_id   NUMBER(19, 0)  NOT NULL,
  module_id     NUMBER(19, 0)  NOT NULL,
  artifact_name VARCHAR2(1024) NOT NULL,
  artifact_type VARCHAR2(64),
  sha1          CHAR(40),
  md5           CHAR(32),
  CONSTRAINT build_artifacts_pk PRIMARY KEY (artifact_id),
  CONSTRAINT build_artifacts_modules_fk FOREIGN KEY (module_id) REFERENCES build_modules (module_id)
);
CREATE INDEX build_artifacts_module_id_idx ON build_artifacts (module_id);
CREATE INDEX build_artifacts_sha1_idx ON build_artifacts (sha1);
CREATE INDEX build_artifacts_md5_idx ON build_artifacts (md5);

CREATE TABLE build_dependencies (
  dependency_id      NUMBER(19, 0)  NOT NULL,
  module_id          NUMBER(19, 0)  NOT NULL,
  dependency_name_id VARCHAR2(1024) NOT NULL,
  dependency_scopes  VARCHAR2(1024),
  dependency_type    VARCHAR2(64),
  sha1               CHAR(40),
  md5                CHAR(32),
  CONSTRAINT build_dependencies_pk PRIMARY KEY (dependency_id),
  CONSTRAINT build_dependencies_modules_fk FOREIGN KEY (module_id) REFERENCES build_modules (module_id)
);
CREATE INDEX build_dependencies_module_idx ON build_dependencies (module_id);
CREATE INDEX build_dependencies_sha1_idx ON build_dependencies (sha1);
CREATE INDEX build_dependencies_md5_idx ON build_dependencies (md5);

CREATE TABLE module_props (
  prop_id    NUMBER(19, 0) NOT NULL,
  module_id  NUMBER(19, 0) NOT NULL,
  prop_key   VARCHAR2(255),
  prop_value VARCHAR2(2048),
  CONSTRAINT module_props_pk PRIMARY KEY (prop_id),
  CONSTRAINT module_props_modules_fk FOREIGN KEY (module_id) REFERENCES build_modules (module_id)
);
CREATE INDEX module_props_module_id_idx ON module_props (module_id);
CREATE INDEX module_props_prop_key_idx ON module_props (prop_key);
CREATE INDEX module_props_prop_value_idx ON module_props (prop_value);
